package com.yourlifeone.planner.config;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** The HTTP client for the prayer and knowledge services, with the configured timeouts. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(UpstreamProperties.class)
public class UpstreamConfiguration {
    @Bean
    RestClient upstreamRestClient(UpstreamProperties properties, RestClient.Builder builder) {
        var httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.requestFactory(requestFactory).build();
    }
}
