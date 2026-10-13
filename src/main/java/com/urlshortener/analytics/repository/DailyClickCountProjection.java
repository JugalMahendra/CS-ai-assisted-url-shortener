package com.urlshortener.analytics.repository;

/** Native-query projection for per-day click counts ({@code day} as ISO 'YYYY-MM-DD'). */
public interface DailyClickCountProjection {

    String getDay();

    long getClicks();
}
