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

class XAdapterTest {

    private final XAdapter adapter = new XAdapter();
    private final List<Object> header = row("Ay", "Görüntülenmeler", "Etkileşimler", "Beğeni");

    private static List<Object> row(Object... cells) {
        return Arrays.asList(cells);
    }

    @SafeVarargs
    private static RawTable table(String name, List<Object>... rows) {
        return new RawTable(name, new ArrayList<>(Arrays.asList(rows)));
    }

    private static LocalDateTime month(int year, int month) {
        return LocalDate.of(year, month, 1).atStartOfDay();
    }

    private static RawTable dailyTable() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row("Tarih", "Ziyaretler"));
        for (int d = 0; d < 40; d++) {
            rows.add(row(LocalDate.of(2025, 10, 1).plusDays(d).atStartOfDay(), (double) (d + 1)));
        }
        return new RawTable("Tüm Veriler", rows);
    }

    @Test
    void wideMonthlyTable_isParsedIntoMonthlyPoints() {
        ParseResult result = adapter.parse(List.of(table("Aylık Özet", header,
                row(month(2025, 10), 3255.0, 67.0, 12.0),
                row(month(2025, 11), 4000.0, 80.0, 15.0))));

        assertEquals(6, result.points().size());
        assertEquals(6, result.dataRows());
        assertTrue(result.issues().isEmpty());
        assertTrue(result.points().stream().allMatch(p -> p.granularity() == Granularity.MONTH));
        assertEquals(Set.of(Metric.VIEWS, Metric.ENGAGEMENT, Metric.LIKES),
                result.points().stream().map(ParsedPoint::metric).collect(Collectors.toSet()));
        ParsedPoint first = result.points().get(0);
        assertEquals(LocalDate.of(2025, 10, 1), first.periodStart());
        assertEquals(Metric.VIEWS, first.metric());
        assertEquals(0, new BigDecimal("3255").compareTo(first.value()));
    }

    @Test
    void unknownColumns_areIgnoredAndReported() {
        ParseResult result = adapter.parse(List.of(table("Aylık Özet",
                row("Ay", "Görüntülenmeler", "Bilinmeyen Sütun"), row(month(2025, 10), 1.0, 2.0))));

        assertEquals(1, result.points().size());
        assertEquals(List.of("Aylık Özet: Bilinmeyen Sütun"), result.ignoredColumns());
    }

    @Test
    void emptyAndDashCells_meanNoDataNotZeroAndNotAnError() {
        ParseResult result = adapter.parse(List.of(table("T", header,
                row(month(2025, 10), 10.0, null, 5.0),
                row(month(2025, 11), 10.0, "-", 5.0))));

        assertEquals(4, result.points().size());
        assertTrue(result.issues().isEmpty());
        assertFalse(result.points().stream().anyMatch(p -> p.metric() == Metric.ENGAGEMENT));
    }

    @Test
    void rowWithoutAnyValue_isReported() {
        ParseResult result = adapter.parse(List.of(table("T", header,
                row(month(2025, 10)), row(month(2025, 11), 1.0))));

        assertEquals(1, result.points().size());
        assertEquals(1, result.issues().size());
        assertEquals(2, result.issues().get(0).row());
        assertTrue(result.issues().get(0).message().contains("ölçüm değeri yok"));
    }

    @Test
    void badMonth_rejectsTheWholeRow() {
        ParseResult result = adapter.parse(List.of(table("T", header,
                row("abc", 1.0, 2.0, 3.0), row(month(2025, 11), 1.0, 2.0, 3.0))));

        assertEquals(3, result.points().size());
        assertEquals(1, result.issues().size());
        assertEquals(2, result.issues().get(0).row());
        assertTrue(result.issues().get(0).message().startsWith("Geçersiz ay"));
    }

    @Test
    void badNumber_rejectsOnlyThatCell() {
        ParseResult result = adapter.parse(List.of(table("T", header,
                row(month(2025, 10), 1.0, "x", 3.0))));

        assertEquals(2, result.points().size());
        assertEquals(3, result.dataRows());
        assertEquals(1, result.issues().size());
        assertTrue(result.issues().get(0).message().startsWith("Etkileşimler: Geçersiz sayı"));
    }

    @Test
    void duplicateMonth_keepsFirstRow() {
        ParseResult result = adapter.parse(List.of(table("T", header,
                row(month(2025, 10), 1.0, 2.0, 3.0), row("10.2025", 9.0, 9.0, 9.0))));

        assertEquals(3, result.points().size());
        assertEquals(3, result.issues().size());
        assertTrue(result.issues().get(0).message().contains("Tekrar eden ay: 2025-10"));
    }

    @Test
    void textMonthsAndNumbers_areAccepted() {
        ParseResult result = adapter.parse(List.of(table("T", row("Month", "Views"),
                row("Ekim 2025", "1.234"), row("Kasım 2025", "2,5"))));

        assertEquals(2, result.points().size());
        assertEquals(LocalDate.of(2025, 10, 1), result.points().get(0).periodStart());
        assertEquals(0, new BigDecimal("1234").compareTo(result.points().get(0).value()));
        assertEquals(LocalDate.of(2025, 11, 1), result.points().get(1).periodStart());
        assertEquals(0, new BigDecimal("2.5").compareTo(result.points().get(1).value()));
    }

    @Test
    void reorderedColumnsAndCsvTable_areHandled() {
        ParseResult result = adapter.parse(List.of(table("CSV", row("Beğeni", "Ay"), row(7.0, month(2025, 10)))));

        assertEquals(1, result.points().size());
        assertEquals(Metric.LIKES, result.points().get(0).metric());
    }

    @Test
    void duplicateMetricHeader_secondColumnIsIgnored() {
        ParseResult result = adapter.parse(List.of(table("T", row("Ay", "Beğeni", "Likes"),
                row(month(2025, 10), 1.0, 2.0))));

        assertEquals(1, result.points().size());
        assertEquals(List.of("T: Likes"), result.ignoredColumns());
    }

    @Test
    void dailyTable_isNotCollapsedIntoMonths() {
        ParseResult result = adapter.parse(List.of(
                new RawTable("Günlük", dailyTable().rows()),
                table("Aylık Özet", header, row(month(2025, 10), 1.0, 2.0, 3.0))));

        assertEquals(3, result.points().size());
        assertEquals(List.of("Günlük"), result.ignoredSheets());
        assertEquals(1, result.issues().size());
        assertTrue(result.issues().get(0).message().startsWith("Günlük veri gibi"));
    }

    private static List<List<Object>> dailyLongRows() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row("Tarih", "Değer"));
        for (int d = 0; d < 40; d++) {
            rows.add(row(LocalDate.of(2025, 10, 1).plusDays(d).atStartOfDay(), (double) (d + 1)));
        }
        return rows;
    }

    @Test
    void onlyDailyOrDerivedSheets_failWithHelpfulMessage() {
        InvalidUploadException ex = assertThrows(InvalidUploadException.class, () -> adapter.parse(List.of(
                new RawTable("Ziyaretler", dailyLongRows()),
                dailyTable())));

        assertTrue(ex.getMessage().contains("Ziyaretler"));
        assertTrue(ex.getMessage().contains("Tüm Veriler"));
    }

    @Test
    void monthlyLongSheetNamedAfterMetric_isAccepted() {
        ParseResult result = adapter.parse(List.of(
                table("goruntulenmeler", row("Ay", "Değer"), row("Ekim 2025", "100"), row("Kasım 2025", "200"))));

        assertEquals(2, result.points().size());
        assertEquals(Metric.VIEWS, result.points().get(0).metric());
        assertEquals(Granularity.MONTH, result.points().get(0).granularity());
    }

    @Test
    void sheetWithoutMonthColumn_isIgnored() {
        ParseResult result = adapter.parse(List.of(
                table("Notlar", row("a", "b")),
                table("Aylık Özet", header, row(month(2025, 10), 1.0, 2.0, 3.0))));

        assertEquals(3, result.points().size());
        assertEquals(List.of("Notlar"), result.ignoredSheets());
    }
}
