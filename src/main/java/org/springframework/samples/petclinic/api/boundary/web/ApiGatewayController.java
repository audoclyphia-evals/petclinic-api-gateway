/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.api.boundary.web;

import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.samples.petclinic.api.application.CustomersServiceClient;
import org.springframework.samples.petclinic.api.application.VetsServiceClient;
import org.springframework.samples.petclinic.api.application.VisitsServiceClient;
import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.samples.petclinic.api.dto.OwnerSummary;
import org.springframework.samples.petclinic.api.dto.VetDetails;
import org.springframework.samples.petclinic.api.dto.Visits;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Function;
/**
 * @author Maciej Szarlinski
 */
@RestController
@RequestMapping("/api/gateway")
public class ApiGatewayController {

    private final CustomersServiceClient customersServiceClient;

    private final VisitsServiceClient visitsServiceClient;

    private final VetsServiceClient vetsServiceClient;

    private final ReactiveCircuitBreakerFactory cbFactory;

    public ApiGatewayController(CustomersServiceClient customersServiceClient,
                                VisitsServiceClient visitsServiceClient,
                                VetsServiceClient vetsServiceClient,
                                ReactiveCircuitBreakerFactory cbFactory) {
        this.customersServiceClient = customersServiceClient;
        this.visitsServiceClient = visitsServiceClient;
        this.vetsServiceClient = vetsServiceClient;
        this.cbFactory = cbFactory;
    }

    @GetMapping(value = "owners/{ownerId}")
    public Mono<OwnerDetails> getOwnerDetails(final @PathVariable int ownerId) {
        return customersServiceClient.getOwner(ownerId)
            .flatMap(owner ->
                visitsServiceClient.getVisitsForPets(owner.getPetIds())
                    .transform(it -> {
                        ReactiveCircuitBreaker cb = cbFactory.create("getOwnerDetails");
                        return cb.run(it, throwable -> emptyVisitsForPets());
                    })
                    .map(addVisitsToOwner(owner))
            );

    }

    @GetMapping(value = "vets/{vetId}/visits")
    public Mono<Visits> getVisitsForVet(final @PathVariable int vetId) {
        ReactiveCircuitBreaker cb = cbFactory.create("getVisitsForVet");
        return cb.run(
            visitsServiceClient.getVisitsForVet(vetId),
            throwable -> emptyVisitsForPets()
        );
    }

    /**
     * Search owners by last name prefix and enrich each result with visit counts.
     * Calls customers-service for the search, then visits-service for each owner's pets.
     * Falls back to an empty list on circuit-breaker open.
     *
     * @param lastName the prefix to search (empty string returns all owners)
     */
    @GetMapping(value = "owners/search")
    public Flux<OwnerSummary> searchOwners(
        @RequestParam(value = "lastName", required = false, defaultValue = "") String lastName) {

        return customersServiceClient.searchOwners(lastName)
            .flatMap(owner -> {
                ReactiveCircuitBreaker cb = cbFactory.create("searchOwners");
                Mono<Visits> visitsMono = cb.run(
                    visitsServiceClient.getVisitsForPets(owner.getPetIds()),
                    throwable -> emptyVisitsForPets()
                );
                return visitsMono.map(visits -> new OwnerSummary(
                    owner.id(),
                    owner.firstName(),
                    owner.lastName(),
                    owner.address(),
                    owner.city(),
                    owner.telephone(),
                    owner.pets().size(),
                    visits.items().size()
                ));
            });
    }

    /**
     * Fetch a single vet's details by ID (name + specialties).
     */
    @GetMapping(value = "vets/{vetId}")
    public Mono<VetDetails> getVetDetails(final @PathVariable int vetId) {
        ReactiveCircuitBreaker cb = cbFactory.create("getVetDetails");
        return cb.run(
            vetsServiceClient.getVet(vetId),
            throwable -> Mono.empty()
        );
    }

    private Function<Visits, OwnerDetails> addVisitsToOwner(OwnerDetails owner) {
        return visits -> {
            owner.pets()
                .forEach(pet -> pet.visits()
                    .addAll(visits.items().stream()
                        .filter(v -> v.petId() == pet.id())
                        .toList())
                );
            return owner;
        };
    }

    private Mono<Visits> emptyVisitsForPets() {
        return Mono.just(new Visits(List.of()));
    }
}
