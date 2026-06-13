# API Gateway Reference

Microservices API gateway for the PetClinic application, aggregating data from multiple backend services.

![Tests](https://img.shields.io/badge/tests-passing-brightgreen.svg)

The `petclinic-api-gateway` module serves as the central entry point for the PetClinic microservices architecture. It aggregates data from the customers, visits, and vets services, applying circuit breaker patterns for fault tolerance and serving an AngularJS-based single-page application for owner, pet, visit, and veterinarian management.

## Overview

The API Gateway acts as a unified facade over the PetClinic microservices ecosystem. It performs server-side data aggregation, combines responses from multiple downstream services into composite responses, and applies resilience patterns to handle partial service failures gracefully.

The gateway sits between the client (browser-based AngularJS application) and three backend microservices:

- **petclinic-customers-service** — manages owners, pets, and related CRUD operations
- **petclinic-visits-service** — manages visit records for pets
- **petclinic-vets-service** — provides the list of veterinarians

```mermaid
flowchart TB
    %% Client Tier - Angular Frontend served from static resources
    %% Source: src/main/resources/static/index.html, app.js (Cluster_33, Cluster_8)
    client([Angular Frontend]) -->|HTTP| gateway
    
    subgraph Gateway_Tier [Gateway Tier]
        %% Main application entry point
        %% Source: ApiGatewayApplication.java (Cluster_16, Context #1)
        gateway[API Gateway Application]
        
        %% Service clients for downstream communication
        %% Source: CustomersServiceClient.java, VisitsServiceClient.java (Cluster_10, Context #1)
        customersClient[Customers Service Client]
        visitsClient[Visits Service Client]
        
        %% Fallback endpoint for service unavailability
        %% Source: FallbackController.java (Cluster_4)
        fallback[Fallback Controller]
        
        %% Circuit breaker configuration
        %% Source: Resilience4j config in ApiGatewayApplication.java (Cluster_12)
        circuitbreaker{{Circuit Breaker}}
        
        %% Load balancer configuration  
        %% Source: @LoadBalanced annotations (Cluster_16)
        loadbalancer[Load Balancer]
    end
    
    subgraph Service_Tier [Service Tier]
        %% Downstream microservices (inferred from service clients)
        %% Source: Service client hostnames (Cluster_10)
        customersService[Customers Service]
        visitsService[Visits Service]
    end
    
    subgraph Infrastructure_Tier [Infrastructure Tier]
        %% Service discovery registry
        %% Source: @EnableDiscoveryClient in ApiGatewayApplication (Cluster_16)
        discovery[(Service Discovery)]
    end
    
    %% Internal gateway relationships
    gateway -->|Uses| customersClient
    gateway -->|Uses| visitsClient
    gateway -->|Provides| fallback
    gateway -->|Registers with| discovery
    gateway -->|Configures| circuitbreaker
    gateway -->|Configures| loadbalancer
    
    %% Downstream service communication (via load balancer)
    customersClient -.->|Load Balanced| customersService
    visitsClient -.->|Load Balanced| visitsService
    
    %% Resilience pattern connections
    circuitbreaker -.->|Protects| customersClient
    circuitbreaker -.->|Protects| visitsClient
    fallback -.->|Handles failures| circuitbreaker
```

### Key Components

| Component | Location | Purpose |
|-----------|----------|---------|
| `ApiGatewayApplication` | `api/ApiGatewayApplication.java` | Spring Boot entry point; configures load-balanced clients, Resilience4J defaults, and static resource routing |
| `ApiGatewayController` | `api/boundary/web/ApiGatewayController.java` | REST controller aggregating owner and visit data via circuit breaker |
| `CustomersServiceClient` | `api/application/CustomersServiceClient.java` | Reactive WebClient for the customers service |
| `VisitsServiceClient` | `api/application/VisitsServiceClient.java` | Reactive WebClient for the visits service |
| `FallbackController` | `api/boundary/web/FallbackController.java` | Fallback endpoint for service unavailability |
| DTO classes | `api/dto/` | `OwnerDetails`, `PetDetails`, `PetType`, `Visits`, `VisitDetails` data transfer objects |
| AngularJS frontend | `resources/static/scripts/` | Single-page application with views for owners, pets, vets, and visits |

## Features

- **Data aggregation** — Combines owner information with visit data in a single API call to the `GET /api/gateway/owners/{ownerId}` endpoint
- **Circuit breaker pattern** — Uses Resilience4J via Spring Cloud Circuit Breaker to wrap downstream service calls, returning empty visit data if the visits service is unavailable
- **Reactive HTTP clients** — Load-balanced `WebClient` instances for non-blocking communication with backend microservices
- **Service discovery** — Integrates with Netflix Eureka for dynamic service location
- **AngularJS frontend** — Single-page application providing views for owner listing, owner creation/editing, pet management, visit management, and veterinarian listing
- **Fluent builder DTOs** — `OwnerDetailsBuilder` and `PetDetailsBuilder` for constructing data transfer objects using a readable builder pattern
- **Fallback handling** — Dedicated fallback endpoint for graceful degradation when downstream services are unreachable
- **Chat interface** — JavaScript-based chat component with message persistence via `localStorage`
- **Default resilience configuration** — Global Resilience4J circuit breaker and time limiter settings (10-second timeout) applied to all reactive circuit breakers

## Requirements

- Java 17 or higher
- Maven 3.6+
- Spring Boot 3.x (managed via parent POM `spring-petclinic-microservices` version 4.0.1)
- Running instances of the downstream services (customers-service, visits-service, vets-service)
- Netflix Eureka service registry for service discovery

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `spring-cloud-starter-netflix-eureka-client` | Service registration and discovery |
| `spring-cloud-starter-circuitbreaker-reactor-resilience4j` | Circuit breaker and time limiter |
| `spring-cloud-starter-gateway-server-webflux` | Reactive gateway support |
| `spring-cloud-starter-config` | Externalized configuration |
| `spring-boot-starter-actuator` | Health checks and monitoring |
| `spring-boot-starter-zipkin` | Distributed tracing |
| `resilience4j-micrometer` | Resilience4J metrics export |
| `micrometer-registry-prometheus` | Prometheus metrics |
| `caffeine` | In-memory caching |

## Installation

### Clone and Build

```bash
# Clone the repository
git clone https://github.com/audoclyphia-evals/petclinic-api-gateway.git
cd petclinic-api-gateway

# Build the project
mvn clean package
```

### Run

```bash
# Start the API gateway (default port 8081)
mvn spring-boot:run
```

The application registers itself with the Eureka service discovery server and expects the following services to be available:

- `customers-service`
- `visits-service`
- `vets-service`

### Verify

```bash
# Check application health via Actuator
curl http://localhost:8081/actuator/health
```

## Quick Start

1. Start the Eureka service registry
2. Start the customers-service, visits-service, and vets-service
3. Start the API gateway:

```bash
mvn spring-boot:run
```

4. Open `http://localhost:8081` in a browser to access the AngularJS application
5. Test the aggregated owner details endpoint:

```bash
curl http://localhost:8081/api/gateway/owners/1
```

Expected response (JSON with owner details and embedded visits):

```json
{
  "id": 1,
  "firstName": "George",
  "lastName": "Franklin",
  "address": "110 W. Liberty St.",
  "city": "Madison",
  "telephone": "6085551023",
  "pets": [
    {
      "id": 1,
      "name": "Leo",
      "birthDate": "2000-09-07",
      "type": { "id": 1, "name": "cat" },
      "visits": []
    }
  ]
}
```

## Usage

### Aggregated Owner Details

The primary backend endpoint aggregates owner data from the customers service and visits from the visits service, attaching visits to each pet:

```mermaid
sequenceDiagram
    actor User
    participant Comp as "OwnerDetailsComponent (Angular)"
    participant ApiGw as "ApiGatewayController"
    participant VisitClient as "VisitsServiceClient"
    participant VisitSvc as "VisitsService (downstream)"

    User->>Comp: navigate to owner details
    activate Comp
    Comp->>ApiGw: GET /owners/{id}
    activate ApiGw
    note over ApiGw: Obtain owner data from CustomersService (not shown)
    ApiGw->>VisitClient: getVisitsForPets(petIds)
    activate VisitClient
    VisitClient->>VisitSvc: HTTP GET /pets/visits?petId={petIds}
    activate VisitSvc
    VisitSvc-->>VisitClient: Visits
    deactivate VisitSvc
    VisitClient-->>ApiGw: Visits
    deactivate VisitClient
    ApiGw-->>Comp: aggregated OwnerDetails
    deactivate ApiGw
    Comp-->>User: display owner details
    deactivate Comp
```

```
GET /api/gateway/owners/{ownerId}
```

The `ApiGatewayController` calls `CustomersServiceClient.getOwner(ownerId)` to retrieve owner and pet data, then calls `VisitsServiceClient.getVisitsForPets(petIds)` to fetch associated visits. The visits are matched to pets by ID via `addVisitsToOwner`. If the visits service call fails, the circuit breaker returns empty visits via `emptyVisitsForPets`.

### Building DTOs with the Fluent API

The `OwnerDetailsBuilder` and `PetDetailsBuilder` provide a readable way to construct data transfer objects:

```mermaid
classDiagram
    %% Backend Core Classes and DTOs - petclinic-api-gateway
    %% Based on repository context

    namespace api_gateway {
        class ApiGatewayApplication {
            <<Main>>
            -Resource indexHtml
            +main(String[] args)$
            +loadBalancedRestTemplate()$: RestTemplate
            +loadBalancedWebClientBuilder()$: WebClient_Builder
            +routerFunction()$: RouterFunction
            +defaultCustomizer()$: Customizer
        }
    }

    namespace boundary_web {
        class ApiGatewayController {
            <<RestController>>
        }
        class FallbackController {
            <<RestController>>
            +fallback()$: ResponseEntity
        }
    }

    namespace application {
        class VisitsServiceClient {
            <<Component>>
            -String hostname
            -WebClient_Builder webClientBuilder
            +VisitsServiceClient(WebClient_Builder webClientBuilder)
            +getVisitsForPets(List petIds)$: Mono
            -joinIds(List petIds)$: String
            +setHostname(String hostname)
        }
        class CustomersServiceClient {
            <<Component>>
        }
    }

    namespace dto {
        class OwnerDetails {
            <<DTO>>
            -int id
            -String firstName
            -String lastName
            -String address
            -String city
            -String telephone
            -List~PetDetails~ pets
        }
        class PetDetails {
            <<DTO>>
        }
        class VisitDetails {
            <<DTO>>
        }
        class PetType {
            <<DTO>>
        }
    }

    %% Relationships based on context
    ApiGatewayController --> VisitsServiceClient : uses
    ApiGatewayController --> CustomersServiceClient : uses
    OwnerDetails "1" *-- "0..*" PetDetails : contains
```

```java
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.PetDetails;
import org.springframework.samples.petclinic.api.dto.PetType;

// Build an OwnerDetails instance
OwnerDetails owner = OwnerDetailsBuilder.anOwnerDetails()
    .id(1)
    .firstName("George")
    .lastName("Franklin")
    .address("110 W. Liberty St.")
    .city("Madison")
    .telephone("6085551023")
    .pets(List.of(
        PetDetailsBuilder.aPetDetails()
            .id(1)
            .name("Leo")
            .birthDate(LocalDate.of(2000, 9, 7))
            .type(new PetType(1, "cat"))
            .visits(new ArrayList<>())
            .build()
    ))
    .build();

// Extract pet IDs for batch visit lookup
List<Integer> petIds = owner.getPetIds();
```

### Owner Form Submission

The AngularJS owner form controller handles both creation and editing of owners by calling the customers-service directly:

```mermaid
sequenceDiagram
    %% source: Cluster_15 - Angular controller for owner form
    participant User as User
    participant OwnerForm as OwnerForm (Angular)
    %% source: Cluster_10 - API gateway functionality
    participant Gateway as ApiGatewayController
    participant Client as CustomersServiceClient
    participant OwnersService as "Owners Service"

    User->>OwnerForm: Submit owner data
    activate OwnerForm
    OwnerForm->>Gateway: POST /owners
    activate Gateway
    Gateway->>Client: createOrUpdate(owner)
    activate Client
    Client->>OwnersService: HTTP Request
    activate OwnersService
    OwnersService-->>Client: 200 OK
    deactivate OwnersService
    Client-->>Gateway: OwnerDetails
    deactivate Client
    Gateway-->>OwnerForm: OwnerDetails
    deactivate Gateway
    OwnerForm-->>User: Success
    deactivate OwnerForm
```

```javascript
// From owner-form.controller.js
// When an ownerId is provided, loads existing owner data via GET
// On submission, sends PUT (edit) or POST (create) to the customers service
```

### Visit Submission

The visits view controller manages loading existing visits and submitting new visit records:

```mermaid
sequenceDiagram
    autonumber
    actor User as User
    participant VisitsCtrl as Visits Controller
    participant ApiGW as API Gateway
    participant VisitClient as Visits Service Client
    participant Fallback as Fallback Controller
    participant VisitSvc as Visits Service

    User->>VisitsCtrl: Submit new visit
    VisitsCtrl->>ApiGW: POST /api/visits

    alt Success path
        ApiGW->>VisitClient: addVisit(visitData)
        VisitClient->>VisitSvc: HTTP POST /pets/visits
        VisitSvc-->>VisitClient: Response success
        VisitClient-->>ApiGW: Return visit details
        ApiGW-->>VisitsCtrl: 200 OK
        VisitsCtrl-->>User: Show success
    else Error path
        ApiGW->>VisitClient: addVisit(visitData)
        VisitClient--xFallback: Call fails
        note right of VisitClient: Circuit breaker opens
        ApiGW->>Fallback: POST /fallback
        Fallback-->>ApiGW: Service unavailable
        ApiGW-->>VisitsCtrl: 503 Service Unavailable
        VisitsCtrl-->>User: Show error
    end
```

```javascript
// From visits.controller.js
// Loads visits for a pet from the visits-service
// Submits new visit records to the visits-service
```

### Service Client Configuration

The `VisitsServiceClient` uses a configurable hostname (defaulting to `http://visits-service/`) to support integration testing:

```java
@Component
public class VisitsServiceClient {

    private String hostname = "http://visits-service/";
    private final WebClient.Builder webClientBuilder;

    public Mono<Visits> getVisitsForPets(final List<Integer> petIds) {
        return webClientBuilder.build()
            .get()
            .uri(hostname + "pets/visits?petId={petId}", joinIds(petIds))
            .retrieve()
            .bodyToMono(Visits.class);
    }
}
```

### Resilience4J Configuration

The default circuit breaker is configured in `ApiGatewayApplication` with a 10-second time limiter:

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

### Fallback Endpoint

The `FallbackController` provides a `POST /fallback` endpoint that returns a service unavailable response when downstream services are unreachable:

```java
@PostMapping("/fallback")
public ResponseEntity<String> fallback() {
    return ResponseEntity.status(HttpStatus.SC_SERVICE_UNAVAILABLE)
        .body("Chat is currently unavailable. Please try again later.");
}
```