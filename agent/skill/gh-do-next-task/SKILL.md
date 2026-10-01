# gh-do-next-task Skill

## Overview
Automates the workflow of picking up the next ready task from GitHub milestones and issues, implementing it, and creating a pull request. This skill streamlines the development process by ensuring tasks flow from "ready" → "in progress" → "PR created".

## Workflow Steps

### Step 1: Fetch Open Milestones and Get First Ready Issue
1. **Fetch all open milestones** from the GitHub repository with the `github-milestones` MCP server's `list_milestones` tool, sorted by milestone number in ascending order (e.g., "Sprint 1", "Sprint 2", etc.)
2. **Identify the first open milestone** in the list
3. **Fetch all issues** in that milestone with `list_issues_by_milestone`, passing the selected milestone's number
4. **Sort issues by task number** (extracted from issue title, e.g., "Task 1:", "Task 2:", etc.)
5. **Find the first issue** with the label `wf:ready`
6. **If no issue is found** with the `wf:ready` label, skip the remaining steps and notify the user that no ready tasks exist
7. **Store the issue data** (number, title, body, URL) for use in subsequent steps

**Tools Required:**
- Use `github-milestones.list_milestones` and `github-milestones.list_issues_by_milestone` to list milestones and their issues, then sort and filter the results as described above
- If the configured GitHub MCP server does not expose a required read operation, stop and tell the user which operation is unavailable; do not fall back to direct REST calls or the GitHub CLI

### Step 2: Mark Issue as In Progress
1. **Add the label `wf:in progress`** to the selected issue and remove 'wf:ready'
2. **Optionally add a comment** to the issue indicating that implementation has started (e.g., "@{bot} Starting implementation...")

**Tools Required:**
- Use the GitHub MCP issue update tool to apply `wf:in progress` and remove `wf:ready`
- Use GitHub MCP tools for any issue comment; do not call the GitHub REST API directly

### Step 3: Implement the Task
1. **Read the issue description and acceptance criteria** to understand the requirements
2. **Perform the implementation work** based on the issue details
   - Create necessary files, modify existing files, update documentation
   - Follow project conventions and coding standards (refer to `AGENTS.md` if available in the project)
   - Reference the sprint plan and architecture guidelines from `SPRINT.md` if available
3. **Run quick quality checks** locally if applicable (linting, static checks, formatting)
4. **Track all file changes** for the commit in the next step

### Step 4: Add Tests to Cover Implemented Functionality
1. **Add or update tests** that validate the behavior introduced in Step 3
2. **Cover acceptance criteria** from the issue with focused test cases
3. **Run the test suite (or targeted tests)** and ensure tests pass locally
4. **Include test artifacts** (fixtures/mocks) only when necessary and keep them minimal

### Step 5: Create Commit and Push
1. **Derive the branch name from the issue title:**
   - Convert to lowercase: `"Task 1: Initialize Python Project"` → `"task 1: initialize python project"`
   - Replace spaces with hyphens: `"task-1-initialize-python-project"`
   - Remove special characters except hyphens
   - Example conversions:
     - "Task 1: Initialize Python Project" → `task-1-initialize-python-project`
     - "Task 2: Setup CI/CD Pipeline" → `task-2-setup-cicd-pipeline`
     - "Task 3: Add User Authentication" → `task-3-add-user-authentication`

2. **Create and checkout the feature branch:**
   ```bash
   git checkout -b {derived-branch-name}
   ```

3. **Stage and commit all changes** with the issue title as the commit message:
   ```bash
   git add .
   git commit -m "{issue-title}"
   ```
   - Example: `git commit -m "Task 1: Initialize Python Project"`

4. **Push the branch** to GitHub:
   ```bash
   git push origin {derived-branch-name}
   ```

**Tools Required:**
- Git commands for local branch creation, staging, committing, and pushing
- Use GitHub MCP tools for GitHub operations; do not use direct REST calls or the GitHub CLI as an API fallback

### Step 6: Create Pull Request
1. **Create a pull request** with:
   - **Title:** Use the issue title (e.g., "Task 1: Initialize Python Project")
   - **Description:** 
     - Reference the issue with `Closes #{issue_number}` or `Fixes #{issue_number}`
     - Include a brief summary of changes made
     - Link to any relevant documentation or acceptance criteria from the issue
     - Use proper markdown formatting
   - **Base branch:** `main` (or the default branch for the repository)
   - **Head branch:** `{derived-branch-name}`

2. **Verify the PR creation** was successful and capture the PR URL

**Tools Required:**
- Use the GitHub MCP pull request creation tool

### Step 7: Assign PR to Repository Owner
1. **Identify the repository owner** (from repository metadata)
2. **Assign the pull request** to the repository owner's GitHub username
3. **Verify assignment** was successful
4. **Add the label `wf:testing`** to the selected issue and remove 'wf:in progress'

**Tools Required:**
- Use GitHub MCP tools to assign the pull request and update the issue labels
- If the configured GitHub MCP server does not expose a required write operation, stop and tell the user which operation is unavailable; do not fall back to direct REST calls or the GitHub CLI

### Step 8: Provide Results
1. **Output the pull request URL** in the format: `https://github.com/{owner}/{repo}/pull/{pr_number}`
2. **Summarize the completed workflow:**
   - Issue title and number
   - Branch name created
   - Commit hash (if available)
   - PR link and status
3. **Provide any additional context** (e.g., next steps, related documentation links)

## Error Handling

### No Ready Issue Found
- **Action:** Notify the user that no issues with the `wf:ready` label exist in the earliest open milestone
- **Suggestion:** Recommend reviewing the sprint backlog or creating new ready tasks

### Implementation Errors
- **Action:** Capture error details and rollback local changes if needed
- **Suggestion:** Return error message and any partial progress information

### GitHub API Errors
- **Handling:**
- Use the GitHub MCP server's returned error details; retry only when the error is transient and the operation is safe to retry
- For authentication errors, ask the user to configure or refresh the environment variable and restart the MCP server; never ask them to send the token in chat
- For permission errors, verify repository access and permissions

### Git/Branch Errors
- **Handling:**
  - Verify branch doesn't already exist before creation
  - Handle merge conflicts or dirty working directory appropriately
  - Suggest manual git operations if automation fails

## Authentication & Setup

### Required
- **GitHub Personal Access Token** available to the GitHub MCP server through its process environment, with permissions for the required repository operations:
  - `repo` (full repository access)
  - `workflow` (if managing GitHub Actions)
- **Git credentials** configured locally or provided via environment for branch pushes
- **Repository context:** owner/repo must be determinable from environment or git remote URL

### Environment Variables
- `GITHUB_PERSONAL_ACCESS_TOKEN` or `GITHUB_TOKEN`: authentication for the configured GitHub MCP server
- `GIT_USER_NAME`, `GIT_USER_EMAIL`: For git commits (optional, uses system config if not set)

### Token Resolution
- The GitHub MCP server must receive its token from `GITHUB_PERSONAL_ACCESS_TOKEN` or `GITHUB_TOKEN` in its environment
- Never read, print, log, or store the token in workspace files, including `mcp.json`; never ask the user to provide it in chat
- If neither environment variable is available to the MCP server, stop and ask the user to configure one in their environment, then restart the server

### Critical Git Workflow Rules
- **NEVER push implementation commits directly to `main`** — always use a feature branch
- The feature branch MUST be created from the current `main` HEAD (not from any prior branch)
- After creating the feature branch, do NOT push any implementation commits to `main` under any circumstances
- If `main` was accidentally polluted, revert it (`git revert <commit>`) AND re-create the feature branch from the clean `main` HEAD with a NEW commit (do not reuse the old commit hash — GitHub will reject a PR with no new commits relative to main)

## Acceptance Criteria

- ✅ Milestone and issue fetching works correctly with proper sorting
- ✅ First issue with `wf:ready` label is correctly identified
- ✅ Label `wf:in progress` is added to the selected issue
- ✅ Tests are added/updated to cover implemented functionality
- ✅ Branch name is correctly derived from issue title
- ✅ Commit message uses the issue title verbatim
- ✅ Branch is pushed to GitHub successfully
- ✅ Pull request is created with proper title, description, and base/head branches
- ✅ PR is assigned to the repository owner
- ✅ PR URL is returned and accessible
- ✅ Proper error handling for edge cases (no ready issues, API failures, git errors)

## Example Execution

**Input:**
- Repository: `FitGenieAI/fitgenieai` (owner: FitGenieAI, repo: fitgenieai)
- Current milestone: Sprint 1
- Ready issue: Issue #5 "Task 1: Initialize Python Project"

**Process:**
1. Fetch milestones → Find "Sprint 1"
2. Fetch issues in Sprint 1 → Find Issue #5 with `wf:ready` label
3. Add `wf:in progress` label to Issue #5
4. Implement the project initialization (create files, setup configs, etc.)
5. Add/update tests for the implementation and run them
6. Create branch: `task-1-initialize-python-project`
7. Commit: "Task 1: Initialize Python Project"
8. Push to GitHub
9. Create PR: "Task 1: Initialize Python Project" with description referencing Issue #5
10. Assign PR to "FitGenieAI" (repo owner)
11. Return: `https://github.com/FitGenieAI/fitgenieai/pull/1`

## Notes & Best Practices

- **Automation friendly:** This skill is designed to be called sequentially by an agent; ensure each step completes successfully before proceeding
- **Idempotency:** If a step fails partway, provide clear status so retry logic can resume appropriately
- **Documentation:** Always update `README.md` or relevant project documentation if new commands, env vars, or directory layouts are introduced
- **Testing:** Before automation, verify the github token has necessary permissions and the repository structure matches expectations
- **Networking:** Ensure GitHub API rate limits are respected; implement appropriate backoff strategies

