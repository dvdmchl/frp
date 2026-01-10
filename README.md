# FRP - Family Resource Planner and Organizer

FRP is a comprehensive application designed to help families efficiently manage their day-to-day tasks and resources. It aims to simplify and streamline family life, offering all the necessary tools for a well-organized household.

## Deployment and Environments

This project uses a modular Docker Compose setup to support both development and production environments.

### Development Mode (with SonarQube)

To start the full stack including SonarQube (development tools), run:

```bash
docker compose up -d
```

Docker automatically loads `docker-compose.yml` (Core) and `docker-compose.override.yml` (Dev Tools).

### Production Mode (Core Services Only)

To run only the core application services (DB, Backend, Frontend) without development tools:

```bash
docker compose -f docker-compose.yml up -d
```

### Environment Isolation

To run multiple environments on the same host (e.g., `frp-dev` and `frp-prod`), use the `-p` (project name) flag to isolate containers and volumes:

**Dev Environment:**

```bash
docker compose -p frp-dev up -d
```

**Prod Environment:**

```bash
docker compose -p frp-prod -f docker-compose.yml up -d
```


## Features

- **!VIP! Family Management**: Add, edit, and manage family members and their roles.
- **!VIP! Calendar**: Organize family events, appointments, and reminders.
- **!VIP! Tasks**: Create and assign tasks to different family members.
- **!VIP! Shopping Lists**: Manage and share grocery lists and other shopping needs.
- **!VIP! Family Accounting**: Track and manage the family budget and expenses.
- **!VIP! Discount Cards**: Store and manage discount and membership cards.
- **!VIP! Property Management**: Keep track of family-owned assets and responsibilities.
- **!VIP! Vehicle Service Tracking**: Schedule and track maintenance, repairs, and other services for family vehicles.
- **!VIP! Energy Consumption Monitoring**: Track household energy usage.
- **!VIP! Pet and Plant Care**: Organize and schedule care routines for pets and plants.
- **Additional Modules**: Additional features can be added to accommodate any unique family needs.

## Tech Stack

- **Front-end**: React, Vite, TypeScript, Tailwind CSS, Flowbite-React
- **Back-end**: Spring Boot, Hibernate
- **Database**: PostgreSQL

## License

This project is licensed under the GNU AGPLv3.

**Note:** The name "FRP" and the project logo are trademarks and are not covered by the AGPL license. You may not use these names in a way that suggests your fork or service is an official product of, or endorsed by, the original project. The project logo and specific visual design elements are reserved and may not be used for commercial products or derivative works without explicit written permission.

By contributing to this project, you agree to the terms of the Contributor License Agreement (CLA) as described in [CONTRIBUTING.md](CONTRIBUTING.md).