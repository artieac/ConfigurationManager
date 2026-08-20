package com.alwaysmoveforward.configurationmanager.security.apikey;

import com.alwaysmoveforward.configurationmanager.data.Entities.ApiKeyEntity;
import com.alwaysmoveforward.configurationmanager.data.repositories.ApiKeyRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.ApiKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates requests carrying a valid {@code Authorization: Bearer <token>}
 * API key — the machine-to-machine counterpart to {@code JwtCookieAuthenticationFilter}'s
 * browser-session cookie. A request can be authenticated by at most one of the
 * two (a browser session has no bearer token; a CI job has no session cookie),
 * so this leaves the request untouched when the header is absent rather than
 * rejecting outright, exactly like the JWT cookie filter does.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyCrypto apiKeyCrypto;

    public ApiKeyAuthenticationFilter(ApiKeyRepository apiKeyRepository, ApiKeyCrypto apiKeyCrypto) {
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyCrypto = apiKeyCrypto;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Optional<String> bearerToken = extractBearerToken(request);

        if (bearerToken.isEmpty()) {
            request.setAttribute("sm.apiKeyAuthFailureReason", "No 'Authorization: Bearer' token provided");
        } else {
            String hashedBearerToken = apiKeyCrypto.hash(bearerToken.get());
            Optional<ApiKey> apiKey = apiKeyRepository.findByTokenHash(hashedBearerToken);

            if (apiKey.isEmpty()) {
                request.setAttribute("sm.apiKeyAuthFailureReason", "API key not found or revoked");
            } else {
                var authorities = List.of(new SimpleGrantedAuthority(ApiClientPrincipal.AUTHORITY));
                var principal = new ApiClientPrincipal(apiKey.get().id(), apiKey.get().systemId(), apiKey.get().name());
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                apiKeyRepository.touchLastUsed(apiKey.get().id());
            }
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}

