import { resolve } from "node:path";
import { pathToFileURL } from "node:url";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

const githubApiUrl = "https://api.github.com";
const githubApiVersion = "2022-11-28";

function repositoryPath(owner, repo, suffix) {
  return `/repos/${encodeURIComponent(owner)}/${encodeURIComponent(repo)}${suffix}`;
}

async function getAllPages(path, token, fetchImpl) {
  const items = [];

  for (let page = 1; ; page += 1) {
    const url = new URL(`${githubApiUrl}${path}`);
    url.searchParams.set("per_page", "100");
    url.searchParams.set("page", String(page));

    const response = await fetchImpl(url, {
      headers: {
        Accept: "application/vnd.github+json",
        Authorization: `Bearer ${token}`,
        "X-GitHub-Api-Version": githubApiVersion,
        "User-Agent": "cv-tracker-github-milestones-mcp",
      },
    });

    if (!response.ok) {
      const detail = await response.text();
      throw new Error(`GitHub API request failed (${response.status}): ${detail || response.statusText}`);
    }

    const pageItems = await response.json();
    if (!Array.isArray(pageItems)) {
      throw new Error("GitHub API returned an unexpected response.");
    }

    items.push(...pageItems);
    if (pageItems.length < 100) {
      return items;
    }
  }
}

function taskNumber(title) {
  const match = title.match(/^\s*Task\s+(\d+)\s*:/i);
  return match ? Number(match[1]) : null;
}

function compareIssues(left, right) {
  const leftTask = taskNumber(left.title);
  const rightTask = taskNumber(right.title);

  if (leftTask !== null && rightTask !== null && leftTask !== rightTask) {
    return leftTask - rightTask;
  }
  if (leftTask === null && rightTask !== null) {
    return 1;
  }
  if (leftTask !== null && rightTask === null) {
    return -1;
  }
  return left.number - right.number;
}

export async function getOpenMilestones({ owner, repo, token, fetchImpl = fetch }) {
  const path = repositoryPath(owner, repo, "/milestones?state=open&sort=number&direction=asc");
  const milestones = await getAllPages(path, token, fetchImpl);

  return milestones
    .sort((left, right) => left.number - right.number)
    .map(({ number, title, state, open_issues, closed_issues, description, due_on, html_url }) => ({
      number,
      title,
      state,
      open_issues,
      closed_issues,
      description,
      due_on,
      html_url,
    }));
}

export async function getIssuesByMilestone({ owner, repo, milestone, token, fetchImpl = fetch }) {
  const query = new URLSearchParams({
    milestone: String(milestone),
    state: "all",
    sort: "created",
    direction: "asc",
  });
  const path = `${repositoryPath(owner, repo, "/issues")}?${query}`;
  const issues = await getAllPages(path, token, fetchImpl);

  return issues
    .filter((issue) => !issue.pull_request)
    .map(({ number, title, body, html_url, labels, state, milestone: issueMilestone }) => ({
      number,
      title,
      body,
      html_url,
      labels: labels.map(({ name }) => name),
      state,
      milestone: issueMilestone?.number ?? null,
    }))
    .sort(compareIssues);
}

function tokenFromEnvironment(environment) {
  const token = environment.GITHUB_PERSONAL_ACCESS_TOKEN || environment.GITHUB_TOKEN;
  if (!token) {
    throw new Error("Set GITHUB_PERSONAL_ACCESS_TOKEN or GITHUB_TOKEN in the MCP server environment.");
  }
  return token;
}

function toolResult(operation) {
  return async (args) => {
    try {
      const value = await operation(args);
      return { content: [{ type: "text", text: JSON.stringify(value) }] };
    } catch (error) {
      return {
        content: [{ type: "text", text: error instanceof Error ? error.message : String(error) }],
        isError: true,
      };
    }
  };
}

export function createGithubMilestonesServer({ fetchImpl = fetch, environment = process.env } = {}) {
  const server = new McpServer({ name: "github-milestones", version: "1.0.0" });
  const annotations = { readOnlyHint: true, destructiveHint: false, idempotentHint: true, openWorldHint: true };
  const repositoryInput = {
    owner: z.string().min(1).describe("GitHub repository owner"),
    repo: z.string().min(1).describe("GitHub repository name"),
  };

  server.registerTool(
    "list_milestones",
    {
      description: "List all open milestones in a repository, sorted by milestone number ascending.",
      inputSchema: z.object(repositoryInput),
      annotations,
    },
    toolResult(({ owner, repo }) =>
      getOpenMilestones({ owner, repo, token: tokenFromEnvironment(environment), fetchImpl }),
    ),
  );

  server.registerTool(
    "list_issues_by_milestone",
    {
      description: "List non-pull-request issues in a milestone, sorted by Task number and then issue number.",
      inputSchema: z.object({ ...repositoryInput, milestone: z.number().int().positive() }),
      annotations,
    },
    toolResult(({ owner, repo, milestone }) =>
      getIssuesByMilestone({ owner, repo, milestone, token: tokenFromEnvironment(environment), fetchImpl }),
    ),
  );

  return server;
}

async function startServer() {
  const server = createGithubMilestonesServer();
  await server.connect(new StdioServerTransport());
}

if (process.argv[1] && pathToFileURL(resolve(process.argv[1])).href === import.meta.url) {
  startServer().catch((error) => {
    process.stderr.write(`${error instanceof Error ? error.message : String(error)}\n`);
    process.exitCode = 1;
  });
}