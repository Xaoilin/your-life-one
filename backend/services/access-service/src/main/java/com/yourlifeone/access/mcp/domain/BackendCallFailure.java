package com.yourlifeone.access.mcp.domain;

import lombok.Getter;

/** Bounded client-facing failure, deliberately without a raw HTTP response or credential-bearing cause. */
@Getter
public class BackendCallFailure extends RuntimeException {
    private final String code;
    private final int status;

    public BackendCallFailure(String code, int status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }
}
