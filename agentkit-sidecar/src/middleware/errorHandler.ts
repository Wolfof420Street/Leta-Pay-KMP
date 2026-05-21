import { NextFunction, Request, Response } from "express";
import { ZodError } from "zod";

export function errorHandler(err: unknown, req: Request, res: Response, _next: NextFunction): Response {
  const requestId = req.headers["x-request-id"] || "";

  if (err instanceof ZodError) {
    return res.status(400).json({
      error: "validation_error",
      requestId,
    });
  }

  // Log full error details internally
  console.error(`[${requestId}] Sidecar error:`, err);

  return res.status(500).json({
    error: "internal_error",
    requestId,
  });
}
