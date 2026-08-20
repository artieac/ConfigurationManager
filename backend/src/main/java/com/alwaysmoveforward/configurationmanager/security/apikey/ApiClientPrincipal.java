package com.alwaysmoveforward.configurationmanager.security.apikey;

/** The identity carried by a request authenticated via an API key rather than a user session. */
public record ApiClientPrincipal(Long apiKeyId, Long systemId, String name) {

    public static final String AUTHORITY = "ROLE_API_CLIENT";
}

