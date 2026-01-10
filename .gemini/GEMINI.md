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
-   `code/multitenancy-lib`: Reusable Spring Boot library for PostgreSQL multitenancy.
    -   Used by `backend` for tenant context, Hibernate integration, and schema-based isolation.
    -   Provides `@Multitenant` annotation for repositories.
-   `code/frontend/frp-fe`: Node.js project for the React application.
    -   `src/api`: Generated API client (do not edit manually unless necessary, generated via `npm run generate-api`).
    -   `src/components`: React components (`UIComponent`, `UserManagement`, etc.).
    -   `src/locales`: Translation files (`en`, `cs`).
-   `docker-compose.yml`: Base Docker Compose configuration containing core services (DB, Backend, Frontend).
-   `docker-compose.override.yml`: Development extensions, including SonarQube. Automatically loaded by Docker in development.

## Development Instructions

### Prerequisites

-   JDK 22
-   Node.js 20+
-   Docker & Docker Compose
-   Maven

### Docker Environment Management

-   **Development Mode** (Core + SonarQube):
    ```bash
    wsl docker compose up -d
    ```
    (Loads `docker-compose.yml` and `docker-compose.override.yml`)

-   **Production Mode** (Core only):
    ```bash
    wsl docker compose -f docker-compose.yml up -d
    ```

-   **Isolated Environments**:
    Use `-p` flag to avoid conflicts:
    ```bash
    wsl docker compose -p frp-dev up -d
    ```

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

### Tool Execution Rules

- **Docker**: All `docker` or `docker compose` commands **MUST** be prefixed with `wsl` (e.g., `wsl docker compose ps`).
- **curl**: `curl` commands should be executed **natively** (without `wsl`).

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

### 5. Security rules

- Services **MUST NOT** access SecurityContextHolder directly
- Authenticated user is accessed only via **CurrentUserProvider**
- Security-related logic lives in security module only

## Best Practices for Agent

-   **Duplicities** Before creating any new method that retrieves or manipulates domain entities, always check if an existing service provides this functionality. Do not duplicate logic. Always reuse existing methods.
-   **Commitment**: Never commit changes using `git`. The user will always commit changes manually.
-   **Local Server Management**: Never attempt to start the database server or backend application automatically. The user will start these components manually upon request.
-   **Git Operations**: Never use `git add`, `git stash`, or `git checkout`. The user will handle these operations manually.
-   **Remote Repository**: Do not use tools that modify the remote GitHub repository (like `delete_file`). All changes must be local.
-   **Backend Testing Policy**: Always write tests for new features. Always check `AbstractDbTest` for integration testing patterns. Use `SharedPostgresContainer`.
-   **Frontend Testing Policy**:
    -   All new frontend code (features, components, hooks) **MUST** include corresponding unit or integration tests using Vitest and React Testing Library.
    -   Tests must cover edge cases, error handling (e.g., API errors), and utilize proper mocking of services.
    -   **Strict Coverage Gate**: The project enforces a minimum of **70% test coverage** (statements, branches, functions, lines) for the frontend.
    -   **AI Agent Rule**: AI agents must generate tests simultaneously with components/hooks. If a test is missing, the code is considered invalid and incomplete.
    -   **Execution**:
        -   Run tests: `npm run test`.
        -   Run tests with coverage: `npm run test:coverage`.
        -   Coverage violation will fail the build/CI.

-   **UI Components**:
    -   Reuse existing components in `src/components/UIComponent` (`Text.tsx`, `Input.tsx`, `Form.tsx`, `ErrorDisplay.tsx`).
    -   **Rule**: AI **MUST NOT** create new low-level components (inputs, buttons, typography) if a corresponding one exists in `UIComponent`.
    -   **No Custom Styles**: Inline styles (`style={{...}}`) and custom CSS classes (outside of Tailwind) are strictly forbidden in feature components. All styling must be handled via Tailwind or by extending `UIComponent` library.
    -   **Storybook**: All components in `UIComponent` **MUST** have a corresponding `.stories.tsx` file. ESLint enforces valid story configurations.

-   **Translations**: Always add user-facing strings to `src/locales/en/translation.json` and `cs/translation.json`. Use `t('key')`.
-   **Strict Types**: Use `import type` for interfaces/types in TypeScript to avoid compilation errors with `verbatimModuleSyntax`.

### Architecture & Code Quality

-   **Architecture Tests**: Backend architecture is enforced by ArchUnit (`ArchitectureTest.java`). Violations cause test failures. These tests ensure the codebase remains maintainable and consistent.
    -   **DTOs**: Must be immutable Java `record`s residing in `..model.dto` packages. Inner classes for DTOs are forbidden.
    -   **Controllers**: Must not be inner classes. Must not access `SecurityContextHolder` or Repositories directly. Must only depend on Services, DTOs, and Constants.
    -   **Layering**: Strict layering is enforced: Controller -> Service -> Repository. No skipping layers or reverse dependencies.

-   **Sonar Analysis**:

    -   **Execution**: SonarQube runs locally and **explicitly**. It is NOT triggered by default build commands.
    -   **Command**: To run Sonar analysis, use:
        ```bash
        mvn verify -Psonar sonar:sonar -pl code/backend
        ```
    -   **Prerequisites**: Ensure `sonarqube` container is running (`docker compose up -d sonarqube`) before running analysis.
    -   **Quality Gate**: The project enforces a **Strict Quality Gate**. The build **will fail** if the Quality Gate is not passed.
    -   **Rule**: **SonarQube analysis is part of backend verification.** Any duplication, code smell, bug, or Quality Gate failure must be fixed immediately.
    -   **AI Agent Policy**: AI agents must reuse existing services and logic. **Duplicate logic is a severe violation** caught by Sonar and must be rectified instantly.

-   **Static Analysis (Checkstyle & SpotBugs)**:
    -   **Checkstyle**: Enforces coding style and specific best practices (e.g., explicit visibility, no field injection, no magic strings in controllers). Configuration is in `code/backend/checkstyle.xml`.
    -   **SpotBugs**: Detects potential bugs (null dereferences, resource leaks) in bytecode.
    -   **Policy**: These tools are configured to **fail the build** on any violation. **Do not fight the tools**; fix the code to comply with the rules.
    -   **Execution**:
        -   Checkstyle runs during `mvn validate`.
        -   SpotBugs runs during `mvn process-classes` (and `verify`).

-   **Frontend Static Analysis & Type Checking**:
    -   **ESLint**: Enforces React best practices and project-specific rules (no magic strings for paths, no inline styles).
    -   **Prettier**: Enforces consistent code formatting.
    -   **TypeScript**: Strict type checking via `tsc`.
    -   **Policy**: These tools are the authority for code quality. The build **will fail** if rules are violated.
    -   **Execution**:
        -   Run all checks: `npm run check-all` (in `code/frontend/frp-fe`).
        -   Fix formatting: `npm run lint:fix`.
        -   Type check only: `npm run tsc`.

### Testing Best Practices
- Do not write disabled tests or early returns to bypass unimplemented logic.
- Every test method must contain meaningful assertions or be removed entirely.
- If the functionality is not implemented, do not create placeholder test methods.
- Do not comment out or disable tests with 'if(true) return;' or similar hacks.

