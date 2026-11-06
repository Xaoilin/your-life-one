package com.yourlifeone.finance.equity.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Equity settings. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(EquityProperties.class)
public class EquityConfiguration {
}
