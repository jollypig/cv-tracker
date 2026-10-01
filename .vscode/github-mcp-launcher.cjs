const { spawn } = require("node:child_process");

const token = process.env.GITHUB_TOKEN;
if (!token) {
  process.stderr.write("GITHUB_TOKEN is required in the workspace .env file.\n");
  process.exit(1);
}

const server = spawn(
  "docker",
  [
    "run",
    "--rm",
    "-i",
    "-e",
    "GITHUB_PERSONAL_ACCESS_TOKEN",
    "ghcr.io/github/github-mcp-server",
  ],
  {
    env: { ...process.env, GITHUB_PERSONAL_ACCESS_TOKEN: token },
    stdio: "inherit",
  },
);

server.on("error", (error) => {
  process.stderr.write(`Failed to start GitHub MCP server: ${error.message}\n`);
  process.exit(1);
});

server.on("exit", (code, signal) => {
  if (signal) {
    process.kill(process.pid, signal);
  } else {
    process.exit(code ?? 1);
  }
});
