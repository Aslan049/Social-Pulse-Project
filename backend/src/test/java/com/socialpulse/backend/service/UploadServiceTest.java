package com.socialpulse.backend.service;

import com.socialpulse.backend.dto.UploadIssue;
import com.socialpulse.backend.dto.UploadResponse;
import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.IssueSeverity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.entity.UploadBatch;
import com.socialpulse.backend.entity.UploadError;
import com.socialpulse.backend.entity.UploadStatus;
import com.socialpulse.backend.exception.InvalidUploadException;
import com.socialpulse.backend.exception.UploadNotFoundException;
import com.socialpulse.backend.ingestion.CsvTabularReader;
import com.socialpulse.backend.ingestion.ParseIssue;
import com.socialpulse.backend.ingestion.ParseResult;
import com.socialpulse.backend.ingestion.ParsedPoint;
import com.socialpulse.backend.ingestion.PlatformAdapter;
import com.socialpulse.backend.ingestion.RawTable;
import com.socialpulse.backend.ingestion.UploadFileValidator;
import com.socialpulse.backend.ingestion.XlsxTabularReader;
import com.socialpulse.backend.quality.DataQualityChecker;
import com.socialpulse.backend.repository.MetricPointWriter;
import com.socialpulse.backend.repository.UploadBatchRepository;
import com.socialpulse.backend.repository.UploadErrorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock UploadBatchRepository repository;
    @Mock UploadErrorRepository errorRepository;
    @Mock MetricPointWriter writer;
    @Mock PlatformAdapter instagramAdapter;

    private UploadService service;

    @BeforeEach
    void setUp() {
        service = new UploadService(
                new UploadFileValidator(),
                List.of(new CsvTabularReader(), new XlsxTabularReader()),
                List.of(instagramAdapter),
                repository,
                errorRepository,
                writer,
                new DataQualityChecker());
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private void assignIdOnSave() {
        when(repository.saveAndFlush(any(UploadBatch.class))).thenAnswer(inv -> {
            UploadBatch b = inv.getArgument(0);
            b.setId(42L);
            return b;
        });
    }

    private static ParsedPoint engagement(int day, String value) {
        return new ParsedPoint(Metric.ENGAGEMENT, Granularity.DAY, LocalDate.of(2026, 1, day), new BigDecimal(value));
    }

    @Test
    @SuppressWarnings("unchecked")
    void upload_withAdapter_storesMetricsAndPersistsIssuesAndWarnings() {
        when(instagramAdapter.platform()).thenReturn(Platform.INSTAGRAM);
        when(instagramAdapter.parse(anyList())).thenReturn(new ParseResult(
                List.of(engagement(1, "5"), engagement(2, "-7")),
                List.of(new ParseIssue("Ziyaretler", 3, "Geçersiz tarih: abc")),
                List.of("Tüm Veriler"),
                List.of("Aylık Özet: Bilinmeyen Sütun"),
                3));
        assignIdOnSave();
        when(writer.upsertAll(eq(1L), eq(42L), eq(Platform.INSTAGRAM), anyList())).thenReturn(2);

        UploadResponse response = service.upload(1L, Platform.INSTAGRAM,
                csv("ziyaretler.csv", "Tarih,Değer\n2026-01-01,5\n"));

        assertEquals(UploadStatus.COMPLETED, response.status());
        assertEquals(3, response.totalRows());
        assertEquals(2, response.importedRows());
        assertEquals(1, response.rejectedRows());
        assertEquals(1, response.warningCount());
        assertEquals(List.of("Tüm Veriler"), response.ignoredSheets());
        assertEquals(List.of("Aylık Özet: Bilinmeyen Sütun"), response.ignoredColumns());
        assertEquals(2, response.issues().size());
        assertEquals(IssueSeverity.ERROR, response.issues().get(0).severity());
        assertEquals(IssueSeverity.WARNING, response.issues().get(1).severity());
        assertEquals("ENGAGEMENT", response.issues().get(1).field());

        ArgumentCaptor<List<UploadError>> captor = ArgumentCaptor.forClass(List.class);
        verify(errorRepository).saveAll(captor.capture());
        List<UploadError> saved = captor.getValue();
        assertEquals(2, saved.size());
        assertEquals(42L, saved.get(0).getBatchId());
        assertEquals(IssueSeverity.ERROR, saved.get(0).getSeverity());
        assertEquals("Ziyaretler", saved.get(0).getSheet());
        assertEquals(3, saved.get(0).getRowNumber());
        assertEquals(IssueSeverity.WARNING, saved.get(1).getSeverity());
        assertEquals("ENGAGEMENT", saved.get(1).getField());
        assertEquals(0, saved.get(1).getRowNumber());
        verify(repository, times(2)).saveAndFlush(any(UploadBatch.class));
    }

    @Test
    void upload_cleanFile_savesNoIssueRows() {
        when(instagramAdapter.platform()).thenReturn(Platform.INSTAGRAM);
        when(instagramAdapter.parse(anyList())).thenReturn(new ParseResult(
                List.of(engagement(1, "5"), engagement(2, "6")), List.of(), List.of(), 2));
        assignIdOnSave();
        when(writer.upsertAll(eq(1L), eq(42L), eq(Platform.INSTAGRAM), anyList())).thenReturn(2);

        UploadResponse response = service.upload(1L, Platform.INSTAGRAM,
                csv("ziyaretler.csv", "Tarih,Değer\n2026-01-01,5\n2026-01-02,6\n"));

        assertEquals(0, response.warningCount());
        assertTrue(response.issues().isEmpty());
        verify(errorRepository, never()).saveAll(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void upload_csvFilenameBecomesTableName() {
        when(instagramAdapter.platform()).thenReturn(Platform.INSTAGRAM);
        when(instagramAdapter.parse(anyList())).thenReturn(new ParseResult(
                List.of(engagement(1, "5")), List.of(), List.of(), 1));
        assignIdOnSave();
        when(writer.upsertAll(any(), any(), any(), anyList())).thenReturn(1);

        service.upload(1L, Platform.INSTAGRAM, csv("ziyaretler.csv", "Tarih,Değer\n2026-01-01,5\n"));

        ArgumentCaptor<List<RawTable>> captor = ArgumentCaptor.forClass(List.class);
        verify(instagramAdapter).parse(captor.capture());
        assertEquals("ziyaretler", captor.getValue().get(0).name());
    }

    @Test
    void upload_platformWithoutAdapter_staysReceivedAndStoresNothing() {
        when(instagramAdapter.platform()).thenReturn(Platform.INSTAGRAM);
        assignIdOnSave();

        UploadResponse response = service.upload(1L, Platform.X,
                csv("x.csv", "Ay,Görüntülenmeler\n2026-01-01,5\n"));

        ArgumentCaptor<UploadBatch> captor = ArgumentCaptor.forClass(UploadBatch.class);
        verify(repository).saveAndFlush(captor.capture());
        assertEquals(UploadStatus.RECEIVED, captor.getValue().getStatus());
        assertEquals(2, captor.getValue().getTotalRows());
        assertEquals(UploadStatus.RECEIVED, response.status());
        verifyNoInteractions(writer, errorRepository);
    }

    @Test
    void upload_noValidPoints_isRejectedAndNothingSaved() {
        when(instagramAdapter.platform()).thenReturn(Platform.INSTAGRAM);
        when(instagramAdapter.parse(anyList())).thenReturn(new ParseResult(
                List.of(), List.of(new ParseIssue("Ziyaretler", 2, "Geçersiz tarih: abc")), List.of(), 1));

        InvalidUploadException ex = assertThrows(InvalidUploadException.class, () ->
                service.upload(1L, Platform.INSTAGRAM, csv("ziyaretler.csv", "Tarih,Değer\nabc,6\n")));

        assertTrue(ex.getMessage().contains("İlk hata"));
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(writer, errorRepository);
    }

    @Test
    void upload_fileWithoutRows_isRejectedAndNothingSaved() {
        assertThrows(InvalidUploadException.class,
                () -> service.upload(1L, Platform.INSTAGRAM, csv("bos.csv", "\n\n")));

        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(writer, errorRepository);
    }

    @Test
    void upload_unsupportedType_isRejected() {
        MockMultipartFile txt = new MockMultipartFile("file", "notlar.txt", "text/plain",
                "abc".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidUploadException.class, () -> service.upload(1L, Platform.X, txt));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void get_returnsPersistedIssues() {
        UploadBatch batch = new UploadBatch();
        batch.setId(5L);
        batch.setPlatform(Platform.INSTAGRAM);
        batch.setOriginalFilename("a.xlsx");
        batch.setWarningCount(1);
        UploadError error = new UploadError();
        error.setSeverity(IssueSeverity.WARNING);
        error.setField("REACH");
        error.setMessage("Aşırı uç değer");
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(batch));
        when(errorRepository.findByBatchIdOrderByIdAsc(5L)).thenReturn(List.of(error));

        UploadResponse response = service.get(1L, 5L);

        assertEquals(5L, response.id());
        assertEquals(1, response.warningCount());
        assertEquals(1, response.issues().size());
        UploadIssue issue = response.issues().get(0);
        assertEquals(IssueSeverity.WARNING, issue.severity());
        assertEquals("REACH", issue.field());
    }

    @Test
    void get_batchOfAnotherUser_isNotFound() {
        when(repository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThrows(UploadNotFoundException.class, () -> service.get(2L, 5L));
        verifyNoInteractions(errorRepository);
    }

    @Test
    void delete_ownBatch_deletesIt() {
        UploadBatch batch = new UploadBatch();
        when(repository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(batch));

        service.delete(1L, 5L);

        verify(repository).delete(batch);
    }

    @Test
    void delete_batchOfAnotherUser_isNotFound() {
        when(repository.findByIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThrows(UploadNotFoundException.class, () -> service.delete(2L, 5L));

        verify(repository, never()).delete(any(UploadBatch.class));
    }
}
