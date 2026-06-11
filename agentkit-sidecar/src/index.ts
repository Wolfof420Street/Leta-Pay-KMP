import "dotenv/config";
import express from "express";
import { buildAgentKit } from "./agentkit";
import { errorHandler } from "./middleware/errorHandler";
import { internalOnly } from "./middleware/internalOnly";
import { logger } from "./logger";
import balanceRouter from "./routes/balance";
import gasRouter from "./routes/gas";
import screenRouter from "./routes/screen";
import stakeRouter from "./routes/stake";
import swapRouter from "./routes/swap";
import transferRouter from "./routes/transfer";
import { readMaxTransactionValue } from "./transactionGuard";

if (!process.env.SIDECAR_INTERNAL_TOKEN && !process.env.SIDECAR_SECRET) {
  throw new Error("SIDECAR_INTERNAL_TOKEN or SIDECAR_SECRET env var is required");
}

const app = express();
let sidecarHealthy = true;

app.use(express.json());
app.use(internalOnly);

app.use("/agentkit/transfer", transferRouter);
app.use("/agentkit/swap", swapRouter);
app.use("/agentkit/stake", stakeRouter);
app.use("/agentkit/gas", gasRouter);
app.use("/agentkit/balance", balanceRouter);
app.use("/agentkit/address", screenRouter);

app.get("/health", async (_req, res) => {
  const checks = {
    internalAuth: Boolean(process.env.SIDECAR_INTERNAL_TOKEN || process.env.SIDECAR_SECRET),
    maxTransactionValueEth: false,
    agentKit: false,
    process: sidecarHealthy,
  };

  try {
    readMaxTransactionValue();
    checks.maxTransactionValueEth = true;
    await buildAgentKit("0x0000000000000000000000000000000000000001");
    checks.agentKit = true;
  } catch {
    checks.agentKit = false;
  }

  const ok = checks.internalAuth && checks.maxTransactionValueEth && checks.agentKit && checks.process;
  res.status(ok ? 200 : 503).json({
    status: ok ? "ok" : "degraded",
    checks,
  });
});

app.use(errorHandler);

const PORT = Number(process.env.PORT ?? 3100);
process.on("unhandledRejection", (reason: unknown) => {
  sidecarHealthy = false;
  const message = reason instanceof Error ? reason.message : String(reason);
  logger.error({ reason: message }, "Unhandled rejection");
});
process.on("uncaughtException", (error) => {
  sidecarHealthy = false;
  logger.error({ name: error.name, message: error.message }, "Uncaught exception");
});
app.listen(PORT, () => {
  logger.info({ port: PORT }, "AgentKit sidecar listening");
});
