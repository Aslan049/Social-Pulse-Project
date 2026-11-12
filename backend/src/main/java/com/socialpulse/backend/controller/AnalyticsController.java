package com.socialpulse.backend.controller;

import com.socialpulse.backend.auth.AuthenticatedUser;
import com.socialpulse.backend.dto.MetricSummary;
import com.socialpulse.backend.dto.MonthlyChangeResponse;
import com.socialpulse.backend.dto.PlatformComparison;
import com.socialpulse.backend.dto.SeriesResponse;
import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/summary")
    public List<MetricSummary> summary(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) Platform platform,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.summary(user.id(), platform, from, to);
    }

    @GetMapping("/series")
    public SeriesResponse series(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam Platform platform,
            @RequestParam Metric metric,
            @RequestParam(defaultValue = "DAY") Granularity granularity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.series(user.id(), platform, metric, granularity, from, to);
    }

    @GetMapping("/platforms")
    public PlatformComparison platforms(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.platformComparison(user.id(), from, to);
    }

    @GetMapping("/monthly-change")
    public MonthlyChangeResponse monthlyChange(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam Platform platform,
            @RequestParam Metric metric,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.monthlyChange(user.id(), platform, metric, from, to);
    }
}
