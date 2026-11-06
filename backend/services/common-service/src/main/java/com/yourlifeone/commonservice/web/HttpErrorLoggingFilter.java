package com.yourlifeone.commonservice.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

/** Logs every client or server error response produced while handling an HTTP request. */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HttpErrorLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(HttpErrorLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        var startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | RuntimeException exception) {
            var status = response.getStatus();
            logError(request, isErrorStatus(status) ? status : HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    startedAt, exception.getClass().getSimpleName());
            throw exception;
        } catch (Error error) {
            var status = response.getStatus();
            logError(request, isErrorStatus(status) ? status : HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    startedAt, error.getClass().getSimpleName());
            throw error;
        } catch (IOException exception) {
            var status = response.getStatus();
            if (isErrorStatus(status)) {
                logError(request, status, startedAt, exception.getClass().getSimpleName());
            } else {
                logger.error(
                        "HTTP request failed before its response completed: method={} path={} responseStatus={} durationMs={} exceptionType={}",
                        request.getMethod(), request.getRequestURI(), status, elapsedMillis(startedAt),
                        exception.getClass().getSimpleName());
            }
            throw exception;
        }

        var status = response.getStatus();
        if (isErrorStatus(status)) {
            logError(request, status, startedAt, "none");
        }
    }

    private static boolean isErrorStatus(int status) {
        return status >= HttpServletResponse.SC_BAD_REQUEST && status < 600;
    }

    private static long elapsedMillis(long startedAt) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
    }

    private static LoggingEventBuilder diagnostic(HttpServletRequest request, int status,
            long startedAt, String exceptionType, boolean serverError) {
        var pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return (serverError ? logger.atError() : logger.atWarn())
                .addKeyValue("http_status", status)
                .addKeyValue("http_route", pattern == null ? "unmatched" : pattern.toString())
                .addKeyValue("duration_ms", elapsedMillis(startedAt))
                .addKeyValue("exception_type", exceptionType);
    }

    private static void logError(HttpServletRequest request, int status, long startedAt, String exceptionType) {
        var message = "HTTP request returned an error: method={} path={} status={} durationMs={} exceptionType={}";
        var arguments = new Object[] {
                request.getMethod(), request.getRequestURI(), status, elapsedMillis(startedAt), exceptionType
        };
        if (status >= HttpServletResponse.SC_INTERNAL_SERVER_ERROR) {
            diagnostic(request, status, startedAt, exceptionType, true).log(message, arguments);
        } else {
            diagnostic(request, status, startedAt, exceptionType, false).log(message, arguments);
        }
    }
}
