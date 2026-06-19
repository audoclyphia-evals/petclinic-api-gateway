# PetClinic API Gateway

> Central API gateway for the Spring PetClinic microservices architecture — orchestrating calls to backend services with built-in resilience.

The PetClinic API Gateway is a Spring Boot application that serves as the unified entry point for the PetClinic microservices system. It aggregates data from the customers-service and visits-service, applies circuit breaker patterns via Resilience4j, and serves the AngularJS frontend. The gateway enables service discovery through Netflix Eureka, load-balanced HTTP communication, and graceful degradation when downstream services are unavailable.

## Project Overview

The API Gateway sits between the client-facing AngularJS frontend and the backend microservices (`petclinic-customers-service`, `petclinic-visits-service`). It is responsible for:

- **Request aggregation** — combining data from multiple backend services into single responses (e.g., merging owner details with their pets' visits)
- **Service discovery** — registering with Netflix Eureka to dynamically locate backend services
- **Resilience** — applying circuit breaker and time limiter configurations through Resilience4j to handle service failures gracefully
- **Frontend serving** — hosting the AngularJS single-page application via static resource routing

### Core Components

| Component | Purpose |
|---|---|
| `ApiGatewayController` | REST controller that orchestrates calls to backend services and returns aggregated responses |
| `CustomersServiceClient` | WebClient-based client for communicating with the customers-service |
| `VisitsServiceClient` | WebClient-based client for communicating with the visits-service |
| `FallbackController` | Returns service-unavailable responses for circuit breaker fallback scenarios |
| `ApiGatewayApplication` | Spring Boot entry point with load-balanced beans and Resilience4j configuration |

## Features

- **Service Discovery Integration** — Registers with Netflix Eureka for automatic discovery of backend microservices
- **Load-Balanced HTTP Clients** — Provides `RestTemplate` and `WebClient.Builder` beans annotated with `@LoadBalanced` for client-side load balancing across service instances
- **Circuit Breaker Protection** — Configures Resilience4j circuit breakers with default settings (10-second timeout) to prevent cascading failures
- **Owner Details Aggregation** — The `GET /api/gateway/owners/{ownerId}` endpoint fetches owner data and their pets' visits in a single request, merging results reactively
- **Vet Visits Proxy** — The `GET /api/gateway/vets/{vetId}/visits` endpoint delegates to the visits-service with circuit breaker fallback
- **AngularJS Frontend** — Serves a complete SPA with owner, pet, vet, and visit management views via static resource routing
- **Graceful Degradation** — Returns empty visit data when the visits-service is unavailable instead of failing the entire request

## Requirements

- **Java 17** or higher
- **Maven 3.6+** for building
- **Docker** (optional) for containerized deployment
- The following backend services must be running and registered with Eureka:
  - `petclinic-customers-service`
  - `petclinic-visits-service`
- **Netflix Eureka Server** for service discovery

## Installation

Clone the repository and build using Maven:

```bash
git clone https://github.com/audoclyphia-evals/petclinic-api-gateway.git
cd petclinic-api-gateway

# Build the project
mvn clean package

# Skip tests if backend services are not running
mvn clean package -DskipTests
```

## Quick Start

1. Ensure Eureka Server and the backend services (customers-service, visits-service) are running.

2. Start the API Gateway:

```bash
java -jar target/spring-petclinic-api-gateway-*.jar
```

3. The application starts on port **8081** by default. Access the frontend at:

```
http://localhost:8081/
```

4. Test the owner details endpoint:

```bash
curl http://localhost:8081/api/gateway/owners/1
```

Expected response (aggregated owner with pets and visits):

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
      "type": { "name": "cat" },
      "visits": []
    }
  ]
}
```

## Usage

### Fetching Owner Details

The gateway aggregates owner data from the customers-service with visit data from the visits-service:

```bash
GET /api/gateway/owners/{ownerId}
```

This endpoint retrieves the owner, extracts pet IDs, fetches visits for those pets, and merges them — all within a circuit breaker-protected reactive pipeline.

### Fetching Vet Visits

```bash
GET /api/gateway/vets/{vetId}/visits
```

Delegates directly to the visits-service, wrapped in a circuit breaker that returns an empty visits list on failure.

### Circuit Breaker Fallback

When the visits-service is unavailable, the gateway returns an empty `Visits` object rather than propagating the error:

```java
// Internal fallback behavior
private Mono<Visits> emptyVisitsForPets() {
    return Mono.just(new Visits(List.of()));
}
```

### Frontend Modules

The AngularJS frontend is organized into these view modules under `src/main/resources/static/scripts/`:

| Module | Path | Purpose |
|---|---|---|
| Owner List | `owner-list/` | Browse and search owners |
| Owner Details | `owner-details/` | View owner info, pets, and visits |
| Owner Form | `owner-form/` | Create or edit owner records |
| Pet Form | `pet-form/` | Create or edit pets for an owner |
| Vet List | `vet-list/` | Browse veterinarians and specialties |
| Visits | `visits/` | Add and view pet visits |

### Data Transfer Objects

The gateway defines these DTOs for internal data transfer:

- **`OwnerDetails`** — Owner information with nested pets list; includes `getPetIds()` helper for visit fetching
- **`PetDetails`** — Pet information including name, type, birth date, and visits list
- **`Visits`** — Collection wrapper for a list of `VisitDetails` objects
- **`VisitDetails`** — Individual visit record with pet ID, date, and description
- **`PetType`** — Pet type descriptor with a name field

## Additional Documentation

For more detailed information, see the following documentation:

- [System Architecture](ARCHITECTURE.md) - Provides a high-level overview of the PetClinic API Gateway system, including components, interactions, and design principles.
- [API Gateway Reference](docs/API_REFERENCE.md) - Documents the REST API endpoints exposed by the API gateway, their parameters, responses, and usage examples.
- [Contributing Guidelines](CONTRIBUTING.md) - Outlines guidelines for contributing to the project, including development setup, coding standards, and pull request process.
- [Deployment Instructions](DEPLOYMENT.md) - Explains how to deploy the PetClinic API Gateway application, including prerequisites, configuration, and steps for production and development environments.

openapi: 3.0.3
info:
  title: API Documentation
  description: Auto-generated API documentation
  version: 1.0.0
paths:
  /api/gateway/owners/{ownerId}:
    get:
      summary: Get owner details with aggregated customer and visit data
      description: Retrieves owner details by ID, aggregating customer and visit data
        from services.
      operationId: getOwnerDetails
      tags:
      - Owner
      responses:
        '200':
          description: Owner details retrieved successfully
        '400':
          description: Bad request - invalid owner ID
        '401':
          description: Unauthorized access
        '500':
          description: Internal server error
      parameters:
      - name: ownerId
        in: path
        required: true
        schema:
          type: integer
        description: Unique identifier of the owner
  /api/gateway/vets/{vetId}/visits:
    get:
      summary: Handles GET request for vet visits, delegating to visits service
      description: Retrieves visits for a specific vet by ID, using circuit breaker
        pattern.
      operationId: getVisitsForVet
      tags:
      - vets
      responses:
        '200':
          description: List of visits for the vet
        '404':
          description: Vet not found
        '500':
          description: Internal server error
      parameters:
      - name: vetId
        in: path
        required: true
        schema:
          type: integer
        description: ID of the vet
  /fallback:
    post:
      summary: Fallback endpoint returning service unavailable for chat
      description: Returns a 503 service unavailable response indicating chat is unavailable.
      operationId: fallback
      tags:
      - Fallback
      responses:
        '503':
          description: Service Unavailable
      requestBody:
        required: false
        content:
          application/json:
            schema:
              type: object
              properties: {}
tags:
- name: Fallback
- name: Owner
- name: vets