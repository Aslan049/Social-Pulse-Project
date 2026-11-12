package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** CSV okuyucu + adaptörler birlikte: dosya adı, ayraç, Türkçe sayı ve tarih biçimleri. */
class AdapterCsvFlowTest {

    private final CsvTabularReader reader = new CsvTabularReader();
    private final InstagramAdapter instagram = new InstagramAdapter();
    private final XAdapter x = new XAdapter();

    private List<RawTable> csv(String filename, String content) {
        return reader.read(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), filename);
    }

    @Test
    void longCsvNamedAfterMetric_isParsedAsDailySeries() {
        ParseResult result = instagram.parse(csv("ziyaretler.csv",
                "Tarih,Değer\n2026-01-01,10\n2026-01-02,20\n2026-01-03,30\n"));

        assertEquals(3, result.points().size());
        assertTrue(result.points().stream().allMatch(p -> p.metric() == Metric.PROFILE_VISITS));
        assertTrue(result.points().stream().allMatch(p -> p.granularity() == Granularity.DAY));
        assertEquals(0, result.rejectedRows());
    }

    @Test
    void wideDailyCsvWithSemicolonAndTurkishFormats_isParsed() {
        ParseResult result = instagram.parse(csv("rapor.csv",
                "Tarih;Ziyaretler;Etkileşim\n01.01.2026;1.234;5\n02.01.2026;2.000;7\n03.01.2026;;9\n"));

        assertEquals(5, result.points().size(), "Boş hücre için nokta üretilmemeli");
        assertEquals(Set.of(Metric.PROFILE_VISITS, Metric.ENGAGEMENT),
                result.points().stream().map(ParsedPoint::metric).collect(Collectors.toSet()));
        ParsedPoint first = result.points().get(0);
        assertEquals(LocalDate.of(2026, 1, 1), first.periodStart());
        assertEquals(0, new java.math.BigDecimal("1234").compareTo(first.value()));
        assertTrue(result.issues().isEmpty());
    }

    @Test
    void monthlyCsvWithTurkishMonthNames_isParsedForX() {
        ParseResult result = x.parse(csv("x.csv",
                "Ay,Görüntülenmeler,Beğeni\nEkim 2025,100,5\nKasım 2025,200,6\n"));

        assertEquals(4, result.points().size());
        assertTrue(result.points().stream().allMatch(p -> p.granularity() == Granularity.MONTH));
        assertEquals(LocalDate.of(2025, 10, 1), result.points().get(0).periodStart());
    }

    @Test
    void monthlyCsvLabeledAsInstagram_isRejected() {
        List<RawTable> tables = csv("x.csv", "Ay,Görüntülenmeler\nEkim 2025,100\nKasım 2025,200\n");

        assertThrows(InvalidUploadException.class, () -> instagram.parse(tables));
    }

    @Test
    void dailyCsvLabeledAsX_isRejected() {
        List<RawTable> tables = csv("rapor.csv",
                "Tarih,Ziyaretler\n2026-01-01,1\n2026-01-02,2\n2026-01-03,3\n");

        assertThrows(InvalidUploadException.class, () -> x.parse(tables));
    }

    @Test
    void twoColumnCsvWithUnknownName_failsWithFilenameHint() {
        List<RawTable> tables = csv("veri.csv", "Tarih,Değer\n2026-01-01,1\n2026-01-02,2\n");

        InvalidUploadException ex = assertThrows(InvalidUploadException.class, () -> instagram.parse(tables));

        assertTrue(ex.getMessage().contains("veri"));
        assertTrue(ex.getMessage().contains("ziyaretler"));
    }
}
