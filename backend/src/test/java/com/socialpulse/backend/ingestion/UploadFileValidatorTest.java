package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.exception.InvalidUploadException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class UploadFileValidatorTest {

    private final UploadFileValidator validator = new UploadFileValidator();

    private MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    @Test
    void nullFile_isRejected() {
        assertThrows(InvalidUploadException.class, () -> validator.validate(null));
    }

    @Test
    void emptyFile_isRejected() {
        assertThrows(InvalidUploadException.class, () -> validator.validate(file("a.csv", new byte[0])));
    }

    @Test
    void unsupportedExtension_isRejected() {
        assertThrows(InvalidUploadException.class,
                () -> validator.validate(file("notlar.txt", "abc".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void fakeXlsx_isRejected() {
        assertThrows(InvalidUploadException.class,
                () -> validator.validate(file("rapor.xlsx", "bu bir excel dosyasi degil".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void xlsxWithZipSignature_isAccepted() {
        byte[] zipHeader = {0x50, 0x4B, 0x03, 0x04, 0x00, 0x00};
        assertEquals("rapor.xlsx", validator.validate(file("rapor.xlsx", zipHeader)));
    }

    @Test
    void validCsv_returnsFilename() {
        assertEquals("rapor.csv", validator.validate(file("rapor.csv", "a,b\n1,2".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void pathInFilename_isStripped() {
        assertEquals("passwd.csv",
                validator.validate(file("../../etc/passwd.csv", "a,b".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void tooLargeFile_isRejected() {
        byte[] big = new byte[(int) UploadFileValidator.MAX_BYTES + 1];
        assertThrows(InvalidUploadException.class, () -> validator.validate(file("buyuk.csv", big)));
    }
}
