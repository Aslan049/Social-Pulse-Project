package com.socialpulse.backend.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.stream.Stream;

/**
 * Ham hücre değerlerini tarihe ve sayıya çevirir. Geçersiz değerde IllegalArgumentException
 * (Türkçe, kullanıcıya gösterilebilir mesajla) fırlatır.
 */
public final class CellParser {

    // Türkçe bağlamda gün/ay/yıl sırası varsayılır; AA/GG/YYYY desteklenmez (belirsizlik)
    private static final List<DateTimeFormatter> DATE_FORMATS = Stream.of(
                    "uuuu-MM-dd", "d.M.uuuu", "d/M/uuuu", "uuuu/M/d")
            .map(pattern -> DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT))
            .toList();

    private static final BigDecimal MAX_ABS = new BigDecimal("1E16");

    private CellParser() {
    }

    public static LocalDate toDate(Object cell) {
        if (cell instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (cell instanceof LocalDate date) {
            return date;
        }
        if (cell instanceof String text) {
            String t = text.trim();
            if (t.length() > 10 && (t.charAt(10) == ' ' || t.charAt(10) == 'T')) {
                t = t.substring(0, 10); // "2026-01-05 00:00:00" gibi tarih-saat metinleri
            }
            for (DateTimeFormatter format : DATE_FORMATS) {
                try {
                    return LocalDate.parse(t, format);
                } catch (DateTimeParseException ignored) {
                    // sıradaki biçimi dene
                }
            }
            throw new IllegalArgumentException("Geçersiz tarih: " + shorten(text));
        }
        throw new IllegalArgumentException(cell == null ? "Tarih boş" : "Geçersiz tarih");
    }

    /** Boş veya "-" ise null (değer yok), geçersizse IllegalArgumentException. */
    public static BigDecimal toNumber(Object cell) {
        if (cell == null) {
            return null;
        }
        if (cell instanceof Number number) {
            double d = number.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) {
                throw new IllegalArgumentException("Geçersiz sayı");
            }
            return checkRange(BigDecimal.valueOf(d));
        }
        if (cell instanceof String text) {
            String t = text.trim();
            if (t.isEmpty() || t.equals("-") || t.equals("–") || t.equals("—")) {
                return null;
            }
            return checkRange(parseNumeric(t));
        }
        throw new IllegalArgumentException("Sayı bekleniyordu");
    }

    private static BigDecimal parseNumeric(String original) {
        String s = original.replace(" ", "").replace("\u00A0", "");
        boolean negative = false;
        if (s.startsWith("-")) {
            negative = true;
            s = s.substring(1);
        } else if (s.startsWith("+")) {
            s = s.substring(1);
        }
        if (!s.matches("[0-9.,]+")) {
            throw new IllegalArgumentException("Geçersiz sayı: " + shorten(original));
        }

        boolean hasDot = s.indexOf('.') >= 0;
        boolean hasComma = s.indexOf(',') >= 0;
        String normalized;
        if (hasDot && hasComma) {
            // Son görülen ayraç ondalık ayraçtır
            if (s.lastIndexOf(',') > s.lastIndexOf('.')) {
                normalized = s.replace(".", "").replace(',', '.');
            } else {
                normalized = s.replace(",", "");
            }
        } else if (hasComma) {
            normalized = resolveSingleSeparator(s, ',', original);
        } else if (hasDot) {
            normalized = resolveSingleSeparator(s, '.', original);
        } else {
            normalized = s;
        }

        try {
            BigDecimal value = new BigDecimal(normalized);
            return negative ? value.negate() : value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Geçersiz sayı: " + shorten(original));
        }
    }

    /**
     * Tek tür ayraç varsa: 3'lü gruplar halindeyse binlik ayraç ("1.234", "1,234,567"),
     * tek ayraçsa ondalık ayraç ("12,5") kabul edilir. Metrikler sayım olduğu için "1.234" 1234 sayılır.
     */
    private static String resolveSingleSeparator(String s, char separator, String original) {
        String escaped = java.util.regex.Pattern.quote(String.valueOf(separator));
        if (s.matches("\\d{1,3}(" + escaped + "\\d{3})+")) {
            return s.replace(String.valueOf(separator), "");
        }
        long count = s.chars().filter(c -> c == separator).count();
        if (count == 1) {
            return s.replace(separator, '.');
        }
        throw new IllegalArgumentException("Geçersiz sayı: " + shorten(original));
    }

    private static BigDecimal checkRange(BigDecimal value) {
        if (value.abs().compareTo(MAX_ABS) >= 0) {
            throw new IllegalArgumentException("Değer çok büyük");
        }
        return value;
    }

    private static String shorten(String value) {
        String v = value.trim();
        return v.length() <= 30 ? v : v.substring(0, 30) + "…";
    }
}
