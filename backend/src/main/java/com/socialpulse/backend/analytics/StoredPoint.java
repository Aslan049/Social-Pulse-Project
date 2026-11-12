package com.socialpulse.backend.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Veritabanından okunan tek ölçüm. */
public record StoredPoint(LocalDate periodStart, BigDecimal value) {}
