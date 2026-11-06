package com.yourlifeone.access.identity.api.controller;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publishes public verification keys only; private signing material never leaves the service. */
@RestController
@RequiredArgsConstructor
public class SigningKeysController {
    private final JWKSet keys;

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> keys() {
        return keys.toPublicJWKSet().toJSONObject();
    }
}
