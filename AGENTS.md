# Repository Guidelines

## Project Structure & Module Organization

This repository is the Spring Boot backend for QuickClock.

- `src/main/java/br/com/pabelis/quickclock/domain/` contains domain records, entities, and rules.
- `src/main/java/br/com/pabelis/quickclock/application/` contains use cases and ports.
- `src/main/java/br/com/pabelis/quickclock/adapters/` contains inbound and outbound adapters, such as HTTP controllers and persistence.
- `src/main/resources/` contains Spring configuration.
- `src/test/java/` contains unit and integration tests.
- `PROJECT_CONTEXT.md` contains shared product rules.

Keep business rules out of controllers. Controllers should call application use cases.

## Build, Test, and Development Commands

Use Maven from the repository root:

- `mvn test` - run automated tests.
- `mvn spring-boot:run` - run the API locally.
- `mvn package` - build the executable jar.
- `docker build -t quickclock-backend .` - build the container image.
- `docker run --rm -p 8080:8080 quickclock-backend` - run the API container.

Health check:

```bash
curl http://localhost:8080/api/health
```

## Coding Style & Naming Conventions

Use Java 21 and Spring Boot. Follow hexagonal architecture:

- Domain classes must not depend on Spring.
- Application services coordinate use cases.
- Adapters handle HTTP, persistence, and external tools.

Use `PascalCase` for classes, `camelCase` for methods and fields, and package names in lowercase. Prefer small classes with explicit names, for example `CreateCompanyUseCase` or `WorkDayController`.

## Testing Guidelines

Use JUnit 5 and Spring Boot Test. Write tests with each behavior change.

Prefer fast unit tests for domain and application rules. Use Spring MVC tests for controllers and full context tests only when wiring matters.

Test names should describe behavior, for example `returnsOkStatus` or `rejectsFutureWorkDay`.

## Commit & Pull Request Guidelines

Use Conventional Commits with a scope and simple English:

- `feat(api): add health endpoint`
- `test(workday): cover future date rejection`
- `docs(readme): add container commands`

Each issue should use its own branch, for example `issue-1-spring-api-foundation`. Keep commits small and clear. This project is not in production yet, so completed issue branches can be merged into `main` after verification.

## Agent-Specific Instructions

Before editing, inspect the current repository state. Keep changes scoped to the active issue and do not remove user-created files unless explicitly requested.

Keep `PROJECT_CONTEXT.md` synchronized with the related QuickClock projects:

- Flutter reference app: `/mnt/c/projetos/ponto-eletronico`
- Spring backend: `/mnt/z/Spring/QuickClock`
- React PWA frontend: `/mnt/z/react/QuickClock`

If product rules or architecture context change in one path, update the same context in the other paths during the same work.
