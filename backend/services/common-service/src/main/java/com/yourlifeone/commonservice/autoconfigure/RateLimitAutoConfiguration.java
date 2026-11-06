package com.yourlifeone.commonservice.autoconfigure;

import com.yourlifeone.commonservice.ratelimit.RateLimitInterceptor;
import com.yourlifeone.commonservice.ratelimit.RateLimitProperties;
import com.yourlifeone.commonservice.ratelimit.UserRateLimiter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Limits every service's API per signed-in user ({@code app.rate-limit.*}); on unless disabled. */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "app.rate-limit", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    UserRateLimiter userRateLimiter(RateLimitProperties properties) {
        return new UserRateLimiter(properties);
    }

    /** Runs before every other interceptor, so a refused request does no work. */
    @Bean
    WebMvcConfigurer rateLimitWebMvcConfigurer(UserRateLimiter limiter) {
        return new WebMvcConfigurer() {
            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new RateLimitInterceptor(limiter))
                        .addPathPatterns("/api/**")
                        .order(Ordered.HIGHEST_PRECEDENCE);
            }
        };
    }
}
