package com.proyecto.servicios.dto.onboarding.sepomex;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZippoPostalResponse {

    @JsonProperty("post code")
    private String postCode;

    @JsonProperty("country")
    private String country;

    @JsonProperty("country abbreviation")
    private String countryAbbreviation;

    @JsonProperty("places")
    private List<ZippoPlaceResponse> places;
}