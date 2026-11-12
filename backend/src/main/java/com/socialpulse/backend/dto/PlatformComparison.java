package com.socialpulse.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Platformlar arası aylık karşılaştırma. Analiz penceresi (windowStart-windowEnd) platformların
 * aralıklarının birleşimidir; her platform bu pencereye göre değerlendirilir, böylece bir platformun
 * daha az ay içermesi (örn. X'te eksik Eylül) görünür kalır.
 */
public record PlatformComparison(LocalDate windowStart, LocalDate windowEnd, List<LocalDate> months,
                                 List<MetricComparison> metrics) {}
