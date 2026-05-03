import { NextFunction, Request, Response } from "express";
import { ZodError } from "zod";

export function errorHandler(err: unknown, _req: Request, res: Response, _next: NextFunction) {
  if (err instanceof ZodError) {
    return res.status(400).json({
      success: false,
      error: "VALIDATION_ERROR",
      message: "Request payload validation failed",
      details: err.issues,
    });
  }

  const message = err instanceof Error ? err.message : "Unknown sidecar error";
  const normalized = message.toLowerCase();

  if (normalized.includes("insufficient") && normalized.includes("balance")) {
    return res.status(422).json({ success: false, error: "INSUFFICIENT_FUNDS", message });
  }
  if (normalized.includes("slippage")) {
    return res.status(422).json({ success: false, error: "SLIPPAGE_EXCEEDED", message });
  }
  if (normalized.includes("not available") || normalized.includes("not found")) {
    return res.status(404).json({ success: false, error: "ROUTE_NOT_FOUND", message });
  }
  if (normalized.includes("forbidden")) {
    return res.status(403).json({ success: false, error: "FORBIDDEN", message });
  }

  return res.status(500).json({
    success: false,
    error: "SIDECAR_INTERNAL_ERROR",
    message,
  });
}
