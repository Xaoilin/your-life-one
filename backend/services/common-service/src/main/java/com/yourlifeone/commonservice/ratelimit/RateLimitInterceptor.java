package com.yourlifeone.commonservice.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Refuses a signed-in user's request once their bucket is empty, before any work (or queued write)
 * starts. Anonymous requests, such as health checks, are not limited here.
 */
public class RateLimitInterceptor implements HandlerInterceptor {
    private final UserRateLimiter limiter;

    public RateLimitInterceptor(UserRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        var user = request.getUserPrincipal();
        if (user == null) {
            return true;
        }
        var decision = limiter.tryAcquire(user.getName());
        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision.retryAfterSeconds());
        }
        return true;
    }
}
