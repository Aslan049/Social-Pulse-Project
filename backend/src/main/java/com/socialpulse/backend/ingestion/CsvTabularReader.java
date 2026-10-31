package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.exception.InvalidUploadException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * CSV okuyucu: ayraç (virgül, noktalı virgül, sekme), BOM, UTF-16 ve Türkçe kodlama (Windows-1254)
 * yedeğini otomatik ele alır. Tüm hücreler metin olarak döner; sayı ve tarih ayrıştırma adaptörlerin işidir.
 */
@Component
public class CsvTabularReader implements TabularReader {

    private static final char[] CANDIDATE_DELIMITERS = {',', ';', '\t'};

    @Override
    public boolean supports(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".csv");
    }

    @Override
    public List<RawTable> read(InputStream in) {
        String text;
        try {
            text = decode(in.readAllBytes());
        } catch (IOException e) {
            throw new InvalidUploadException("Dosya okunamadı");
        }
        text = stripSepDirective(text);
        char delimiter = detectDelimiter(text);

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimiter)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .build();

        List<List<Object>> rows = new ArrayList<>();
        try (CSVParser parser = format.parse(new StringReader(text))) {
            for (CSVRecord record : parser) {
                List<Object> cells = new ArrayList<>(record.size());
                for (int i = 0; i < record.size(); i++) {
                    String value = record.get(i);
                    cells.add(value == null || value.isEmpty() ? null : value);
                }
                trimTrailingNulls(cells);
                if (cells.isEmpty()) {
                    continue;
                }
                rows.add(cells);
                if (rows.size() > MAX_ROWS_PER_TABLE) {
                    throw new InvalidUploadException("Dosyada çok fazla satır var");
                }
            }
        } catch (InvalidUploadException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw new InvalidUploadException("CSV dosyası okunamadı veya biçimi bozuk");
        }
        return List.of(new RawTable("CSV", rows));
    }

    private static String decode(byte[] bytes) {
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xFE) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
        }
        if (bytes.length >= 2 && (bytes[0] & 0xFF) == 0xFE && (bytes[1] & 0xFF) == 0xFF) {
            return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
        }
        String text;
        try {
            text = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            // UTF-8 değilse Türkçe Windows kodlamasına düş (Excel'in eski CSV çıktısı)
            Charset fallback = Charset.isSupported("windows-1254")
                    ? Charset.forName("windows-1254")
                    : StandardCharsets.ISO_8859_1;
            text = new String(bytes, fallback);
        }
        if (!text.isEmpty() && text.charAt(0) == '\uFEFF') {
            text = text.substring(1);
        }
        return text;
    }

    /** Bazı dışa aktarımlar ilk satıra "sep=;" yazar; bu satır veri değildir. */
    private static String stripSepDirective(String text) {
        if (text.regionMatches(true, 0, "sep=", 0, 4)) {
            int newline = text.indexOf('\n');
            return newline < 0 ? "" : text.substring(newline + 1);
        }
        return text;
    }

    private static char detectDelimiter(String text) {
        String firstLine = null;
        for (String line : text.split("\\R", 20)) {
            if (!line.isBlank()) {
                firstLine = line;
                break;
            }
        }
        if (firstLine == null) {
            return ',';
        }
        char best = ',';
        int bestCount = 0;
        for (char candidate : CANDIDATE_DELIMITERS) {
            int count = 0;
            for (int i = 0; i < firstLine.length(); i++) {
                if (firstLine.charAt(i) == candidate) {
                    count++;
                }
            }
            if (count > bestCount) {
                best = candidate;
                bestCount = count;
            }
        }
        return best;
    }

    private static void trimTrailingNulls(List<Object> cells) {
        int last = cells.size() - 1;
        while (last >= 0 && cells.get(last) == null) {
            cells.remove(last--);
        }
    }
}
