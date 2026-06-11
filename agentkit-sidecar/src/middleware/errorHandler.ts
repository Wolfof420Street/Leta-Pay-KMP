import { NextFunction, Request, Response } from "express";
import { ZodError } from "zod";
import { logger } from "../logger";

export function errorHandler(err: unknown, req: Request, res: Response, _next: NextFunction): Response {
  const requestId = normalizeRequestId(req.headers["x-request-id"]);

  if (err instanceof ZodError) {
    return res.status(400).json({
      error: "validation_error",
      requestId,
      issues: err.issues,
    });
  }

  // Log full error details internally
  logger.error({ requestId, error: err instanceof Error ? err.message : String(err) }, "Sidecar error");

  return res.status(500).json({
    error: "internal_error",
    requestId,
  });
}

function normalizeRequestId(value: string | string[] | undefined): string {
  if (Array.isArray(value)) {
    return value[0] ?? "";
  }

  return value ?? "";
}
