package com.yourlifeone.commonservice.testing;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractFixturesTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private static ArrayList<String> compare(String expected, String actual) {
        var mismatches = new ArrayList<String>();
        ContractFixtures.compareShape(JSON.readTree(expected), JSON.readTree(actual), "$", mismatches);
        return mismatches;
    }

    @Test
    void acceptsTheSameShapeWithOtherValuesAndExtraFields() {
        assertTrue(compare("{\"id\":\"a\",\"count\":1,\"items\":[{\"ok\":true}],\"note\":null}",
                "{\"id\":\"b\",\"count\":2.5,\"items\":[{\"ok\":false},{\"ok\":true}],\"note\":\"x\",\"extra\":1}").isEmpty());
    }

    @Test
    void reportsMissingFieldsAndChangedTypes() {
        var mismatches = compare("{\"id\":\"a\",\"items\":[{\"ok\":true}],\"count\":1}",
                "{\"items\":[{\"ok\":\"yes\"}],\"count\":\"1\"}");

        assertEquals(3, mismatches.size());
        assertTrue(mismatches.contains("$.id: missing"));
        assertTrue(mismatches.contains("$.items[0].ok: expected BOOLEAN but was STRING"));
        assertTrue(mismatches.contains("$.count: expected NUMBER but was STRING"));
    }
}
