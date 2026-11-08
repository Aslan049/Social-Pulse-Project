package com.socialpulse.backend.ingestion;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CellParserMonthTest {

    private static final LocalDate OCT_2025 = LocalDate.of(2025, 10, 1);

    @Test
    void dateCells_areTruncatedToFirstDayOfMonth() {
        assertEquals(OCT_2025, CellParser.toMonth(LocalDateTime.of(2025, 10, 17, 0, 0)));
        assertEquals(OCT_2025, CellParser.toMonth(LocalDate.of(2025, 10, 31)));
    }

    @Test
    void numericMonthTexts_areAccepted() {
        assertEquals(OCT_2025, CellParser.toMonth("2025-10"));
        assertEquals(OCT_2025, CellParser.toMonth("10.2025"));
        assertEquals(OCT_2025, CellParser.toMonth("10/2025"));
        assertEquals(OCT_2025, CellParser.toMonth("10-2025"));
    }

    @Test
    void fullDateTexts_fallBackToFirstOfMonth() {
        assertEquals(OCT_2025, CellParser.toMonth("2025-10-01"));
        assertEquals(OCT_2025, CellParser.toMonth("17.10.2025"));
    }

    @Test
    void turkishMonthNames_areAccepted() {
        assertEquals(OCT_2025, CellParser.toMonth("Ekim 2025"));
        assertEquals(OCT_2025, CellParser.toMonth("EKİM 2025"));
        assertEquals(LocalDate.of(2026, 2, 1), CellParser.toMonth("Şubat 2026"));
        assertEquals(LocalDate.of(2026, 5, 1), CellParser.toMonth("Mayıs 2026"));
        assertEquals(LocalDate.of(2026, 8, 1), CellParser.toMonth("Ağustos 2026"));
    }

    @Test
    void englishMonthNames_areAccepted() {
        assertEquals(OCT_2025, CellParser.toMonth("October 2025"));
        assertEquals(OCT_2025, CellParser.toMonth("Oct 2025"));
        assertEquals(OCT_2025, CellParser.toMonth("Oct. 2025"));
        assertEquals(OCT_2025, CellParser.toMonth("2025 Ekim"));
    }

    @Test
    void invalidMonths_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> CellParser.toMonth("Foo 2025"));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toMonth("13.2025"));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toMonth("2025-00"));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toMonth("abc"));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toMonth(null));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toMonth(5.0));
    }
}
