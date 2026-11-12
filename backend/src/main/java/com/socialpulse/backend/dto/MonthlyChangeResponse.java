package com.socialpulse.backend.dto;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;

import java.time.LocalDate;
import java.util.List;

public record MonthlyChangeResponse(Platform platform, Metric metric, Granularity sourceGranularity,
                                    LocalDate windowStart, LocalDate windowEnd, List<MonthlyChangeRow> rows) {}
