package com.alwaysmoveforward.configurationmanager.data.Entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Mirrors the response body of Auth0's GET /userinfo. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Auth0UserInfoResponseEntity(
        @JsonProperty("sub") String subject,
        @JsonProperty("email") String email,
        @JsonProperty("name") String name) {
}

