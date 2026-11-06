package com.yourlifeone.access.identity.domain;

import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;

/** Minimal browser-session identity; provider access and refresh tokens are deliberately absent. */
public record AccountPrincipal(UUID userId, String email) implements Principal, Serializable {
    @Override
    public String getName() {
        return userId.toString();
    }
}
