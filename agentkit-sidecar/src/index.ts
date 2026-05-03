import "dotenv/config";
import express from "express";
import { errorHandler } from "./middleware/errorHandler";
import { internalOnly } from "./middleware/internalOnly";
import balanceRouter from "./routes/balance";
import gasRouter from "./routes/gas";
import screenRouter from "./routes/screen";
import stakeRouter from "./routes/stake";
import swapRouter from "./routes/swap";
import transferRouter from "./routes/transfer";

const app = express();
app.use(express.json());
app.use(internalOnly);

app.use("/agentkit/transfer", transferRouter);
app.use("/agentkit/swap", swapRouter);
app.use("/agentkit/stake", stakeRouter);
app.use("/agentkit/gas", gasRouter);
app.use("/agentkit/balance", balanceRouter);
app.use("/agentkit/address", screenRouter);

app.get("/health", (_req, res) => res.json({ ok: true }));
app.use(errorHandler);

const PORT = Number(process.env.PORT ?? 3100);
app.listen(PORT, () => {
  // eslint-disable-next-line no-console
  console.log(`AgentKit sidecar listening on :${PORT}`);
});
