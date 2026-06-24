# Contributing Guidelines

Guidelines for developing, testing, and contributing to the PetClinic API Gateway.

The PetClinic API Gateway is a Spring Boot application that acts as an API gateway for the Spring PetClinic microservices architecture. It aggregates data from downstream microservices (customers-service, visits-service, vets-service) and serves an AngularJS frontend for managing owners, pets, and visits. This document covers the development workflow, testing procedures, and contribution standards for the gateway module.

## Development

### Prerequisites

- **Java** (version specified by the parent POM `spring-petclinic-microservices` 4.0.1)
- **Apache Maven** — build and dependency management
- **Node/npm** — only if modifying SCSS styles (for the `css` profile)

### Setup

1. **Clone the repository** and ensure the parent POM (`spring-petclinic-microservices`) is available in the local Maven repository or accessible from the configured repository.

2. **Build the project:**

   ```bash
   mvn clean install
   ```

3. **Run the application:**

   ```bash
   mvn spring-boot:run
   ```

   The gateway starts on port **8081** by default (configured via `docker.image.exposed.port`).

4. **Compile CSS (optional):**

   The `css` profile compiles SCSS to CSS using Bootstrap webjars:

   ```bash
   mvn generate-resources -Pcss
   ```

### Project Structure

The backend code is located under `src/main/java/org/springframework/samples/petclinic/api/`:

```
src/main/java/org/springframework/samples/petclinic/api/
├── ApiGatewayApplication.java          # Application entry point and configuration
├── application/
│   ├── CustomersServiceClient.java     # WebClient for customers-service (owner search + details)
│   ├── VetsServiceClient.java          # WebClient for vets-service (vet details + listing)
│   └── VisitsServiceClient.java        # WebClient for visits-service
├── boundary/web/
│   ├── ApiGatewayController.java       # REST controller (owner details, owner search, vet details, vet visits)
│   └── FallbackController.java         # POST /fallback endpoint (503)
└── dto/
    ├── OwnerDetails.java               # Owner data transfer object + builder
    ├── OwnerSummary.java               # Lightweight owner summary with pet and visit counts
    ├── PetDetails.java                 # Pet data transfer object + builder
    ├── PetType.java                    # Pet type record
    ├── VetDetails.java                 # Vet data transfer object with specialties
    ├── VisitDetails.java               # Visit data transfer object
    └── Visits.java                     # Collection wrapper for VisitDetails
```

The frontend (AngularJS application) is located under `src/main/resources/static/`:

```
src/main/resources/static/
├── index.html                          # Main entry page for the Angular SPA
└── scripts/
    ├── app.js                          # Main AngularJS application module configuration
    ├── fragments/                      # Common HTML fragments (footer, nav, welcome)
    ├── genai/                          # Chat functionality (chat.js)
    ├── infrastructure/                 # Infrastructure services (HTTP error handling)
    ├── owner-details/                  # Owner details module (component, controller, template, routing)
    ├── owner-form/                     # Owner form module (component, controller, template, routing)
    ├── owner-list/                     # Owner list module (component, controller, template, routing)
    ├── pet-form/                       # Pet form module (component, controller, template, routing)
    ├── vet-list/                       # Vet list module (component, controller, template, routing)
    └── visits/                         # Visits module (component, controller, template, routing)
```

### Key Architecture

- **Service Discovery:** Enabled via `@EnableDiscoveryClient` (Netflix Eureka).
- **Load-Balanced Clients:** Both `WebClient.Builder` and `RestTemplate` are annotated with `@LoadBalanced` for service-to-service calls.
- **Circuit Breaker:** Resilience4J is configured with a default 10-second timeout. The `ApiGatewayController` wraps calls to downstream services with reactive circuit breakers.
- **Service Clients:** `CustomersServiceClient`, `VisitsServiceClient`, and `VetsServiceClient` each use a `WebClient.Builder` to make reactive HTTP calls to their respective downstream services. The controller aggregates data across these services (e.g., enriching owner details with visits, or owner search results with visit counts).
- **Static Resources:** A `RouterFunction` bean serves the Angular SPA from `classpath:/static/`.

### Adding New Frontend Modules

Each frontend feature follows the same AngularJS module pattern:

1. Create a directory under `src/main/resources/static/scripts/<module-name>/`.
2. Define the module file (e.g., `<module-name>.js`) with `ui.router` configuration.
3. Define a component file linking the template and controller.
4. Define the controller with `$http` and `$state` dependencies.
5. Define the HTML template.
6. Register the module in `app.js`.

## Testing

### Running Tests

```bash
# Run all tests
mvn test

# Run tests for a specific class
mvn test -Dtest=ApiGatewayControllerTest

# Run integration tests
mvn test -Dtest=VisitsServiceClientIntegrationTest
```

### Test Structure

Tests are located under `src/test/java/org/springframework/samples/petclinic/api/` and follow the same package structure as the main code.

| Test Class | Purpose | Approach |
|---|---|---|
| `ApiGatewayApplicationTests` | Verifies the Spring context loads without configuration errors | Application context smoke test |
| `ApiGatewayControllerTest` | Validates gateway controller behavior under normal and error conditions | Mocked `CustomersServiceClient`, `VisitsServiceClient`, and `VetsServiceClient` |
| `VisitsServiceClientIntegrationTest` | Tests the visits service client's ability to fetch visits under available service conditions | `MockWebServer` (OkHttp) to simulate the visits-service |
| `CircuitBreakerConfiguration` | Provides Resilience4J beans (circuit breaker, time limiter, bulkhead) for test contexts | Test configuration class |

### Writing New Tests

- **Unit tests** for controllers should mock all service clients using the existing `ApiGatewayControllerTest` pattern. The controller injects `CustomersServiceClient`, `VisitsServiceClient`, and `VetsServiceClient`, so all three should be mocked.
- **Integration tests** for service clients should use `MockWebServer` to simulate HTTP responses from downstream services, as demonstrated in `VisitsServiceClientIntegrationTest`.
- Circuit breaker behavior in tests can use the `CircuitBreakerConfiguration` class for bean provisioning.
- Test assertions use JUnit 5 (`junit-jupiter-api` and `junit-jupiter-engine`).

### Test Dependencies

- **JUnit 5** — test framework
- **OkHttp MockWebServer** (`mockwebserver3-junit5`) — simulates HTTP services
- **Spring Boot Test** and **WebFlux Test** starters — context loading and reactive testing

## Contributing

### Workflow

1. Fork the repository and create a feature branch from `main`.
2. Implement changes following the project's coding conventions.
3. Add or update tests to cover new functionality or bug fixes.
4. Run the full test suite (`mvn test`) and ensure all tests pass.
5. Submit a pull request with a clear description of the changes.

### Code Style

- **Java:** Follow standard Spring Boot conventions. Use records for DTOs (as seen in `VetDetails`, `PetType`). Apply the builder pattern for complex object construction (as seen in `OwnerDetails`, `PetDetails`).
- **JavaScript:** Follow AngularJS patterns used in the existing modules. Each feature should be a self-contained module with its own component, controller, template, and route configuration.
- **HTML Templates:** Use AngularJS directives and Bootstrap 5 for styling.

### Commit Messages

- Use imperative mood (e.g., "Add circuit breaker to vet visits endpoint").
- Keep the subject line under 72 characters.
- Reference related issues where applicable.

### Reporting Issues

Report issues through the project's issue tracker. Include steps to reproduce, expected behavior, actual behavior, and relevant environment details (Java version, Maven version, OS).