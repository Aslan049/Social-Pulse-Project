package com.socialpulse.backend.ingestion;

import java.util.List;

/**
 * Bir adaptörün çıktısı.
 *
 * @param points        geçerli ölçümler
 * @param issues        atlanan satırlar ve nedenleri
 * @param ignoredSheets tanınmayan veya türetilmiş olduğu için okunmayan sayfalar
 * @param dataRows      tanınan sayfalarda incelenen veri satırı sayısı
 */
public record ParseResult(List<ParsedPoint> points, List<ParseIssue> issues,
                          List<String> ignoredSheets, int dataRows) {

    public int rejectedRows() {
        return issues.size();
    }
}
