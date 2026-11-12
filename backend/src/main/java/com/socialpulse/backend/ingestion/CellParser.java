package com.socialpulse.backend.ingestion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Pattern YEAR_THEN_MONTH = Pattern.compile("^(\\d{4})[-/.](\\d{1,2})$");
    private static final Pattern MONTH_THEN_YEAR = Pattern.compile("^(\\d{1,2})[-/.](\\d{4})$");
    private static final Pattern NAME_THEN_YEAR = Pattern.compile("^(\\p{L}+)\\.?\\s*[-/]?\\s*(\\d{4})$");
    private static final Pattern YEAR_THEN_NAME = Pattern.compile("^(\\d{4})\\s*[-/]?\\s*(\\p{L}+)\\.?$");

    /** Ay adları (Türkçe ve İngilizce, tam ve kısa), MetricAliases.normalize ile normalize edilmiş anahtarlarla. */
    private static final Map<String, Integer> MONTH_NAMES = new HashMap<>();

    static {
        String[][] names = {
                {"ocak", "oca", "january", "jan"},
                {"subat", "sub", "february", "feb"},
                {"mart", "mar", "march"},
                {"nisan", "nis", "april", "apr"},
                {"mayis", "may"},
                {"haziran", "haz", "june", "jun"},
                {"temmuz", "tem", "july", "jul"},
                {"agustos", "agu", "august", "aug"},
                {"eylul", "eyl", "september", "sep", "sept"},
                {"ekim", "eki", "october", "oct"},
                {"kasim", "kas", "november", "nov"},
                {"aralik", "ara", "december", "dec"}
        };
        for (int i = 0; i < names.length; i++) {
            for (String name : names[i]) {
                MONTH_NAMES.put(name, i + 1);
            }
        }
    }

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

    /**
     * Bir ayı, ayın ilk günü olarak döner. Excel tarih hücrelerini, "2025-10", "10.2025", "10/2025",
     * "Ekim 2025", "Oct 2025" gibi metinleri ve tam tarihleri kabul eder.
     */
    public static LocalDate toMonth(Object cell) {
        if (cell instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate().withDayOfMonth(1);
        }
        if (cell instanceof LocalDate date) {
            return date.withDayOfMonth(1);
        }
        if (cell instanceof String text) {
            String t = text.trim();
            Matcher m = YEAR_THEN_MONTH.matcher(t);
            if (m.matches()) {
                return monthOf(Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), text);
            }
            m = MONTH_THEN_YEAR.matcher(t);
            if (m.matches()) {
                return monthOf(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)), text);
            }
            m = NAME_THEN_YEAR.matcher(t);
            if (m.matches()) {
                Integer month = MONTH_NAMES.get(MetricAliases.normalize(m.group(1)));
                if (month != null) {
                    return monthOf(Integer.parseInt(m.group(2)), month, text);
                }
            }
            m = YEAR_THEN_NAME.matcher(t);
            if (m.matches()) {
                Integer month = MONTH_NAMES.get(MetricAliases.normalize(m.group(2)));
                if (month != null) {
                    return monthOf(Integer.parseInt(m.group(1)), month, text);
                }
            }
            try {
                return toDate(t).withDayOfMonth(1);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Geçersiz ay: " + shorten(text));
            }
        }
        throw new IllegalArgumentException(cell == null ? "Ay boş" : "Geçersiz ay");
    }

    private static LocalDate monthOf(int year, int month, String original) {
        if (month < 1 || month > 12 || year < 1900 || year > 2100) {
            throw new IllegalArgumentException("Geçersiz ay: " + shorten(original));
        }
        return LocalDate.of(year, month, 1);
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
