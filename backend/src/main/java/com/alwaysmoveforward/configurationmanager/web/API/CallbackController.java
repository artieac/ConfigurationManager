package com.alwaysmoveforward.configurationmanager.web.API;

import com.alwaysmoveforward.configurationmanager.security.FrontendProperties;
import com.alwaysmoveforward.configurationmanager.security.jwt.JwtService;
import com.alwaysmoveforward.configurationmanager.services.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;

/**
 * Auth0 redirects here with an authorization code after the user signs in.
 * We check the signed {@code state} value from {@link LoginController} (see
 * {@code LoginStateService} — self-verifying, so no session/cookie needed to
 * check it against), exchange the code, and hand the browser back a signed
 * JWT cookie — from this point on the frontend never talks to Auth0 directly.
 */
@RestController
public class CallbackController extends ControllerBase {

    private final AuthService authService;
    private final JwtService jwtService;
    private final FrontendProperties frontendProperties;

    public CallbackController(AuthService authService, JwtService jwtService, FrontendProperties frontendProperties) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.frontendProperties = frontendProperties;
    }

    @GetMapping("/api/auth/callback")
    public ResponseEntity<Void> callback(@RequestParam("code") String code, @RequestParam("state") String state,
                                          HttpServletResponse response) {
        if (!authService.isValidLoginState(state)) {
            return ResponseEntity.status(400).build();
        }

        AuthService.LoginResult result = authService.completeLogin(code);

        addCookie(response, ResponseCookie.from(jwtService.cookieName(), result.jwtToken())
                .httpOnly(true)
                .secure(jwtService.cookieSecure())
                .sameSite("Lax")
                .domain(blankToNull(jwtService.cookieDomain()))
                .path("/")
                .maxAge(Duration.ofSeconds(jwtService.expirationSeconds()))
                .build());

        return ResponseEntity.status(302).location(URI.create(frontendProperties.baseUrl())).build();
    }
}

