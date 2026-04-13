package com.vanmors.ndbx.utils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


public class DateUtils {
    public static Instant parseDateFromYYYYMMDD(final String dateStr) {
        if (dateStr == null || dateStr.length() != 8) {
            return null;
        }
        try {
            final LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
            return date.atStartOfDay(ZoneOffset.UTC).toInstant();
        } catch (final Exception e) {
            return null;
        }
    }
}

