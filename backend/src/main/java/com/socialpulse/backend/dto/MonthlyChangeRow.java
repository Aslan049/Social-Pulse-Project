package com.socialpulse.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param changePercent önceki aya göre yüzde değişim; iki ay da değerliyse ve önceki ay 0 değilse dolu
 * @param comparable    iki ay da tam veriliyse (OK) true; kısmi ay yüzdesi yanıltıcı olabilir
 */
public record MonthlyChangeRow(LocalDate month, BigDecimal value, Double coverage, CellStatus status,
                               BigDecimal previousValue, BigDecimal changePercent, boolean comparable,
                               String note) {}
