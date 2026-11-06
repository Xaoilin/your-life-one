package com.yourlifeone.life.testing;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/** Signed-in requests for the life-service tests: as the app, or as an AI agent (an OAuth client). */
public final class LifeRequests {
    private final MockMvc mockMvc;

    public LifeRequests(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    public ResultActions as(UUID user, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(user.toString()))));
    }

    /** A Supabase OAuth token issued to an agent carries its client_id. */
    public ResultActions asAgent(UUID user, String clientId, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.with(jwt().jwt(token -> token.subject(user.toString())
                .claim("client_id", clientId))));
    }

    /** Sends the same request from several threads at once and returns every result. */
    public static List<MvcResult> concurrently(int times, Attempt attempt) throws Exception {
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(times);
        try {
            var futures = new ArrayList<Future<MvcResult>>();
            for (int index = 0; index < times; index++) {
                var number = index;
                Callable<MvcResult> call = () -> {
                    start.await();
                    return attempt.run(number).andReturn();
                };
                futures.add(pool.submit(call));
            }
            start.countDown();
            var results = new ArrayList<MvcResult>();
            for (var future : futures) results.add(future.get());
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    public static String body(MvcResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    /** One of several concurrent requests; {@code number} tells them apart. */
    @FunctionalInterface
    public interface Attempt {
        ResultActions run(int number) throws Exception;
    }
}
