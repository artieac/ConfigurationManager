package com.alwaysmoveforward.configurationmanager.services;

import com.alwaysmoveforward.configurationmanager.data.repositories.Auth0Repository;
import com.alwaysmoveforward.configurationmanager.data.repositories.UserRepository;
import com.alwaysmoveforward.configurationmanager.domainmodel.Auth0UserProfile;
import com.alwaysmoveforward.configurationmanager.domainmodel.Role;
import com.alwaysmoveforward.configurationmanager.domainmodel.User;
import com.alwaysmoveforward.configurationmanager.security.jwt.JwtService;
import com.alwaysmoveforward.configurationmanager.security.jwt.LoginStateService;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the login → Auth0 → callback → JWT-cookie flow. The only role
 * assigned automatically is {@link Role#READ_ONLY} for brand-new users — an
 * ADMIN must explicitly promote anyone who needs to write or delete.
 */
@Service
public class AuthService {

    private static final Role DEFAULT_ROLE_FOR_NEW_USERS = Role.READ_ONLY;

    private final Auth0Repository auth0Repository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final LoginStateService loginStateService;

    public AuthService(Auth0Repository auth0Repository, UserRepository userRepository, JwtService jwtService,
                        LoginStateService loginStateService) {
        this.auth0Repository = auth0Repository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.loginStateService = loginStateService;
    }

    public String generateLoginState() {
        return loginStateService.issueState();
    }

    /** Guards against login CSRF — see {@link LoginStateService} for how this stays stateless. */
    public boolean isValidLoginState(String state) {
        return loginStateService.isValid(state);
    }

    public String buildAuthorizeUrl(String state) {
        return auth0Repository.buildAuthorizeUrl(state);
    }

    public record LoginResult(User user, String jwtToken) {
    }

    public LoginResult completeLogin(String code) {
        String accessToken = auth0Repository.exchangeCodeForAccessToken(code);
        Auth0UserProfile profile = auth0Repository.fetchUserProfile(accessToken);

        User user = userRepository.findByAuth0UserId(profile.subject())
                .map(existing -> existing.withProfileRefreshedFrom(profile))
                .orElseGet(() -> User.newFromAuth0Login(profile, DEFAULT_ROLE_FOR_NEW_USERS));

        User saved = userRepository.save(user);
        String token = jwtService.issueToken(saved);
        return new LoginResult(saved, token);
    }
}

