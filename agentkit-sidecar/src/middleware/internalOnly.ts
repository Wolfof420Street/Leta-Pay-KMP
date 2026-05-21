import { NextFunction, Request, Response } from "express";
import crypto from "crypto";

export function internalOnly(req: Request, res: Response, next: NextFunction): void | Response {
  const configuredSecret = process.env.SIDECAR_INTERNAL_TOKEN || process.env.SIDECAR_SECRET || "";
  if (!configuredSecret) {
    return res.status(500).json({ error: "internal_error" });
  }

  const secret = (req.headers["x-internal-token"] || req.headers["x-sidecar-secret"] || "") as string;

  if (secret.length !== configuredSecret.length) {
    return res.status(403).json({ error: "Forbidden" });
  }

  if (!crypto.timingSafeEqual(Buffer.from(secret), Buffer.from(configuredSecret))) {
    return res.status(403).json({ error: "Forbidden" });
  }

  next();
}
