package com.yourlifeone.planner.testing;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import static org.hamcrest.Matchers.endsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

/**
 * The prayer and knowledge services, as the planner calls them, answered by a {@link MockRestServiceServer}
 * from what each test set in {@link PrayerOutcomes} and {@link KnowledgeCounts} for the caller (the forwarded
 * token names the user, see {@link PlannerRequests}).
 */
@TestConfiguration(proxyBeanMethods = false)
public class UpstreamStubs {
    @Bean
    PrayerOutcomes prayerOutcomes(JsonMapper json) {
        return new PrayerOutcomes(json);
    }

    @Bean
    KnowledgeCounts knowledgeCounts(JsonMapper json) {
        return new KnowledgeCounts(json);
    }

    @Bean
    @Primary
    RestClient stubbedUpstreamRestClient(PrayerOutcomes prayer, KnowledgeCounts knowledge) {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();
        server.expect(ExpectedCount.manyTimes(), requestTo(endsWith(PrayerOutcomes.PATH)))
                .andExpect(method(HttpMethod.GET)).andRespond(prayer::respond);
        server.expect(ExpectedCount.manyTimes(), requestTo(endsWith(KnowledgeCounts.PATH)))
                .andExpect(method(HttpMethod.GET)).andRespond(knowledge::respond);
        return builder.build();
    }
}
