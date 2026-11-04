package com.socialpulse.backend.dto;

/** Yükleme sırasında atlanan bir satır. */
public record UploadIssue(String sheet, int row, String message) {}
