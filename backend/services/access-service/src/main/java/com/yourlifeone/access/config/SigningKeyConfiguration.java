package com.yourlifeone.access.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.text.ParseException;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Loads a persistent product-specific key. Production never generates a throwaway signing key. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AccessProperties.class)
public class SigningKeyConfiguration {
    @Bean
    JWKSet signingKeySet(AccessProperties properties) throws ParseException {
        JWKSet keys = JWKSet.parse(properties.signingKeys());
        var active = keys.getKeyByKeyId(properties.signingKeyId());
        if (!(active instanceof RSAKey rsa) || !rsa.isPrivate() || rsa.size() < 2048) {
            throw new IllegalArgumentException("The active signing key must be a private RSA key of at least 2048 bits.");
        }
        return keys;
    }

    @Bean
    JWKSource<SecurityContext> signingKeys(JWKSet keys) {
        return new ImmutableJWKSet<>(keys);
    }

    @Bean
    JwtEncoder jwtEncoder(JWKSource<SecurityContext> signingKeys) {
        return new NimbusJwtEncoder(signingKeys);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
