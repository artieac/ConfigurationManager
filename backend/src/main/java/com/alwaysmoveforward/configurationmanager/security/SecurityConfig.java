package com.alwaysmoveforward.configurationmanager.security;

import com.alwaysmoveforward.configurationmanager.data.repositories.ApiKeyRepository;
import com.alwaysmoveforward.configurationmanager.security.apikey.ApiKeyAuthenticationFilter;
import com.alwaysmoveforward.configurationmanager.security.apikey.ApiKeyCrypto;
import com.alwaysmoveforward.configurationmanager.security.jwt.JwtCookieAuthenticationFilter;
import com.alwaysmoveforward.configurationmanager.security.jwt.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;
    private final FrontendProperties frontendProperties;
    private final ApiKeyRepository apiKeyRepository;
    private final ApiKeyCrypto apiKeyCrypto;
    private final CorsProperties corsProperties;

    public SecurityConfig(JwtService jwtService, FrontendProperties frontendProperties,
                           ApiKeyRepository apiKeyRepository, ApiKeyCrypto apiKeyCrypto,
                           CorsProperties corsProperties) {
        this.jwtService = jwtService;
        this.frontendProperties = frontendProperties;
        this.apiKeyRepository = apiKeyRepository;
        this.apiKeyCrypto = apiKeyCrypto;
        this.corsProperties = corsProperties;
    }

    /** ADMIN inherits READ_WRITE's authorities, which inherits READ_ONLY's — keeps @PreAuthorize checks to a single role name. */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("READ_WRITE")
                .role("READ_WRITE").implies("READ_ONLY")
                .build();
    }

    @Bean
    static MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                // Disabled: this is a cookie-authenticated REST API (no server-rendered forms), and the
                // CSRF-cookie-must-be-readable-cross-subdomain setup was causing more problems than the
                // protection was worth here. The JWT cookie is SameSite=Lax, which already blocks it from
                // being sent on genuine cross-site POSTs (forms/fetches from a different registrable
                // domain) — the residual risk this leaves on the table is same-site (sibling subdomain)
                // forgery, which isn't a concern for this deployment.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // CORS preflight requests never carry credentials/cookies (spec behavior, not
                        // something withCredentials controls) — without this, Spring Security rejects
                        // the preflight itself with 401, so the browser never sends the real request.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // Logout only clears the cookie — it has nothing to check a session against, so
                        // there's no reason a missing/invalid/expired JWT cookie should block it from working.
                        .requestMatchers("/api/auth/login", "/api/auth/callback", "/api/auth/logout", "/actuator/health").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new JwtCookieAuthenticationFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
                // Machine-credential counterpart to the JWT cookie filter above — a request is
                // authenticated by at most one of the two, since a browser session carries no bearer
                // token and a CI job carries no session cookie.
                .addFilterBefore(new ApiKeyAuthenticationFilter(apiKeyRepository, apiKeyCrypto), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint((request, response, authException) -> {
                            // authException is the actual thing that made Spring Security invoke this
                            // entry point — reporting it directly (instead of only our own JWT-filter
                            // reason, which is legitimately absent when the JWT cookie itself was fine)
                            // tells us definitively what's rejecting an already-authenticated request.
                            String jwtReason = (String) request.getAttribute(JwtCookieAuthenticationFilter.FAILURE_REASON_ATTRIBUTE);
                            String apiKeyReason = (String) request.getAttribute("sm.apiKeyAuthFailureReason");
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("text/plain;charset=UTF-8");
                            response.getWriter().write(
                                    "authException=" + authException.getClass().getName() + ": " + authException.getMessage()
                                            + " | jwtFilterReason=" + (jwtReason != null ? jwtReason : "(none)")
                                            + " | apiKeyFilterReason=" + (apiKeyReason != null ? apiKeyReason : "(none)"));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            // If this is what's actually firing instead of the entry point above, you'll
                            // see 403 rather than 401 — telling us it's an authorization (CSRF/role)
                            // problem, not an authentication one.
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("text/plain;charset=UTF-8");
                            response.getWriter().write("accessDeniedException=" + accessDeniedException.getClass().getName()
                                    + ": " + accessDeniedException.getMessage());
                        }))
                .logout((httpSecurity)-> httpSecurity
                        .logoutUrl("/api/auth/logout")
                        .clearAuthentication(true)
                        .addLogoutHandler(logoutHandler())
                        .deleteCookies(this.jwtService.cookieName()));


        return http.build();
    }

    private LogoutHandler logoutHandler() {
        return (request, response, authentication) -> {
            try {
                response.addCookie(this.jwtService.generateExpiredCookie());
                response.sendRedirect(this.frontendProperties.baseUrl());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

