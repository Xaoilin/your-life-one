package com.yourlifeone.commonservice.ratelimit;

import io.github.bucket4j.TimeMeter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/** Keeps HTTP rate-limit assertions independent of real-time token refills. */
@TestConfiguration(proxyBeanMethods = false)
public class FixedRateLimitClockConfiguration {
    @Bean
    UserRateLimiter fixedClockUserRateLimiter(RateLimitProperties properties) {
        return new UserRateLimiter(properties, new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                return 0;
            }

            @Override
            public boolean isWallClockBased() {
                return false;
            }
        });
    }
}
