package com.yourlifeone.planner.infrastructure.upstream;

/** Another service could not answer (unreachable, too slow, or an error status). */
public class UpstreamUnavailableException extends RuntimeException {
    public UpstreamUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
