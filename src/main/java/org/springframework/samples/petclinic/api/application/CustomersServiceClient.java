/*  // Defines the CustomersServiceClient class as a Spring component in the application package, including necessary imports for reactive web communication and DTO handling.
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
package org.springframework.samples.petclinic.api.application;

import org.springframework.samples.petclinic.api.dto.OwnerDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A Spring-managed component that uses WebClient to make reactive HTTP calls to the customers-service, providing methods for fetching individual owner details and searching owners by last name prefix.
 */
@Component
public class CustomersServiceClient {

    private final WebClient.Builder webClientBuilder;

    public CustomersServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<OwnerDetails> getOwner(final int ownerId) {
        return webClientBuilder.build().get()
            .uri("http://customers-service/owners/{ownerId}", ownerId)
            .retrieve()
            .bodyToMono(OwnerDetails.class);
    }

    /**
     * Performs a GET request to the customers-service endpoint '/owners/search' with the lastName parameter to filter owners by last name prefix, and returns a Flux of OwnerDetails objects representing the search results.
     */
    public Flux<OwnerDetails> searchOwners(final String lastName) {
        return webClientBuilder.build().get()
            .uri("http://customers-service/owners/search?lastName={lastName}", lastName)
            .retrieve()
            .bodyToFlux(OwnerDetails.class);
    }
}
