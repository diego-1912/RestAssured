# API Test Framework

A RestAssured-based test framework covering **REST**, **GraphQL**, and **WebSocket** APIs, built on Java 17 + JUnit 5 + Maven.

## Stack

| Concern | Library |
|---|---|
| Build / runner | Maven, JUnit 5 |
| REST | RestAssured 6.x |
| GraphQL | RestAssured (POST + `{data, errors}` envelope parsing) |
| WebSocket | Java-WebSocket |
| JSON | Jackson |
| Config | SnakeYAML |
| Test data | DataFaker |
| Assertions | Hamcrest, JSON Schema Validator |
| Polling/retry | Awaitility, JUnit Pioneer (`@RetryingTest`) |
| Reporting | Allure, SLF4J + Logback |

## Project layout

```
src/test/java/
├── config/          ConfigManager, Environment, EnvironmentConfig
├── clients/
│   ├── rest/        service-object REST clients, auth providers, filters
│   ├── graphql/      GraphQLClient, query loader, schema introspection, assertions
│   └── websocket/    WebSocketTestClient + factory
├── models/          POJOs shared across REST/GraphQL (Post, User)
│   └── graphql/      {data, errors} envelope + per-query result wrappers
├── specs/           RequestSpec/ResponseSpec factories
├── utils/           data generation, polling/retry, correlation IDs, DB verification
├── tests/
│   ├── rest/
│   ├── graphql/
│   ├── websocket/
│   └── flows/        cross-cutting (correlation id + polling) demo
└── resources/
    ├── config/       dev.yaml / qa.yaml / staging.yaml / prod.yaml
    ├── graphql/       .graphql query/mutation files
    └── schemas/       JSON Schema files
```

The example clients/tests target public sandboxes so the suite runs out of the box:
REST → `jsonplaceholder.typicode.com`, GraphQL → `graphqlzero.almansi.me/api`, WebSocket → `wss://ws.postman-echo.com/raw`.
Point `resources/config/*.yaml` at your own services to reuse the framework for a real project.

## What each class does, and how they talk to each other

Think of this framework as a few layers stacked on top of each other. Each layer only
talks to the layer directly below it, so a test never has to know about YAML files or
HTTP filters, and a client never has to know which test is calling it.

```
tests/*              <- what you read when a test fails ("business" checks)
clients/, specs/      <- "how do I call this API" (one class per concern)
config/               <- "which environment, which URL, which secret"
utils/                <- small helpers shared by everything above
```

### 1. Config layer - "which environment am I running against?"

- **`Environment`** - just the enum `DEV/QA/STAGING/PROD`. Reads `-Denv=qa` or the
  `ENV` variable and defaults to `DEV`.
- **`EnvironmentConfig`** - a plain data class shaped exactly like `config/<env>.yaml`
  (base URLs, timeouts, retry settings, auth settings, DB settings). It doesn't do
  anything besides hold values.
- **`ConfigManager`** - the only class that reads YAML. On first use it asks
  `Environment` which environment is active, loads the matching `EnvironmentConfig`,
  and caches it. Every other class in the framework asks `ConfigManager` for a URL,
  a timeout, or a secret instead of touching YAML or `System.getenv` directly.
  `ConfigManager.resolveSecret(...)` is how a secret's *value* is looked up, given the
  *name* of the environment variable that YAML points at (YAML itself never holds a
  real password or key).

### 2. Spec layer - "build the request/response contract once, reuse everywhere"

- **`RequestSpecFactory.restSpec()`** - assembles one ready-to-use RestAssured request
  spec for REST calls: base URL and timeouts from `ConfigManager`, then bolts on the
  filters below, then applies auth. Every `clients/rest` class calls this instead of
  building its own request - that's what keeps auth/retry/logging consistent across
  the whole suite.
- **`ResponseSpecFactory`** - common response assertions (status-code range, response
  time under the configured SLA) that REST tests attach with `.then().spec(...)`
  instead of repeating the same Hamcrest matchers in every test.
- `GraphQLClient` builds its own equivalent spec internally (GraphQL only ever POSTs
  to one endpoint, so it doesn't need a shared factory).

### 3. Filters - things that happen to *every* REST request automatically

Filters are plugged into `RequestSpecFactory` and run transparently:

- **`CorrelationIdFilter`** - stamps every outgoing REST call with the current test's
  correlation id (see `CorrelationContext` below), so you can trace one call across
  REST, GraphQL and WebSocket logs.
- **`RetryFilter`** - automatically retries idempotent calls (GET/PUT/DELETE/etc.) on
  transient failures (429/502/503/504 or I/O errors) with exponential backoff, using
  the attempt count/backoff from `EnvironmentConfig.RetryConfig`.

### 4. Auth - "how do I attach credentials to a request?"

- **`AuthenticationProvider`** - one-method interface: "take a request spec, return it
  with auth applied." Everything else in this package is an implementation of it:
  `NoAuthProvider`, `BasicAuthProvider`, `BearerTokenAuthProvider`, `ApiKeyAuthProvider`,
  `OAuth2AuthProvider` (this last one also caches the access token in memory and
  silently refreshes it before it expires).
- **`AuthProviderFactory`** - reads `auth.type` from `ConfigManager` (`NONE`, `BASIC`,
  `BEARER`, `API_KEY`, `OAUTH2`) and hands back the matching provider, with its
  secrets already resolved. `RequestSpecFactory` calls this factory so a test never
  chooses an auth strategy itself - swapping `auth.type` in the YAML is enough.

### 5. REST clients - "service objects" tests actually call

- **`BaseRestClient`** - a thin parent with `get/post/put/patch/delete` helpers that
  all go through `RequestSpecFactory.restSpec()`.
- **`PostsApiClient` / `UsersApiClient`** - one class per resource, extending
  `BaseRestClient`. Each method name describes a business action
  (`listPostsByUser`, `createPost`, `getUserAsPojo`) instead of exposing raw
  `given().when().then()` chains to the test. Tests depend on these, never on
  RestAssured directly - that's what "service-object" means here.

### 6. GraphQL - request/response envelope handling

- **`GraphQLQueryLoader`** - reads `.graphql` files from `resources/graphql` and
  caches them, so queries live as version-controlled files instead of Java strings.
- **`GraphQLClient`** - POSTs a `{query, variables, operationName}` body (built from
  the `GraphQLRequest` model) to the GraphQL endpoint, and can parse the
  `{data, errors}` response into a typed `GraphQLResponse<T>`. Because GraphQL
  servers return HTTP 200 even on failure, callers must check the envelope, not just
  the status code.
- **`GraphQLAssertions`** - ready-made checks for that envelope (`assertCleanSuccess`,
  `assertHasErrors`, `assertPartialSuccess`, ...) so tests don't hand-roll the same
  null/empty checks.
- **`SchemaIntrospector`** - optional: runs the GraphQL introspection query once,
  caches which types/fields exist, and lets a test validate a query references real
  fields before sending it - catching a typo in a `.graphql` file earlier than a
  server error would.
- **models under `models/graphql/`** - `GraphQLRequest` (outgoing envelope),
  `GraphQLResponse<T>`/`GraphQLError` (incoming envelope), and per-query result
  shapes (`PostResult`, `PostsPageResult`, `PageMeta`, `CreatePostResult`) that
  `data` gets deserialized into.

### 7. WebSocket - asynchronous push messages

- **`WebSocketTestClient`** - wraps the Java-WebSocket library. Incoming messages are
  pushed onto an internal queue instead of being handled in a callback, so a test can
  simply call `awaitMessage(timeout)` / `awaitMessageMatching(...)` / `awaitJson(...)`
  and block until something arrives (or the timeout fails the test) - the same
  request/response *feel* RestAssured gives REST calls, applied to an async protocol.
- **`WebSocketClientFactory`** - builds a `WebSocketTestClient` pointed at the
  configured URL, attaches the current correlation id as a connect header, and can
  connect with retry/backoff (`connectWithRetry()`) for resilience scenarios.

### 8. Cross-cutting utils - small helpers shared by every layer above

- **`CorrelationContext`** - a per-test-thread id. `CorrelationIdFilter` (REST),
  `GraphQLClient` (GraphQL) and `WebSocketClientFactory` (WebSocket) all read the same
  id, so one test can prove that a REST call, a GraphQL call and a WebSocket push all
  belong to the same trace.
- **`PollingUtils`** - wraps Awaitility so a test can say "keep re-reading this value
  until condition X is true, or fail after N seconds" - used when a write needs time
  to become visible (eventual consistency) or when waiting on an async event.
- **`TestDataGenerator`** - builds random-but-valid `Post`/`User` payloads with
  DataFaker, so tests don't collide on fixed IDs/emails when run repeatedly or in
  parallel.
- **`IdGenerator`** - plain UUID helpers, e.g. for idempotency-key headers.
- **`SchemaValidation`** - one-liner around RestAssured's JSON-schema-validator
  module, pointed at files in `resources/schemas`.
- **`TestDataCleanup`** - a per-test stack of cleanup actions. A test registers
  "delete what I just created" right after creating it; `BaseTest` drains the stack
  in `@AfterEach`, in reverse order, whether the test passed or failed.
- **`DbVerifier`** - optional JDBC cross-check so a test can confirm what the API
  returned actually landed in the database, using connection details from
  `ConfigManager` and credentials from environment variables (never YAML).

### 9. Tests - where it all comes together

- **`BaseTest`** - parent class for every test. Before each test it starts a fresh
  correlation id and puts it in the logging context; after each test it runs whatever
  cleanup tasks were registered with `TestDataCleanup`, then clears the correlation id.
  `PostsApiTest`, `UsersApiTest`, `PostsGraphQLTest`, `EchoWebSocketTest` all extend it.
- **`CrossCuttingUtilsTest`** (in `tests/flows`) - a worked example showing the pieces
  above cooperating: it makes a REST call and a GraphQL call in the same test and
  shows the correlation id stays identical across both, then uses `PollingUtils` to
  retry a read until a condition is met.

### Putting it together: one REST request, start to finish

1. A test calls `postsApi.getPost(1)` (`PostsApiClient` → `BaseRestClient.get(...)`).
2. `BaseRestClient` asks `RequestSpecFactory.restSpec()` for a spec.
3. `RequestSpecFactory` asks `ConfigManager` for the base URL/timeouts, asks
   `AuthProviderFactory` for the right `AuthenticationProvider`, and attaches
   `CorrelationIdFilter` + `RetryFilter`.
4. The request goes out with auth headers, a correlation-id header, and automatic
   retry-on-transient-failure baked in.
5. The test asserts on the response using `ResponseSpecFactory.ok()` (status + SLA)
   and, for JSON shape, `SchemaValidation.matchesSchema(...)`.
6. If the test created data, it registers a `TestDataCleanup` task; `BaseTest` cleans
   it up afterwards regardless of pass/fail.

## Running

```bash
mvn test                              # default env = dev
mvn test -Denv=qa                     # switch environment
mvn test -Denv=qa -Dgroups=smoke      # only @Tag("smoke")
mvn test -Dgroups=rest,graphql        # only REST + GraphQL tests
mvn test -Dtest=PostsApiTest          # a single class
```

Environments are resolved from `-Denv=<dev|qa|staging|prod>` (or the `ENV` env var), defaulting to `dev`, and loaded from `src/test/resources/config/<env>.yaml` via `ConfigManager`.

### Secrets

Nothing sensitive lives in the YAML files - each `auth:` block only names the **environment variable** holding a credential (`clientIdEnvVar`, `apiKeyEnvVar`, etc). Set the actual values before running:

```bash
export API_CLIENT_ID=...
export API_CLIENT_SECRET=...
```

`ConfigManager.resolveSecret(...)` fails fast with a clear error if a required variable is unset. `auth.type` in each YAML selects `NONE | BASIC | BEARER | OAUTH2 | API_KEY`; `OAuth2AuthProvider` caches and auto-refreshes the access token.

## Tags

`smoke`, `regression`, `rest`, `graphql`, `websocket` - combine via `-Dgroups=`. Parallel execution is on by default (methods concurrent, classes same-thread; see `src/test/resources/junit-platform.properties`).

## Reports

```bash
mvn test
mvn io.qameta.allure:allure-maven:serve
```

## CI

`.github/workflows/api-tests.yml` runs a `{dev, qa} x {smoke, regression}` matrix on every push/PR and uploads Surefire + Allure artifacts.

## Notes on RestAssured usage

Every REST/GraphQL client obtains its spec via `RequestSpecFactory.restSpec()` / the private `GraphQLClient.spec()`, both of which wrap the built `RequestSpecification` in `RestAssured.given().spec(...)` before firing a request. Calling an HTTP verb directly on a spec built by `RequestSpecBuilder` trips a known RestAssured/Groovy bug ([rest-assured/rest-assured#938](https://github.com/rest-assured/rest-assured/issues/938)) - don't bypass these factories.
# RestAssured
