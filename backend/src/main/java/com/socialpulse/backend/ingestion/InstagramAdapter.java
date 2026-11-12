package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Platform;
import org.springframework.stereotype.Component;

/**
 * Instagram: günlük seri. Her metrik için ayrı sayfa (Tarih, Değer; metrik sayfa adından), ya da
 * tarih + metrik sütunlu geniş tablo (CSV). Özet ve birleşik sayfalar okunmaz.
 */
@Component
public class InstagramAdapter extends AbstractTableAdapter {

    @Override
    public Platform platform() {
        return Platform.INSTAGRAM;
    }

    @Override
    protected Granularity expectedGranularity() {
        return Granularity.DAY;
    }

    @Override
    protected String platformLabel() {
        return "Instagram";
    }
}
