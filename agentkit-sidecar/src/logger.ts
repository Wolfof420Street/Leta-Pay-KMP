type LogContext = Record<string, unknown>;

function write(level: "info" | "error", context: LogContext, message: string): void {
  const line = JSON.stringify({
    timestamp: new Date().toISOString(),
    level,
    message,
    ...context,
  });

  if (level === "error") {
    process.stderr.write(`${line}\n`);
    return;
  }

  process.stdout.write(`${line}\n`);
}

export const logger = {
  info(context: LogContext, message: string): void {
    write("info", context, message);
  },
  error(context: LogContext, message: string): void {
    write("error", context, message);
  },
};
