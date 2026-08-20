package com.alwaysmoveforward.configurationmanager.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Authenticates every request purely from the signed JWT cookie set at login —
 * there is no server-side session store. Leaves the request unauthenticated
 * (rather than rejecting outright) when the cookie is missing/invalid, so
 * public endpoints (login redirect, health check) still work; protected
 * endpoints are rejected downstream by Spring Security's authorization rules.
 */
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger LOG = LoggerFactory.getLogger(JwtCookieAuthenticationFilter.class);

    /**
     * Request attribute the entry point reads to explain a 401 in the response body. Kept alongside
     * the response header below (belt and suspenders) since attribute propagation into exception
     * handling turned out to be the thing in doubt.
     */
    public static final String FAILURE_REASON_ATTRIBUTE = "sm.jwtAuthFailureReason";

    /**
     * Same reason, but set directly on the response as a header the moment it's known — response
     * headers, unlike request attributes, can't be lost by anything downstream short of
     * response.reset(), so this is the more reliable of the two while we're debugging this.
     */
    public static final String DEBUG_HEADER = "X-Sm-Jwt-Filter";

    private final JwtService jwtService;

    public JwtCookieAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        Optional<String> cookieValue = extractCookie(request, jwtService.cookieName());
        if (cookieValue.isEmpty()) {
            String reason = "No '" + jwtService.cookieName() + "' cookie present on this request";
            request.setAttribute(FAILURE_REASON_ATTRIBUTE, reason);
            response.setHeader(DEBUG_HEADER, reason);
            LOG.debug("{} ({} {})", reason, request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        Optional<AuthenticatedPrincipal> principal = jwtService.parseAndValidate(cookieValue.get());
        if (principal.isEmpty()) {
            String reason = "'" + jwtService.cookieName() + "' cookie was present but failed validation (expired, wrong signature, or malformed)";
            request.setAttribute(FAILURE_REASON_ATTRIBUTE, reason);
            response.setHeader(DEBUG_HEADER, reason);
        } else {
            var authorities = List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                    principal.get().role().authority()));
            var authentication = new UsernamePasswordAuthenticationToken(principal.get(), null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            response.setHeader(DEBUG_HEADER, "authenticated as user " + principal.get().userId() + " (" + principal.get().role() + ")");
        }

        filterChain.doFilter(request, response);
    }

    private Optional<String> extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        String firstMatch = null;
        int matchCount = 0;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                matchCount++;
                if (firstMatch == null) {
                    firstMatch = cookie.getValue();
                }
            }
        }
        if (matchCount > 1) {
            // Browsers send every cookie whose Domain/Path matches, even if a differently-scoped
            // stale one is still hanging around from an earlier Domain/Path config. We can't control
            // which one the servlet container hands us first, and it may not be the current one.
            LOG.warn("Found {} cookies named '{}' — likely a stale cookie from an earlier Domain/Path "
                    + "config; clear cookies for this site and log in again", matchCount, name);
        }
        return Optional.ofNullable(firstMatch);
    }
}

