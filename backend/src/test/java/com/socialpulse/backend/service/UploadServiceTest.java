package com.socialpulse.backend.service;

import com.socialpulse.backend.dto.UploadResponse;
import com.socialpulse.backend.entity.Platform;
import com.socialpulse.backend.entity.UploadBatch;
import com.socialpulse.backend.entity.UploadStatus;
import com.socialpulse.backend.exception.InvalidUploadException;
import com.socialpulse.backend.exception.UploadNotFoundException;
import com.socialpulse.backend.ingestion.CsvTabularReader;
import com.socialpulse.backend.ingestion.UploadFileValidator;
import com.socialpulse.backend.ingestion.XlsxTabularReader;
import com.socialpulse.backend.repository.UploadBatchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadServiceTest {

    @Mock UploadBatchRepository repository;

    private UploadService service;

    @BeforeEach
    void setUp() {
        service = new UploadService(
                new UploadFileValidator(),
                List.of(new CsvTabularReader(), new XlsxTabularReader()),
                repository);
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void upload_csv_createsBatchAndSummarizesSheets() {
        when(repository.saveAndFlush(any(UploadBatch.class))).thenAnswer(inv -> {
            UploadBatch b = inv.getArgument(0);
            b.setId(42L);
            return b;
        });

        UploadResponse response = service.upload(1L, Platform.INSTAGRAM,
                csv("ziyaretler.csv", "Tarih,Değer\n2026-01-01,5\n2026-01-02,7\n"));

        ArgumentCaptor<UploadBatch> captor = ArgumentCaptor.forClass(UploadBatch.class);
        verify(repository).saveAndFlush(captor.capture());
        UploadBatch saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals(Platform.INSTAGRAM, saved.getPlatform());
        assertEquals("ziyaretler.csv", saved.getOriginalFilename());
        assertEquals(UploadStatus.RECEIVED, saved.getStatus());
        assertEquals(3, saved.getTotalRows());

        assertEquals(42L, response.id());
        assertEquals(1, response.sheets().size());
        assertEquals(List.of("Tarih", "Değer"), response.sheets().get(0).header());
    }

    @Test
    void upload_fileWithoutRows_isRejectedAndNothingSaved() {
        assertThrows(InvalidUploadException.class,
                () -> service.upload(1L, Platform.X, csv("bos.csv", "\n\n")));

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void upload_unsupportedType_isRejected() {
        MockMultipartFile txt = new MockMultipartFile("file", "notlar.txt", "text/plain", "abc".getBytes(StandardCharsets.UTF_8));

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
