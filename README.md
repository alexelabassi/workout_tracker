# Workout Thesis Platform

Evidence-Informed Workout Template Discovery and Training History Platform.

Backend: Spring Boot 3.5.x (Java 21) · Frontend: React + TypeScript (Vite) · DB: PostgreSQL 16 + Flyway · Docker Compose.

The production build serves the React frontend and the REST API together at `http://localhost:8080`.

## Layout

```text
backend/      Spring Boot app, Flyway migrations, Dockerfile
frontend/     React + TypeScript (Vite) app
docs/         Specification, architecture, schema, API contract, etc.
docker-compose.yml
```

## Run with Docker (recommended)

Requires Docker + Docker Compose.

```bash
docker compose up --build
```

This builds the frontend, packages it into the Spring Boot jar, starts PostgreSQL 16, applies the Flyway migration, and serves everything at `http://localhost:8080`.

- App / UI: http://localhost:8080
- Health: http://localhost:8080/api/health

Stop and remove containers (keep data volume):

```bash
docker compose down
```

## Backend development

The build requires a JDK 21. The frontend is built automatically by Maven
(`frontend-maven-plugin`) and bundled into the jar.

```bash
mvn -f backend/pom.xml verify   # compile, build frontend, run Testcontainers tests
mvn -f backend/pom.xml spring-boot:run
```

Tests use [Testcontainers](https://testcontainers.com/) to run against a real
PostgreSQL 16 container (no H2), so a running Docker daemon is required for
`mvn verify`.

The backend expects a PostgreSQL instance. Defaults (overridable via
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`):

```text
jdbc:postgresql://localhost:5432/workout   user: workout   password: workout
```

You can start just the database with `docker compose up postgres`.

## Frontend development

For fast iteration, run the Vite dev server (port 5173). It proxies `/api`
requests to the backend on port 8080.

```bash
cd frontend
npm install
npm run dev
```

For the final/demo build, the frontend is served by Spring Boot at
`http://localhost:8080` (no separate dev server needed) — see the Docker
instructions above.

## Implementation roadmap

See `docs/TASKS.md`. Phase 1 (this milestone) delivers the runnable
infrastructure: backend, frontend, Docker Compose, PostgreSQL, Flyway migration,
and the `/api/health` endpoint.
