import { NextFunction, Request, Response } from "express";
import crypto from "crypto";

export function internalOnly(req: Request, res: Response, next: NextFunction): void | Response {
  const configuredSecret = process.env.SIDECAR_INTERNAL_TOKEN || process.env.SIDECAR_SECRET || "";
  if (!configuredSecret) {
    return res.status(500).json({ error: "internal_error" });
  }

  const secret = readSecretHeader(req.headers["x-internal-token"]) ??
    readSecretHeader(req.headers["x-sidecar-secret"]);
  if (!secret) {
    return res.status(403).json({ error: "Forbidden" });
  }

  const a = Buffer.from(secret, "utf8");
  const b = Buffer.from(configuredSecret, "utf8");
  if (a.length !== b.length) {
    return res.status(403).json({ error: "Forbidden" });
  }

  if (!crypto.timingSafeEqual(a, b)) {
    return res.status(403).json({ error: "Forbidden" });
  }

  next();
}

function readSecretHeader(value: string | string[] | undefined): string | null {
  if (Array.isArray(value)) {
    return null;
  }
  return value?.trim() ?? null;
}
