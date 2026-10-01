import assert from "node:assert/strict";
import { test } from "node:test";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

test("server exposes read-only milestone and milestone-issue tools", async () => {
  const transport = new StdioClientTransport({ command: process.execPath, args: ["server.js"] });
  const client = new Client({ name: "milestone-tools-test", version: "1.0.0" });

  try {
    await client.connect(transport);
    const tools = (await client.listTools()).tools;
    const byName = new Map(tools.map((tool) => [tool.name, tool]));

    assert.deepEqual([...byName.keys()].sort(), ["list_issues_by_milestone", "list_milestones"]);
    assert.equal(byName.get("list_milestones").annotations.readOnlyHint, true);
    assert.equal(byName.get("list_issues_by_milestone").annotations.readOnlyHint, true);
    assert.ok(byName.get("list_issues_by_milestone").inputSchema.properties.milestone);
  } finally {
    await client.close();
  }
});