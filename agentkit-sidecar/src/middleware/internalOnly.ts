import { NextFunction, Request, Response } from "express";

export function internalOnly(req: Request, res: Response, next: NextFunction): void | Response {
  if (!process.env.SIDECAR_SECRET) {
    return res.status(500).json({ error: "Sidecar secret is not configured" });
  }
  const secret = req.headers["x-sidecar-secret"];
  if (secret !== process.env.SIDECAR_SECRET) {
    return res.status(403).json({ error: "Forbidden" });
  }
  next();
}
