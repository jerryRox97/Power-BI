/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.client;

import lombok.Getter;

/**
 * Exception thrown when a Power BI API call fails.
 * Contains HTTP status code and error details for diagnostics.
 */
@Getter
public class ApiException extends RuntimeException {

    private final int statusCode;
    private final String errorCode;

    public ApiException(int statusCode, String errorCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public ApiException(int statusCode, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.errorCode = errorCode;
    }

    public boolean isTokenExpired() {
        return "TokenExpired".equalsIgnoreCase(errorCode)
                || statusCode == 401
                || (statusCode == 403 && "TokenExpired".equalsIgnoreCase(errorCode));
    }

    public boolean isThrottled() {
        return statusCode == 429;
    }
}
