
package com.railbook.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TestAuthController {

    @GetMapping("/api/auth/me")
    public Map<String, String> currentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal().equals("anonymousUser")) {
            return Map.of("message", "Not authenticated");
        }

        return Map.of(
                "email", authentication.getName(),
                "message", "Authenticated session is active"
        );
    }
}
