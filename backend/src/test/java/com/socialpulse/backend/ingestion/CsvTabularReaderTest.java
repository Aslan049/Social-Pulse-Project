package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvTabularReaderTest {

    private final CsvTabularReader reader = new CsvTabularReader();

    private List<List<Object>> read(byte[] bytes) {
        List<RawTable> tables = reader.read(new ByteArrayInputStream(bytes));
        assertEquals(1, tables.size());
        return tables.get(0).rows();
    }

    private List<List<Object>> read(String text) {
        return read(text.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void commaDelimited_isParsed() {
        List<List<Object>> rows = read("Tarih,Değer\n2026-01-01,5\n");

        assertEquals(2, rows.size());
        assertEquals(List.of("Tarih", "Değer"), rows.get(0));
        assertEquals(List.of("2026-01-01", "5"), rows.get(1));
    }

    @Test
    void semicolonDelimited_isDetected() {
        List<List<Object>> rows = read("Tarih;Değer\n2026-01-01;5\n");

        assertEquals(List.of("Tarih", "Değer"), rows.get(0));
        assertEquals(List.of("2026-01-01", "5"), rows.get(1));
    }

    @Test
    void utf8Bom_isRemoved() {
        List<List<Object>> rows = read("\uFEFFTarih,Değer\n2026-01-01,5\n");

        assertEquals("Tarih", rows.get(0).get(0));
    }

    @Test
    void sepDirectiveLine_isSkipped() {
        List<List<Object>> rows = read("sep=;\nTarih;Değer\n2026-01-01;5\n");

        assertEquals(2, rows.size());
        assertEquals("Tarih", rows.get(0).get(0));
    }

    @Test
    void turkishWindows1254_fallbackWorks() {
        byte[] bytes = "Beğeni;Yorum\n3;4\n".getBytes(Charset.forName("windows-1254"));

        List<List<Object>> rows = read(bytes);

        assertEquals("Beğeni", rows.get(0).get(0));
    }

    @Test
    void utf16WithBom_isDecoded() {
        byte[] body = "a,b\n1,2\n".getBytes(StandardCharsets.UTF_16LE);
        byte[] bytes = new byte[body.length + 2];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xFE;
        System.arraycopy(body, 0, bytes, 2, body.length);

        List<List<Object>> rows = read(bytes);

        assertEquals(List.of("a", "b"), rows.get(0));
    }

    @Test
    void quotedFieldsWithDelimiterAndNewline_areKept() {
        List<List<Object>> rows = read("a,b\n\"x,y\",\"satir1\nsatir2\"\n");

        assertEquals("x,y", rows.get(1).get(0));
        assertEquals("satir1\nsatir2", rows.get(1).get(1));
    }

    @Test
    void blankLinesAndTrailingEmptyColumns_areDropped() {
        List<List<Object>> rows = read("a,b,,\n\n1,2,,\n");

        assertEquals(2, rows.size());
        assertEquals(2, rows.get(0).size());
    }

    @Test
    void emptyCells_becomeNull() {
        List<List<Object>> rows = read("a,b,c\n1,,3\n");

        assertNull(rows.get(1).get(1));
    }

    @Test
    void emptyInput_givesTableWithNoRows() {
        List<RawTable> tables = reader.read(new ByteArrayInputStream(new byte[0]));

        assertEquals(0, tables.get(0).rowCount());
    }

    @Test
    void supports_onlyCsv() {
        assertTrue(reader.supports("RAPOR.CSV"));
        assertFalse(reader.supports("rapor.xlsx"));
    }

    @Test
    void malformedQuote_doesNotCrashAsUnexpectedError() {
        // Kapanmayan tırnak: ya okunur ya da InvalidUploadException; başka hata türü sızmamalı
        try {
            reader.read(new ByteArrayInputStream("a,b\n\"acik tirnak,2\n".getBytes(StandardCharsets.UTF_8)));
        } catch (InvalidUploadException expected) {
            // kabul edilebilir
        }
    }
}
