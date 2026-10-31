package com.socialpulse.backend.ingestion;

import java.util.List;

/**
 * CSV veya XLSX dosyasından okunan ham tablo. Hücre değerleri String, Double, LocalDateTime,
 * Boolean veya null olabilir. Tamamen boş satırlar atlanır, satır sonundaki boş hücreler kırpılır.
 */
public record RawTable(String name, List<List<Object>> rows) {

    public int rowCount() {
        return rows.size();
    }

    public int columnCount() {
        return rows.stream().mapToInt(List::size).max().orElse(0);
    }

    /** İlk satırın (genellikle başlık) metin hâli; yüklenen dosyanın yapısını görmek için. */
    public List<String> headerPreview() {
        if (rows.isEmpty()) {
            return List.of();
        }
        return rows.get(0).stream()
                .limit(20)
                .map(cell -> cell == null ? "" : truncate(String.valueOf(cell)))
                .toList();
    }

    private static String truncate(String value) {
        return value.length() <= 60 ? value : value.substring(0, 60);
    }
}
