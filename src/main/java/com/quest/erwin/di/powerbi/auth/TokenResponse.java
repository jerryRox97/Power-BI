/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.auth;

import java.time.Instant;

/**
 * Immutable token response holding the access token and its expiration time.
 */
public record TokenResponse(String accessToken, Instant expiresAt) {

    public static TokenResponse of(String accessToken, long expiresInSeconds) {
        return new TokenResponse(accessToken, Instant.now().plusSeconds(expiresInSeconds));
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isExpiringSoon(long bufferSeconds) {
        return Instant.now().plusSeconds(bufferSeconds).isAfter(expiresAt);
    }
}
