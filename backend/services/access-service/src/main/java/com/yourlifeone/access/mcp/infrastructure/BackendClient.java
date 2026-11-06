package com.yourlifeone.access.mcp.infrastructure;

import com.yourlifeone.access.mcp.config.McpBackendProperties;
import com.yourlifeone.access.mcp.domain.BackendCall;
import com.yourlifeone.access.mcp.domain.BackendCallFailure;
import com.yourlifeone.access.mcp.domain.BackendService;
import com.yourlifeone.access.mcp.domain.McpDomain;
import java.net.http.HttpClient;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

/** Calls only configured product services; credentials never follow redirects or caller-selected URLs. */
@Component
public class BackendClient {
    private final Map<BackendService, RestClient> clients = new EnumMap<>(BackendService.class);
    private final ObjectMapper json;

    public BackendClient(McpBackendProperties properties, ObjectMapper json) {
        this.json = json;
        var requests = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(properties.timeout()).followRedirects(HttpClient.Redirect.NEVER).build());
        requests.setReadTimeout(properties.timeout());
        clients.put(BackendService.LIFE, RestClient.builder().requestFactory(requests).baseUrl(properties.life().toString()).build());
        clients.put(BackendService.FINANCE, RestClient.builder().requestFactory(requests).baseUrl(properties.finance().toString()).build());
        clients.put(BackendService.KNOWLEDGE, RestClient.builder().requestFactory(requests).baseUrl(properties.knowledge().toString()).build());
    }

    public boolean canAccess(McpDomain domain, Jwt token) {
        try {
            call(BackendCall.builder().service(domain.backend()).method(HttpMethod.GET).path(domain.accessPath()).build(), token);
            return true;
        } catch (BackendCallFailure failure) {
            if (failure.getStatus() == 401 || failure.getStatus() == 403) return false;
            throw failure;
        }
    }

    public Object call(BackendCall call, Jwt token) {
        try {
            var request = clients.get(call.service()).method(call.method()).uri(builder -> {
                builder.path(call.path());
                var variables = new LinkedHashMap<>(call.variables());
                call.query().forEach((name, value) -> {
                    if (value != null) {
                        var parameter = "__query_" + name;
                        builder.queryParam(name, "{" + parameter + "}");
                        variables.put(parameter, value);
                    }
                });
                return builder.build(variables);
            }).header(HttpHeaders.AUTHORIZATION, "Bearer " + token.getTokenValue()).accept(MediaType.APPLICATION_JSON);
            if (call.requestId() != null) request.header("Idempotency-Key", call.requestId());
            if (call.body() != null) request.contentType(MediaType.APPLICATION_JSON).body(call.body());
            return request.retrieve().body(Object.class);
        } catch (RestClientResponseException failure) {
            throw responseFailure(failure);
        } catch (RestClientException failure) {
            throw new BackendCallFailure("service_unavailable", 503, "The product service is unavailable. Retry later.");
        }
    }

    private BackendCallFailure responseFailure(RestClientResponseException failure) {
        var code = "service_rejected";
        var message = "The product service rejected this operation.";
        try {
            var error = json.readTree(failure.getResponseBodyAsString());
            if (error.path("code").isString()) code = error.path("code").asText();
            if (error.path("message").isString()) message = error.path("message").asText();
        } catch (RuntimeException ignored) {
            // Never expose upstream HTML, transport internals or arbitrary response text.
        }
        if (message.length() > 1024) message = message.substring(0, 1024);
        return new BackendCallFailure(code, failure.getStatusCode().value(), message);
    }
}
