package com.socialpulse.backend.dto;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bir platformdaki bir metriğin özeti. Negatif değerler (veri hatası sayılır) istatistiklere dahil
 * edilmez ve eksik dönem gibi davranır; sayısı negativeCount'ta verilir.
 *
 * @param periods         geçerli (negatif olmayan) değeri olan dönem sayısı
 * @param coverage        kendi aralığında (ilk-son dönem) gözlenen dönemlerin oranı (0-1)
 * @param internalMissing kendi aralığı içindeki eksik dönem sayısı (gerçek boşluk)
 * @param windowStart     analiz penceresinin başı (tüm platformların aralığının birleşimi)
 * @param windowCoverage  analiz penceresinde gözlenen dönemlerin oranı (0-1)
 * @param leadingMissing  pencerenin başındaki, metrik başlamadan önceki dönem sayısı (eksik veri olmayabilir)
 * @param trailingMissing pencerenin sonundaki, metrik bittikten sonraki dönem sayısı
 */
public record MetricSummary(
        Platform platform,
        Metric metric,
        Granularity granularity,
        long periods,
        BigDecimal total,
        BigDecimal average,
        BigDecimal min,
        BigDecimal max,
        LocalDate firstPeriod,
        LocalDate lastPeriod,
        double coverage,
        long internalMissing,
        LocalDate windowStart,
        LocalDate windowEnd,
        double windowCoverage,
        long leadingMissing,
        long trailingMissing,
        long negativeCount
) {}
