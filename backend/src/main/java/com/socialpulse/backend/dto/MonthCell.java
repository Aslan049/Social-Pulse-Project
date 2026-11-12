package com.socialpulse.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param month    ayın ilk günü
 * @param value    ayın değeri; veri yoksa null (0 değildir)
 * @param coverage günlük veriden toplanan aylarda gözlenen gün oranı; platformun hazır aylık değerinde null
 */
public record MonthCell(LocalDate month, BigDecimal value, Double coverage, CellStatus status) {}
