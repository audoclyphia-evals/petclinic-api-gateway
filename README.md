# Spring PetClinic API Gateway

The `petclinic-api-gateway` service acts as the single entry point for the Spring PetClinic microservices system. It aggregates data from the **petclinic-customers-service**, **petclinic-visits-service**, and **petclinic-vets-service** into unified responses, serves the AngularJS single-page application, and provides resilient routing to downstream services.

This gateway is one component of a larger microservices ecosystem:

| Service | Role |
|---------|------|
| **petclinic-api-gateway** | API gateway, frontend host, request aggregation |
| **petclinic-customers-service** | Owner and pet CRUD operations |
| **petclinic-visits-service** | Visit management and retrieval |
| **petclinic-vets-service** | Veterinarian data |

The following key features are provided:

- **Request Aggregation** — Combines owner details with visit data into a single response; enriches owner search results with visit counts (see [Usage](#usage) for endpoint details)
- **Vet Details Retrieval** — Proxies vet profile data from the vets-service with circuit-breaker protection
- **Owner Search** — Searches owners by last name prefix and enriches results with per-owner visit counts from the visits-service
- **Load-Balanced Service Discovery** — Uses Netflix Eureka for dynamic service location with `@LoadBalanced` `WebClient` and `RestTemplate` beans
- **Resilience4J Circuit Breakers** — Applies default circuit breaker and time-limiter configuration (10-second timeout) to all reactive service calls, with fallback to empty results on failure
- **Static Resource Serving** — Hosts the AngularJS frontend via a Spring `RouterFunction` that serves `index.html` and static assets
- **AngularJS Frontend Modules** — Includes UI for owner list, owner details, owner form, pet form, vet list, and visit management with `ui-router` navigation
- **Client-Side Chat Interface** — Provides an interactive chat support widget with Markdown rendering and `localStorage`-persisted history
- **Fallback Endpoint** — Exposes a fallback endpoint returning `503` for service unavailability scenarios

```mermaid
flowchart TB
    Placeholder[No diagram content was provided to fix]
``` — High-level view of the PetClinic microservices topology and how the API gateway fits into the system.

![API Gateway Core Classes](docs/class_api_gateway_core_classes.mmd) — Class diagram showing the core Java components: controllers, service clients, and DTOs.

![Owner Details Aggregation Flow](docs/sequence_owner_details_aggregation_flow.mmd) — Sequence diagram illustrating the request flow when aggregating owner details with visit data.

To run the gateway, certain prerequisites are required.

## Prerequisites

- **Java** 17 or higher
- **Maven** 3.x
- **Service Discovery**: A running Netflix Eureka server (required for service registration and discovery)
- **Downstream Services**: The `petclinic-customers-service`, `petclinic-visits-service`, and `petclinic-vets-service` must be registered with Eureka for the gateway to proxy requests successfully
- **Spring Cloud Config** (optional): Externalized configuration server

### Recommended Tools

- **Docker** — for running Netflix Eureka and downstream services
- **A modern web browser** — for accessing the AngularJS frontend

## Installation

To install and build the project, follow the steps below.

```bash
# Clone the repository
git clone https://github.com/audoclyphia-evals/petclinic-api-gateway.git
cd petclinic-api-gateway

# Build the project
mvn clean package

# Skip tests if downstream services are not running
mvn clean package -DskipTests
```

## Quickstart

1. Ensure a Eureka server and the downstream services are running.

2. Start the API gateway:

```bash
mvn spring-boot:run
```

3. Verify the application is running:

```bash
curl http://localhost:8081/
```

This returns the AngularJS frontend's `index.html`.

4. Query owner details through the gateway:

```bash
curl http://localhost:8081/api/gateway/owners/1
```

Returns an `OwnerDetails` object enriched with visit data for each pet, or a fallback response if the visits-service is unavailable.

5. Search for owners:

```bash
curl 'http://localhost:8081/api/gateway/owners/search?lastName=Be'
```

Returns a list of `OwnerSummary` objects matching the last name prefix, each with a visit count.

6. Fetch a veterinarian's details:

```bash
curl http://localhost:8081/api/gateway/vets/1
```

Returns a `VetDetails` object for the specified veterinarian, or an empty response if the circuit breaker triggers.

Once running, you can interact with the gateway as described in the following sections.

## Usage

### Endpoint Overview

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/gateway/owners/{ownerId}` | Fetches owner details combined with visit history for all pets |
| `GET` | `/api/gateway/owners/search` | Searches owners by last name prefix, enriched with visit counts |
| `GET` | `/api/gateway/vets/{vetId}` | Fetches a single veterinarian's details |
| `GET` | `/api/gateway/vets/{vetId}/visits` | Fetches visits for a specific veterinarian |
| `POST` | `/fallback` | Returns `503` — used as fallback for service unavailability |

Refer to [api_documentation.yaml](api_documentation.yaml) for the complete API reference with schemas and status codes.

### Endpoint Details

The `GET /api/gateway/owners/{ownerId}` endpoint aggregates data from the **petclinic-customers-service** (owner and pet information) with visit records from the **petclinic-visits-service**. If the visits-service is unreachable, the circuit breaker returns empty visit lists for each pet.

The `GET /api/gateway/owners/search` endpoint searches owners by last name prefix via the petclinic-customers-service, then enriches each result with a visit count obtained from the petclinic-visits-service. Circuit breakers wrap both downstream calls independently.

Example:

```bash
curl -s http://localhost:8081/api/gateway/owners/1 | jq
```

Search for owners:

```bash
curl -s 'http://localhost:8081/api/gateway/owners/search?lastName=' | jq
```

### Frontend Navigation

The AngularJS frontend uses `ui-router` for state-based navigation with routes for:

- **Owner list** — Search and list all owners
- **Owner details** — View owner info, pets, and visit history
- **Owner form** — Create or edit owner records
- **Pet form** — Create or edit pet records
- **Vet list** — Display all veterinarians
- **Visits** — View and add visit records for a pet

## 📚 Additional Documentation

For more detailed information, see the following documentation:

- [PetClinic API Gateway Architecture](ARCHITECTURE.md) — Documents the overall system architecture, including the role of the API gateway, service integration, and frontend-backend interaction.
- [Contributing Guidelines](CONTRIBUTING.md) — Provides guidelines for developers to contribute to the project, including setup, coding standards, and testing.

## API Reference

The following OpenAPI specification provides a detailed schema for the endpoints.

```yaml
openapi: 3.0.3
info:
  title: API Documentation
  description: Auto-generated API documentation
  version: 1.0.0
paths:
  /api/gateway/owners/{ownerId}:
    get:
      summary: Retrieve owner details with visits
      description: Aggregates owner details from customers service and adds pet visits
        from visits service with circuit breaker fallback
      operationId: getOwnerDetails
      tags:
      - Gateway
      responses:
        '200':
          description: Owner details with pet visits
        '400':
          description: Bad request
        '401':
          description: Unauthorized
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
      summary: Retrieves visits for a specific vet
      description: Retrieves all visits associated with a vet ID, using circuit breaker
        for resilience.
      operationId: getVisitsForVet
      tags:
      - Visits
      responses:
        '200':
          description: Successful retrieval of visits
        '400':
          description: Bad request, invalid vet ID
        '401':
          description: Unauthorized access
        '500':
          description: Internal server error
      parameters:
      - name: vetId
        in: path
        required: true
        schema:
          type: integer
        description: Unique identifier of the vet
  /fallback:
    post:
      summary: Handle fallback for service unavailability
      description: Returns a 503 response when downstream services are unavailable.
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
  /api/gateway/vets/{vetId}:
    get:
      summary: HTTP GET endpoint to fetch a single vet's details by ID.
      description: Fetches vet details by ID with circuit breaker protection.
      operationId: getVetDetails
      tags:
      - Vets
      responses:
        '200':
          description: Success
        '400':
          description: Bad request
        '401':
          description: Unauthorized
        '500':
          description: Internal server error
      parameters:
      - name: vetId
        in: path
        required: true
        schema:
          type: integer
        description: The unique identifier of the vet
  /api/gateway/owners/search:
    get:
      summary: Search owners by last name with visit counts
      description: Searches owners by last name prefix and enriches results with visit
        counts from visits-service. Returns all owners if lastName is empty.
      operationId: searchOwners
      tags:
      - Owners
      responses:
        '200':
          description: Success
        '500':
          description: Internal server error
      parameters:
      - name: lastName
        in: query
        required: false
        schema:
          type: string
          default: ''
        description: Last name prefix for case-insensitive search
tags:
- name: Fallback
- name: Gateway
- name: Owners
- name: Vets
- name: Visits
```