/*  // OwnerSummary.java
//
// This DTO record represents a lightweight summary of an owner, designed for efficient API responses.
// It contains essential owner details (ID, first name, last name, address, city, telephone) and aggregated
// counts: petCount (total pets owned) and visitCount (total visits across all pets). These counts are
// derived from the visits-service through service aggregation logic, as seen in the application's API gateway.
// This record is used in endpoints like the search endpoint to provide concise owner information without
// full entity overhead, optimizing network and processing efficiency.
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
package org.springframework.samples.petclinic.api.dto;

/**
 * Lightweight owner summary returned by the search endpoint.
 * Includes the owner's basic details, their pet count, and total visit count
 * across all pets (aggregated from the visits-service).
 */
public record OwnerSummary(
    int id,
    String firstName,
    String lastName,
    String address,
    String city,
    String telephone,
    int petCount,
    int visitCount) {
}
