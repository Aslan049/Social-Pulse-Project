package com.socialpulse.backend.ingestion;

import com.socialpulse.backend.exception.InvalidUploadException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * XLSX okuyucu: her sayfa bir RawTable olur. Tarih hücreleri LocalDateTime, sayılar Double,
 * metinler String olarak döner. Formül hücrelerinde saklanmış sonuç kullanılır.
 */
@Component
public class XlsxTabularReader implements TabularReader {

    @Override
    public boolean supports(String filename) {
        return filename.toLowerCase(Locale.ROOT).endsWith(".xlsx");
    }

    @Override
    public List<RawTable> read(InputStream in) {
        try (Workbook workbook = WorkbookFactory.create(in)) {
            List<RawTable> tables = new ArrayList<>();
            for (Sheet sheet : workbook) {
                List<List<Object>> rows = new ArrayList<>();
                for (Row row : sheet) {
                    List<Object> cells = readRow(row);
                    if (cells.isEmpty()) {
                        continue;
                    }
                    rows.add(cells);
                    if (rows.size() > MAX_ROWS_PER_TABLE) {
                        throw new InvalidUploadException("Sayfada çok fazla satır var: " + sheet.getSheetName());
                    }
                }
                tables.add(new RawTable(sheet.getSheetName(), rows));
            }
            return tables;
        } catch (InvalidUploadException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidUploadException("Dosya geçerli bir .xlsx dosyası değil veya okunamadı");
        }
    }

    private List<Object> readRow(Row row) {
        int end = row.getLastCellNum(); // hücre yoksa -1
        List<Object> cells = new ArrayList<>(Math.max(end, 0));
        for (int i = 0; i < end; i++) {
            cells.add(readCell(row.getCell(i)));
        }
        int last = cells.size() - 1;
        while (last >= 0 && cells.get(last) == null) {
            cells.remove(last--);
        }
        return cells;
    }

    private Object readCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType() == CellType.FORMULA
                ? cell.getCachedFormulaResultType()
                : cell.getCellType();
        switch (type) {
            case STRING: {
                String value = cell.getStringCellValue().trim();
                return value.isEmpty() ? null : value;
            }
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue();
                }
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            default:
                return null;
        }
    }
}
