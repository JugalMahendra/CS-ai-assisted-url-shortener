package com.urlshortener.analytics.service;

import com.urlshortener.analytics.api.AnalyticsResponse;
import com.urlshortener.analytics.api.DailyClickCount;
import com.urlshortener.analytics.repository.ClickEventRepository;
import com.urlshortener.shortening.domain.ShortLink;
import com.urlshortener.shortening.repository.ShortLinkRepository;
import com.urlshortener.shortening.service.ShortLinkNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Builds analytics for a short link: the aggregate total (the denormalized counter), first/last click
 * timestamps, and a per-day time series derived from click events.
 */
@Service
public class AnalyticsService {

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;

    public AnalyticsService(ShortLinkRepository shortLinkRepository,
                            ClickEventRepository clickEventRepository) {
        this.shortLinkRepository = shortLinkRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String code) {
        ShortLink link = shortLinkRepository.findByCode(code)
                .orElseThrow(() -> new ShortLinkNotFoundException(code));
        Long id = link.getId();

        List<DailyClickCount> timeSeries = clickEventRepository.findDailyCounts(id).stream()
                .map(row -> new DailyClickCount(LocalDate.parse(row.getDay()), row.getClicks()))
                .toList();

        return new AnalyticsResponse(
                link.getCode(),
                link.getClickCount(),
                clickEventRepository.findFirstByShortLinkIdOrderByOccurredAtAsc(id)
                        .map(event -> event.getOccurredAt()).orElse(null),
                clickEventRepository.findFirstByShortLinkIdOrderByOccurredAtDesc(id)
                        .map(event -> event.getOccurredAt()).orElse(null),
                timeSeries);
    }
}
