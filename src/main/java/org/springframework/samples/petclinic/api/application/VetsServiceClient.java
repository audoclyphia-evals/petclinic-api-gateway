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
package org.springframework.samples.petclinic.api.application;

import org.springframework.samples.petclinic.api.dto.VetDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Client for the vets-service.
 */
@Component
public class VetsServiceClient {

    private String hostname = "http://vets-service/";

    private final WebClient.Builder webClientBuilder;

    public VetsServiceClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    /**
     * Fetch a single vet by ID.
     */
    public Mono<VetDetails> getVet(final int vetId) {
        return webClientBuilder.build()
            .get()
            .uri(hostname + "vets/{vetId}", vetId)
            .retrieve()
            .bodyToMono(VetDetails.class);
    }

    /**
     * Fetch all vets.
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
