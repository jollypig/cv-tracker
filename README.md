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

People created by a signed-in account are associated with that account. Existing records remain unowned after the ownership migration and are not visible to new accounts; assign them through a trusted migration or administrative process before enabling access.

## Checks

- Frontend production build: `npm run build` from `frontend/`.
- Backend tests: `mvn test` from `backend/`.

Flyway applies SQL migrations from `backend/src/main/resources/db/migration` on backend startup. PostgreSQL data is kept in the Compose named volume `postgres_data`.

Database settings can be overridden with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. The local Hikari pool can be tuned with `DB_POOL_MAX_SIZE`, `DB_POOL_MIN_IDLE`, and `DB_CONNECTION_TIMEOUT_MS`.

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