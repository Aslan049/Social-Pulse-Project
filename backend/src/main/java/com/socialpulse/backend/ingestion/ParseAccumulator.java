package com.socialpulse.backend.ingestion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Bir dosyanın tabloları işlenirken birikenler (noktalar, sorunlar, yok sayılanlar). */
final class ParseAccumulator {

    final List<ParsedPoint> points = new ArrayList<>();
    final List<ParseIssue> issues = new ArrayList<>();
    final List<String> ignoredSheets = new ArrayList<>();
    final List<String> ignoredColumns = new ArrayList<>();
    final Set<String> seen = new HashSet<>();
    int dataRows = 0;

    void ignoreSheet(String name) {
        ignoredSheets.add(name);
    }

    void issue(String sheet, int row, String message) {
        issues.add(new ParseIssue(sheet, row, message));
    }

    ParseResult toResult() {
        return new ParseResult(points, issues, ignoredSheets, ignoredColumns, dataRows);
    }
}
