package com.urlshortener.analytics.api;

import java.time.LocalDate;

/** Clicks recorded on a single UTC calendar day. */
public record DailyClickCount(LocalDate date, long clicks) {
}
