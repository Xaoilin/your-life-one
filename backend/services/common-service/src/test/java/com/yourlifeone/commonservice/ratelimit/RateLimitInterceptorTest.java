package com.yourlifeone.commonservice.ratelimit;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitInterceptorTest {
    private final RateLimitInterceptor interceptor = new RateLimitInterceptor(
            new UserRateLimiter(new RateLimitProperties(true, 1, 1, 100, Duration.ofMinutes(10))));

    private MockHttpServletRequest requestFrom(String user) {
        var request = new MockHttpServletRequest("GET", "/api/profile/v1/settings");
        if (user != null) request.setUserPrincipal(() -> user);
        return request;
    }

    @Test
    void refusesASignedInUserOnceTheirBucketIsEmpty() {
        assertTrue(interceptor.preHandle(requestFrom("user"), new MockHttpServletResponse(), new Object()));

        var refused = assertThrows(RateLimitExceededException.class,
                () -> interceptor.preHandle(requestFrom("user"), new MockHttpServletResponse(), new Object()));

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, refused.status());
        assertEquals("rate_limited", refused.code());
        assertEquals(60, refused.retryAfterSeconds());
    }

    @Test
    void leavesAnonymousRequestsToTheSecurityRules() {
        for (int request = 0; request < 5; request++) {
            assertTrue(interceptor.preHandle(requestFrom(null), new MockHttpServletResponse(), new Object()));
        }
    }
}
