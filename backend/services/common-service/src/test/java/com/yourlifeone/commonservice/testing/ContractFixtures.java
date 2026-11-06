package com.yourlifeone.commonservice.testing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * Consumer contract fixtures shared with the React app: one JSON file per response, holding the
 * request, the status and an example body. The React app uses them as its API mocks; this class
 * checks the provider's real responses still have the same shape.
 *
 * <p>Run with {@code -Dcontracts.record=true} to (re)write fixtures from real responses.
 */
public final class ContractFixtures {
    private static final JsonMapper JSON = JsonMapper.builder().enable(SerializationFeature.INDENT_OUTPUT).build();

    private final Path directory;

    /** @param directory relative to the repository root, e.g. {@code contracts/prayer-service} */
    public ContractFixtures(String directory) {
        this.directory = repositoryRoot().resolve(directory);
    }

    /** Shared request examples are checked by both deployed sides of the API. */
    public String requestBody(String name) {
        return read(directory.resolve(name + ".json")).path("request").path("body").toString();
    }

    public static boolean recording() {
        return Boolean.getBoolean("contracts.record");
    }

    /** Records the fixture, or verifies the actual response against it. */
    public void check(String name, String method, String path, int status, String actualBody) {
        var actual = JSON.readTree(actualBody == null || actualBody.isEmpty() ? "null" : actualBody);
        var file = directory.resolve(name + ".json");
        if (recording()) {
            write(file, method, path, status, actual);
            return;
        }
        if (!Files.exists(file)) {
            throw new AssertionError("Missing contract fixture " + file + "; run with -Dcontracts.record=true");
        }
        var fixture = read(file);
        var mismatches = new ArrayList<String>();
        if (fixture.path("status").asInt() != status) {
            mismatches.add("status: expected " + fixture.path("status").asInt() + " but was " + status);
        }
        compareShape(fixture.path("body"), actual, "$", mismatches);
        if (!mismatches.isEmpty()) {
            throw new AssertionError("Response for " + name + " no longer matches the React app's contract:\n  "
                    + String.join("\n  ", mismatches));
        }
    }

    /**
     * Every field in the fixture must exist in the response with the same JSON type. A null in the
     * fixture accepts any value; each response array element must match the fixture's first element.
     */
    static void compareShape(JsonNode expected, JsonNode actual, String path, List<String> mismatches) {
        if (expected.isNull() || expected.isMissingNode()) return;
        if (actual == null || actual.isMissingNode()) {
            mismatches.add(path + ": missing");
            return;
        }
        if (!sameType(expected, actual)) {
            mismatches.add(path + ": expected " + expected.getNodeType() + " but was " + actual.getNodeType());
            return;
        }
        if (expected.isObject()) {
            for (Iterator<String> names = expected.propertyNames().iterator(); names.hasNext(); ) {
                var name = names.next();
                compareShape(expected.get(name), actual.get(name), path + "." + name, mismatches);
            }
        } else if (expected.isArray() && !expected.isEmpty()) {
            for (int index = 0; index < actual.size(); index++) {
                compareShape(expected.get(0), actual.get(index), path + "[" + index + "]", mismatches);
            }
        }
    }

    private static boolean sameType(JsonNode expected, JsonNode actual) {
        return expected.isNumber() ? actual.isNumber() : expected.getNodeType() == actual.getNodeType();
    }

    private static void write(Path file, String method, String path, int status, JsonNode body) {
        ObjectNode fixture = JSON.createObjectNode();
        fixture.putObject("request").put("method", method).put("path", path);
        fixture.put("status", status);
        fixture.set("body", body);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, JSON.writeValueAsString(fixture) + "\n");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static JsonNode read(Path file) {
        try {
            return JSON.readTree(Files.readString(file));
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static Path repositoryRoot() {
        var current = Path.of("").toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("mvnw"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Repository root with mvnw not found");
        return current;
    }
}
