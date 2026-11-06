package com.yourlifeone.access.identity.application;

import com.yourlifeone.access.config.AccessProperties;
import com.yourlifeone.access.identity.api.response.SessionResponse;
import com.yourlifeone.access.identity.api.response.SessionUser;
import com.yourlifeone.access.identity.domain.AccountPrincipal;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Browser tokens identify only this account and product. They carry no agent client approval. */
@Service
@RequiredArgsConstructor
public class BrowserAccessTokens {
    private final AccessProperties properties;
    private final JwtEncoder encoder;
    private final Clock clock;

    public SessionResponse issue(AccountPrincipal account) {
        var issuedAt = clock.instant();
        var expiresAt = issuedAt.plus(properties.accessTokenLifetime());
        var claims = JwtClaimsSet.builder()
                .issuer(properties.publicUrl().toString())
                .subject(account.userId().toString())
                .audience(List.of(properties.audience()))
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", "authenticated")
                .build();
        var header = JwsHeader.with(SignatureAlgorithm.RS256).keyId(properties.signingKeyId()).build();
        var token = encoder.encode(JwtEncoderParameters.from(header, claims));
        return new SessionResponse(new SessionUser(account.userId(), account.email()), token.getTokenValue(), expiresAt);
    }
}
