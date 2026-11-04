package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Metric;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MetricAliasesTest {

    @Test
    void turkishNamesMapToMetrics() {
        assertEquals(Metric.ENGAGEMENT, MetricAliases.find("Etkileşim").orElseThrow());
        assertEquals(Metric.VIEWS, MetricAliases.find("Görüntülemeler").orElseThrow());
        assertEquals(Metric.VIEWS, MetricAliases.find("Görüntülenmeler").orElseThrow());
        assertEquals(Metric.REACH, MetricAliases.find("Erişim").orElseThrow());
        assertEquals(Metric.NEW_FOLLOWERS, MetricAliases.find("Takipler").orElseThrow());
        assertEquals(Metric.NEW_FOLLOWERS, MetricAliases.find("Yeni takip sayısı").orElseThrow());
        assertEquals(Metric.PROFILE_VISITS, MetricAliases.find("Ziyaretler").orElseThrow());
        assertEquals(Metric.PROFILE_VISITS, MetricAliases.find("Profil ziyaretleri").orElseThrow());
        assertEquals(Metric.LINK_CLICKS, MetricAliases.find("Bağlantı Tıklamaları").orElseThrow());
        assertEquals(Metric.UNFOLLOWS, MetricAliases.find("Takibi bırakmalar").orElseThrow());
        assertEquals(Metric.POSTS_CREATED, MetricAliases.find("Gönderi Oluştur").orElseThrow());
    }

    @Test
    void caseAccentAndSpacingAreIgnored() {
        assertEquals(Metric.LINK_CLICKS, MetricAliases.find("baglanti tiklamalari").orElseThrow());
        assertEquals(Metric.VIEWS, MetricAliases.find("GÖRÜNTÜLEMELER").orElseThrow());
        assertEquals("baglantitiklamalari", MetricAliases.normalize("Bağlantı  Tıklamaları!"));
    }

    @Test
    void englishNamesAreSupported() {
        assertEquals(Metric.PROFILE_VISITS, MetricAliases.find("Profile Visits").orElseThrow());
        assertEquals(Metric.REACH, MetricAliases.find("Reach").orElseThrow());
    }

    @Test
    void derivedSheetsAreNotMetrics() {
        assertTrue(MetricAliases.find("Tüm Veriler").isEmpty());
        assertTrue(MetricAliases.find("İstatistikler").isEmpty());
        assertTrue(MetricAliases.find(null).isEmpty());
    }
}
