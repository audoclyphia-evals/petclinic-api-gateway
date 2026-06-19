# Contributing Guidelines

## Development

### Prerequisites

- **Java** runtime (required by Spring Boot)
- **Maven** build tool
- A running **Spring Cloud** infrastructure for full integration testing (Eureka service discovery, downstream microservices: `petclinic-customers-service`, `petclinic-visits-service`, `petclinic-vets-service`)

### Setting Up

Clone the repository and build the project:

```bash
git clone https://github.com/audoclyphia-evals/petclinic-api-gateway.git
cd petclinic-api-gateway
mvn clean install
```

### Building

The project uses Maven with two additional build profiles:

| Profile | Command | Purpose |
|---------|---------|---------|
| Default | `mvn clean install` | Standard compilation and test execution |
| `buildDocker` | `mvn clean install -PbuildDocker` | Builds the application and creates a Docker image (exposed port `8081`) |
| `css` | `mvn clean install -Pcss` | Compiles SCSS to CSS using Bootstrap Webjar sources via `libsass-maven-plugin` |

The `css` profile unpacks the Bootstrap Webjar and compiles SCSS files from `src/main/resources/static/scss/` into `src/main/resources/static/css/`.

### Project Layout

```
src/
├── main/
│   ├── java/org/springframework/samples/petclinic/api/
│   │   ├── ApiGatewayApplication.java       # Application entry point & bean config
│   │   ├── application/                      # Service clients (CustomersServiceClient, VisitsServiceClient)
│   │   ├── boundary/web/                     # REST controllers (ApiGatewayController, FallbackController)
│   │   └── dto/                              # Data transfer objects (OwnerDetails, PetDetails, Visits, etc.)
│   └── resources/static/                     # AngularJS frontend (scripts/, index.html)
└── test/java/org/springframework/samples/petclinic/api/
    ├── ApiGatewayApplicationTests.java
    ├── application/                          # Integration tests for service clients
    └── boundary/web/                         # Controller unit tests & test configurations
```

### Key Technical Decisions

- **Reactive HTTP**: Service clients (`CustomersServiceClient`, `VisitsServiceClient`) use Spring WebFlux `WebClient` returning `Mono` types.
- **Circuit Breaking**: Resilience4j circuit breakers are configured via `defaultCustomizer` in `ApiGatewayApplication` with a 10-second timeout. The `ApiGatewayController` wraps service calls in circuit breakers with fallback to empty results.
- **Service Discovery**: The application uses Netflix Eureka for service discovery. Service clients reference downstream services by logical names (e.g., `http://customers-service/owners/{ownerId}`).
- **Load Balancing**: Both `RestTemplate` and `WebClient.Builder` beans are annotated with `@LoadBalanced`.
- **DTOs**: Immutable records with builder patterns (`OwnerDetailsBuilder`, `PetDetailsBuilder`) using fluent APIs.

---

## Testing

### Running Tests

Execute the full test suite:

```bash
mvn test
```

### Test Structure

All test classes reside under `src/test/java/org/springframework/samples/petclinic/api/`:

| Test Class | Location | Description |
|------------|----------|-------------|
| `ApiGatewayApplicationTests` | `ApiGatewayApplicationTests.java` | Verifies that the Spring application context loads successfully |
| `ApiGatewayControllerTest` | `boundary/web/ApiGatewayControllerTest.java` | Unit tests for `ApiGatewayController` using `@WebFluxTest` with mocked service clients |
| `VisitsServiceClientIntegrationTest` | `application/VisitsServiceClientIntegrationTest.java` | Integration test for `VisitsServiceClient` using MockWebServer to verify HTTP communication and response parsing |
| `CircuitBreakerConfiguration` | `boundary/web/CircuitBreakerConfiguration.java` | Test support configuration defining circuit breaker, time limiter, bulkhead registries and Resilience4J properties |

### Test Dependencies

- **JUnit Jupiter** (`junit-jupiter-api`, `junit-jupiter-engine`) — test framework
- **Spring Boot Test** — application context and integration testing
- **Spring Boot WebFlux Test** (`spring-boot-starter-webflux-test`) — reactive controller testing
- **OkHttp3 MockWebServer** (`mockwebserver3-junit5`) — HTTP mock server for service client integration tests

### Writing New Tests

**Controller tests** use `@WebFluxTest` and mock service clients:

```java
@WebFluxTest(controllers = ApiGatewayController.class)
class ApiGatewayControllerTest {
    @MockBean
    private CustomersServiceClient customersServiceClient;
    
    @MockBean
    private VisitsServiceClient visitsServiceClient;
    
    @MockBean
    private ReactiveCircuitBreakerFactory cbFactory;
    
    @Autowired
    private WebTestClient webTestClient;
    
    // Test methods...
}
```

Service clients under test are mocked (e.g., `CustomersServiceClient`, `VisitsServiceClient`) while circuit breaker infrastructure is provided by the test `CircuitBreakerConfiguration`.

**Integration tests** for service clients use OkHttp's `MockWebServer` to simulate downstream service responses, verifying:

- Correct HTTP method and URI construction
- Response deserialization into DTOs (`Visits`, `OwnerDetails`)
- Query parameter encoding (e.g., comma-separated pet IDs via `joinIds`)

**Context load tests** verify that the full Spring Boot application context boots without errors:

```java
@SpringBootTest
class ApiGatewayApplicationTests {
    @Test
    void contextLoads() {
    }
}
```

### Conventions

- Test classes follow the naming pattern `{ClassUnderTest}Test` for unit tests and `{ClassUnderTest}IntegrationTest` for integration tests.
- Test classes are placed in the same package structure as the production code they test.
- Use `@MockBean` for service client dependencies in controller tests.
- Use `assertVisitDescriptionEquals` (or similar assertion helpers) when verifying visit data in integration tests.