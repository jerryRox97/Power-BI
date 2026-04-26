/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quest.erwin.di.powerbi.auth.TokenStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.function.Function;

/**
 * HTTP proxy for Power BI REST API calls.
 * Automatically injects Bearer token, retries on 401/403 TokenExpired,
 * and respects rate limiting.
 */
public class PowerBIClient {

    private static final Logger log = LoggerFactory.getLogger(PowerBIClient.class);
    private static final int MAX_RETRIES = 1;
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    private final TokenStore tokenStore;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    public PowerBIClient(TokenStore tokenStore) {
        this(tokenStore, new RateLimiter(200));
    }

    public PowerBIClient(TokenStore tokenStore, RateLimiter rateLimiter) {
        this.tokenStore = tokenStore;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
        this.rateLimiter = rateLimiter;
    }

    /**
     * Executes a GET request and parses the response with the given parser function.
     */
    public <T> T get(String url, Function<String, T> parser) {
        return executeWithRetry(() -> {
            rateLimiter.acquire();
            HttpRequest request = buildGetRequest(url);
            HttpResponse<String> response = send(request);
            checkForErrors(response);
            return parser.apply(response.body());
        });
    }

    /**
     * Executes a GET request and returns the raw response body.
     */
    public String getRaw(String url) {
        return get(url, body -> body);
    }

    /**
     * Executes a GET request and returns parsed JSON.
     */
    public JsonNode getJson(String url) {
        return get(url, this::parseJson);
    }

    /**
     * Executes a POST request with a JSON body and parses the response.
     */
    public <T> T post(String url, String jsonBody, Function<String, T> parser) {
        return executeWithRetry(() -> {
            rateLimiter.acquire();
            HttpRequest request = buildPostRequest(url, jsonBody);
            HttpResponse<String> response = send(request);
            checkForErrors(response);
            return parser.apply(response.body());
        });
    }

    /**
     * Executes a POST request and returns parsed JSON.
     */
    public JsonNode postJson(String url, String jsonBody) {
        return post(url, jsonBody, this::parseJson);
    }

    /**
     * Deserializes JSON response body into the specified type.
     */
    public <T> T getAs(String url, Class<T> type) {
        return get(url, body -> deserialize(body, type));
    }

    /**
     * Returns the current valid access token.
     */
    public String getAccessToken() {
        return tokenStore.getValidToken();
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private HttpRequest buildGetRequest(String url) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + tokenStore.getValidToken())
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .GET()
                .build();
    }

    private HttpRequest buildPostRequest(String url, String jsonBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + tokenStore.getValidToken())
                .header("Content-Type", "application/json")
                .timeout(TIMEOUT)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ApiException(0, "NetworkError", "HTTP request failed: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(0, "Interrupted", "HTTP request interrupted", e);
        }
    }

    private void checkForErrors(HttpResponse<String> response) {
        int status = response.statusCode();
        if (status >= 200 && status < 300) {
            return;
        }

        String body = response.body() != null ? response.body() : "";
        String errorCode = extractErrorCode(body);

        if (isTokenExpiredError(status, errorCode)) {
            tokenStore.forceRefresh();
            throw new ApiException(status, "TokenExpired", "Token expired, forcing refresh");
        }

        if (status == 429) {
            throw new ApiException(status, "TooManyRequests", "API rate limit exceeded: " + body);
        }

        throw new ApiException(status, errorCode, "API error (HTTP " + status + "): " + body);
    }

    private boolean isTokenExpiredError(int status, String errorCode) {
        return (status == 401 || status == 403) && "TokenExpired".equalsIgnoreCase(errorCode);
    }

    private String extractErrorCode(String body) {
        try {
            JsonNode json = objectMapper.readTree(body);
            if (json.has("error") && json.get("error").has("code")) {
                return json.get("error").get("code").asText("");
            }
        } catch (Exception ignored) {
            // not JSON or no error code
        }
        return "";
    }

    private JsonNode parseJson(String body) {
        try {
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new ApiException(0, "ParseError", "Failed to parse JSON response: " + e.getMessage(), e);
        }
    }

    private <T> T deserialize(String body, Class<T> type) {
        try {
            return objectMapper.readValue(body, type);
        } catch (Exception e) {
            throw new ApiException(0, "DeserializationError",
                    "Failed to deserialize response to " + type.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private <T> T executeWithRetry(ApiCall<T> call) {
        int attempts = 0;
        while (true) {
            try {
                return call.execute();
            } catch (ApiException ex) {
                if (ex.isTokenExpired() && attempts < MAX_RETRIES) {
                    log.warn("Token expired, retrying (attempt {})", attempts + 1);
                    attempts++;
                    continue;
                }
                throw ex;
            }
        }
    }

    @FunctionalInterface
    private interface ApiCall<T> {
        T execute();
    }
}
