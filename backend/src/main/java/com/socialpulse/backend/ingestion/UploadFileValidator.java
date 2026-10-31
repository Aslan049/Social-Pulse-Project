package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.exception.InvalidUploadException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

/** Yüklenen dosyanın uzantısını, boyutunu ve (XLSX için) imzasını denetler. */
@Component
public class UploadFileValidator {

    public static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("csv", "xlsx");

    /** Geçerliyse güvenli (yol bilgisi temizlenmiş) dosya adını döner. */
    public String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Dosya boş veya seçilmedi");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidUploadException("Dosya 10 MB'tan büyük olamaz");
        }

        String name = sanitizeFilename(file.getOriginalFilename());
        String extension = extensionOf(name);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidUploadException("Yalnızca .csv ve .xlsx dosyaları desteklenir");
        }
        if (extension.equals("xlsx") && !hasZipSignature(file)) {
            throw new InvalidUploadException("Dosya geçerli bir .xlsx dosyası değil");
        }
        return name;
    }

    static String sanitizeFilename(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("\\p{Cntrl}", "").trim();
        if (name.length() > 200) {
            name = name.substring(name.length() - 200); // uzantıyı koru
        }
        return name.isBlank() ? "dosya" : name;
    }

    private static String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /** XLSX bir ZIP arşividir: ilk dört bayt PK\u0003\u0004 olmalıdır. */
    private static boolean hasZipSignature(MultipartFile file) {
        try {
            byte[] head = file.getInputStream().readNBytes(4);
            return head.length == 4
                    && head[0] == 0x50 && head[1] == 0x4B && head[2] == 0x03 && head[3] == 0x04;
        } catch (IOException e) {
            return false;
        }
    }
}
