package com.yourlifeone.finance.review.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Banking review settings. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ReviewProperties.class)
public class ReviewConfiguration {
}
