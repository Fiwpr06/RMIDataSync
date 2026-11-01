package com.fiwpr06.rmidatasync.shared.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Tien ich dinh dang thoi gian
 */
public final class DateTimeUtil {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DateTimeUtil() {}

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public static String formatEpochMillis(long millis) {
        if (millis <= 0) return "-";
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault()).format(FORMATTER);
    }
}
