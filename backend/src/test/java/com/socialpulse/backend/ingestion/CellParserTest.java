package com.socialpulse.backend.ingestion;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CellParserTest {

    private static void assertNumber(String expected, Object cell) {
        BigDecimal actual = CellParser.toNumber(cell);
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "Beklenen " + expected + ", gelen " + actual);
    }

    @Test
    void dates_areParsedFromDateCellsAndCommonTextFormats() {
        LocalDate expected = LocalDate.of(2026, 1, 5);
        assertEquals(expected, CellParser.toDate(LocalDateTime.of(2026, 1, 5, 0, 0)));
        assertEquals(expected, CellParser.toDate("2026-01-05"));
        assertEquals(expected, CellParser.toDate("05.01.2026"));
        assertEquals(expected, CellParser.toDate("05/01/2026"));
        assertEquals(expected, CellParser.toDate("2026-01-05 00:00:00"));
    }

    @Test
    void invalidDates_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> CellParser.toDate("abc"));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toDate("31.02.2026"));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toDate(null));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toDate(5.0));
    }

    @Test
    void numericCells_areConverted() {
        assertNumber("1883", 1883.0);
        assertNumber("7", 7);
        assertNumber("1219106", 1219106.0);
        assertNumber("-7", -7.0);
    }

    @Test
    void textNumbers_supportTurkishAndEnglishFormats() {
        assertNumber("1234", "1.234");
        assertNumber("1234", "1,234");
        assertNumber("1234.5", "1.234,5");
        assertNumber("1234.5", "1,234.5");
        assertNumber("12.5", "12,5");
        assertNumber("1234567", "1.234.567");
        assertNumber("-7", "-7");
        assertNumber("42", " 42 ");
    }

    @Test
    void missingValues_returnNull() {
        assertNull(CellParser.toNumber(null));
        assertNull(CellParser.toNumber("  "));
        assertNull(CellParser.toNumber("-"));
    }

    @Test
    void invalidNumbers_areRejected() {
        assertThrows(IllegalArgumentException.class, () -> CellParser.toNumber("abc"));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toNumber("1.2.3"));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toNumber(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toNumber(1e17));
        assertThrows(IllegalArgumentException.class, () -> CellParser.toNumber(Boolean.TRUE));
    }
}
