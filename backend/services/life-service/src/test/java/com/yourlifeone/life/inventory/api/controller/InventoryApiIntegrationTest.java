package com.yourlifeone.life.inventory.api.controller;

import com.yourlifeone.commonservice.command.IdempotencyKeys;
import com.yourlifeone.commonservice.testing.PostgresTestContainer;
import com.yourlifeone.commonservice.testing.RabbitTestContainer;
import com.yourlifeone.life.testing.LifeRequests;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Inventory through the full request path against a real PostgreSQL and RabbitMQ. */
@SpringBootTest(properties = "spring.datasource.hikari.data-source-properties.sslmode=disable")
@AutoConfigureMockMvc
@Import({PostgresTestContainer.class, RabbitTestContainer.class})
class InventoryApiIntegrationTest {
    private static final String INVENTORY = "/api/life/v1/inventory";
    private static final String ITEM = """
            {"name":" Rail slides ","category":"component","subcategory":"mechanical_hardware",
             "imageUrl":"https://m.media-amazon.com/images/I/rail.jpg","trackingMode":"counted","quantity":2,
             "unit":"pairs","brand":"Acme","dimensions":{"length":550,"unit":"mm"},
             "specifications":{"bearing":"Ball bearing","mount":"Side mount"},"condition":"new",
             "tags":["drawer"," drawer ","desk"],"notes":"","projectCatalogKeys":["magnus-desk"]}""";
    private static final String NEED = """
            {"name":"Screws M4","category":"fastener","subcategory":"screws_fasteners","requiredQuantity":20,
             "unit":"pcs","specifications":{"thread":"M4"},"priority":"high","status":"needed","notes":""}""";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    private LifeRequests requests;

    @BeforeEach
    void setUp() {
        requests = new LifeRequests(mockMvc);
    }

    private void saveItem(UUID user, String id, String body) throws Exception {
        requests.as(user, put(INVENTORY + "/items/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    @Test
    void savesAnItemWithDimensionsSpecificationsAndTidiedTags() throws Exception {
        var user = UUID.randomUUID();
        saveItem(user, "rail", ITEM);

        requests.as(user, get(INVENTORY))
                .andExpect(jsonPath("$.items[0].name").value("Rail slides"))
                .andExpect(jsonPath("$.items[0].dimensions.length").value(550))
                .andExpect(jsonPath("$.items[0].dimensions.width").doesNotExist())
                .andExpect(jsonPath("$.items[0].specifications.mount").value("Side mount"))
                .andExpect(jsonPath("$.items[0].tags.length()").value(2))
                .andExpect(jsonPath("$.items[0].lastVerifiedAt").isNotEmpty());
        // Dropping dimensions and a specification replaces them.
        saveItem(user, "rail", ITEM.replace(",\"dimensions\":{\"length\":550,\"unit\":\"mm\"}", "")
                .replace(",\"mount\":\"Side mount\"", ""));
        requests.as(user, get(INVENTORY))
                .andExpect(jsonPath("$.items[0].dimensions").doesNotExist())
                .andExpect(jsonPath("$.items[0].specifications.mount").doesNotExist());
    }

    @Test
    void refusesASubcategoryOutsideItsCategoryAndNonHttpsImages() throws Exception {
        var user = UUID.randomUUID();

        requests.as(user, put(INVENTORY + "/items/x").contentType(MediaType.APPLICATION_JSON)
                        .content(ITEM.replace("\"component\"", "\"tool\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_subcategory"));
        requests.as(user, put(INVENTORY + "/items/x").contentType(MediaType.APPLICATION_JSON)
                        .content(ITEM.replace("https://m.media", "http://m.media")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_image_url"));
        requests.as(user, put(INVENTORY + "/items/x").contentType(MediaType.APPLICATION_JSON)
                        .content(ITEM.replace("{\"length\":550,\"unit\":\"mm\"}", "{\"unit\":\"mm\"}")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("invalid_dimensions"));
    }

    @Test
    void aBatchEnrichesExistingItemsKeepingUnspecifiedFieldsAndCreatesNewOnes() throws Exception {
        var user = UUID.randomUUID();
        saveItem(user, "rail", ITEM);

        requests.as(user, post(INVENTORY + "/items").contentType(MediaType.APPLICATION_JSON).content("""
                        {"items":[{"id":"rail","model":"R-550"},{"name":"PLA filament","quantity":3,
                          "category":"material","subcategory":"filament","unit":"spools"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].model").value("R-550"))
                .andExpect(jsonPath("$.items[0].brand").value("Acme"))
                .andExpect(jsonPath("$.items[0].specifications.bearing").value("Ball bearing"))
                .andExpect(jsonPath("$.items[1].trackingMode").value("counted"))
                .andExpect(jsonPath("$.items[1].condition").value("unknown"));
        requests.as(user, post(INVENTORY + "/items").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"name\":\"No quantity\"}]}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("incomplete_item"));
    }

    @Test
    void searchesOwnedItemsAndOpenNeedsAndChecksStock() throws Exception {
        var user = UUID.randomUUID();
        saveItem(user, "rail", ITEM);
        saveItem(user, "old", ITEM.replace("Rail slides", "Old rail slides"));
        requests.as(user, post(INVENTORY + "/items/old/archive")).andExpect(jsonPath("$.archivedAt").isNotEmpty());
        requests.as(user, put(INVENTORY + "/needs/screws").contentType(MediaType.APPLICATION_JSON).content(NEED))
                .andExpect(status().isOk());

        requests.as(user, get(INVENTORY + "/search").param("query", "BALL bearing"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value("rail"));
        requests.as(user, get(INVENTORY + "/search").param("projectCatalogKey", "magnus-desk"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.needs.length()").value(0));
        requests.as(user, get(INVENTORY + "/search").param("query", "m4"))
                .andExpect(jsonPath("$.needs[0].id").value("screws"));
        requests.as(user, get(INVENTORY + "/check").param("name", "rail").param("requiredQuantity", "2")
                        .param("unit", "PAIRS"))
                .andExpect(jsonPath("$.sufficient").value(true))
                .andExpect(jsonPath("$.matches.length()").value(1));
        requests.as(user, get(INVENTORY + "/check").param("name", "rail").param("requiredQuantity", "3"))
                .andExpect(jsonPath("$.sufficient").value(false));
    }

    @Test
    void acquiringANeedAddsToTheLinkedItemOrCreatesOneExactlyOnce() throws Exception {
        var user = UUID.randomUUID();
        saveItem(user, "screw-box", ITEM.replace("Rail slides", "M4 screws").replace("\"pairs\"", "\"pcs\""));
        requests.as(user, put(INVENTORY + "/needs/linked").contentType(MediaType.APPLICATION_JSON)
                .content(NEED.replace("\"requiredQuantity\"", "\"linkedItemId\":\"screw-box\",\"requiredQuantity\"")))
                .andExpect(status().isOk());
        requests.as(user, put(INVENTORY + "/needs/fresh").contentType(MediaType.APPLICATION_JSON)
                .content(NEED.replace("\"needed\"", "\"ordered\""))).andExpect(jsonPath("$.orderedAt").isNotEmpty());

        requests.as(user, post(INVENTORY + "/needs/linked/acquire"))
                .andExpect(jsonPath("$.need.status").value("acquired"))
                .andExpect(jsonPath("$.item.quantity").value(22));
        requests.as(user, post(INVENTORY + "/needs/linked/acquire")).andExpect(jsonPath("$.item.quantity").value(22));
        requests.as(user, post(INVENTORY + "/needs/fresh/acquire").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newItemId\":\"new-screws\"}"))
                .andExpect(jsonPath("$.item.id").value("new-screws"))
                .andExpect(jsonPath("$.item.condition").value("new"))
                .andExpect(jsonPath("$.need.linkedItemId").value("new-screws"));
        requests.as(user, put(INVENTORY + "/needs/other").contentType(MediaType.APPLICATION_JSON)
                        .content(NEED.replace("\"needed\"", "\"acquired\"")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("acquire_through_stock"));
    }

    @Test
    void anAcquisitionIntoAnItemWithAnotherUnitIsRefused() throws Exception {
        var user = UUID.randomUUID();
        saveItem(user, "rail", ITEM);
        requests.as(user, put(INVENTORY + "/needs/n").contentType(MediaType.APPLICATION_JSON)
                .content(NEED.replace("\"requiredQuantity\"", "\"linkedItemId\":\"rail\",\"requiredQuantity\"")));

        requests.as(user, post(INVENTORY + "/needs/n/acquire"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("unit_mismatch"));
    }

    @Test
    void concurrentDuplicateAcquisitionsAddStockOnce() throws Exception {
        var user = UUID.randomUUID();
        saveItem(user, "box", ITEM.replace("\"pairs\"", "\"pcs\""));
        requests.as(user, put(INVENTORY + "/needs/n").contentType(MediaType.APPLICATION_JSON)
                .content(NEED.replace("\"requiredQuantity\"", "\"linkedItemId\":\"box\",\"requiredQuantity\"")));

        var sameKey = LifeRequests.concurrently(8, attempt -> requests.as(user, post(INVENTORY + "/needs/n/acquire")
                .header(IdempotencyKeys.HEADER, "acquire-n")));
        var differentKeys = LifeRequests.concurrently(8, attempt -> requests.as(user,
                post(INVENTORY + "/needs/n/acquire").header(IdempotencyKeys.HEADER, "acquire-n-" + attempt)));

        for (var response : sameKey) assertEquals(200, response.getResponse().getStatus());
        for (var response : differentKeys) assertEquals(200, response.getResponse().getStatus());
        assertEquals(22, JdbcClient.create(dataSource).sql("SELECT quantity FROM inventory.item WHERE user_id = ?")
                .param(user).query(Integer.class).single());
    }
}
