# FRP – instructions for AI agents

Shared instructions for all coding agents (Codex reads this file directly, Claude Code imports it via `CLAUDE.md`).
Keep this file the single source of truth; do not duplicate rules into agent-specific files.

## Project overview

FRP (Family Resource Planning) manages family tasks, resources, calendar, accounting and more.
Full-stack application: Spring Boot backend + React frontend, PostgreSQL with schema-per-tenant multitenancy.

Repository: https://github.com/dvdmchl/frp (wiki as submodule in `docs/wiki`, repo dvdmchl/frp-wiki).

### Tech stack

- **Backend**: Java 22, Spring Boot 4.0.x, Spring Data JPA (Hibernate), PostgreSQL, Flyway, MapStruct, Lombok,
  springdoc-openapi, JJWT, Testcontainers, ArchUnit, Checkstyle, SpotBugs, JaCoCo
- **Multitenancy**: [spring-pg-multitenancy](https://github.com/dvdmchl/spring-pg-multitenancy)
  (`org.dreamabout.sw:spring-pg-multitenancy`), a separate repository (moved out in #68)
- **Frontend**: React 19, TypeScript, Vite, Tailwind CSS 4, Flowbite-React, Axios, i18next, Vitest,
  React Testing Library, Storybook

Exact versions live in `code/backend/pom.xml` and `code/frontend/frp-fe/package.json`; check them instead of
relying on this file.

## Project structure

- `code/backend` – Maven module with the Spring Boot application
  - `src/main/java/org/dreamabout/sw/frp/be` – sources (`config`, `module/<name>/...`)
  - `src/main/resources` – `application*.properties`, `modules.yaml`, Flyway migrations in `db/migration`
    (`common` = shared `frp_public` schema, `modules/<module>` = per-tenant migrations)
  - `src/test/java` – unit, integration and architecture tests
- `code/frontend/frp-fe` – React application
  - `src/api` – generated API client (do not edit by hand, regenerate via `npm run generate-api`)
  - `src/components` – `UIComponent` (low-level shared components), `UserManagement`, `Modules`
  - `src/locales` – translations (`en`, `cs`)
- `code/db-init` – SQL run by the PostgreSQL container on first start
- `docker-compose.yml` – core services (db, backend, frontend); `docker-compose.override.yml` – dev tools (SonarQube)
- `docs/wiki` – project wiki (git submodule)

## Workflow rules

- **Single developer.** No pull requests and no feature branches unless the user explicitly asks.
  Work directly on `main`.
- **Commits.** Message format `#<issue> - <description>` (e.g. `#72 - Add transaction and journal account page`).
  Agents may commit and push to `main` themselves once the relevant tests and checks pass.
  Commit only your own changes; never revert, stash or discard work you did not make.
- **Issues.** Work is tracked in GitHub issues in dvdmchl/frp. Every new issue must also be added to the linked
  GitHub Project **FRP** (https://github.com/users/dvdmchl/projects/10) with a Status; an issue only in the repo
  is not tracked. The gh token needs the `project` scope (`gh auth refresh -s project`, ask the user to run it).
  ```bash
  gh issue create -R dvdmchl/frp --title "[BE] ..." --body "..." --project "FRP"   # lands without Status
  ITEM=$(gh project item-add 10 --owner dvdmchl --url <issue-url> --format json -q .id)   # idempotent, returns item id
  gh project item-edit --project-id PVT_kwHOATHpmc4AqTXv --id $ITEM \
    --field-id PVTSSF_lAHOATHpmc4AqTXvzghkl2o --single-select-option-id <status>
  ```
  Status option ids: Backlog `f75ad846`, In progress `47fc9ee4`, Done `98236657`. Set In progress when you
  start the work and Done when you close the issue (`gh project field-list 10 --owner dvdmchl` lists all fields).
  Close the issue when the work is done and pushed.
- **CI.** GitHub Actions (`.github/workflows/ci.yml`) runs `mvn verify` for the backend and `npm run check-all` for
  the frontend on every push to `main`. Check that the run is green after pushing.
- **No remote file tools.** Make all changes locally and push with git; do not edit or delete files through
  the GitHub API.

## Environment (Windows + WSL)

- Docker runs in WSL; there is no `docker` on the Windows PATH. Prefix every Docker command with `wsl`
  (`wsl docker compose ps`). Run `curl` natively, without `wsl`.
- Testcontainers works from Windows Maven through the WSL Docker daemon, configured via `DOCKER_HOST`
  or `~/.testcontainers.properties` (`docker.host=tcp://<wsl-ip>:2375`). Integration tests therefore run with
  plain `mvn test`; no manually started database is needed.
- The multitenancy library is not on Maven Central. Install it into the local `.m2` before building the
  backend, and again after changing it:
  ```bash
  mvn -f ../../libs/spring-pg-multitenancy/pom.xml install   # local clone: C:\dev\Projects\libs\spring-pg-multitenancy
  ```

### Local services

- Agents may start what they need for verification: Testcontainers (automatic), the `db` service
  (`./start-db.cmd` or `wsl docker compose up -d db`), the backend (`mvn spring-boot:run -pl code/backend`,
  e.g. for `generate-api`) and SonarQube.
- Stop only what you started yourself. Never stop, remove or recreate containers or volumes the user started,
  and never run `docker compose down -v` or delete volumes without the user's explicit request.

### Docker Compose

```bash
wsl docker compose up -d                          # development: core + SonarQube
wsl docker compose -f docker-compose.yml up -d    # production: core only
wsl docker compose -p frp-dev up -d               # isolated environment
```

## Build and test

### Backend

```bash
mvn clean compile -pl code/backend        # compile (Checkstyle runs in validate)
mvn test -pl code/backend                 # unit + integration tests (Testcontainers)
mvn verify -pl code/backend               # incl. SpotBugs and JaCoCo coverage gate (80 % lines)
mvn spring-boot:run -pl code/backend      # run (needs the db service)
mvn verify -Psonar sonar:sonar -f code/backend/pom.xml   # Sonar analysis (needs the sonarqube container and SONAR_TOKEN)
```

### Frontend (in `code/frontend/frp-fe`)

```bash
npm install
npm run dev              # dev server
npm run build            # tsc -b && vite build
npm run test             # Vitest
npm run test:coverage    # Vitest with coverage gate
npm run check-all        # ESLint + tsc + tests with coverage
npm run lint:fix         # fix lint/formatting
npm run generate-api     # regenerate src/api from the running backend (port 8080)
```

## Engineering practices

These apply to all code (backend, frontend, tests) and complement the conventions below.

- **Test-first (TDD).** For new behaviour write a failing test first, then the minimal code that makes it pass, then
  refactor with the tests green. A bug fix starts with a test that reproduces the bug; keep it as a regression test.
- **Test quality.** One behaviour per test, descriptive names (`shouldXWhenY`), Arrange-Act-Assert structure.
  Tests are deterministic: no `Thread.sleep` or fixed timeouts, no dependence on test order, current time or locale
  (inject a `Clock`). Mock only boundaries you do not own; prefer real collaborators and the real database for
  persistence logic.
- **DRY, but not prematurely.** Do not copy logic, constants, SQL, validation or test setup; extract a shared method,
  helper, component or fixture instead. Introduce an abstraction when a third copy would appear, not speculatively.
- **KISS / YAGNI.** Implement only what the issue needs: no speculative options, extension points, unused parameters
  or "just in case" code. Prefer the simplest solution that keeps the code readable.
- **Clean code.** Intention-revealing names; small, focused methods, classes and components; early returns instead
  of deep nesting. No dead or commented-out code, no `System.out`/`printStackTrace`/`console.log` (log through
  SLF4J), no `TODO` without an issue number. Never return `null` for collections or `Optional`. Prefer immutability
  (`final`, records, unmodifiable collections).
- **Scope.** Keep changes focused on the issue. Unrelated refactoring or cleanup goes into its own issue and commit.
- **Dependencies.** Do not add a new library, Maven plugin or npm package without the user's approval; prefer what
  the project already uses.
- **Security.** No secrets in the repository, tests or logs. Validate input at the boundary (Bean Validation on
  request DTOs). SQL only with bind parameters; never concatenate user input into SQL or identifiers.

## Backend conventions

- **Reuse, do not duplicate.** Before adding a method that reads or changes domain entities, check whether an
  existing service already does it. Duplicate logic is a Sonar violation and must be fixed.
- **Architecture (ArchUnit, `ArchitectureTest`)** – violations fail the build:
  - DTOs are immutable Java `record`s in `..model.dto` packages; no inner DTO classes.
  - Controllers are not inner classes, do not access `SecurityContextHolder` or repositories, and depend only on
    services, DTOs and constants.
  - Strict layering Controller -> Service -> Repository; no skipped layers or reverse dependencies.
- **Security.** Only `SecurityContextService` (`config/security`) touches `SecurityContextHolder`; services get the
  authenticated user through it. Security logic stays in the security package.
- **Errors.** Exceptions are handled globally in `RestExceptionHandler`; error responses are `ResponseEntity<ErrorDto>`.
- **Transactions.** Use `@Transactional`. For manual SQL use the injected `JdbcTemplate` so it joins the Spring
  transaction; never call `dataSource.getConnection()` directly.
- **Multitenancy.** Each tenant has its own PostgreSQL schema; shared data lives in `frp_public`
  (`multitenancy.default-schema`). The library switches Hibernate's tenant and `search_path`; mark tenant-scoped
  repositories (or services/methods) with `@Multitenant`, provide the tenant via `TenantResolver`, and propagate the context to
  async work with `MultitenancyTaskDecorator`.
- **Schemas.** Create, copy, delete and list tenant schemas through `SchemaService` (`module/common/service`). It checks
  ownership and access (`SchemaEntity`, `SchemaAccessEntity`) and delegates the schema itself to the library's
  `TenantSchemaManager` (name validation, tenant migrations, data copy, drop, orphans). `FrpTenantRegistry` lists the
  tenant schemas from `frp_schema`; the library migrates all of them on startup. Copy order of tables comes from
  `TableCopyPriorityProvider` beans; `SchemaCreationListener` beans seed new schemas (they get the owner id).
- **Migrations.** Flyway; shared schema in `db/migration/common`, per-tenant module migrations in
  `db/migration/modules/<module>`. Never edit an applied migration, add a new version instead.
- **Static analysis.** Checkstyle (`code/backend/checkstyle.xml`, runs in `validate`; method length, complexity,
  parameter count, imports, empty catch, switch default), SpotBugs (threshold Medium, `process-classes`) and the JaCoCo
  line coverage gate (80 %, `verify`) fail the build on any violation. Fix the code, do not fight or suppress the tools.
- **Sonar.** Runs only explicitly (command above) with a strict quality gate; any bug, smell, duplication or gate
  failure must be fixed.

## Frontend conventions

- Reuse components from `src/components/UIComponent` (`Text`, `Input`, `Form`, `ErrorDisplay`, ...). Do not create
  new low-level components (inputs, buttons, typography) when one exists.
- No inline styles (`style={{...}}`) or custom CSS classes in feature components; use Tailwind or extend `UIComponent`.
- Every `UIComponent` component has a `.stories.tsx` file.
- API errors are `ApiError` (parses the backend `ErrorDto`); show them with `<ErrorDisplay error={apiError} />`.
- All user-facing strings go to `src/locales/en/translation.json` and `src/locales/cs/translation.json`; use `t('key')`.
- Use `import type` for types (`verbatimModuleSyntax`).
- ESLint, Prettier and `tsc` are the authority; the build fails on violations.

## Testing policy

- Every new feature comes with tests, written first (TDD, see Engineering practices); code without tests is incomplete.
- Backend integration tests extend `AbstractDbTest` and use `SharedPostgresContainer`.
- Frontend: Vitest + React Testing Library for all new components, hooks and features, including edge cases and
  API error handling with proper mocks. Coverage gate: at least 70 % statements, branches, functions and lines.
- No disabled tests, placeholder tests, early returns or `if (true) return;` hacks. Every test has meaningful
  assertions or is removed.
- Run the relevant tests (and `npm run check-all` for frontend changes) before committing.

## Definition of done

- Tests written first cover the new behaviour; coverage does not drop.
- `mvn verify -pl code/backend` passes for backend changes (not just `mvn test`), `npm run check-all` for frontend
  changes.
- README or wiki (`docs/wiki`) updated when behaviour, configuration or the API changed.
- Committed as `#<issue> - <description>`, pushed to `main`, the issue closed and its project Status set to Done.
