/*  // This file defines the VetDetails record, a data transfer object (DTO) used in the API gateway to encapsulate veterinary data from the vets-service. The record includes immutable fields for the vet's id (unique identifier), firstName, lastName, and a List of Specialty records, each representing a vet's specialty with its own id and name. This DTO supports data transfer between microservices, enabling the API gateway to serve vet information in client responses.
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

import java.util.List;

/**
 * DTO representing a vet with their specialties, as returned by the vets-service.
 */
public record VetDetails(
    int id,
    String firstName,
    String lastName,
    List<Specialty> specialties) {

    public record Specialty(int id, String name) {}
}
