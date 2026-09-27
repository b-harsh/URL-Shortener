# Distributed URL Shortener

A backend URL-shortening service built with Java and Spring Boot. The application supports URL creation, redirection, user authentication, URL management, caching, rate limiting, and application metrics.

## Tech Stack

* **Language:** Java 21

* **Framework:** Spring Boot

* **Database:** PostgreSQL

* **Caching and Rate Limiting:** Redis

* **Authentication:** Spring Security, JWT, BCrypt

* **Database Migrations:** Flyway

* **Monitoring:** Spring Boot Actuator, Micrometer

* **Build Tool:** Maven

* **Containerization:** Docker and Docker Compose (in progress)

## Features

### URL Shortening

* Create short URLs from valid HTTP and HTTPS URLs.

* Generate short codes using Base62 encoding.

* Support optional URL expiration.

* Validate submitted URLs before storing them.

* Redirect short URLs to their original destinations.

* Return appropriate errors for invalid, inactive, expired, or missing URLs.

### Authentication and Authorization

* User registration with BCrypt password hashing.

* JWT-based authentication.

* Authenticated URL creation and management.

* Ownership-based access control for user URLs.

* Prevent users from managing URLs belonging to other users.

### URL Management

* View details of a short URL.

* Deactivate and reactivate URLs.

* Retrieve a user's URLs with pagination.

* Validate pagination parameters.

### Redis Caching

* Cache original URLs for faster redirect lookups.

* Apply time-to-live values to cached entries.

* Evict cached URLs when their active status changes.

### Rate Limiting

* Limit URL creation requests per authenticated user.

* Track repeated failed login attempts using Redis.

* Return rate-limit responses when configured thresholds are exceeded.

### Monitoring and Observability

* Spring Boot Actuator health and metrics endpoints.

* Custom metrics for:

  * URL creations

  * Redirect lookups

  * URL creation rate-limit rejections

* Request correlation IDs in HTTP responses and application logs.

### Database

* PostgreSQL persistence.

* Flyway database migrations.

* Database constraints and indexes for URL storage and user relationships.

## Project Structure

```
distributed-url-shortener/
├── src/
│   ├── main/
│   │   ├── java/com/harsh/shortener/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   └── resources/
│   │       ├── db/migration/
│   │       └── application.properties
│   └── test/
├── docker/
│   └── docker-compose.yml
├── Dockerfile
├── .dockerignore
├── pom.xml
└── README.md
```

The exact package and file layout may evolve as the project develops.

## Prerequisites

To run the application locally, install:

* Java 21

* Maven, or use the included Maven wrapper

* PostgreSQL

* Redis

## Configuration

The application reads its configuration from `src/main/resources/application.properties`.

The default local configuration expects:

| Service                 | Default address  |
| ----------------------- | ---------------- |
| PostgreSQL              | `localhost:5432` |
| Redis                   | `localhost:6379` |
| Spring Boot application | `localhost:8080` |

Configure the PostgreSQL database and Redis instance before starting the application.

The application supports environment-variable overrides for database connection settings, Redis host and port, and the JWT secret.

Do not use development credentials or a default JWT secret in production. Supply a strong secret through an environment variable and keep it out of source control.

## Running Locally

### 1. Start PostgreSQL and Redis

Make sure PostgreSQL and Redis are running and that the PostgreSQL database matches the configured connection settings.

### 2. Run database migrations and tests

From the project root:

```
.\mvnw.cmd test
```

Flyway applies the database migrations when the application starts.

### 3. Start the application

```
.\mvnw.cmd spring-boot:run
```

The application starts on:

```
http://localhost:8080
```

## API Overview

The API uses JSON request and response bodies. Protected endpoints require a valid JWT access token.

| Method | Endpoint                              | Description                        |
| ------ | ------------------------------------- | ---------------------------------- |
| POST   | `/api/v1/auth/register`               | Register a user                    |
| POST   | `/api/v1/auth/login`                  | Authenticate and obtain a token    |
| POST   | `/api/v1/urls`                        | Create a short URL                 |
| GET    | `/r/{shortCode}`                      | Redirect to the original URL       |
| GET    | `/api/v1/urls/{shortCode}`            | Get URL details                    |
| PATCH  | `/api/v1/urls/{shortCode}/deactivate` | Deactivate a URL                   |
| PATCH  | `/api/v1/urls/{shortCode}/reactivate` | Reactivate a URL                   |
| GET    | `/api/v1/urls`                        | List the authenticated user's URLs |

The exact request and response schemas are defined in the application DTOs and controllers.

## Monitoring

Spring Boot Actuator exposes health and metrics endpoints.

```
GET /actuator/health
GET /actuator/info
GET /actuator/metrics
```

Custom application metrics include URL creation counts, redirect lookups, and URL creation rate-limit rejections.

## Testing

The project includes automated tests for application behavior, authentication, URL ownership, and rate limiting.

Run the test suite with:

```
.\mvnw.cmd test
```

## Docker

Docker Compose configuration is being developed to run the application with PostgreSQL and Redis.

The containerized setup is not yet considered complete. Follow the repository's Docker configuration and project updates for the latest status.

## Future Improvements

* Complete and verify the Docker Compose deployment.

* Add continuous integration to run tests on pushes and pull requests.

* Expand automated tests for cache invalidation and failed-login throttling.

* Add API documentation and request examples.

* Evaluate database query plans and indexes under representative workloads.

* Add load testing and document measured performance.
