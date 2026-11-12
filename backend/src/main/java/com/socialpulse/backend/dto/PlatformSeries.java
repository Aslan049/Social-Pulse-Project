package com.socialpulse.backend.dto;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Platform;

import java.time.LocalDate;
import java.util.List;

public record PlatformSeries(
        Platform platform,
        Granularity sourceGranularity,
        List<MonthCell> cells,
        int monthsWithData,
        List<LocalDate> missingMonths,
        List<LocalDate> partialMonths
) {}
