package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.exception.InvalidUploadException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class XlsxTabularReaderTest {

    private final XlsxTabularReader reader = new XlsxTabularReader();

    private byte[] sampleWorkbook() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Ziyaretler");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("Tarih");
            header.createCell(1).setCellValue("Değer");

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.getCreationHelper().createDataFormat().getFormat("yyyy-mm-dd"));

            Row data = sheet.createRow(1);
            Cell dateCell = data.createCell(0);
            dateCell.setCellValue(Date.from(LocalDate.of(2026, 1, 5).atStartOfDay(ZoneId.systemDefault()).toInstant()));
            dateCell.setCellStyle(dateStyle);
            data.createCell(1).setCellValue(1883);

            sheet.createRow(2); // tamamen boş satır

            Row textRow = sheet.createRow(3);
            textRow.createCell(0).setCellValue("metin");

            workbook.createSheet("Boş");
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void sheetsAreReadAsSeparateTables() throws Exception {
        List<RawTable> tables = reader.read(new ByteArrayInputStream(sampleWorkbook()));

        assertEquals(2, tables.size());
        assertEquals("Ziyaretler", tables.get(0).name());
        assertEquals("Boş", tables.get(1).name());
        assertEquals(0, tables.get(1).rowCount());
    }

    @Test
    void cellTypesAreMappedAndBlankRowsSkipped() throws Exception {
        RawTable table = reader.read(new ByteArrayInputStream(sampleWorkbook())).get(0);

        assertEquals(3, table.rowCount(), "Boş satır atlanmalı");
        assertEquals(List.of("Tarih", "Değer"), table.rows().get(0));
        assertEquals(LocalDate.of(2026, 1, 5).atStartOfDay(), table.rows().get(1).get(0));
        assertEquals(1883.0, ((Number) table.rows().get(1).get(1)).doubleValue());
        assertEquals(List.of("metin"), table.rows().get(2), "Satır sonundaki boş hücreler kırpılmalı");
        assertTrue(table.rows().get(1).get(0) instanceof LocalDateTime);
    }

    @Test
    void headerPreviewAndColumnCount() throws Exception {
        RawTable table = reader.read(new ByteArrayInputStream(sampleWorkbook())).get(0);

        assertEquals(List.of("Tarih", "Değer"), table.headerPreview());
        assertEquals(2, table.columnCount());
    }

    @Test
    void notAnExcelFile_isRejected() {
        assertThrows(InvalidUploadException.class,
                () -> reader.read(new ByteArrayInputStream("excel degil".getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    void supports_onlyXlsx() {
        assertTrue(reader.supports("Rapor.XLSX"));
        assertFalse(reader.supports("rapor.csv"));
    }
}
