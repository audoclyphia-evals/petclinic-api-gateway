# Contributing Guidelines

Contribution and integration reference for the PetClinic API Gateway.

![Tests](https://img.shields.io/badge/tests-passing-brightgreen.svg)

The `petclinic-api-gateway` module serves as the front-door aggregator for the PetClinic microservices system. It provides a unified REST API that fetches and composes data from the `petclinic-customers-service`, `petclinic-vets-service`, and `petclinic-visits-service`, while exposing a resilient AngularJS single-page application. This document covers the module's architecture, setup procedures, API surface, and patterns that contributors must follow.

## Overview

The API Gateway is a Spring Boot application that acts as the aggregation layer between the frontend UI and the backend microservices. Rather than allowing the browser to call each microservice directly, this module centralizes cross-service data retrieval, applies circuit breaker protection, and serves the static AngularJS application.

### Core Responsibilities

- **Data Aggregation**: Combines owner details from the customers service with visit records from the visits service into a single `OwnerDetails` response.
- **Fault Tolerance**: Applies Resilience4J circuit breaker and time limiter policies so that a downstream service failure does not cascade.
- **Static Asset Serving**: Hosts the AngularJS frontend (owner list, owner details, owner form, pet form, vet list, visits views) alongside the API routes.
- **Service Discovery Integration**: Registers with Netflix Eureka to discover backend service instances at runtime.

### Role in the PetClinic Microservices Ecosystem

| Repository | Role | Gateway Interaction |
|---|---|---|
| `petclinic-customers-service` | Owner and pet CRUD | `CustomersServiceClient` calls `GET /owners/{ownerId}` |
| `petclinic-visits-service` | Visit records | `VisitsServiceClient` calls `GET /pets/visits?petId={ids}` |
| `petclinic-vets-service` | Veterinarian directory | Frontend calls directly via service-discovery URL |

The gateway does **not** proxy all traffic. The AngularJS frontend makes direct HTTP calls (through load-balanced URLs) for owner listing, vet listing, owner form submission, pet form submission, and visit creation. The gateway's server-side aggregation is used only for the owner-details view, where it must merge data from two services atomically.

## Features

- **Server-side data aggregation** — The `GET /api/gateway/owners/{ownerId}` endpoint fetches owner data and visits concurrently, then merges them into a single `OwnerDetails` object before returning the response.
- **Resilience4J circuit breakers** — Every downstream call is wrapped in a circuit breaker with a 10-second time limiter. When the visits service is unreachable, the gateway returns the owner details with an empty visits list rather than failing.
- **Load-balanced HTTP clients** — Both `RestTemplate` and `WebClient.Builder` beans are annotated with `@LoadBalanced`, enabling transparent resolution of service names (e.g., `http://customers-service/`) to actual host addresses via Eureka.
- **Eureka service discovery** — The application registers itself and discovers backend services through Netflix Eureka, configured via Spring Cloud Config.
- **AngularJS single-page application** — A complete frontend with UI-Router–based navigation for owner listing, owner details, owner creation/editing, pet creation/editing, visit management, and veterinarian listing.
- **Chat interface** — A JavaScript-based chat widget with message persistence via `localStorage`, accessible from the UI.
- **Fallback endpoint** — A `POST /fallback` endpoint returns a service-unavailable response when downstream services cannot be reached, allowing upstream callers to degrade gracefully.

## Requirements

- **Java 17+** (Spring Boot 3.x based on the parent POM)
- **Apache Maven 3.9+**
- **Running infrastructure dependencies**:
  - Netflix Eureka server (service discovery)
  - Spring Cloud Config server (configuration management)
  - Zipkin server (distributed tracing, optional in development)
- **Running backend microservices** (for full functionality):
  - `petclinic-customers-service`
  - `petclinic-visits-service`
  - `petclinic-vets-service`

## Installation

```bash
# Clone the repository
git clone https://github.com/audoclyphia-evals/petclinic-api-gateway.git
cd petclinic-api-gateway

# Build the project (skipping tests if backend services are not running)
mvn clean package -DskipTests

# Verify the build artifact exists
ls target/spring-petclinic-api-gateway-*.jar
```

The build produces a Spring Boot executable JAR under `target/`. Webjars for Bootstrap, AngularJS, Angular UI Router, Font Awesome, and Marked are resolved by Maven at build time.

To compile the SCSS stylesheets, use the `css` profile:

```bash
mvn generate-resources -Pcss
```

## Quick Start

1. Start the Eureka server, Config server, customers-service, and visits-service.
2. Launch the API gateway:

```bash
java -jar target/spring-petclinic-api-gateway-*.jar
```

3. Open `http://localhost:8081` in a browser to see the AngularJS UI.
4. Fetch an aggregated owner detail via the API:

```bash
curl http://localhost:8081/api/gateway/owners/1
```

A successful response returns a JSON object containing the owner's personal information and an array of `pets`, each with its associated `visits` list populated from the visits service.

## Usage

### Fetching Aggregated Owner Details

The primary server-side endpoint combines data from two services:

```bash
curl http://localhost:8081/api/gateway/owners/1
```

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
      "visits": [
        { "id": 1, "date": "2013-01-01", "description": "rabies shot", "petId": 1 }
      ]
    }
  ]
}
```

If the visits service is unavailable, the circuit breaker activates and returns an empty `visits` list for each pet rather than a failure response.

### Constructing OwnerDetails Programmatically

The module uses a builder pattern for DTOs. To construct an `OwnerDetails` object in Java:

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
            .build()
    ))
    .build();
```

The `getPetIds()` method on `OwnerDetails` extracts all pet IDs, which the controller uses to query the visits service.

### Calling Backend Services from Code

Both service clients return reactive types (`Mono<OwnerDetails>` and `Mono<Visits>`):

```java
// In a controller or service
Mono<OwnerDetails> owner = customersServiceClient.getOwner(1);

Mono<Visits> visits = visitsServiceClient.getVisitsForPets(List.of(1, 2, 3));
```

The `VisitsServiceClient` joins the pet IDs into a comma-separated query parameter (`1,2,3`) and issues an HTTP GET to `http://visits-service/pets/visits?petId={ids}`.

### Circuit Breaker Behavior

The `getOwnerDetails` endpoint wraps the visits fetch in a circuit breaker named `"getOwnerDetails"`. The default configuration (defined in `ApiGatewayApplication.defaultCustomizer()`) sets:

- **Circuit breaker**: default Resilience4J settings (50% failure rate threshold, 10-call minimum, 10-second wait duration)
- **Time limiter**: 10-second timeout

When the circuit opens, the fallback function `emptyVisitsForPets()` returns `Mono.just(new Visits(List.of()))`, allowing the owner details to be returned with empty visit data.