package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class InstagramAdapterTest {

    private final InstagramAdapter adapter = new InstagramAdapter();

    private static List<Object> row(Object... cells) {
        return Arrays.asList(cells);
    }

    @SafeVarargs
    private static RawTable table(String name, List<Object>... rows) {
        return new RawTable(name, new ArrayList<>(Arrays.asList(rows)));
    }

    private static LocalDateTime day(int d) {
        return LocalDate.of(2026, 1, d).atStartOfDay();
    }

    @Test
    void metricSheetsAreParsedAndDerivedSheetsIgnored() {
        ParseResult result = adapter.parse(List.of(
                table("İstatistikler", row("INSTAGRAM PERFORMANS İSTATİSTİKLERİ")),
                table("Ziyaretler", row("Tarih", "Değer"), row(day(1), 10.0), row(day(2), 20.0), row(day(3), 30.0)),
                table("Etkileşim", row("Tarih", "Değer"), row(day(1), 1.0), row(day(2), -7.0)),
                table("Tüm Veriler", row("Tarih", "Ziyaretler", "Etkileşim"), row(day(1), 10.0, 1.0))));

        assertEquals(5, result.points().size());
        assertEquals(List.of("İstatistikler", "Tüm Veriler"), result.ignoredSheets());
        assertEquals(5, result.dataRows());
        assertTrue(result.issues().isEmpty());
        assertTrue(result.points().stream().allMatch(p -> p.granularity() == Granularity.DAY));
        assertEquals(Set.of(Metric.PROFILE_VISITS, Metric.ENGAGEMENT),
                result.points().stream().map(ParsedPoint::metric).collect(Collectors.toSet()));
        assertTrue(result.points().stream().anyMatch(p -> p.value().compareTo(new BigDecimal("-7")) == 0),
                "Negatif değer bu aşamada atılmaz, sonraki doğrulama katmanında işaretlenir");
    }

    @Test
    void missingDays_produceNoRows() {
        ParseResult result = adapter.parse(List.of(
                table("Takipler", row("Tarih", "Değer"), row(day(15), 39.0), row(day(16), 41.0))));

        assertEquals(2, result.points().size());
        assertEquals(Metric.NEW_FOLLOWERS, result.points().get(0).metric());
    }

    @Test
    void badDatesAndValues_areReportedWithRowNumbers() {
        ParseResult result = adapter.parse(List.of(
                table("Ziyaretler", row("Tarih", "Değer"), row(day(1), 10.0), row("abc", 5.0), row(day(3), "x"))));

        assertEquals(1, result.points().size());
        assertEquals(3, result.dataRows());
        assertEquals(2, result.rejectedRows());
        assertEquals(3, result.issues().get(0).row());
        assertEquals(4, result.issues().get(1).row());
        assertTrue(result.issues().get(0).message().startsWith("Geçersiz tarih"));
        assertTrue(result.issues().get(1).message().startsWith("Geçersiz sayı"));
    }

    @Test
    void emptyValue_isRejectedNotZero() {
        ParseResult result = adapter.parse(List.of(
                table("Ziyaretler", row("Tarih", "Değer"), row(day(1), null), row(day(2)))));

        assertTrue(result.points().isEmpty());
        assertEquals(2, result.issues().size());
        assertTrue(result.issues().get(0).message().contains("Değer boş"));
    }

    @Test
    void duplicateDate_keepsFirstAndReportsSecond() {
        ParseResult result = adapter.parse(List.of(
                table("Ziyaretler", row("Tarih", "Değer"), row(day(1), 10.0), row(day(1), 99.0))));

        assertEquals(1, result.points().size());
        assertEquals(0, new BigDecimal("10").compareTo(result.points().get(0).value()));
        assertEquals(1, result.issues().size());
        assertTrue(result.issues().get(0).message().contains("Tekrar"));
    }

    @Test
    void englishSheetAndHeaderNames_areRecognized() {
        ParseResult result = adapter.parse(List.of(
                table("Reach", row("Date", "Value"), row("2026-01-05", "1.234"))));

        assertEquals(1, result.points().size());
        assertEquals(Metric.REACH, result.points().get(0).metric());
        assertEquals(LocalDate.of(2026, 1, 5), result.points().get(0).periodStart());
        assertEquals(0, new BigDecimal("1234").compareTo(result.points().get(0).value()));
    }

    @Test
    void reorderedColumns_areHandled() {
        ParseResult result = adapter.parse(List.of(
                table("Erişim", row("Değer", "Tarih"), row(5.0, day(1)))));

        assertEquals(1, result.points().size());
        assertEquals(0, new BigDecimal("5").compareTo(result.points().get(0).value()));
    }

    @Test
    void unrecognizedHeader_isReportedButOtherSheetsStillWork() {
        ParseResult result = adapter.parse(List.of(
                table("Ziyaretler", row("A", "B"), row(day(1), 1.0)),
                table("Erişim", row("Tarih", "Değer"), row(day(1), 5.0))));

        assertEquals(1, result.points().size());
        assertEquals(List.of("Ziyaretler"), result.ignoredSheets());
        assertEquals(1, result.issues().size());
        assertEquals(1, result.issues().get(0).row());
    }

    @Test
    void noRecognizableSheet_failsWithHelpfulMessage() {
        InvalidUploadException ex = assertThrows(InvalidUploadException.class, () -> adapter.parse(List.of(
                table("Sayfa1", row("a", "b")), table("CSV", row("x")))));

        assertTrue(ex.getMessage().contains("Sayfa1"));
        assertTrue(ex.getMessage().contains("CSV"));
    }

    @Test
    void emptyMetricSheet_isIgnored() {
        ParseResult result = adapter.parse(List.of(
                table("Ziyaretler"),
                table("Erişim", row("Tarih", "Değer"), row(day(1), 1.0))));

        assertEquals(1, result.points().size());
        assertTrue(result.ignoredSheets().contains("Ziyaretler"));
    }

    @Test
    void textDatesAndNumbers_areAccepted() {
        ParseResult result = adapter.parse(List.of(
                table("Ziyaretler", row("Tarih", "Değer"), row("05.01.2026", "1.234"))));

        assertEquals(1, result.points().size());
        assertEquals(LocalDate.of(2026, 1, 5), result.points().get(0).periodStart());
        assertEquals(0, new BigDecimal("1234").compareTo(result.points().get(0).value()));
    }
}
