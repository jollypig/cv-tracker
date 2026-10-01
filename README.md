# CV Management

Starter monorepo for the CV management application described in [the architecture and delivery plan](cv-management-architecture-and-sprints_v2.md).

## Prerequisites

- Java 17 or newer
- Maven 3.9+
- Node.js 22.18+ and npm
- Docker Compose

## GitHub MCP

VS Code loads the GitHub MCP servers from `.vscode/mcp.json`. The `github-milestones` server provides read-only `list_milestones` and `list_issues_by_milestone` tools. Set `GITHUB_TOKEN` in the workspace `.env` file; it is read by the MCP process and must not be committed. Install the server dependency once with `npm ci --prefix agent/mcp`, then restart the MCP server in VS Code.

## Run locally

The defaults are for local development only. The backend uses the `local` Spring profile by default; override these settings with environment variables as needed. `.env` is ignored by Git.

1. Start PostgreSQL: `docker compose up -d postgres`.
2. Start the backend from `backend/`: `mvn spring-boot:run`.
3. In another terminal, start the frontend from `frontend/`: `npm install` then `npm run dev`.

The frontend is available at <http://localhost:5173>, the API status endpoint at <http://localhost:8080/api/v1/status>, the OpenAPI UI at <http://localhost:8080/swagger-ui.html>, and Actuator health at <http://localhost:8080/actuator/health>.

## Checks

- Frontend production build: `npm run build` from `frontend/`.
- Backend tests: `mvn test` from `backend/`.

Flyway applies SQL migrations from `backend/src/main/resources/db/migration` on backend startup. PostgreSQL data is kept in the Compose named volume `postgres_data`.

Database settings can be overridden with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. The local Hikari pool can be tuned with `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, and `DB_CONNECTION_TIMEOUT_MS`.