package com.socialpulse.backend.dto;

import com.socialpulse.backend.entity.Metric;

import java.util.List;

public record MetricComparison(Metric metric, List<PlatformSeries> platforms) {}
