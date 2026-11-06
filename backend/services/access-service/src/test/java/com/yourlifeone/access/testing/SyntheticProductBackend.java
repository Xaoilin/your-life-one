package com.yourlifeone.access.testing;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import tools.jackson.databind.json.JsonMapper;

/** Content-free approval probes and recorded synthetic API requests; no real product is contacted. */
public final class SyntheticProductBackend implements AutoCloseable {
    private final HttpServer server;
    private final JsonMapper json = JsonMapper.builder().build();
    private final List<RecordedBackendRequest> requests = new CopyOnWriteArrayList<>();
    private volatile String reviewBody = "{\"review\":null}";
    private volatile Set<String> allowedDomains = Set.of("finance", "equity", "inventory", "employment");

    public SyntheticProductBackend() throws IOException {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        requests.clear();
        reviewBody = "{\"review\":null}";
        allow(Set.of("finance", "equity", "inventory", "employment"));
    }

    public void review(String body) {
        reviewBody = body;
    }

    public void allow(Set<String> domains) {
        allowedDomains = Set.copyOf(domains);
    }

    public List<RecordedBackendRequest> requests() {
        return List.copyOf(requests);
    }

    private void handle(HttpExchange exchange) throws IOException {
        var path = exchange.getRequestURI().getPath();
        var token = exchange.getRequestHeaders().getFirst("Authorization").substring("Bearer ".length());
        var claims = json.readTree(Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        if (path.endsWith("/access")) {
            String domain = path.contains("/review/") ? "finance" : path.contains("/equity/") ? "equity"
                    : path.contains("/inventory/") ? "inventory" : "employment";
            boolean allowed = allowedDomains.contains(domain) && "native-agent".equals(claims.path("client_id").asText());
            exchange.sendResponseHeaders(allowed ? 204 : 403, -1);
            exchange.close();
            return;
        }
        var rawBody = exchange.getRequestBody().readAllBytes();
        requests.add(RecordedBackendRequest.builder().method(exchange.getRequestMethod()).path(path)
                .query(exchange.getRequestURI().getRawQuery()).requestId(exchange.getRequestHeaders().getFirst("Idempotency-Key"))
                .userId(claims.path("sub").asText()).clientId(claims.path("client_id").asText())
                .body(rawBody.length == 0 ? null : json.readTree(rawBody)).build());
        String response = path.equals("/api/finance/v1/review") ? reviewBody
                : path.endsWith("/positions") ? "{\"positions\":[],\"total\":0}"
                : path.endsWith("/projects/resolve") ? "{\"projects\":[]}"
                : "{\"id\":\"synthetic-record\",\"items\":[],\"needs\":[]}";
        var bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
