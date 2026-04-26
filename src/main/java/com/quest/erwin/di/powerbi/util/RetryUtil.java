/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Simple retry utility with exponential backoff.
 */
public final class RetryUtil {

    private static final Logger log = LoggerFactory.getLogger(RetryUtil.class);

    private RetryUtil() {}

    /**
     * Retries a supplier up to maxAttempts times with exponential backoff.
     */
    public static <T> T retry(int maxAttempts, long initialDelayMs, Supplier<T> action) {
        int attempt = 0;
        while (true) {
            try {
                attempt++;
                return action.get();
            } catch (Exception e) {
                if (attempt >= maxAttempts) {
                    log.error("All {} retry attempts exhausted", maxAttempts);
                    throw e;
                }
                long delay = initialDelayMs * (1L << (attempt - 1));
                log.warn("Attempt {}/{} failed: {}. Retrying in {}ms", attempt, maxAttempts, e.getMessage(), delay);
                sleep(delay);
            }
        }
    }

    /**
     * Retries a runnable up to maxAttempts times with exponential backoff.
     */
    public static void retryVoid(int maxAttempts, long initialDelayMs, Runnable action) {
        retry(maxAttempts, initialDelayMs, () -> {
            action.run();
            return null;
        });
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
