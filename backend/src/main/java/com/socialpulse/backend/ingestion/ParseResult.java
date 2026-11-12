package com.socialpulse.backend.ingestion;

import java.util.List;

/**
 * Bir adaptörün çıktısı.
 *
 * @param points         geçerli ölçümler
 * @param issues         atlanan satırlar/hücreler ve nedenleri
 * @param ignoredSheets  tanınmayan veya türetilmiş olduğu için okunmayan sayfalar
 * @param ignoredColumns tanınan sayfalarda metrik olarak tanınmayıp yok sayılan sütunlar ("Sayfa: Sütun")
 * @param dataRows       incelenen ölçüm sayısı (uzun tabloda satır, geniş tabloda dolu metrik hücresi)
 */
public record ParseResult(List<ParsedPoint> points, List<ParseIssue> issues,
                          List<String> ignoredSheets, List<String> ignoredColumns, int dataRows) {

    public ParseResult(List<ParsedPoint> points, List<ParseIssue> issues,
                       List<String> ignoredSheets, int dataRows) {
        this(points, issues, ignoredSheets, List.of(), dataRows);
    }

    public int rejectedRows() {
        return issues.size();
    }
}
