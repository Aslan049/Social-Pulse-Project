package com.socialpulse.backend.service;

import com.socialpulse.backend.dto.SheetSummary;
import com.socialpulse.backend.dto.UploadIssue;
import com.socialpulse.backend.dto.UploadResponse;
import com.socialpulse.backend.entity.IssueSeverity;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.entity.UploadBatch;
import com.socialpulse.backend.entity.UploadError;
import com.socialpulse.backend.entity.UploadStatus;
import com.socialpulse.backend.exception.InvalidUploadException;
import com.socialpulse.backend.exception.UploadNotFoundException;
import com.socialpulse.backend.ingestion.ParseIssue;
import com.socialpulse.backend.ingestion.ParseResult;
import com.socialpulse.backend.ingestion.PlatformAdapter;
import com.socialpulse.backend.ingestion.RawTable;
import com.socialpulse.backend.ingestion.TabularReader;
import com.socialpulse.backend.ingestion.UploadFileValidator;
import com.socialpulse.backend.quality.DataQualityChecker;
import com.socialpulse.backend.quality.DataQualityChecker.QualityFinding;
import com.socialpulse.backend.repository.MetricPointWriter;
import com.socialpulse.backend.repository.UploadBatchRepository;
import com.socialpulse.backend.repository.UploadErrorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UploadService {

    private static final int MAX_ISSUES_IN_RESPONSE = 100;
    private static final int MAX_PERSISTED_ISSUES = 1000;
    private static final int MAX_DETAIL_ISSUES = 500;

    private final UploadFileValidator validator;
    private final List<TabularReader> readers;
    private final List<PlatformAdapter> adapters;
    private final UploadBatchRepository batchRepository;
    private final UploadErrorRepository errorRepository;
    private final MetricPointWriter metricPointWriter;
    private final DataQualityChecker qualityChecker;

    /**
     * Dosyayı doğrular, ham tablolara okur, platformun adaptörüyle metriklere çevirir, veri kalitesi
     * uyarılarını üretir ve hepsini tek işlemde kaydeder: hata olursa hiçbir şey kaydedilmez.
     * Dosyanın kendisi saklanmaz. Adaptörü olmayan bir platformda yalnızca okunur ve kayıt
     * RECEIVED durumunda kalır.
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
            tables = reader.read(in, filename);
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
        List<QualityFinding> findings = result != null ? qualityChecker.check(result.points()) : List.of();

        UploadBatch batch = new UploadBatch();
        batch.setUserId(userId);
        batch.setPlatform(platform);
        batch.setOriginalFilename(filename);
        batch.setStatus(UploadStatus.RECEIVED);
        batch.setTotalRows(result != null ? result.dataRows() : rawRows);
        batch.setRejectedRows(result != null ? result.rejectedRows() : 0);
        batch.setWarningCount((int) findings.stream().filter(f -> f.severity() == IssueSeverity.WARNING).count());
        batchRepository.saveAndFlush(batch);

        List<String> ignoredSheets = List.of();
        List<String> ignoredColumns = List.of();
        List<UploadIssue> issues = List.of();
        if (result != null) {
            int imported = metricPointWriter.upsertAll(userId, batch.getId(), platform, result.points());
            batch.setImportedRows(imported);
            batch.setStatus(UploadStatus.COMPLETED);
            batchRepository.saveAndFlush(batch);

            List<UploadIssue> all = toIssues(result, findings);
            persistIssues(batch.getId(), all);
            issues = all.stream().limit(MAX_ISSUES_IN_RESPONSE).toList();
            ignoredSheets = result.ignoredSheets();
            ignoredColumns = result.ignoredColumns();
        }

        List<SheetSummary> sheets = tables.stream()
                .map(t -> new SheetSummary(t.name(), t.rowCount(), t.columnCount(), t.headerPreview()))
                .toList();
        return UploadResponse.of(batch, sheets, ignoredSheets, ignoredColumns, issues);
    }

    private static List<UploadIssue> toIssues(ParseResult result, List<QualityFinding> findings) {
        List<UploadIssue> all = new ArrayList<>();
        for (ParseIssue issue : result.issues()) {
            all.add(new UploadIssue(IssueSeverity.ERROR, issue.sheet(), issue.row(), null, issue.message()));
        }
        for (QualityFinding finding : findings) {
            String field = finding.metric() == null ? null : finding.metric().name();
            all.add(new UploadIssue(finding.severity(), null, 0, field, finding.message()));
        }
        return all;
    }

    /** En fazla MAX_PERSISTED_ISSUES kayıt saklanır; gerçek ret sayısı batch.rejectedRows'ta durur. */
    private void persistIssues(Long batchId, List<UploadIssue> issues) {
        List<UploadError> rows = issues.stream()
                .limit(MAX_PERSISTED_ISSUES)
                .map(i -> {
                    UploadError row = new UploadError();
                    row.setBatchId(batchId);
                    row.setSeverity(i.severity());
                    row.setSheet(truncate(i.sheet(), 100));
                    row.setRowNumber(i.row());
                    row.setField(truncate(i.field(), 100));
                    row.setMessage(truncate(i.message(), 500));
                    return row;
                })
                .toList();
        if (!rows.isEmpty()) {
            errorRepository.saveAll(rows);
        }
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
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

    /** Bir yüklemenin ayrıntısı ve kalıcı hata/uyarı listesi (en fazla 500 kayıt). */
    @Transactional(readOnly = true)
    public UploadResponse get(Long userId, Long batchId) {
        UploadBatch batch = batchRepository.findByIdAndUserId(batchId, userId)
                .orElseThrow(UploadNotFoundException::new);
        List<UploadIssue> issues = errorRepository.findByBatchIdOrderByIdAsc(batch.getId()).stream()
                .limit(MAX_DETAIL_ISSUES)
                .map(e -> new UploadIssue(e.getSeverity(), e.getSheet(), e.getRowNumber(), e.getField(), e.getMessage()))
                .toList();
        return UploadResponse.of(batch, List.of(), List.of(), List.of(), issues);
    }

    /** Yalnızca kendi yüklemesini silebilir; bağlı metrik ve hata kayıtları veritabanında CASCADE ile silinir. */
    @Transactional
    public void delete(Long userId, Long batchId) {
        UploadBatch batch = batchRepository.findByIdAndUserId(batchId, userId)
                .orElseThrow(UploadNotFoundException::new);
        batchRepository.delete(batch);
    }
}
