package com.socialpulse.backend.ingestion;

/** Atlanan bir satırın nedeni. Satır numarası, boş satırlar atlandığı için okunan sıraya göredir. */
public record ParseIssue(String sheet, int row, String message) {}
