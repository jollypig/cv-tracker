# CV Management

Starter monorepo for the CV management application described in [the architecture and delivery plan](cv-management-architecture-and-sprints_v2.md).

## Prerequisites

- Java 17 or newer
- Maven 3.9+
- Node.js 22.18+ and npm
- Docker Compose

## Run locally

The defaults are for local development only. Override them with environment variables if needed; `.env` is ignored by Git.

1. Start PostgreSQL: `docker compose up -d postgres`.
2. Start the backend from `backend/`: `mvn spring-boot:run`.
3. In another terminal, start the frontend from `frontend/`: `npm install` then `npm run dev`.

The frontend is available at <http://localhost:5173>, the API status endpoint at <http://localhost:8080/api/v1/status>, the OpenAPI UI at <http://localhost:8080/swagger-ui.html>, and Actuator health at <http://localhost:8080/actuator/health>.

## Checks

- Frontend production build: `npm run build` from `frontend/`.
- Backend tests: `mvn test` from `backend/`.

Flyway applies SQL migrations from `backend/src/main/resources/db/migration` on backend startup. PostgreSQL data is kept in the Compose named volume `postgres_data`.