/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple token-bucket rate limiter for Power BI API calls.
 * Ensures compliance with API rate limits (200 requests per hour for admin APIs).
 */
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final long minIntervalMs;
    private final AtomicLong lastCallTime = new AtomicLong(0);

    /**
     * @param maxCallsPerHour maximum API calls allowed per hour
     */
    public RateLimiter(int maxCallsPerHour) {
        this.minIntervalMs = TimeUnit.HOURS.toMillis(1) / maxCallsPerHour;
    }

    /**
     * Blocks until it is safe to make the next API call.
     */
    public void acquire() {
        long now = System.currentTimeMillis();
        long last = lastCallTime.get();
        long elapsed = now - last;

        if (elapsed < minIntervalMs) {
            long waitMs = minIntervalMs - elapsed;
            try {
                log.debug("Rate limiter: waiting {}ms before next API call", waitMs);
                Thread.sleep(waitMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        lastCallTime.set(System.currentTimeMillis());
    }
}
