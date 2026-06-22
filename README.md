# Spring PetClinic API Gateway

![Tests](https://img.shields.io/badge/tests-passing-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-17+-orange.svg?logo=openjdk)
![Spring_Boot](https://img.shields.io/badge/Spring_Boot-3.x-brightgreen.svg?logo=springboot)
![Spring_Cloud](https://img.shields.io/badge/Spring_Cloud-green.svg?logo=spring)
![AngularJS](https://img.shields.io/badge/AngularJS-1.8-red.svg?logo=angularjs)
![Maven](https://img.shields.io/badge/Maven-4.0-blue.svg?logo=apachemaven)

The central API gateway for the Spring PetClinic microservices architecture, routing requests between the AngularJS frontend and backend services with built-in resilience patterns.

## Project Overview

The `petclinic-api-gateway` service acts as the single entry point for the PetClinic microservices system. It aggregates data from the **customers-service** and **visits-service** into unified responses, serves the AngularJS single-page application, and provides circuit-breaker-protected routing to downstream services.

This gateway is one component of a larger microservices ecosystem:

| Service | Role |
|---------|------|
| **petclinic-api-gateway** | API gateway, frontend host, request aggregation |
| **petclinic-customers-service** | Owner and pet CRUD operations |
| **petclinic-visits-service** | Visit management and retrieval |
| **petclinic-vets-service** | Veterinarian data |

To run the gateway, additional infrastructure such as a Netflix Eureka server and downstream services are required (see [Requirements](#requirements)).

## Features

- **Request Aggregation** — Combines owner details with visit data into a single response (see [Usage](#usage) for endpoint details)
- **Load-Balanced Service Discovery** — Uses Netflix Eureka for dynamic service location with `@LoadBalanced` `WebClient` and `RestTemplate` beans
- **Resilience4J Circuit Breakers** — Applies default circuit breaker and time-limiter configuration (10-second timeout) to all reactive service calls, with fallback to empty results on failure
- **Static Resource Serving** — Hosts the AngularJS frontend via a Spring `RouterFunction` that serves `index.html` and static assets
- **AngularJS Frontend Modules** — Includes UI for owner list, owner details, owner form, pet form, vet list, and visit management with `ui-router` navigation
- **Client-Side Chat Interface** — Provides an interactive chat support widget with Markdown rendering and `localStorage`-persisted history
- **Fallback Endpoint** — Exposes a fallback endpoint returning `503` for chat service degradation
- **Observability** — Integrates Micrometer with Prometheus registry, Zipkin tracing, and Jolokia for monitoring

## Requirements

The following prerequisites are needed to run and use the gateway:

- **Java** 17 or higher
- **Maven** 3.x
- **Service Discovery**: A running Netflix Eureka server (required for service registration and discovery)
- **Downstream Services**: The `petclinic-customers-service` and `petclinic-visits-service` must be registered with Eureka for the gateway to proxy requests successfully (see [Project Overview](#project-overview) for service details)
- **Spring Cloud Config** (optional): Externalized configuration server

### Recommended Tools

- **Docker** — for running Netflix Eureka and downstream services
- **A modern web browser** — for accessing the AngularJS frontend

To install and build the project, follow the steps below.

## Installation

```bash
# Clone the repository
git clone https://github.com/audoclyphia-evals/petclinic-api-gateway.git
cd petclinic-api-gateway

# Build the project
mvn clean package

# Skip tests if downstream services are not running
mvn clean package -DskipTests
```

Once installed, you can start the gateway using the quick start instructions.

## Quick Start

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

Once running, you can access the gateway endpoints and frontend as described in [Usage](#usage).

## Usage

### Gateway Endpoints

The API gateway exposes the following endpoints:

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/gateway/owners/{ownerId}` | Fetches owner details combined with visit history for all pets |
| `GET` | `/api/gateway/vets/{vetId}/visits` | Fetches visits for a specific veterinarian |
| `POST` | `/fallback` | Returns `503` — used as chat service fallback |

Refer to [API Documentation](#api-documentation) for the complete API reference with schemas.

The `GET /api/gateway/owners/{ownerId}` endpoint aggregates data from the **customers-service** (owner and pet information) with visit records from the **visits-service**. If the visits-service is unreachable, the circuit breaker returns empty visit lists for each pet.

Example:

```bash
curl -s http://localhost:8081/api/gateway/owners/1 | jq
```

### Frontend Navigation

The AngularJS frontend uses `ui-router` for state-based navigation with routes for:

- **Owner list** — Search and list all owners
- **Owner details** — View owner info, pets, and visit history
- **Owner form** — Create or edit owner records
- **Pet form** — Create or edit pet records
- **Vet list** — Display all veterinarians
- **Visits** — View and add visit records for a pet

For more details on the architecture and contribution guidelines, see the additional documentation below.

## Additional Documentation

- [System Architecture Overview](ARCHITECTURE.md) — Essential for understanding the hybrid backend (Spring Boot) and frontend (AngularJS) architecture, including resilience mechanisms, service interactions, and project structure, which is not detailed in this document.
- [Development and Contribution Guidelines](CONTRIBUTING.md) — Provides guidelines for development, testing, and contributing to the project, critical for developers working with the mixed Java/JavaScript codebase, and ensures consistent practices.
- [API Documentation](api_documentation.yaml) — Generated API reference file

## API Documentation

```yaml
openapi: 3.0.3
info:
  title: API Documentation
  description: Auto-generated API documentation
  version: 1.0.0
paths:
  /api/gateway/owners/{ownerId}:
    get:
      summary: Fetches owner details and enriches with visits
      description: Retrieves owner details by ID and enriches the response with visit information for the owner's pets.
      operationId: getOwnerDetails
      tags:
      - Owners
      responses:
        '200':
          description: Owner details retrieved successfully
        '400':
          description: Bad request due to invalid owner ID
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
      summary: Fetches visits for a specific veterinarian
      description: Retrieves visits associated with the specified veterinarian ID
      operationId: getVisitsForVet
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
        description: The unique identifier of the veterinarian
  /fallback:
    post:
      summary: Returns a 503 fallback response
      description: Returns 503 Service Unavailable with chat unavailable message
      operationId: fallbackPost
      tags:
      - Fallback
      responses:
        '503':
          description: Service Unavailable - Chat is currently unavailable
      requestBody:
        required: false
        content:
          application/json:
            schema:
              type: object
tags:
- name: Fallback
- name: Owners
- name: Vets
 ```