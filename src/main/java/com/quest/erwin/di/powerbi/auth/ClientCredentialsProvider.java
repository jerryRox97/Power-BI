/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Acquires OAuth2 tokens from Azure AD using client_credentials grant flow.
 * Uses Java 11+ HttpClient — no Spring dependency.
 */
public class ClientCredentialsProvider implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(ClientCredentialsProvider.class);

    private static final String TOKEN_URL_TEMPLATE = "https://login.microsoftonline.com/%s/oauth2/token";
    private static final String RESOURCE = "https://analysis.windows.net/powerbi/api";
    private static final String GRANT_TYPE = "client_credentials";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ClientCredentialsProvider(String tenantId, String clientId, String clientSecret) {
        this.tokenUrl = String.format(TOKEN_URL_TEMPLATE, tenantId);
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public TokenResponse acquireToken() throws AuthException {
        try {
            String formBody = buildFormBody();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUrl))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .timeout(TIMEOUT)
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return parseTokenResponse(response.body());
            }

            String errorDesc = extractErrorDescription(response.body());
            throw new AuthException("Token acquisition failed (HTTP " + response.statusCode() + "): " + errorDesc);

        } catch (AuthException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new AuthException("Token acquisition failed: " + e.getMessage(), e);
        }
    }

    private String buildFormBody() {
        return "client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret)
                + "&grant_type=" + encode(GRANT_TYPE)
                + "&resource=" + encode(RESOURCE);
    }

    private TokenResponse parseTokenResponse(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            String accessToken = json.get("access_token").asText();
            long expiresIn = json.has("expires_in") ? json.get("expires_in").asLong() : 3600L;
            log.debug("Token acquired, expires in {} seconds", expiresIn);
            return TokenResponse.of(accessToken, expiresIn);
        } catch (Exception e) {
            throw new AuthException("Failed to parse token response: " + e.getMessage(), e);
        }
    }

    private String extractErrorDescription(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.has("error_description")) {
                return json.get("error_description").asText();
            }
            return body;
        } catch (Exception e) {
            return body;
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
