package com.alwaysmoveforward.configurationmanager.data.repositories;

import com.alwaysmoveforward.configurationmanager.data.Entities.Auth0TokenResponseEntity;
import com.alwaysmoveforward.configurationmanager.data.Entities.Auth0UserInfoResponseEntity;
import com.alwaysmoveforward.configurationmanager.domainmodel.Auth0UserProfile;
import com.alwaysmoveforward.configurationmanager.security.Auth0.Auth0Properties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Repository;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;

/**
 * The only class in the codebase that talks to Auth0 directly. Callers get back
 * domain objects/primitives — never the raw Auth0 API response shapes.
 */
@Repository
public class Auth0Repository {

    private final Auth0Properties properties;
    private final RestClient restClient;

    public Auth0Repository(Auth0Properties properties) {
        this.properties = properties;
        this.restClient = RestClient.create();
    }

    public String buildAuthorizeUrl(String state) {
        return UriComponentsBuilder.fromUriString("https://" + properties.domain() + "/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", properties.clientId())
                .queryParam("redirect_uri", properties.callbackUrl())
                .queryParam("scope", "openid profile email")
                .queryParamIfPresent("audience", java.util.Optional.ofNullable(emptyToNull(properties.audience())))
                .queryParam("state", state)
                .encode(StandardCharsets.UTF_8)
                .build()
                .toUriString();
    }

    /** Exchanges an authorization code for tokens and returns the access token to use against /userinfo. */
    public String exchangeCodeForAccessToken(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("code", code);
        form.add("redirect_uri", properties.callbackUrl());

        Auth0TokenResponseEntity response = restClient.post()
                .uri("https://" + properties.domain() + "/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Auth0TokenResponseEntity.class);

        return response != null ? response.accessToken() : null;
    }

    public Auth0UserProfile fetchUserProfile(String accessToken) {
        Auth0UserInfoResponseEntity response = restClient.get()
                .uri("https://" + properties.domain() + "/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Auth0UserInfoResponseEntity.class);

        if (response == null) {
            throw new IllegalStateException("Auth0 /userinfo returned an empty response");
        }
        return new Auth0UserProfile(response.subject(), response.email(), response.name());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

