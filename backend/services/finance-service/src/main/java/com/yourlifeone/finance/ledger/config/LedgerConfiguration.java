package com.yourlifeone.finance.ledger.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Ledger settings. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LedgerProperties.class)
public class LedgerConfiguration {
}
