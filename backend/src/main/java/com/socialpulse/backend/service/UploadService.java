package com.socialpulse.backend.service;

import com.socialpulse.backend.dto.SheetSummary;
import com.socialpulse.backend.dto.UploadIssue;
import com.socialpulse.backend.dto.UploadResponse;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.entity.UploadBatch;
import com.socialpulse.backend.entity.UploadStatus;
import com.socialpulse.backend.exception.InvalidUploadException;
import com.socialpulse.backend.exception.UploadNotFoundException;
import com.socialpulse.backend.ingestion.ParseIssue;
import com.socialpulse.backend.ingestion.ParseResult;
import com.socialpulse.backend.ingestion.PlatformAdapter;
import com.socialpulse.backend.ingestion.RawTable;
import com.socialpulse.backend.ingestion.TabularReader;
import com.socialpulse.backend.ingestion.UploadFileValidator;
import com.socialpulse.backend.repository.MetricPointWriter;
import com.socialpulse.backend.repository.UploadBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final int MAX_ISSUES_IN_RESPONSE = 50;

    private final UploadFileValidator validator;
    private final List<TabularReader> readers;
    private final List<PlatformAdapter> adapters;
    private final UploadBatchRepository batchRepository;
    private final MetricPointWriter metricPointWriter;

    /**
     * Dosyayı doğrular, ham tablolara okur ve platformun adaptörü varsa metrikleri ortak modele
     * çevirip kaydeder. Adaptörü olmayan platformda (Gün 7'ye kadar X) yalnızca okunur ve kayıt
     * RECEIVED durumunda kalır. Her şey tek işlemdedir: hata olursa hiçbir şey kaydedilmez.
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

        int rawRows = tables.stream().mapToInt(RawTable::rowCount).sum();
        if (rawRows == 0) {
            throw new InvalidUploadException("Dosyada okunabilir veri bulunamadı");
        }

        Optional<PlatformAdapter> adapter = adapters.stream()
                .filter(a -> a.platform() == platform)
                .findFirst();
        ParseResult result = adapter.map(a -> a.parse(tables)).orElse(null);
        if (result != null && result.points().isEmpty()) {
            throw new InvalidUploadException("Dosyada içe aktarılabilir geçerli satır bulunamadı."
                    + firstIssueText(result));
        }

        UploadBatch batch = new UploadBatch();
        batch.setUserId(userId);
        batch.setPlatform(platform);
        batch.setOriginalFilename(filename);
        batch.setStatus(UploadStatus.RECEIVED);
        batch.setTotalRows(result != null ? result.dataRows() : rawRows);
        batch.setRejectedRows(result != null ? result.rejectedRows() : 0);
        batchRepository.saveAndFlush(batch);

        List<String> ignoredSheets = List.of();
        List<UploadIssue> issues = List.of();
        if (result != null) {
            int imported = metricPointWriter.upsertAll(userId, batch.getId(), platform, result.points());
            batch.setImportedRows(imported);
            batch.setStatus(UploadStatus.COMPLETED);
            batchRepository.saveAndFlush(batch);
            ignoredSheets = result.ignoredSheets();
            issues = result.issues().stream()
                    .limit(MAX_ISSUES_IN_RESPONSE)
                    .map(i -> new UploadIssue(i.sheet(), i.row(), i.message()))
                    .toList();
        }

        List<SheetSummary> sheets = tables.stream()
                .map(t -> new SheetSummary(t.name(), t.rowCount(), t.columnCount(), t.headerPreview()))
                .toList();
        return UploadResponse.of(batch, sheets, ignoredSheets, issues);
    }

    private static String firstIssueText(ParseResult result) {
        if (result.issues().isEmpty()) {
            return "";
        }
        ParseIssue first = result.issues().get(0);
        return " İlk hata: " + first.sheet() + " sayfası, satır " + first.row() + ": " + first.message();
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
