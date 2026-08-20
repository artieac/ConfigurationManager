package com.alwaysmoveforward.configurationmanager.web.API;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public abstract class ControllerBase {

    protected void addCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader("Set-Cookie", cookie.toString());
    }

    /**
     * A cookie's `Domain` (and `Path`) attribute must match exactly for a
     * later Set-Cookie to overwrite/clear it — an omitted `Domain` is not
     * the same as the one it was originally set with. Every cookie this app
     * sets or clears should route its domain through this so they always agree.
     */
    protected String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}

