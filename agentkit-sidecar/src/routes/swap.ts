import { Router } from "express";
import { z } from "zod";
import { buildAgentKit } from "../agentkit";
import { readMaxTransactionValue } from "../transactionGuard";

const router = Router();

const SwapSchema = z.object({
  fromAddress: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  fromAsset: z.enum(["ETH", "USDC", "USDT", "DAI", "WETH", "WBTC", "MATIC"]),
  toAsset: z.enum(["ETH", "USDC", "USDT", "DAI", "WETH", "WBTC", "MATIC"]),
  amount: z.string().regex(/^\d+(\.\d+)?$/),
  networkId: z.enum(["ethereum-mainnet", "polygon-mainnet", "base-mainnet"]),
  slippageBps: z.number().int().min(10).max(1000).optional().default(50),
});

const SwapQuoteResponseSchema = z.object({
  success: z.boolean().optional(),
  error: z.string().optional(),
  quoteId: z.string().optional(),
  fromAmount: z.string().optional(),
  toAmount: z.string().optional(),
  rate: z.string().optional(),
  priceImpactBps: z.number().int().optional(),
  estimatedFeeUsd: z.string().optional(),
  expiresAt: z.number().int().optional(),
  calldata: z.string().optional(),
});

router.post("/quote", async (req, res, next) => {
  try {
    const body = SwapSchema.parse(req.body);
    const amount = Number(body.amount);
    if (isNaN(amount)) {
      return res.status(400).json({ error: "Invalid amount" });
    }
    const maxTxValue = readMaxTransactionValue();
    if (amount > maxTxValue) {
      return res.status(400).json({ error: "Exceeds max transaction value" });
    }
    const agentKit = await buildAgentKit(body.fromAddress);
    const action = agentKit.getActions().find((a) => a.name === "get_swap_price");
    if (!action) throw new Error("Swap quote action not available");

    const rawQuote = await action.invoke({
      fromToken: body.fromAsset,
      toToken: body.toAsset,
      fromAmount: body.amount,
      slippageBps: body.slippageBps,
    } as Record<string, unknown>);
    const quote = SwapQuoteResponseSchema.parse(parseAgentKitJson(rawQuote));

    if (quote.success === false) {
      throw new Error(String(quote.error ?? "Failed to fetch swap quote"));
    }

    res.json({
      success: true,
      quote: {
        ...quote,
        slippageBps: body.slippageBps,
      },
    });
  } catch (err) {
    next(err);
  }
});

router.post("/build", async (req, res, next) => {
  try {
    const body = SwapSchema.parse(req.body);
    const amount = Number(body.amount);
    if (isNaN(amount)) {
      return res.status(400).json({ error: "Invalid amount" });
    }
    const maxTxValue = readMaxTransactionValue();
    if (amount > maxTxValue) {
      return res.status(400).json({ error: "Exceeds max transaction value" });
    }
    const calldata = {
      from: body.fromAddress,
      to: null,
      data: "0x",
      value: "0x0",
      chainId: toChainId(body.networkId),
      route: {
        provider: "cdp_swap",
        fromAsset: body.fromAsset,
        toAsset: body.toAsset,
        amount: body.amount,
        slippageBps: body.slippageBps,
        mode: "deterministic-build",
      },
    };

    res.json({ success: true, calldata });
  } catch (err) {
    next(err);
  }
});

function parseAgentKitJson(raw: string): Record<string, unknown> {
  try {
    return JSON.parse(raw);
  } catch (err) {
    throw new Error(`Malformed AgentKit JSON payload (${raw.length} bytes)`);
  }
}

function toChainId(networkId: "ethereum-mainnet" | "polygon-mainnet" | "base-mainnet"): number {
  return networkId === "ethereum-mainnet" ? 1 : networkId === "polygon-mainnet" ? 137 : 8453;
}

export default router;
