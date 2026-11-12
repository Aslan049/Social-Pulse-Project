package com.socialpulse.backend.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Yüzde değişim hesabı. Önceki değer yoksa veya 0 ise anlamlı bir yüzde yoktur: null döner. */
public final class GrowthCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private GrowthCalculator() {
    }

    /** (şimdiki − önceki) / önceki × 100, bir ondalık basamakla. */
    public static BigDecimal percentChange(BigDecimal current, BigDecimal previous) {
        if (current == null || previous == null || previous.signum() == 0) {
            return null;
        }
        return current.subtract(previous).multiply(HUNDRED).divide(previous, 1, RoundingMode.HALF_UP);
    }
}
