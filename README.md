# petclinic-api-gateway

API Gateway for the Spring PetClinic Microservices Application

![Tests](https://img.shields.io/badge/tests-passing-brightgreen.svg)

The `petclinic-api-gateway` serves as the central entry point for the PetClinic microservices architecture. It aggregates data from downstream services — customers, visits, and veterinarians — and presents a unified API to the frontend. This repository implements that gateway component, using Spring Cloud with service discovery (Netflix Eureka) and Resilience4j circuit breakers to ensure fault-tolerant communication between services.

## Overview

The gateway performs three primary roles: routing client requests to appropriate backend services, aggregating responses from multiple services into composite responses, and providing fault tolerance through circuit breaker patterns. The project consists of a Spring Boot backend and an AngularJS single-page application frontend, both packaged together. The backend exposes REST endpoints that combine data from the customers and visits services, while the frontend provides the user interface for managing owners, pets, visits, and veterinarians.

**Sibling repositories in the microservices ecosystem:**

| Service | Description |
|---------|-------------|
| `petclinic-customers-service` | Manages owner and pet data |
| `petclinic-vets-service` | Provides veterinarian information |
| `petclinic-visits-service` | Manages pet visit records |

## Features

- **Data Aggregation**: The `ApiGatewayController` combines owner data from the customers service with visit data from the visits service into a single `OwnerDetails` response, eliminating the need for clients to make multiple service calls.
- **Reactive Service Clients**: `CustomersServiceClient` and `VisitsServiceClient` use Spring WebFlux `WebClient` for non-blocking HTTP communication with downstream services.
- **Circuit Breaker Fault Tolerance**: Resilience4j circuit breakers wrap calls to the visits service, returning empty visit data as a fallback when the service is unavailable rather than propagating failures.
- **Service Discovery Integration**: Netflix Eureka client registration enables dynamic discovery of downstream service instances, supporting load balancing and high availability.
- **Load-Balanced HTTP Clients**: Both `RestTemplate` and `WebClient.Builder` beans are configured with `@LoadBalanced` for transparent service-to-service load balancing.
- **Fluent Builder DTOs**: `OwnerDetails` and `PetDetails` use builder patterns with static factory methods (`anOwnerDetails()`, `aPetDetails()`) for clean, readable object construction.
- **AngularJS Frontend**: A single-page application with UI Router providing views for owner listing, owner details, owner form, pet form, visits, and veterinarian listing.
- **Resilience Fallback Endpoint**: The `FallbackController` provides a `POST /fallback` endpoint that returns a service-unavailable response for degraded scenarios.
- **Chat Interface**: A JavaScript-based chat component (`chat.js`) with message handling and localStorage persistence, accessible through the frontend.

## Requirements

The following software and network access are required to build and run the application.

- Java 17 or higher
- Maven 3.6+
- Network access to Eureka service registry (required for service discovery)

**Runtime dependencies managed via Maven:**

- Spring Boot (webflux, actuator, cache, zipkin)
- Spring Cloud (Eureka client, Gateway WebFlux, Config, Circuit Breaker Reactor Resilience4j)
- Resilience4j (circuit breaker, time limiter, micrometer integration)
- AngularJS 1.8.3, Bootstrap 5.3.3, Angular UI Router 1.0.30 (via WebJars)

## Installation

To get started, clone the repository and build the project using Maven.

```bash
# Clone the repository
git clone https://github.com/audoclyphia-evals/petclinic-api-gateway.git
cd petclinic-api-gateway

# Build the project
mvn clean package

# Skip tests during build (if downstream services are not running)
mvn clean package -DskipTests
```

The application exposes port `8081` for Docker image builds and defaults to port `8080` when run directly with Spring Boot.

## Quick Start

After installation, ensure the PetClinic microservices infrastructure (Eureka, customers-service, visits-service, vets-service) is running, then start the gateway.

```bash
mvn spring-boot:run
```

Once started, the gateway serves the AngularJS frontend at `http://localhost:8080/` and exposes the aggregated API, for example:

```
GET http://localhost:8080/api/gateway/owners/{ownerId}
```

This endpoint returns an `OwnerDetails` object combining owner information with their pets' visit history. For detailed API specifications, see the [API Documentation](#api-documentation).

## Usage

### Retrieving Owner Details with Visits

The primary gateway endpoint aggregates owner and visit data. The `OwnerDetails` response includes the owner's information along with a list of their pets, each containing a history of visits.

```java
// GET /api/gateway/owners/{ownerId}
// Returns: OwnerDetails with pets and their visits populated
```

Example response structure:

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
      "type": { "id": 1, "name": "cat" },
      "birthDate": "2010-09-07",
      "visits": [
        { "id": 1, "date": "2013-01-01", "description": "rabies shot" }
      ]
    }
  ]
}
```

### Building OwnerDetails with the Fluent API

The `OwnerDetails` and `PetDetails` DTOs use fluent builder patterns for clean construction.

```java
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
            .type(new PetType(1, "cat"))
            .birthDate(LocalDate.of(2010, 9, 7))
            .visits(new ArrayList<>())
            .build()
    ))
    .build();

// Extract pet IDs for visit lookup
List<Integer> petIds = owner.getPetIds(); // [1]
```

### Service Client Configuration

The `VisitsServiceClient` allows overriding the target hostname for testing purposes.

```java
VisitsServiceClient client = new VisitsServiceClient(webClientBuilder);
client.setHostname("http://localhost:8082/");
Mono<Visits> visits = client.getVisitsForPets(List.of(1, 2, 3));
```

### Circuit Breaker Configuration

Circuit breaker configuration applies a 10-second timeout and default Resilience4j circuit breaker settings. When the visits service is unavailable, the circuit breaker returns an empty `Visits` object rather than propagating the error.

```java
@Bean
public Customizer<ReactiveResilience4JCircuitBreakerFactory> defaultCustomizer() {
    return factory -> factory.configureDefault(id -> new Resilience4JConfigBuilder(id)
        .circuitBreakerConfig(CircuitBreakerConfig.ofDefaults())
        .timeLimiterConfig(TimeLimiterConfig.custom()
            .timeoutDuration(Duration.ofSeconds(10))
            .build())
        .build());
}
```

## API Documentation

The following details the REST endpoints exposed by the API gateway.

### OpenAPI Specification

```yaml
openapi: 3.0.3
info:
  title: API Documentation
  description: Auto-generated API documentation
  version: 1.0.0
paths:
  /api/gateway/owners/{ownerId}:
    get:
      summary: Get owner details with visits using circuit breaker
      description: Retrieves complete owner details including visits for their pets with circuit breaker pattern for fault tolerance
      operationId: getOwnerDetails
      tags:
      - Gateway
      - Owners
      responses:
        '200':
          description: Owner details with visits successfully retrieved
        '400':
          description: Invalid owner ID provided
        '401':
          description: Unauthorized access
        '500':
          description: Internal server error or service unavailable
      parameters:
      - name: ownerId
        in: path
        required: true
        schema:
          type: integer
        description: Unique identifier of the owner
  /fallback:
    post:
      summary: Handles fallback POST requests when services are unavailable
      description: Returns a fallback response when services are unavailable.
      operationId: fallback
      tags:
      - Fallback
      responses:
        '503':
          description: Chat is currently unavailable. Please try again later.
      requestBody:
        required: false
        content:
          application/json:
            schema:
              type: object
tags:
- name: Fallback
- name: Gateway
- name: Owners
```