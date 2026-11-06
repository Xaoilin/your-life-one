package com.yourlifeone.access.identity.api.response;

import java.util.UUID;

public record SessionUser(UUID id, String email) {
}
