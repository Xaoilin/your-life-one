package com.yourlifeone.access.agent.application;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded single-instance limits protect public registration and token exchange before parsing request bodies. */
public class AgentEndpointLimits extends OncePerRequestFilter {
    private final Cache<String, Bucket> addresses = Caffeine.newBuilder()
            .maximumSize(10_000).expireAfterAccess(Duration.ofHours(1)).build();
    private final Bucket registrations = bucket(100, Duration.ofHours(1));
    private final Bucket tokens = bucket(10_000, Duration.ofMinutes(1));

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        boolean registration = path.equals("/oauth2/register");
        if (!"POST".equals(request.getMethod()) || (!registration && !path.equals("/oauth2/token"))) {
            chain.doFilter(request, response);
            return;
        }
        if (request.getContentLengthLong() > 32_768) {
            response.sendError(413, "The request is too large.");
            return;
        }
        // The immediate peer is trusted. Caddy applies the public request-size limit and never exposes this port.
        String key = path + ":" + request.getRemoteAddr();
        var address = addresses.get(key, ignored -> registration ? bucket(20, Duration.ofHours(1)) : bucket(120, Duration.ofMinutes(1)));
        if (!address.tryConsume(1) || !(registration ? registrations : tokens).tryConsume(1)) {
            response.setHeader("Retry-After", registration ? "3600" : "60");
            response.sendError(429, "Too many requests. Please wait and try again.");
            return;
        }
        chain.doFilter(request, response);
    }

    private static Bucket bucket(long capacity, Duration window) {
        return Bucket.builder().addLimit(Bandwidth.builder().capacity(capacity).refillIntervally(capacity, window).build()).build();
    }
}
