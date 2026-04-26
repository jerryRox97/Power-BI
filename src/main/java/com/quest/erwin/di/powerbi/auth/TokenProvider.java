/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.auth;

/**
 * Strategy interface for acquiring OAuth2 tokens.
 * Implementations handle different authentication flows
 * (client credentials, device code, managed identity, etc.).
 */
public interface TokenProvider {

    /**
     * Acquires a fresh token from the identity provider.
     *
     * @return a {@link TokenResponse} containing the access token and expiry
     * @throws AuthException if token acquisition fails
     */
    TokenResponse acquireToken() throws AuthException;
}
