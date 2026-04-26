/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Thread-safe token cache with proactive refresh.
 * Tokens are refreshed when within {@code REFRESH_BUFFER_SECONDS} of expiry.
 * Only one thread performs the refresh while others wait.
 */
public class TokenStore {

    private static final Logger log = LoggerFactory.getLogger(TokenStore.class);
    private static final long REFRESH_BUFFER_SECONDS = 300; // 5 minutes

    private final TokenProvider tokenProvider;
    private final ReentrantLock refreshLock = new ReentrantLock();
    private volatile TokenResponse currentToken;

    public TokenStore(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    /**
     * Returns a valid access token, refreshing proactively if needed.
     */
    public String getValidToken() {
        TokenResponse token = currentToken;
        if (token != null && !token.isExpiringSoon(REFRESH_BUFFER_SECONDS)) {
            return token.accessToken();
        }
        return refreshToken().accessToken();
    }

    /**
     * Forces a token refresh regardless of current validity.
     * Used when API returns 401/403 TokenExpired.
     */
    public TokenResponse forceRefresh() {
        log.info("Force-refreshing token due to API rejection");
        return refreshToken();
    }

    public boolean hasToken() {
        return currentToken != null;
    }

    public String getCurrentTokenValue() {
        TokenResponse token = currentToken;
        return token != null ? token.accessToken() : "";
    }

    private TokenResponse refreshToken() {
        refreshLock.lock();
        try {
            TokenResponse token = currentToken;
            if (token != null && !token.isExpiringSoon(REFRESH_BUFFER_SECONDS)) {
                return token;
            }
            log.debug("Acquiring fresh token");
            TokenResponse freshToken = tokenProvider.acquireToken();
            currentToken = freshToken;
            log.info("Token refreshed, expires at {}", freshToken.expiresAt());
            return freshToken;
        } finally {
            refreshLock.unlock();
        }
    }
}
