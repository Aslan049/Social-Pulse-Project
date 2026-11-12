package com.socialpulse.backend.analytics;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class GrowthCalculatorTest {

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void increaseAndDecrease_areCalculatedWithOneDecimal() {
        assertEquals(0, bd("20.0").compareTo(GrowthCalculator.percentChange(bd("120"), bd("100"))));
        assertEquals(0, bd("-20.0").compareTo(GrowthCalculator.percentChange(bd("80"), bd("100"))));
        assertEquals(0, bd("-66.7").compareTo(GrowthCalculator.percentChange(bd("1"), bd("3"))));
    }

    @Test
    void previousZeroOrMissing_givesNoPercentage() {
        assertNull(GrowthCalculator.percentChange(bd("5"), bd("0")));
        assertNull(GrowthCalculator.percentChange(bd("5"), null));
        assertNull(GrowthCalculator.percentChange(null, bd("5")));
    }
}
