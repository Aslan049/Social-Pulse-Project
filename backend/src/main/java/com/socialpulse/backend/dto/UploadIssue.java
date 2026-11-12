package com.socialpulse.backend.dto;

import com.socialpulse.backend.entity.IssueSeverity;

/**
 * Yükleme sırasında atlanan bir satır (ERROR) veya bir veri kalitesi uyarısı (WARNING).
 * Uyarılarda sheet boştur, field ilgili metriğin anahtarını taşır.
 */
public record UploadIssue(IssueSeverity severity, String sheet, int row, String field, String message) {}
