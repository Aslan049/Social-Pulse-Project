package com.socialpulse.backend.quality;

import com.socialpulse.backend.entity.IssueSeverity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.ingestion.InstagramAdapter;
import com.socialpulse.backend.ingestion.ParseResult;
import com.socialpulse.backend.ingestion.PlatformAdapter;
import com.socialpulse.backend.ingestion.XAdapter;
import com.socialpulse.backend.ingestion.XlsxTabularReader;
import com.socialpulse.backend.quality.DataQualityChecker.QualityFinding;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Gerçek biçimli iki dosyada veri kalitesi kontrolleri. */
class RealFilesQualityTest {

    private final DataQualityChecker checker = new DataQualityChecker();

    private List<QualityFinding> findingsFor(String resource, PlatformAdapter adapter) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/samples/" + resource)) {
            assertNotNull(in, resource + " test kaynaklarında bulunamadı");
            ParseResult result = adapter.parse(new XlsxTabularReader().read(in));
            return checker.check(result.points());
        }
    }

    @Test
    void instagramFile_reportsLateStartOfFollowsAsInfoNotAsMissingData() throws Exception {
        List<QualityFinding> findings = findingsFor("instagram_gunluk.xlsx", new InstagramAdapter());

        assertTrue(findings.stream().anyMatch(f -> f.metric() == Metric.NEW_FOLLOWERS
                && f.severity() == IssueSeverity.INFO
                && f.message().startsWith("Metrik 2025-10-15 tarihinde başlıyor; bu yüklemedeki ortak aralığın ilk 44 gün")));
        assertTrue(findings.stream().noneMatch(f -> f.metric() == Metric.NEW_FOLLOWERS
                && f.severity() == IssueSeverity.WARNING));
    }

    @Test
    void instagramFile_warnsAboutTheNegativeValue() throws Exception {
        List<QualityFinding> findings = findingsFor("instagram_gunluk.xlsx", new InstagramAdapter());

        assertTrue(findings.stream().anyMatch(f -> f.metric() == Metric.ENGAGEMENT
                && f.severity() == IssueSeverity.WARNING
                && f.message().startsWith("Negatif değer: 2025-09-19 = -7")));
    }

    @Test
    void instagramFile_flagsTheExtremeReachDays() throws Exception {
        List<QualityFinding> findings = findingsFor("instagram_gunluk.xlsx", new InstagramAdapter());

        assertTrue(findings.stream().anyMatch(f -> f.metric() == Metric.REACH
                && f.message().startsWith("Aşırı uç değer")));
    }

    @Test
    void instagramFile_hasTwentyThreeWarningsAndOneInfo() throws Exception {
        List<QualityFinding> findings = findingsFor("instagram_gunluk.xlsx", new InstagramAdapter());

        assertEquals(23, findings.stream().filter(f -> f.severity() == IssueSeverity.WARNING).count());
        assertEquals(1, findings.stream().filter(f -> f.severity() == IssueSeverity.INFO).count());
    }

    @Test
    void xFile_hasNoFindings() throws Exception {
        assertTrue(findingsFor("x_aylik_ozet.xlsx", new XAdapter()).isEmpty());
    }
}
