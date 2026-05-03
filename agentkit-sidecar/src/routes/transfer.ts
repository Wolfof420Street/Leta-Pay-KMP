import { Router } from "express";
import { z } from "zod";
import { buildAgentKit } from "../agentkit";

const router = Router();

const TransferBuildSchema = z.object({
  fromAddress: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  toAddress: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  asset: z.enum(["ETH", "USDC", "USDT", "DAI", "MATIC"]),
  amount: z.string(),
  networkId: z.enum(["ethereum-mainnet", "polygon-mainnet", "base-mainnet"]),
});

router.post("/build", async (req, res, next) => {
  try {
    const body = TransferBuildSchema.parse(req.body);
    const agentKit = await buildAgentKit(body.fromAddress);
    const transferAction = agentKit.getActions().find((a) => a.name === "transfer");
    if (!transferAction) throw new Error("Transfer action not available");

    // Invoke AgentKit for validation and provider consistency; fail closed if it returns a broadcast result.
    const result = await transferAction.invoke({
      amount: body.amount,
      tokenAddress: tokenAddress(body.asset, body.networkId),
      destinationAddress: body.toAddress,
      gasless: false,
    } as never);

    if (typeof result === "string" && result.toLowerCase().includes("transaction hash")) {
      throw new Error("Transfer action attempted execution; sidecar only supports unsigned transaction builds");
    }

    const amountWei = toWeiAmount(body.amount, body.asset);
    const calldata = {
      from: body.fromAddress,
      to: tokenAddress(body.asset, body.networkId),
      data: `0xa9059cbb${padAddress(body.toAddress)}${padUint(amountWei)}`,
      value: "0x0",
      chainId: toChainId(body.networkId),
      metadata: { agentkit: result },
    };

    res.json({ success: true, calldata });
  } catch (err) {
    next(err);
  }
});

function toWeiAmount(amount: string, tokenAddressOrSymbol: string): bigint {
  const decimals = tokenAddressOrSymbol === "ETH" ? 18 : 6;
  const [whole, frac = ""] = amount.split(".");
  if (!/^\d+$/.test(whole) || !/^\d*$/.test(frac)) {
    throw new Error("Invalid amount format");
  }
  const normalizedFrac = (frac + "0".repeat(decimals)).slice(0, decimals);
  return BigInt(whole) * (10n ** BigInt(decimals)) + BigInt(normalizedFrac || "0");
}

function toChainId(networkId: "ethereum-mainnet" | "polygon-mainnet" | "base-mainnet"): number {
  return networkId === "ethereum-mainnet" ? 1 : networkId === "polygon-mainnet" ? 137 : 8453;
}

function tokenAddress(
  asset: "ETH" | "USDC" | "USDT" | "DAI" | "MATIC",
  networkId: "ethereum-mainnet" | "polygon-mainnet" | "base-mainnet",
): string {
  if (asset === "USDC") {
    if (networkId === "base-mainnet") return "0x833589fCD6eDb6E08f4c7C32D4f71b54bdA02913";
    if (networkId === "polygon-mainnet") return "0x3c499c542cef5e3811e1192ce70d8cc03d5c3359";
    return "0xA0b86991c6218b36c1d19d4a2e9eb0ce3606eb48";
  }
  if (asset === "USDT") {
    if (networkId === "base-mainnet") return "0xfde4C96c8593536E31F229EA8f37b2ADa2699bb2";
    if (networkId === "polygon-mainnet") return "0xc2132D05D31c914a87C6611C10748AEb04B58e8F";
    return "0xdAC17F958D2ee523a2206206994597C13D831ec7";
  }
  if (asset === "DAI") {
    if (networkId === "base-mainnet") return "0x50c5725949A6F0c72E6C4a641F24049A917DB0Cb";
    if (networkId === "polygon-mainnet") return "0x8f3Cf7ad23Cd3CaDbD9735AFf958023239c6A063";
    return "0x6B175474E89094C44Da98b954EedeAC495271d0F";
  }
  if (asset === "MATIC") {
    if (networkId === "polygon-mainnet") return "0x0000000000000000000000000000000000001010";
    throw new Error("MATIC is only available on polygon-mainnet");
  }
  throw new Error("Native ETH transfer is not supported in this build endpoint");
}

function padAddress(address: string): string {
  return address.toLowerCase().replace(/^0x/, "").padStart(64, "0");
}

function padUint(value: bigint): string {
  return value.toString(16).padStart(64, "0");
}

export default router;
