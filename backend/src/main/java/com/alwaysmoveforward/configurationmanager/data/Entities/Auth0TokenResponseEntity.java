package com.alwaysmoveforward.configurationmanager.data.Entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Mirrors the response body of Auth0's POST /oauth/token. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Auth0TokenResponseEntity(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("expires_in") long expiresIn) {
}

