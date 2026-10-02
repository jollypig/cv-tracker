import assert from "node:assert/strict";
import { test } from "node:test";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";
import { getIssuesByMilestone, getOpenMilestones } from "./server.js";

const token = "test-token";

function jsonResponse(body) {
  return new Response(JSON.stringify(body), { status: 200 });
}

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

test("open milestones are paginated and sorted by milestone number", async () => {
  const pages = [];
  const milestones = Array.from({ length: 100 }, (_, index) => ({ number: 100 - index, title: `Sprint ${index}` }));
  const result = await getOpenMilestones({
    owner: "example",
    repo: "project",
    token,
    fetchImpl: async (url, options) => {
      assert.equal(options.headers.Authorization, `Bearer ${token}`);
      pages.push(Number(new URL(url).searchParams.get("page")));
      return jsonResponse(pages.length === 1 ? milestones : [{ number: 101, title: "Sprint 101" }]);
    },
  });

  assert.deepEqual(pages, [1, 2]);
  assert.equal(result[0].number, 1);
  assert.equal(result.at(-1).number, 101);
});

test("milestone issues exclude pull requests and sort numbered tasks numerically", async () => {
  const result = await getIssuesByMilestone({
    owner: "example",
    repo: "project",
    milestone: 4,
    token,
    fetchImpl: async (url) => {
      const requestUrl = new URL(url);
      assert.equal(requestUrl.searchParams.get("milestone"), "4");
      assert.equal(requestUrl.searchParams.get("state"), "all");
      return jsonResponse([
        { number: 10, title: "Task 10: Ten", body: "body", labels: [{ name: "wf:ready" }], state: "open" },
        { number: 12, title: "Task 2: Two", body: "body", labels: [], state: "open" },
        { number: 11, title: "Task 1: One", body: "body", labels: [], state: "open" },
        { number: 13, title: "PR", pull_request: { url: "https://api.github.com/pulls/13" }, labels: [], state: "open" },
      ]);
    },
  });

  assert.deepEqual(result.map((issue) => issue.number), [11, 12, 10]);
  assert.deepEqual(result[2].labels, ["wf:ready"]);
});