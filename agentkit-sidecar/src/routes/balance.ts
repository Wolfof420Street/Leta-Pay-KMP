import { Router } from "express";
import { z } from "zod";

const router = Router();

const ParamsSchema = z.object({
  address: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
  network: z.enum(["ethereum-mainnet", "polygon-mainnet", "base-mainnet"]),
});

router.get("/:address/:network", async (req, res, next) => {
  try {
    const params = ParamsSchema.parse(req.params);
    res.json({
      success: true,
      address: params.address,
      networkId: params.network,
      balances: [
        {
          asset: "ETH",
          amount: "0.0",
          usdValue: "0.0",
        },
      ],
    });
  } catch (err) {
    next(err);
  }
});

export default router;
