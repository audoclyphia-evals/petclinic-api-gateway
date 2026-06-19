# project_overview

The `petclinic-api-gateway` is a Spring Boot microservice that acts as the centralized API gateway for the PetClinic microservices architecture. It aggregates data from downstream backend services, serves the AngularJS-based single-page application frontend, and provides resilience mechanisms such as circuit breakers and time limiters to ensure fault tolerance.

The gateway communicates with three sibling services via service discovery:

- **petclinic-customers-service** — provides owner and pet data
- **petclinic-visits-service** — provides visit records
- **petclinic-vets-service** — provides veterinarian data

The gateway orchestrates calls across these services, combining results into unified responses for the frontend. It also hosts a generative AI chat interface with a fallback controller for circuit breaker failures.

### Key Components

| Component | Purpose |
|---|---|
| `ApiGatewayApplication` | Spring Boot entry point; configures load balancing, circuit breaker defaults, and static resource routing |
| `ApiGatewayController` | REST controller exposing gateway endpoints that aggregate customer and visit data |
| `CustomersServiceClient` | WebClient-based client for retrieving owner details from the customers-service |
| `VisitsServiceClient` | WebClient-based client for retrieving visits from the visits-service |
| `FallbackController` | Returns a service-unavailable response for chat requests when circuit breakers trip |
| `OwnerDetails` / `PetDetails` / `Visits` | Data transfer objects exchanged between gateway and services |
| AngularJS Frontend | Single-page application modules for owner, pet, vet, and visit views served from `static/` |

# development

### Prerequisites

- **Java** (JDK 17 or later — consistent with Spring Boot 3.x and Spring Cloud)
- **Apache Maven** 3.x
- Access to the parent POM at `org.springframework.samples:spring-petclinic-microservices:4.0.1`
- A running **Netflix Eureka** service discovery server (required for `@EnableDiscoveryClient`)
- Downstream services registered with Eureka: `customers-service`, `visits-service`, `vets-service`

### Building

```bash
# Full build including tests
mvn clean package

# Skip tests during build
mvn clean package -DskipTests

# Build the Docker image (requires 'buildDocker' profile)
mvn clean package -PbuildDocker
```

### Running Locally

```bash
# Start the API Gateway (default port 8081)
mvn spring-boot:run
```

The application requires a running Eureka server and downstream services to function. Without them, service client calls will fail.

### Running Tests

```bash
# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=ApiGatewayApplicationTests
mvn test -Dtest=ApiGatewayControllerTest
mvn test -Dtest=VisitsServiceClientIntegrationTest
```

The test suite includes:

- **ApiGatewayApplicationTests** — verifies the Spring application context loads successfully
- **ApiGatewayControllerTest** — uses `@WebFluxTest` to test gateway REST endpoints with mocked service clients
- **VisitsServiceClientIntegrationTest** — uses a mock web server (OkHttp MockWebServer) to verify HTTP communication and response parsing with the visits-service

### CSS Compilation

A `css` Maven profile is available for compiling SCSS to CSS using Bootstrap source files:

```bash
mvn generate-resources -Pcss
```

This unpacks Bootstrap webjar SCSS sources and compiles them via the `libsass-maven-plugin`.

### Configuration

The gateway uses the following Spring Cloud components:

| Dependency | Purpose |
|---|---|
| `spring-cloud-starter-netflix-eureka-client` | Service discovery registration and lookup |
| `spring-cloud-starter-circuitbreaker-reactor-resilience4j` | Reactive circuit breaker support |
| `spring-cloud-starter-config` | Externalized configuration via Spring Cloud Config |
| `spring-cloud-starter-gateway-server-webflux` | WebFlux-based gateway server |

### Key Dependencies

| Dependency | Version | Purpose |
|---|---|---|
| `angularjs` (WebJar) | 1.8.3 | Frontend UI framework |
| `angular-ui-router` (WebJar) | 1.0.30 | Client-side routing |
| `bootstrap` (WebJar) | 5.3.3 | CSS framework |
| `font-awesome` (WebJar) | 4.7.0 | Icon library |
| `marked` (WebJar) | 14.1.2 | Markdown rendering for chat |
| `okhttp3` + `mockwebserver3-junit5` | 5.0.0-alpha.14 | Test HTTP client and mock server |
| `resilience4j-micrometer` | — | Circuit breaker metrics |
| `micrometer-registry-prometheus` | — | Prometheus metrics export |
| `caffeine` | — | Local caching |