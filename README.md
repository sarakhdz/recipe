# Culinary Compass — Recipe Management API

A Java/Spring Boot backend for managing recipes and finding recipes that match a set of
dietary/preparation criteria. There is no graphical interface — the application is a pure
HTTP JSON API.

## What the application does

- Create, retrieve, update, and delete recipes. Each recipe has a name, description,
  food/dietary category, number of servings, cooking instructions, and one or more
  ingredients (each with a quantity and unit).
- Retrieve ingredients: list all known ingredients, retrieve a single ingredient, or list
  the ingredients that belong to a specific recipe.
- Search/filter recipes by any combination of: category, number of servings, ingredients
  that must be present, ingredients that must not be present, and keywords that must
  appear in the cooking instructions. Criteria can be used independently or combined —
  combining criteria narrows the results (AND, not OR).
- Recipe data is stored in PostgreSQL and survives application restarts.
- Read operations (`GET`) are open to anyone. Write operations (`POST`/`PUT`/`DELETE`)
  require HTTP Basic authentication.

## Requirements

- Java 21 (only needed if running outside Docker — see below)
- Docker and Docker Compose (recommended way to run the app; also required to run the
  integration tests, which use Testcontainers to start a real, throwaway PostgreSQL
  instance)

## Configuration

The application takes all secrets and environment-specific values from environment
variables — nothing is hardcoded in source. A template is provided at `.env.example`:

```
POSTGRES_DB=recipe_db
POSTGRES_USER=recipe
POSTGRES_PASSWORD=change_me
APP_USERNAME=admin
APP_PASSWORD=change_me
```

Copy it to `.env` and fill in real values before running the app:

```bash
cp .env.example .env
```

`.env` is listed in `.gitignore` and must never be committed — it is only read locally by
Docker Compose to populate the containers' environment variables.

| Variable | Used by | Purpose |
|---|---|---|
| `POSTGRES_DB` | `db`, `app` | Database name |
| `POSTGRES_USER` | `db`, `app` | Database user |
| `POSTGRES_PASSWORD` | `db`, `app` | Database password |
| `APP_USERNAME` | `app` | Username required for write operations (HTTP Basic Auth) |
| `APP_PASSWORD` | `app` | Password required for write operations (HTTP Basic Auth) |

## Build and run

### Option 1 — Docker Compose (recommended)

This starts both the application and a PostgreSQL database, fully networked together,
with data persisted in a named Docker volume so it survives `docker compose down`.

```bash
cp .env.example .env   # then edit .env with real values
docker compose up --build
```

The API is then available at `http://localhost:8080`.

To stop everything:
```bash
docker compose down
```
(the database contents are preserved in the `pgdata` volume and will still be there next
time you run `docker compose up`)

### Option 2 — Run locally without Docker

Requires a PostgreSQL instance reachable at the configured host, and the same environment
variables set in your shell (`DB_HOST` defaults to `localhost` if unset).

```bash
export DB_NAME=recipe_db DB_USERNAME=recipe DB_PASSWORD=change_me
export APP_USERNAME=admin APP_PASSWORD=change_me
./mvnw spring-boot:run
```

## Running the tests

```bash
./mvnw test
```

This runs both:
- **Unit and `@WebMvcTest` slice tests** — fast, no Docker required, mock the
  service/repository layers.
- **Integration tests** (`RecipeIntegrationTest`) — boot the full Spring context and use
  [Testcontainers](https://testcontainers.com/) to start a real, temporary PostgreSQL
  container automatically. **Docker must be running** on your machine for these to pass;
  the container is created and destroyed automatically per test run, entirely separate
  from the `docker compose` containers used to actually run the app.

## API usage

All endpoints return JSON. Interactive examples below use `curl`; a Basic Auth header is
required only for write operations, using the `APP_USERNAME`/`APP_PASSWORD` values from
your `.env`.

### Recipes

**Create a recipe** — `POST /recipes` (requires auth)
```bash
curl -X POST http://localhost:8080/recipes \
  -H "Content-Type: application/json" \
  -u admin:change_me \
  -d '{
    "name": "Tomato Soup",
    "description": "A simple soup",
    "category": "soup",
    "servings": 4,
    "instructions": "Boil water and simmer tomatoes",
    "ingredients": [
      {"name": "tomato", "quantity": 4, "unit": "pcs"},
      {"name": "water", "quantity": 1, "unit": "liter"}
    ]
  }'
```
Returns `201 Created` with the created recipe, including its generated `id`.

**Get a recipe by id** — `GET /recipes/{id}` (no auth required)
```bash
curl http://localhost:8080/recipes/1
```
Returns `404 Not Found` if the id doesn't exist.

**Update a recipe** — `PUT /recipes/{id}` (requires auth)
```bash
curl -X PUT http://localhost:8080/recipes/1 \
  -H "Content-Type: application/json" \
  -u admin:change_me \
  -d '{
    "name": "Tomato Soup (updated)",
    "description": "A simple soup",
    "category": "soup",
    "servings": 6,
    "instructions": "Boil water and simmer tomatoes for longer",
    "ingredients": [
      {"name": "tomato", "quantity": 6, "unit": "pcs"},
      {"name": "water", "quantity": 1, "unit": "liter"}
    ]
  }'
```
The request body replaces the recipe's fields and ingredient list entirely.

**Delete a recipe** — `DELETE /recipes/{id}` (requires auth)
```bash
curl -X DELETE http://localhost:8080/recipes/1 -u admin:change_me
```
Returns `204 No Content`. Returns `404 Not Found` if the id doesn't exist.

**Search / list recipes** — `GET /recipes` (no auth required, paginated)
```bash
curl "http://localhost:8080/recipes?category=soup&servings=4&include=tomato&exclude=nuts&keywords=simmer&page=0&size=20"
```
All query parameters are optional and can be combined freely:

| Parameter | Type | Meaning |
|---|---|---|
| `category` | string | Recipe's food/dietary category must match exactly (case-insensitive) |
| `servings` | integer | Recipe's number of servings must match exactly |
| `include` | string, repeatable | Recipe must contain **every** listed ingredient |
| `exclude` | string, repeatable | Recipe must contain **none** of the listed ingredients |
| `keywords` | string, repeatable | Recipe's instructions must contain **every** listed word |
| `page`, `size` | integer | Standard pagination (default `size=20`, `page=0`) |

Repeat a parameter to pass multiple values, e.g. `?include=tomato&include=basil`.
Omitting all parameters returns every recipe, paginated. A search that matches nothing
returns `200 OK` with an empty `content` array — not an error. An invalid value (e.g.
`?servings=abc`) returns `400 Bad Request` with a structured error body, not a server
error.

**Get a recipe's ingredients** — `GET /recipes/{id}/ingredients` (no auth required)
```bash
curl http://localhost:8080/recipes/1/ingredients
```

### Ingredients

**List all ingredients** — `GET /ingredients` (no auth required)
```bash
curl http://localhost:8080/ingredients
```

**Get a single ingredient** — `GET /ingredients/{id}` (no auth required)
```bash
curl http://localhost:8080/ingredients/1
```
Returns `404 Not Found` if the id doesn't exist.

### Error responses

All errors share a consistent shape:
```json
{
  "errorCode": "NOT_FOUND",
  "message": "Recipe with id 999 not found",
  "details": [],
  "timestamp": "2026-09-29T12:00:00Z"
}
```
`errorCode` is a stable machine-readable string (`NOT_FOUND`, `VALIDATION_ERROR`,
`INVALID_REQUEST`, `INTERNAL_ERROR`); `message` is human-readable; `details` holds a list
of field-level messages for validation errors. Unexpected server errors are logged
server-side and never leak stack traces or internal details to the client.

## Key decisions and trade-offs

- **PostgreSQL + Spring Data JPA with `Specification`**: recipe search needs an arbitrary,
  optional combination of filters. `Specification` composes each filter independently and
  combines only the ones actually supplied (`Specification.allOf`, after filtering out
  unused criteria), rather than writing one large query per possible parameter
  combination.
- **Ingredients as a shared, deduplicated entity**: an ingredient (e.g. "tomato") is stored
  once and referenced by every recipe that uses it, via a join entity (`RecipeIngredient`)
  that also carries the per-recipe quantity and unit. This avoids duplicate ingredient rows
  and lets `GET /ingredients` return a clean, deduplicated list.
- **HTTP Basic Auth, single configured user**: the assignment requires only that
  data-changing operations be restricted to authorized users — it does not call for
  multiple users, roles, or self-service registration. A single admin credential pair,
  sourced from environment variables, satisfies this simply. `GET` endpoints are
  deliberately left open, since read access isn't a data-changing operation.
- **`ddl-auto: update`**: acceptable for this assignment's scope; a production system would
  use versioned migrations (e.g. Flyway/Liquibase) instead, to make schema changes
  reviewable and reversible.
- **Docker Compose with a named volume**: the app and database run as separate containers
  networked together; PostgreSQL's data directory is mounted to a named volume (`pgdata`)
  so data survives `docker compose down`, not just an application restart.

## Assumptions and limitations

- Single hardcoded admin credential — no multi-user support, roles, or permission levels.
- No rate limiting or account lockout on authentication attempts.
- `depends_on` in `docker-compose.yml` waits for the database container to *start*, not for
  PostgreSQL to be *ready* to accept connections; a slow-starting database could cause a
  transient connection failure on the very first `docker compose up`. A production setup
  would add a proper `healthcheck`.
- Category matching is an exact (case-insensitive) match, not a fuzzy or hierarchical
  match (e.g. "soup" won't match "soups").
