package com.socialpulse.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param coverage gözlenen günlerin beklenen günlere oranı (0-1). Platformun kendi verdiği
 *                 değerlerde (günlük seri, hazır aylık özet) hesaplanamadığı için null.
 */
public record SeriesPointDto(LocalDate periodStart, BigDecimal value, Double coverage) {}
