package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Bir adaptörün ürettiği tek ölçüm (metric_points tablosundaki bir satır). */
public record ParsedPoint(Metric metric, Granularity granularity, LocalDate periodStart, BigDecimal value) {}
