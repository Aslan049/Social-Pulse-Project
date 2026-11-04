package com.socialpulse.backend.service;

import com.socialpulse.backend.dto.UploadResponse;
import com.socialpulse.backend.entity.Granularity;
import com.socialpulse.backend.entity.Metric;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.entity.UploadBatch;
import com.socialpulse.backend.entity.UploadStatus;
import com.socialpulse.backend.exception.InvalidUploadException;
import com.socialpulse.backend.exception.UploadNotFoundException;
import com.socialpulse.backend.ingestion.CsvTabularReader;
import com.socialpulse.backend.ingestion.ParseIssue;
import com.socialpulse.backend.ingestion.ParseResult;
import com.socialpulse.backend.ingestion.ParsedPoint;
import com.socialpulse.backend.ingestion.PlatformAdapter;
import com.socialpulse.backend.ingestion.UploadFileValidator;
import com.socialpulse.backend.ingestion.XlsxTabularReader;
import com.socialpulse.backend.repository.MetricPointWriter;
import com.socialpulse.backend.repository.UploadBatchRepository;
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
                writer);
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

    @Test
    void upload_withAdapter_storesMetricsAndCompletesBatch() {
        when(instagramAdapter.platform()).thenReturn(Platform.INSTAGRAM);
        ParsedPoint point = new ParsedPoint(Metric.VIEWS, Granularity.DAY, LocalDate.of(2026, 1, 1), new BigDecimal("5"));
        when(instagramAdapter.parse(anyList())).thenReturn(new ParseResult(
                List.of(point),
                List.of(new ParseIssue("Ziyaretler", 3, "Geçersiz tarih: abc")),
                List.of("Tüm Veriler"),
                2));
        assignIdOnSave();
        when(writer.upsertAll(eq(1L), eq(42L), eq(Platform.INSTAGRAM), anyList())).thenReturn(1);

        UploadResponse response = service.upload(1L, Platform.INSTAGRAM,
                csv("ziyaretler.csv", "Tarih,Değer\n2026-01-01,5\nabc,6\n"));

        verify(writer).upsertAll(eq(1L), eq(42L), eq(Platform.INSTAGRAM), anyList());
        assertEquals(UploadStatus.COMPLETED, response.status());
        assertEquals(2, response.totalRows());
        assertEquals(1, response.importedRows());
        assertEquals(1, response.rejectedRows());
        assertEquals(List.of("Tüm Veriler"), response.ignoredSheets());
        assertEquals(1, response.issues().size());
        assertEquals("Ziyaretler", response.issues().get(0).sheet());
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
        verifyNoInteractions(writer);
    }

    @Test
    void upload_noValidPoints_isRejectedAndNothingSaved() {
        when(instagramAdapter.platform()).thenReturn(Platform.INSTAGRAM);
        when(instagramAdapter.parse(anyList())).thenReturn(new ParseResult(
                List.of(),
                List.of(new ParseIssue("Ziyaretler", 2, "Geçersiz tarih: abc")),
                List.of(),
                1));

        InvalidUploadException ex = assertThrows(InvalidUploadException.class, () ->
                service.upload(1L, Platform.INSTAGRAM, csv("ziyaretler.csv", "Tarih,Değer\nabc,6\n")));

        assertTrue(ex.getMessage().contains("İlk hata"));
        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(writer);
    }

    @Test
    void upload_fileWithoutRows_isRejectedAndNothingSaved() {
        assertThrows(InvalidUploadException.class,
                () -> service.upload(1L, Platform.INSTAGRAM, csv("bos.csv", "\n\n")));

        verify(repository, never()).saveAndFlush(any());
        verifyNoInteractions(writer);
    }

    @Test
    void upload_unsupportedType_isRejected() {
        MockMultipartFile txt = new MockMultipartFile("file", "notlar.txt", "text/plain",
                "abc".getBytes(StandardCharsets.UTF_8));

        assertThrows(InvalidUploadException.class, () -> service.upload(1L, Platform.X, txt));

        verify(repository, never()).saveAndFlush(any());
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
