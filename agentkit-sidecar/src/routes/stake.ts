import { Router } from "express";
import { z } from "zod";

const router = Router();

const StakeSchema = z.object({
  fromAddress: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  protocol: z.enum(["LIDO", "AAVE"]),
  amount: z.string(),
  networkId: z.enum(["ethereum-mainnet", "polygon-mainnet", "base-mainnet"]),
});

router.post("/build", async (req, res, next) => {
  try {
    const body = StakeSchema.parse(req.body);
    const chainId = body.networkId === "polygon-mainnet" ? 137 : body.networkId === "base-mainnet" ? 8453 : 1;
    const amountWei = toWei(body.amount, 18);
    const opportunityId = body.protocol === "LIDO" ? "LIDO_STETH_STAKE" : "AAVE_V3_SUPPLY_WETH";
    const to = body.protocol === "LIDO" ? "0xae7ab96520DE3A18E5e111B5EaAb095312D7fE84" : "0x87870Bca3F3fD6335C3f4ce8392D69350B4fA4E2";
    const data = body.protocol === "LIDO" ? "0xa1903eab" : `0x617ba037${padAddress("0xC02aaA39b223FE8D0A0E5C4F27eAD9083C756Cc2")}${padUint(amountWei)}${padAddress(body.fromAddress)}${padUint(0n)}`;
    const value = body.protocol === "LIDO" ? `0x${amountWei.toString(16)}` : "0x0";

    res.json({
      success: true,
      calldata: {
        from: body.fromAddress,
        to,
        data,
        value,
        chainId,
        metadata: {
          protocol: body.protocol,
          opportunityId,
          amount: body.amount,
          mode: "deterministic-build",
        },
      },
    });
  } catch (err) {
    next(err);
  }
});

function toWei(amount: string, decimals: number): bigint {
  const [whole, frac = ""] = amount.split(".");
  if (!/^\d+$/.test(whole) || !/^\d*$/.test(frac)) {
    throw new Error("Invalid amount format");
  }
  const normalizedFrac = (frac + "0".repeat(decimals)).slice(0, decimals);
  return BigInt(whole) * (10n ** BigInt(decimals)) + BigInt(normalizedFrac || "0");
}

function padAddress(address: string): string {
  return address.toLowerCase().replace(/^0x/, "").padStart(64, "0");
}

function padUint(value: bigint): string {
  return value.toString(16).padStart(64, "0");
}

export default router;
