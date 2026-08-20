package com.alwaysmoveforward.configurationmanager.web.API;

import com.alwaysmoveforward.configurationmanager.security.jwt.AuthenticatedPrincipal;
import com.alwaysmoveforward.configurationmanager.security.jwt.JwtService;
import com.alwaysmoveforward.configurationmanager.web.Models.CurrentUserViewModel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController extends ControllerBase {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/api/auth/me")
    public CurrentUserViewModel me(@AuthenticationPrincipal AuthenticatedPrincipal principal) {
        return CurrentUserViewModel.from(principal);
    }
}

