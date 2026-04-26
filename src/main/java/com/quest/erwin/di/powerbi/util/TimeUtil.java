/*
 * Copyright 2024 Quest Software Inc.
 * ALL RIGHTS RESERVED.
 */
package com.quest.erwin.di.powerbi.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Date/time utility methods.
 */
public final class TimeUtil {

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private TimeUtil() {}

    public static String nowIso() {
        return LocalDateTime.now(ZoneOffset.UTC).format(ISO_FORMATTER);
    }

    public static String toIso(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC).format(ISO_FORMATTER);
    }

    public static String formatDuration(long startMs) {
        long elapsed = System.currentTimeMillis() - startMs;
        long seconds = elapsed / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        }
        return seconds + "." + (elapsed % 1000) + "s";
    }
}
