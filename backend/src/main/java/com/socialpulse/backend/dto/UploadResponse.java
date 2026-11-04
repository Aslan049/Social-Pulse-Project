package com.socialpulse.backend.dto;

import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.entity.UploadBatch;
import com.socialpulse.backend.entity.UploadStatus;

import java.time.Instant;
import java.util.List;

public record UploadResponse(
        Long id,
        Platform platform,
        String filename,
        UploadStatus status,
        int totalRows,
        int importedRows,
        int rejectedRows,
        Instant uploadedAt,
        List<SheetSummary> sheets,
        List<String> ignoredSheets,
        List<UploadIssue> issues
) {
    public static UploadResponse of(UploadBatch batch, List<SheetSummary> sheets) {
        return of(batch, sheets, List.of(), List.of());
    }

    public static UploadResponse of(UploadBatch batch, List<SheetSummary> sheets,
                                    List<String> ignoredSheets, List<UploadIssue> issues) {
        return new UploadResponse(
                batch.getId(),
                batch.getPlatform(),
                batch.getOriginalFilename(),
                batch.getStatus(),
                batch.getTotalRows(),
                batch.getImportedRows(),
                batch.getRejectedRows(),
                batch.getUploadedAt(),
                sheets,
                ignoredSheets,
                issues);
    }
}
