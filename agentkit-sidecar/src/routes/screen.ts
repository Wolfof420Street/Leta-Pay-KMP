import { Router } from "express";
import { z } from "zod";

const router = Router();

const ScreenSchema = z.object({
  address: z.string().regex(/^0x[a-fA-F0-9]{40}$/),
});

router.post("/screen", async (req, res, next) => {
  try {
    const body = ScreenSchema.parse(req.body);
    res.json({
      success: true,
      address: body.address,
      accepted: true,
      reasons: [],
    });
  } catch (err) {
    next(err);
  }
});

export default router;
