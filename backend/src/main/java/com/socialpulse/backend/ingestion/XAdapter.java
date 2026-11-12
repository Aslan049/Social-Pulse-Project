package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Platform;
import org.springframework.stereotype.Component;

/**
 * X: aylık özet. Satır = ay, sütun = metrik (geniş tablo) ya da metrik başına (Ay, Değer) sayfası.
 * Günlük görünen tablo aya çökertilmez, reddedilir.
 */
@Component
public class XAdapter extends AbstractTableAdapter {

    @Override
    public Platform platform() {
        return Platform.X;
    }

    @Override
    protected Granularity expectedGranularity() {
        return Granularity.MONTH;
    }

    @Override
    protected String platformLabel() {
        return "X";
    }
}
