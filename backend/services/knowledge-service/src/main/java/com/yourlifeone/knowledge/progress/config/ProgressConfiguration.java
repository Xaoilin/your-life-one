package com.yourlifeone.knowledge.progress.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ProgressProperties.class)
public class ProgressConfiguration {
}
