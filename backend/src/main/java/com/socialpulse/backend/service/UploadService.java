package com.socialpulse.backend.service;

import com.socialpulse.backend.dto.SheetSummary;
import com.socialpulse.backend.dto.UploadResponse;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.entity.UploadBatch;
import com.socialpulse.backend.entity.UploadStatus;
import com.socialpulse.backend.exception.InvalidUploadException;
import com.socialpulse.backend.exception.UploadNotFoundException;
import com.socialpulse.backend.ingestion.RawTable;
import com.socialpulse.backend.ingestion.TabularReader;
import com.socialpulse.backend.ingestion.UploadFileValidator;
import com.socialpulse.backend.repository.UploadBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UploadService {

    private final UploadFileValidator validator;
    private final List<TabularReader> readers;
    private final UploadBatchRepository batchRepository;

    /**
     * Gün 5: dosyayı doğrular, ham tablolara okur ve yükleme kaydı oluşturur.
     * Metriklerin çıkarılması ve kaydedilmesi (adaptörler) Gün 6'da eklenir.
     * Dosyanın kendisi saklanmaz.
     */
    @Transactional
    public UploadResponse upload(Long userId, Platform platform, MultipartFile file) {
        String filename = validator.validate(file);
        TabularReader reader = readers.stream()
                .filter(r -> r.supports(filename))
                .findFirst()
                .orElseThrow(() -> new InvalidUploadException("Desteklenmeyen dosya türü"));

        List<RawTable> tables;
        try (InputStream in = file.getInputStream()) {
            tables = reader.read(in);
        } catch (IOException e) {
            throw new InvalidUploadException("Dosya okunamadı");
        }

        int totalRows = tables.stream().mapToInt(RawTable::rowCount).sum();
        if (totalRows == 0) {
            throw new InvalidUploadException("Dosyada okunabilir veri bulunamadı");
        }

        UploadBatch batch = new UploadBatch();
        batch.setUserId(userId);
        batch.setPlatform(platform);
        batch.setOriginalFilename(filename);
        batch.setStatus(UploadStatus.RECEIVED);
        batch.setTotalRows(totalRows);
        batchRepository.saveAndFlush(batch);

        List<SheetSummary> sheets = tables.stream()
                .map(t -> new SheetSummary(t.name(), t.rowCount(), t.columnCount(), t.headerPreview()))
                .toList();
        return UploadResponse.of(batch, sheets);
    }

    @Transactional(readOnly = true)
    public List<UploadResponse> list(Long userId) {
        return batchRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                .map(b -> UploadResponse.of(b, List.of()))
                .toList();
    }

    /** Yalnızca kendi yüklemesini silebilir; bağlı metrik kayıtları veritabanında CASCADE ile silinir. */
    @Transactional
    public void delete(Long userId, Long batchId) {
        UploadBatch batch = batchRepository.findByIdAndUserId(batchId, userId)
                .orElseThrow(UploadNotFoundException::new);
        batchRepository.delete(batch);
    }
}
