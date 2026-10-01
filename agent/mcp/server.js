import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const apiUrl = "https://api.github.com";
const server = new McpServer({
  name: "github-milestones",
  version: "1.0.0",
});

async function githubGet(path, params) {
  const token = process.env.GITHUB_TOKEN ?? process.env.GITHUB_PERSONAL_ACCESS_TOKEN;
  if (!token) {
    throw new Error("Set GITHUB_TOKEN in the workspace .env file to use GitHub milestone tools.");
  }

  const url = new URL(path, apiUrl);
  for (const [key, value] of Object.entries(params)) {
    url.searchParams.set(key, String(value));
  }

  const response = await fetch(url, {
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${token}`,
      "X-GitHub-Api-Version": "2022-11-28",
      "User-Agent": "cv-tracker-github-milestones-mcp",
    },
  });

  const result = await response.json();
  if (!response.ok) {
    throw new Error(`GitHub API returned ${response.status}: ${result.message ?? "Request failed"}`);
  }
  return result;
}

function resultContent(value) {
  return { content: [{ type: "text", text: JSON.stringify(value, null, 2) }] };
}

const commonInput = {
  owner: z.string().min(1).describe("GitHub repository owner"),
  repo: z.string().min(1).describe("GitHub repository name"),
  state: z.enum(["open", "closed", "all"]).default("open"),
  page: z.number().int().min(1).default(1),
  per_page: z.number().int().min(1).max(100).default(100),
};

server.registerTool(
  "list_milestones",
  {
    description: "List milestones in a GitHub repository. Results are paginated; state defaults to open.",
    inputSchema: commonInput,
    annotations: { readOnlyHint: true, openWorldHint: true },
  },
  async ({ owner, repo, state, page, per_page }) => {
    try {
      const milestones = await githubGet(
        `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/milestones`,
        { state, page, per_page },
      );
      return resultContent(milestones);
    } catch (error) {
      return { ...resultContent({ error: error.message }), isError: true };
    }
  },
);

server.registerTool(
  "list_issues_by_milestone",
  {
    description: "List issues in a GitHub repository milestone. Pass the milestone number returned by list_milestones. Pull requests are excluded.",
    inputSchema: {
      ...commonInput,
      milestone: z.number().int().min(0).describe("Milestone number; use 0 for issues without a milestone"),
    },
    annotations: { readOnlyHint: true, openWorldHint: true },
  },
  async ({ owner, repo, milestone, state, page, per_page }) => {
    try {
      const issues = await githubGet(
        `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}/issues`,
        { milestone, state, page, per_page },
      );
      return resultContent(issues.filter((issue) => !issue.pull_request));
    } catch (error) {
      return { ...resultContent({ error: error.message }), isError: true };
    }
  },
);

await server.connect(new StdioServerTransport());