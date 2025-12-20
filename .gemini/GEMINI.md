# Gemini CLI Project Guide

This document contains useful information, instructions, and context for the Gemini CLI agent to effectively work on the Family Resource Planning (FRP) project.

## Project Overview

FRP is a comprehensive application for managing family tasks, resources, calendar, accounting, and more. It is a full-stack application composed of a Spring Boot backend and a React frontend.

### Tech Stack

-   **Backend**:
    -   Java 22
    -   Spring Boot 4.0.0
    -   Spring Data JPA (Hibernate)
    -   PostgreSQL 17.1 (Database)
    -   Flyway (Database Migration)
    -   MapStruct (DTO Mapping)
    -   Testcontainers (Integration Testing)
-   **Frontend**:
    -   React 19
    -   TypeScript
    -   Vite
    -   Tailwind CSS
    -   Flowbite-React (UI Components)
    -   Axios (HTTP Client)
    -   i18next (Internationalization)

## Repository
Hosted on GitHub.
URL: https://github.com/dvdmchl/frp

## Project Structure

-   `code/backend`: Maven project for the Spring Boot application.
    -   `src/main/java`: Source code.
    -   `src/test/java`: Tests (Unit and Integration).
    -   `src/main/resources`: Configuration (`application.properties`) and SQL migrations (`db/migration`).
-   `code/frontend/frp-fe`: Node.js project for the React application.
    -   `src/api`: Generated API client (do not edit manually unless necessary, generated via `npm run generate-api`).
    -   `src/components`: React components (`UIComponent`, `UserManagement`, etc.).
    -   `src/locales`: Translation files (`en`, `cs`).
-   `docker-compose.yml`: Setup for running the full stack (DB, Backend, Frontend) or just DB.

## Development Instructions

### Prerequisites

-   JDK 22
-   Node.js 20+
-   Docker & Docker Compose
-   Maven

### Running the Database

To start only the PostgreSQL database (useful for local development):

```bash
./start-db.cmd
```
Or manually:
```bash
docker compose up -d db
```

### Backend Development

-   **Build & Compile**:
    ```bash
    mvn clean compile -pl code/backend
    ```
-   **Run Tests**:
    ```bash
    mvn test -pl code/backend
    ```
-   **Run Application**:
    ```bash
    mvn spring-boot:run -pl code/backend
    ```
    (Ensure DB is running)

### Frontend Development

-   **Install Dependencies**:
    ```bash
    cd code/frontend/frp-fe
    npm install
    ```
-   **Run Dev Server**:
    ```bash
    cd code/frontend/frp-fe
    npm run dev
    ```
-   **Build**:
    ```bash
    cd code/frontend/frp-fe
    npm run build
    ```
    (This runs `tsc -b && vite build`)

## Key Workflows & Commands

### 1. Generating Frontend API Client

When the Backend API changes (Controllers, DTOs), update the Frontend client:

1.  Start the Backend (must be running on port 8080).
2.  Run the generation script:
    ```bash
    cd code/frontend/frp-fe
    npm run generate-api
    ```
    This updates `src/api` using `openapi-typescript-codegen`.

### 2. Database Migrations

-   Managed by **Flyway**.
-   Locations: `code/backend/src/main/resources/db/migration`.
-   New schemas should be created via `SchemaService` which handles `CREATE SCHEMA` and Flyway migration programmatically for multi-tenancy.

### 3. Error Handling

-   **Backend**: Exceptions are handled globally in `RestExceptionHandler.java`. Always return `ResponseEntity<ErrorDto>`.
-   **Frontend**: API errors are caught as `ApiError`. Use `ErrorDisplay.tsx` component to show errors.
    -   **Parsing**: `ApiError.ts` automatically parses the backend's `ErrorDto`.
    -   **Display**: `<ErrorDisplay error={apiError} />` provides a standardized UI with expandable stack traces.

### 4. Transaction Management

-   **Backend**: Use `@Transactional`.
-   **Caution**: For manual JDBC operations (e.g., in `SchemaService`), inject `JdbcTemplate` to ensure participation in the Spring transaction. Do NOT use `dataSource.getConnection()` directly as it bypasses the transaction manager.

## Best Practices for Agent

-   **Duplicities** Before creating any new method that retrieves or manipulates domain entities, always check if an existing service provides this functionality. Do not duplicate logic. Always reuse existing methods.
-   **Commitment**: Never commit changes using `git`. The user will always commit changes manually.
-   **Local Server Management**: Never attempt to start the database server or backend application automatically. The user will start these components manually upon request.
-   **Git Operations**: Never use `git add`, `git stash`, or `git checkout`. The user will handle these operations manually.
-   **Remote Repository**: Do not use tools that modify the remote GitHub repository (like `delete_file`). All changes must be local.
-   **Backend Testing Policy**: Always write tests for new features. Always check `AbstractDbTest` for integration testing patterns. Use `SharedPostgresContainer`.
-   **Frontend Testing Policy**: All new frontend code (features, components, bug fixes) must include corresponding unit or integration tests, aiming for a minimum of 70% test coverage. Utilize Vitest and React Testing Library (RTL).
-   **UI Components**: Reuse existing components in `src/components/UIComponent` (`Text.tsx`, `Input.tsx`, `Form.tsx`, `ErrorDisplay.tsx`).
-   **Translations**: Always add user-facing strings to `src/locales/en/translation.json` and `cs/translation.json`. Use `t('key')`.
-   **Strict Types**: Use `import type` for interfaces/types in TypeScript to avoid compilation errors with `verbatimModuleSyntax`.

### Architecture & Code Quality

-   **Architecture Tests**: Backend architecture is enforced by ArchUnit (`ArchitectureTest.java`). Violations cause test failures. These tests ensure the codebase remains maintainable and consistent.
    -   **DTOs**: Must be immutable Java `record`s residing in `..model.dto` packages. Inner classes for DTOs are forbidden.
    -   **Controllers**: Must not be inner classes. Must not access `SecurityContextHolder` or Repositories directly. Must only depend on Services, DTOs, and Constants.
    -   **Layering**: Strict layering is enforced: Controller -> Service -> Repository. No skipping layers or reverse dependencies.

-   **Sonar Analysis**:
    -   Analysis runs automatically in the CI pipeline (GitHub Actions).
    -   **Quality Gate**: The project enforces a "zero new issues" policy. Any new bugs, vulnerabilities, or code smells will cause the pipeline to fail.
    -   **Action**: Sonar findings must be fixed, not ignored or marked as false positives unless absolutely necessary and justified.