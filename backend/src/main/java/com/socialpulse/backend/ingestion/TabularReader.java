package com.socialpulse.backend.ingestion;

import java.io.InputStream;
import java.util.List;

/** Bir dosya türünü ham tablolara okuyan bileşen. Adaptörler dosya türünü bilmez. */
public interface TabularReader {

    int MAX_ROWS_PER_TABLE = 100_000;

    boolean supports(String filename);

    /** Okunamayan veya bozuk dosyada InvalidUploadException fırlatır. */
    List<RawTable> read(InputStream in);
}
