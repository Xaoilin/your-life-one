package com.yourlifeone.commonservice.web;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpErrorLoggingFilterTest {
    private final HttpErrorLoggingFilter filter = new HttpErrorLoggingFilter();
    private Logger filterLogger;
    private Level previousLevel;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void captureFilterLogs() {
        filterLogger = (Logger) LoggerFactory.getLogger(HttpErrorLoggingFilter.class);
        previousLevel = filterLogger.getLevel();
        filterLogger.setLevel(Level.TRACE);
        appender = new ListAppender<>();
        appender.start();
        filterLogger.addAppender(appender);
    }

    @AfterEach
    void stopCapturingFilterLogs() {
        filterLogger.detachAppender(appender);
        filterLogger.setLevel(previousLevel);
        appender.stop();
    }

    @Test
    void logsEveryClientAndServerErrorResponseWithoutQueryParameters() throws Exception {
        var notFound = new MockHttpServletRequest("GET", "/missing");
        notFound.setQueryString("token=private");
        filter.doFilterInternal(notFound, new MockHttpServletResponse(), (request, response) ->
                ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_NOT_FOUND));

        var unavailable = new MockHttpServletRequest("GET", "/api/prayer/health/database");
        filter.doFilterInternal(unavailable, new MockHttpServletResponse(), (request, response) ->
                ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE));

        var events = appender.list;
        assertEquals(2, events.size());
        assertEquals(Level.WARN, events.get(0).getLevel());
        assertEquals(Level.ERROR, events.get(1).getLevel());
        assertTrue(events.get(0).getFormattedMessage().contains("method=GET path=/missing status=404"));
        assertTrue(events.get(1).getFormattedMessage()
                .contains("method=GET path=/api/prayer/health/database status=503"));
        assertTrue(events.stream().noneMatch(event -> event.getFormattedMessage().contains("private")));
    }

    @Test
    void doesNotLogSuccessfulResponses() throws Exception {
        filter.doFilterInternal(new MockHttpServletRequest("GET", "/api/prayer/health"),
                new MockHttpServletResponse(), (request, response) ->
                        ((HttpServletResponse) response).setStatus(HttpServletResponse.SC_OK));

        assertTrue(appender.list.isEmpty());
    }

    @Test
    void logsUnhandledServletExceptionsAsServerErrorsBeforeRethrowing() {
        var request = new MockHttpServletRequest("GET", "/api/prayer/health/database");
        var exception = new ServletException("database failure");

        assertThrows(ServletException.class, () -> filter.doFilterInternal(request,
                new MockHttpServletResponse(), (ignoredRequest, ignoredResponse) -> {
                    throw exception;
                }));

        List<ILoggingEvent> events = appender.list;
        assertEquals(1, events.size());
        assertEquals(Level.ERROR, events.getFirst().getLevel());
        assertTrue(events.getFirst().getFormattedMessage()
                .contains("status=500 durationMs="));
        assertTrue(events.getFirst().getFormattedMessage().contains("exceptionType=ServletException"));
    }
}
