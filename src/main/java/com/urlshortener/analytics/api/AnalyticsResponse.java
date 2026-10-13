package com.urlshortener.analytics.api;

import java.time.Instant;
import java.util.List;

/** Aggregate click metrics for a short link plus a per-day time series (ascending). */
public record AnalyticsResponse(
        String code,
        long totalClicks,
        Instant firstClickAt,
        Instant lastClickAt,
        List<DailyClickCount> timeSeries) {
}
