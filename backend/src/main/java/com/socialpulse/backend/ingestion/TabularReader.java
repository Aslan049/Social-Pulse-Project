package com.socialpulse.backend.ingestion;

import java.io.InputStream;
import java.util.List;

/** Bir dosya türünü ham tablolara okuyan bileşen. Adaptörler dosya türünü bilmez. */
public interface TabularReader {

    int MAX_ROWS_PER_TABLE = 100_000;

    boolean supports(String filename);

    /** Okunamayan veya bozuk dosyada InvalidUploadException fırlatır. */
    List<RawTable> read(InputStream in);

    /**
     * Dosya adını da bilen okuma. CSV'de sayfa kavramı olmadığından tablo adı dosya adından
     * (uzantısız) türetilir; böylece "ziyaretler.csv" iki sütunlu bir dosyanın metriğini belirtebilir.
     */
    default List<RawTable> read(InputStream in, String filename) {
        return read(in);
    }
}
