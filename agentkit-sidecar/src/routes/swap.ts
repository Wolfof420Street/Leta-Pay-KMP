import { Router } from "express";
import { z } from "zod";
import { buildAgentKit } from "../agentkit";

const router = Router();

const SwapSchema = z.object({
  fromAddress: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  fromAsset: z.string(),
  toAsset: z.string(),
  amount: z.string(),
  networkId: z.enum(["ethereum-mainnet", "polygon-mainnet", "base-mainnet"]),
  slippageBps: z.number().int().min(10).max(1000).optional().default(50),
});

router.post("/quote", async (req, res, next) => {
  try {
    const body = SwapSchema.parse(req.body);
    const agentKit = await buildAgentKit(body.fromAddress);
    const action = agentKit.getActions().find((a) => a.name === "get_swap_price");
    if (!action) throw new Error("Swap quote action not available");

    const rawQuote = await action.invoke({
      fromToken: body.fromAsset,
      toToken: body.toAsset,
      fromAmount: body.amount,
      slippageBps: body.slippageBps,
    } as never);
    const quote = parseAgentKitJson(rawQuote);

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
    const agentKit = await buildAgentKit(body.fromAddress);
    const action = agentKit.getActions().find((a) => a.name === "get_swap_price");
    if (!action) throw new Error("Swap quote action not available");

    const rawQuote = await action.invoke({
      fromToken: body.fromAsset,
      toToken: body.toAsset,
      fromAmount: body.amount,
      slippageBps: body.slippageBps,
    } as never);
    const quote = parseAgentKitJson(rawQuote);
    if (quote.success === false) {
      throw new Error(String(quote.error ?? "Failed to fetch swap quote"));
    }

    // Build-only response. Route execution stays in client wallet + backend orchestration layers.
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
        quote,
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
  } catch {
    throw new Error(`Unexpected AgentKit response: ${raw}`);
  }
}

function toChainId(networkId: "ethereum-mainnet" | "polygon-mainnet" | "base-mainnet"): number {
  return networkId === "ethereum-mainnet" ? 1 : networkId === "polygon-mainnet" ? 137 : 8453;
}

export default router;
