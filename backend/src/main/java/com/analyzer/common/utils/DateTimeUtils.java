package com.analyzer.common.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class DateTimeUtils {

    private static final DateTimeFormatter ISO_LOCAL =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final DateTimeFormatter HUMAN_READABLE =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm:ss z");

    private DateTimeUtils() {}

    // ── Factories =>

    public static Instant nowUtc() {
        return Instant.now(Clock.systemUTC());
    }

    public static Instant startOfDay(LocalDate date, ZoneId zone) {
        return date.atStartOfDay(zone).toInstant();
    }

    public static Instant endOfDay(LocalDate date, ZoneId zone) {
        return date.atTime(LocalTime.MAX).atZone(zone).toInstant();
    }

    public static Instant daysAgo(int days) {
        return nowUtc().minus(days, ChronoUnit.DAYS);
    }

    public static Instant hoursAgo(int hours) {
        return nowUtc().minus(hours, ChronoUnit.HOURS);
    }

    // ── Formatting =>

    public static String toIsoLocal(Instant instant, ZoneId zone) {
        return ISO_LOCAL.format(instant.atZone(zone));
    }

    public static String toHumanReadable(Instant instant, ZoneId zone) {
        return HUMAN_READABLE.format(instant.atZone(zone));
    }

    public static String toIsoUtc(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    // ── Arithmetic =>

    public static long secondsBetween(Instant from, Instant to) {
        return ChronoUnit.SECONDS.between(from, to);
    }

    public static long millisBetween(Instant from, Instant to) {
        return ChronoUnit.MILLIS.between(from, to);
    }

    public static Duration durationBetween(Instant from, Instant to) {
        return Duration.between(from, to);
    }

    // ── Guards =>

    public static boolean isBefore(Instant a, Instant b) {
        return a.isBefore(b);
    }

    public static boolean isWithinLast(Instant instant, int minutes) {
        return instant.isAfter(nowUtc().minus(minutes, ChronoUnit.MINUTES));
    }
}