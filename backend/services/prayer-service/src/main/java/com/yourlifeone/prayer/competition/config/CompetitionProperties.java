package com.yourlifeone.prayer.competition.config;

import java.net.URI;
import java.util.UUID;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Disabled until an operator configures an explicit pair. Keys stay in server secret configuration. */
@ConfigurationProperties("app.prayer.competition")
public record CompetitionProperties(UUID ownerId, String readKey, URI peerUrl, String peerKey) {
    public boolean canShare() {
        return ownerId != null && readKey != null && readKey.length() >= 32;
    }

    public boolean canReadPeer() {
        return canShare() && peerUrl != null && "https".equals(peerUrl.getScheme())
                && peerUrl.getHost() != null && "/api/prayer/v1/competition/summary".equals(peerUrl.getPath())
                && peerUrl.getUserInfo() == null && peerUrl.getQuery() == null && peerUrl.getFragment() == null
                && peerKey != null && peerKey.length() >= 32;
    }

    @Override
    public String toString() {
        return "CompetitionProperties[configured=" + canReadPeer() + "]";
    }
}
