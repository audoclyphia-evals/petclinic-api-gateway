# Deployment Instructions

## Overview

This document covers deployment procedures for the **PetClinic API Gateway** (`petclinic-api-gateway`), a Spring Boot microservice that acts as the central entry point for the PetClinic application. The API Gateway aggregates data from downstream services—`petclinic-customers-service`, `petclinic-visits-service`, and `petclinic-vets-service`—exposing unified REST endpoints and serving an AngularJS front-end.

The gateway handles:

- **Request aggregation** — Combines customer and visit data into a single owner details response
- **Service discovery** — Registers with Netflix Eureka for dynamic service lookup
- **Load balancing** — Distributes requests across service instances via Spring Cloud LoadBalancer
- **Circuit breaking** — Uses Resilience4j to gracefully degrade when downstream services are unavailable
- **Static asset serving** — Hosts the AngularJS single-page application

For a detailed architecture overview, see the repository documentation. For API endpoint documentation, refer to the REST controller endpoints.

## Prerequisites

The following infrastructure must be available before deploying the API Gateway:

| Dependency | Purpose | Required Version |
|---|---|---|
| Java (JDK) | Runtime | 17+ (compatible with Spring Boot 3.x parent) |
| Maven | Build tool | 3.8+ |
| Netflix Eureka Server | Service discovery | Compatible with `spring-cloud-starter-netflix-eureka-client` |
| Spring Cloud Config Server | External configuration | Compatible with `spring-cloud-starter-config` |
| Zipkin Server | Distributed tracing | Compatible with `spring-boot-starter-zipkin` |

The following downstream services must be registered and running:

- **`petclinic-customers-service`** — Provides owner and pet data (`http://customers-service/owners/{ownerId}`)
- **`petclinic-visits-service`** — Provides visit data (`http://visits-service/pets/visits`, `http://visits-service/vets/{vetId}/visits`)
- **`petclinic-vets-service`** — Provides veterinarian data (consumed by the front-end)

## Building

Build the application JAR using Maven:

```bash
# Standard build (skips tests if desired for speed)
mvn clean package -DskipTests

# Full build with tests
mvn clean package
```

The build produces a Spring Boot executable JAR at `target/spring-petclinic-api-gateway-4.0.1.jar`.

### Build Profiles

The `pom.xml` defines two optional Maven profiles:

- **`css`** — Compiles SCSS to CSS using the `libsass-maven-plugin` and unpacks Bootstrap webjar resources
- **`buildDocker`** — Executes Docker image build via `exec-maven-plugin` using a Dockerfile from the parent project's `docker/` directory

```bash
# Build with CSS compilation
mvn clean package -Pcss

# Build Docker image
mvn clean package -PbuildDocker
```

## Configuration

### Service Discovery

The application is annotated with `@EnableDiscoveryClient`, which registers it with a Eureka server. The Eureka server address is configured via Spring Cloud Config or application properties:

```yaml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

### Circuit Breaker Defaults

The gateway configures Resilience4j circuit breaker defaults in `ApiGatewayApplication.java`:

```java
@Bean
public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
    return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
        .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
        .timeLimiterConfig(TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10)).build())
        .build());
}
```

This sets a 10-second time limiter and default circuit breaker thresholds for all circuit breakers created by the `ReactiveCircuitBreakerFactory`.

Two named circuit breakers are used in the controller layer:

- **`getOwnerDetails`** — Protects the owner details aggregation call
- **`getVisitsForVet`** — Protects the vet visits call

### Downstream Service Hostnames

The `VisitsServiceClient` uses a configurable `hostname` field that defaults to the load-balanced service URL. In production with Eureka, this resolves to:

- `http://customers-service/` — via `CustomersServiceClient`
- `http://visits-service/` — via `VisitsServiceClient`

### Exposed Port

The Docker image exposes port **8081** (configured in `pom.xml` via `docker.image.exposed.port`).

## Running

### Local Development

```bash
# Run with Spring Boot Maven plugin
mvn spring-boot:run

# Or run the packaged JAR
java -jar target/spring-petclinic-api-gateway-4.0.1.jar
```

The application starts on the configured port (default: `8081`) and serves the AngularJS front-end at the root path (`/`).

### With External Configuration

When using Spring Cloud Config and Eureka:

```bash
java -jar target/spring-petclinic-api-gateway-4.0.1.jar \
  --spring.config.import=configserver:http://localhost:8888 \
  --eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

### Health and Monitoring

The gateway includes Spring Boot Actuator and Prometheus metrics:

- **Health endpoint**: `GET /actuator/health`
- **Prometheus metrics**: `GET /actuator/prometheus`
- **Jolokia JMX**: Available via `jolokia-core` dependency

## Docker Deployment

When using the `buildDocker` profile, a Docker image is built using the parent project's Dockerfile. The image:

- Exposes port **8081**
- Runs the Spring Boot JAR as the container entrypoint

```bash
# Build the Docker image
mvn clean package -PbuildDocker

# Run the container
docker run -p 8081:8081 \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://eureka-server:8761/eureka/ \
  spring-petclinic-api-gateway
```

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | Eureka server URL | `http://localhost:8761/eureka/` |
| `SPRING_CONFIG_IMPORT` | Spring Cloud Config server address | N/A |
| `SERVER_PORT` | Application listen port | `8081` |

## Verifying the Deployment

After startup, verify the gateway is operational:

```bash
# Check application health
curl http://localhost:8081/actuator/health

# Test owner details endpoint (requires customers-service and visits-service)
curl http://localhost:8081/api/gateway/owners/1

# Test vet visits endpoint (requires visits-service)
curl http://localhost:8081/api/gateway/vets/1/visits
```

The front-end UI should be accessible at `http://localhost:8081/`.

### Fallback Behavior

When the GenAI chat backend is unavailable, the `POST /fallback` endpoint returns a `503 Service Unavailable` response with the message:

```
Chat is currently unavailable. Please try again later.
```

Circuit breaker failures on the owner details and vet visits endpoints return an empty visits list rather than propagating the error to the client.

## Additional Documentation

- **System Architecture** — High-level system design, components, and interaction diagrams
- **API Gateway Reference** — Complete REST API endpoint documentation with request/response examples