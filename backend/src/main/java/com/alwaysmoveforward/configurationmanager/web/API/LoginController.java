package com.alwaysmoveforward.configurationmanager.web.API;

import com.alwaysmoveforward.configurationmanager.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * First hop of the login flow: the frontend navigates the browser here (not an
 * XHR — this is a full-page redirect) and we bounce to Auth0's hosted login
 * page. {@code state} is a signed, short-lived token (see {@code LoginStateService})
 * that {@link CallbackController} verifies by signature and expiry alone — no
 * cookie or server-side session is used to protect this flow against CSRF.
 */
@RestController
public class LoginController extends ControllerBase {

    private final AuthService authService;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/api/auth/login")
    public ResponseEntity<Void> login() {
        String state = authService.generateLoginState();

        return ResponseEntity.status(302)
                .location(URI.create(authService.buildAuthorizeUrl(state)))
                .build();
    }
}

