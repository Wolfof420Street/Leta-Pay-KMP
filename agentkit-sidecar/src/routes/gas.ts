import { Router } from "express";
import { z } from "zod";

const router = Router();

const GasSchema = z.object({
  fromAddress: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  calldata: z
    .object({
      to: z.string().optional(),
      data: z.string().optional(),
    })
    .optional(),
  networkId: z.enum(["ethereum-mainnet", "polygon-mainnet", "base-mainnet"]).optional(),
});

router.post("/estimate", async (req, res, next) => {
  try {
    const body = GasSchema.parse(req.body);
    const rpcUrl = rpcByNetwork(body.networkId ?? "base-mainnet");
    const estimateGasHex = await rpc(rpcUrl, "eth_estimateGas", [
      {
        from: body.fromAddress,
        to: body.calldata?.to,
        data: body.calldata?.data ?? "0x",
      },
    ]);
    const gasPriceHex = await rpc(rpcUrl, "eth_gasPrice", []);
    const estimatedGasUnits = Number.parseInt(estimateGasHex, 16);
    const gasPriceWei = BigInt(gasPriceHex);

    res.json({
      success: true,
      estimate: {
        estimatedGasUnits,
        gasPriceWei: gasPriceWei.toString(),
        estimatedFeeWei: (BigInt(estimatedGasUnits) * gasPriceWei).toString(),
        networkName: body.networkId ?? "base-mainnet",
      },
    });
  } catch (err) {
    next(err);
  }
});

async function rpc(rpcUrl: string, method: string, params: unknown[]): Promise<string> {
  const response = await fetch(rpcUrl, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      jsonrpc: "2.0",
      id: 1,
      method,
      params,
    }),
  });
  const payload = (await response.json()) as { result?: string; error?: { message?: string } };
  if (!response.ok || payload.error || !payload.result) {
    throw new Error(payload.error?.message ?? `RPC ${method} failed`);
  }
  return payload.result;
}

function rpcByNetwork(networkId: "ethereum-mainnet" | "polygon-mainnet" | "base-mainnet"): string {
  if (networkId === "ethereum-mainnet") return process.env.ETHEREUM_RPC_URL ?? "https://ethereum.publicnode.com";
  if (networkId === "polygon-mainnet") return process.env.POLYGON_RPC_URL ?? "https://polygon-rpc.com";
  return process.env.BASE_RPC_URL ?? "https://mainnet.base.org";
}

export default router;
