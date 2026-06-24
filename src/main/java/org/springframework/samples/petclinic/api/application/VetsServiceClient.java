/*  // This Java file contains the VetsServiceClient class definition and its required imports for interacting with the vets-service in the petclinic API gateway.
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

import org.springframework.samples.petclinic.api.dto.VetDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A Spring @Component that acts as a reactive HTTP client for the vets-service. It uses WebClient.Builder to construct WebClient instances and provides methods to retrieve VetDetails objects via HTTP GET requests to the /vets endpoints.
 */
@Component
public class VetsServiceClient {

    private String hostname = "http://vets-service/";

    private final WebClient.Builder webClientBuilder;

    public VetsServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Fetches a single vet by ID by making a GET request to the vets-service at the URI path /vets/{vetId}. The method constructs the URI using the hostname field and returns a Mono<VetDetails> for asynchronous handling of the single response.
     */
    public Mono<VetDetails> getVet(final int vetId) {
        return webClientBuilder.build()
            .get()
            .uri(hostname + "vets/{vetId}", vetId)
            .retrieve()
            .bodyToMono(VetDetails.class);
    }

    /**
     * Fetches all vets by making a GET request to the vets-service at the URI path /vets. It uses the hostname field to build the request URI and returns a Flux<VetDetails> to stream multiple vet details reactively.
     */
    public Flux<VetDetails> getAllVets() {
        return webClientBuilder.build()
            .get()
            .uri(hostname + "vets")
            .retrieve()
            .bodyToFlux(VetDetails.class);
    }

    void setHostname(String hostname) {
        this.hostname = hostname;
    }
}
