package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Metric;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Sayfa ve sütun adlarını ortak metrik sözlüğüne bağlar (Türkçe ve İngilizce takma adlar).
 * Eşleme kodun içine dağılmaz, tek yerde durur: yeni bir başlık görüldüğünde buraya eklenir.
 */
public final class MetricAliases {

    private static final Map<String, Metric> ALIASES = new HashMap<>();

    static {
        add(Metric.VIEWS, "Görüntülemeler", "Görüntülenmeler", "Görüntüleme", "Views", "Impressions");
        add(Metric.ENGAGEMENT, "Etkileşim", "Etkileşimler", "Engagement", "Engagements", "Interactions");
        add(Metric.NEW_FOLLOWERS, "Takipler", "Yeni takip sayısı", "Yeni takipçiler", "Follows", "New followers");
        add(Metric.PROFILE_VISITS, "Ziyaretler", "Profil ziyaretleri", "Profile visits", "Visits");
        add(Metric.REACH, "Erişim", "Reach");
        add(Metric.LINK_CLICKS, "Bağlantı Tıklamaları", "Link clicks");
        add(Metric.UNFOLLOWS, "Takibi bırakmalar", "Unfollows");
        add(Metric.LIKES, "Beğeni", "Beğeniler", "Likes");
        add(Metric.REPLIES, "Yanıtlar", "Replies");
        add(Metric.REPOSTS, "Yeniden gönderiler", "Reposts");
        add(Metric.POSTS_CREATED, "Gönderi Oluştur", "Posts created");
    }

    private MetricAliases() {
    }

    private static void add(Metric metric, String... names) {
        for (String name : names) {
            ALIASES.put(normalize(name), metric);
        }
    }

    public static Optional<Metric> find(String text) {
        if (text == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ALIASES.get(normalize(text)));
    }

    /**
     * Büyük/küçük harf, boşluk, noktalama ve Türkçe aksanları yok sayan karşılaştırma anahtarı.
     * Örn. "Bağlantı Tıklamaları" ve "baglanti tiklamalari" aynı anahtarı üretir.
     */
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        String s = text.replace('ı', 'i').replace('İ', 'I');
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
