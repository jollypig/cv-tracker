# CV Management

Starter monorepo for the CV management application described in [the architecture and delivery plan](cv-management-architecture-and-sprints_v2.md).

## Prerequisites

- Java 17 or newer
- Maven 3.9+
- Node.js 22.18+ and npm
- Docker Compose

## Run locally

The defaults are for local development only. The backend uses the `local` Spring profile by default; override these settings with environment variables as needed. `.env` is ignored by Git.

1. Start PostgreSQL: `docker compose up -d postgres`.
2. Start the backend from `backend/`: `mvn spring-boot:run`.
3. In another terminal, start the frontend from `frontend/`: `npm install` then `npm run dev`.

The frontend is available at <http://localhost:5173>, the API status endpoint at <http://localhost:8080/api/v1/status>, the OpenAPI UI at <http://localhost:8080/swagger-ui.html>, and Actuator health at <http://localhost:8080/actuator/health>.

## Authentication

The API uses OIDC authorization-code login and a server-side session. Configure an OIDC provider before signing in:

- `OIDC_ISSUER_URI`: provider issuer URL.
- `OIDC_CLIENT_ID`: registered client ID.
- `OIDC_CLIENT_SECRET`: secret for confidential clients; optional for public clients.
- `FRONTEND_URL`: frontend URL to open after provider login; defaults to `http://localhost:5173/`.
- `CORS_ALLOWED_ORIGINS`: comma-separated frontend origins; defaults to local Vite ports 5173-5177.
- `SESSION_COOKIE_SAME_SITE` and `SESSION_COOKIE_SECURE`: cookie settings; use `none` and `true` for cross-site HTTPS deployments.

Register `http://localhost:8080/login/oauth2/code/oidc` as the provider redirect URI. The frontend starts login at `/oauth2/authorization/oidc`; API requests use the session cookie and CSRF token cookie. Without OIDC configuration, protected API calls return `401` and the frontend indicates that sign-in is unavailable.

### Keycloak login theme

The `keycloak/themes/folio/login` theme styles Keycloak 26's `keycloak.v2` login pages to match the frontend. Mount `./keycloak/themes` at `/opt/keycloak/themes` in your Keycloak service, then select **Realm settings → Themes → Login theme → folio**. The theme does not replace Keycloak templates, so login, recovery, registration, and identity-provider flows remain native. In development, disable theme caching or restart Keycloak after changing theme files.

People created by a signed-in account are associated with that account. Existing records remain unowned after the ownership migration and are not visible to new accounts; assign them through a trusted migration or administrative process before enabling access.

## Checks

- Frontend production build: `npm run build` from `frontend/`.
- Frontend unit and component tests: `npm test` from `frontend/`.
- Frontend browser tests: `npm run test:e2e` from `frontend/` (install Chromium first with `npx playwright install chromium`).
- Backend tests: `mvn test` from `backend/`.
- Backend PostgreSQL and MinIO integration tests use Testcontainers and require Docker.

Flyway applies SQL migrations from `backend/src/main/resources/db/migration` on backend startup. PostgreSQL data is kept in the Compose named volume `postgres_data`.

Database settings can be overridden with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. The local Hikari pool can be tuned with `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, and `DB_CONNECTION_TIMEOUT_MS`.

CV import uses a local Ollama server by default. Start Ollama bound to loopback and pull the configured model (`ollama pull llama3.2`) before using extraction. Configure `OLLAMA_BASE_URL` (default `http://127.0.0.1:11434`), `OLLAMA_MODEL` (default `llama3.2`), `OLLAMA_MODEL_VERSION` (default `unknown`; an optional model tag or digest stored with import diagnostics), and `CV_AI_REQUEST_TIMEOUT` (default `60s`) as needed. `CV_AI_PROVIDER` defaults to `ollama`. In deployments, keep Ollama on a private application network and do not publish its API port to the public interface.

### Parse CVs with Claude

Set `CV_AI_PROVIDER=anthropic` and supply `ANTHROPIC_API_KEY` through the backend environment, secret manager, or ignored `.env` file. Restart the backend after changing providers. Ollama is not required when Anthropic is selected; it remains the default otherwise. Selecting Anthropic without a key fails startup with a configuration error.

- `ANTHROPIC_MODEL`: Claude model ID; defaults to `claude-sonnet-4-5`.
- `ANTHROPIC_MAX_TOKENS`: maximum output tokens per request; defaults to `8192`. Increase this for long CVs if output is truncated.
- `CV_AI_MODEL_VERSION`: optional model version recorded in import diagnostics. For Ollama, `OLLAMA_MODEL_VERSION` remains supported as a fallback.
- `CV_AI_REQUEST_TIMEOUT`: connection and read timeout per model request; defaults to `60s` for both providers.

Both providers use the same section detection, structured extraction, validation, and review workflow. Claude sends normalized CV content to Anthropic's hosted API and may incur usage charges. Confirm that you have permission to send personal data to that service. Keep the API key server-side and never commit it.

Uploaded source files are processed for the duration of the import request and are not persisted. Spring's multipart handling removes its temporary upload parts when the request completes; only import metadata and the reviewable parsed draft are retained. Import metadata and drafts are account-owned, and import failures expose only safe, generic descriptions rather than parser/provider exception details.

## Production image

Build the backend image from the repository root with `docker build -t cv-management .`. Run it with the Spring `prod` profile and provide `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `FRONTEND_URL`, and `CORS_ALLOWED_ORIGINS` through the deployment environment or secret manager. Configure `OIDC_ISSUER_URI` and `OIDC_CLIENT_ID` to enable sign-in; set `OIDC_CLIENT_SECRET` when required by the identity provider. The production profile enables secure session cookies, structured JSON logs, readiness/liveness health probes, and Prometheus metrics. OpenAPI docs and Swagger UI are disabled by default in production and can be enabled with `OPENAPI_DOCS_ENABLED` and `OPENAPI_SWAGGER_UI_ENABLED`.

The unauthenticated `/actuator/prometheus` scrape endpoint should only be reachable on a trusted monitoring network. GitHub Actions runs backend tests, frontend tests and build, Playwright browser tests, and a Docker image build for pushes and pull requests.

## Skill experience and project evidence

### Merge content from other CVs

In the CV content editor, choose **More actions > Merge from other CVs**, select one or more source CVs, and click **Merge**. The editor saves its current draft first. All selected sources must belong to the signed-in account; they remain unchanged. The backend merges all selected sources in one transaction.

- Existing CV metadata, summary, person details, output settings, section settings, and custom sections stay unchanged.
- Employment positions match by company and start date. Existing position fields stay unchanged; projects within each matching position are added only when the project name and customer company are unique.
- Positions sort by start date and projects by `periodFrom`, newest first, with missing dates last. Standalone projects match by name because they have no company field.
- Skill groups match by name; new skills are added to their corresponding group only if their names are unique across the target CV. Existing skill details, levels, and visibility stay unchanged. Imported project links are mapped to the target projects.
- Certificates and languages match by name. Education matches by institution, start date, and end date, including missing dates.
- All name comparisons ignore surrounding whitespace and case. Sources are processed in selection order, so the first source wins collisions between new entries. Repeating a merge adds no duplicates.

API: `POST /api/v1/cvs/{cvId}/content/merge` with `{"sourceCvIds":["source-cv-uuid"]}` returns the saved merged content. The existing 100-entry limit for each collection also applies to merges.

Skills support entered years of experience, optional years actively used, started-from and last-used dates (either `YYYY` or `YYYY-MM-DD`), frequency (`daily`, `occasionally`, `rarely`), status (`active`, `learning`, `maintaining`, `deprecated`), and linked projects with one-line outcomes. Project links use stable keys retained across content saves, versions, and copies. Standalone projects have period dates and a current-project flag; employment projects use their existing period dates.

- Calculated last used is the latest entered last-used date or linked project end. Current projects use today's date; an employment project without an end uses today only when its employment is current.
- Calculated years of experience uses the entered value (including zero), otherwise the interval from started-from to calculated last-used, when both are known.
- Total experience is the maximum of calculated years of experience and the sum of linked project periods. Overlapping project periods are summed, not merged. Missing or reversed periods do not contribute; duplicate links do not double-count a project.
- Bare years represent January 1. Date intervals are divided by 365.2425 and rounded to two decimals internally. Calculated experience values are then rounded up to whole years in the editor, preview, and export; entered values are retained unchanged.
- Skills last used more than five years ago are flagged, without changing the manually configured order.

Enable **Include experience and project outcomes in output** per skill to show these values in all preview templates and saved-version PDF exports. Skill visibility and hidden employment-project names still apply. Existing skills and snapshots remain compatible. Migration V20 adds the storage fields on backend startup.

## File storage and PDF exports

- Create a PDF from a saved version: `POST /api/v1/cv-versions/{versionId}/exports/pdf`.
- List exports for that version: `GET /api/v1/cv-versions/{versionId}/exports`.
- Download an export: `GET /api/v1/exports/{exportId}/download`.

The default `STORAGE_TYPE=local` stores generated files under `./data/exports` relative to the backend process working directory. Set `STORAGE_LOCAL_BASE_PATH` to use another directory. `CV_EXPORT_DIRECTORY` remains supported as a fallback for existing local setups.

To run the S3-compatible MinIO service and provision the `cv-files` bucket:

```powershell
docker compose up -d minio
docker compose run --rm minio-init
```

For a backend running on the host, configure S3 storage in PowerShell before starting it:

```powershell
$env:STORAGE_TYPE = "s3"
$env:STORAGE_S3_ENDPOINT = "http://localhost:9000"
$env:STORAGE_S3_BUCKET = "cv-files"
$env:STORAGE_S3_REGION = "us-east-1"
$env:STORAGE_S3_PATH_STYLE_ACCESS = "true"
$env:STORAGE_S3_ACCESS_KEY = "cv_minio_user"
$env:STORAGE_S3_SECRET_KEY = "cv_minio_password"
mvn -f backend/pom.xml spring-boot:run
```

To run the S3/MinIO round-trip integration test, start MinIO as above, set the same `STORAGE_S3_*` variables, then run `mvn -f backend/pom.xml -Dtest=StorageIntegrationTest test`. The S3 integration test is skipped when `STORAGE_S3_ENDPOINT` is unset.

## License

This project is licensed under the GNU General Public License v3.0 only. See [LICENSE](LICENSE) for details.