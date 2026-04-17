package bpr.photo.pneu.eprel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class EprelOutputExcelWriter {

    private final DataFormatter dataFormatter = new DataFormatter();

    public void write(
            Path inputPath,
            Path outputPath,
            Map<Integer, String> imageNamesByRow,
            Map<Integer, String> imageUrlsByRow
    ) throws Exception {
        try (InputStream inputStream = Files.newInputStream(inputPath);
             Workbook workbook = new XSSFWorkbook(inputStream)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new IllegalStateException("Feuille introuvable dans " + inputPath.toAbsolutePath());
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalStateException("Ligne d'en-tete introuvable dans " + inputPath.toAbsolutePath());
            }

            Map<String, Integer> headers = getHeaderMap(headerRow);
            int imageNameColumn = ensureColumn(headerRow, headers, "nom image");
            int imageUrlColumn = ensureColumn(headerRow, headers, "url image");

            for (Map.Entry<Integer, String> entry : imageNamesByRow.entrySet()) {
                int rowNumber = entry.getKey();
                int sheetRowIndex = rowNumber - 1;
                Row row = sheet.getRow(sheetRowIndex);
                if (row == null) {
                    continue;
                }

                writeCell(row, imageNameColumn, entry.getValue());
                writeCell(row, imageUrlColumn, imageUrlsByRow.getOrDefault(rowNumber, ""));
            }

            sheet.autoSizeColumn(imageNameColumn);
            sheet.autoSizeColumn(imageUrlColumn);
            Files.createDirectories(outputPath.getParent());

            try (OutputStream outputStream = Files.newOutputStream(outputPath)) {
                workbook.write(outputStream);
            }
        }
    }

    private Map<String, Integer> getHeaderMap(Row headerRow) {
        Map<String, Integer> headers = new HashMap<>();
        for (Cell cell : headerRow) {
            String value = normalize(readCellAsString(cell));
            if (!value.isBlank()) {
                headers.put(value, cell.getColumnIndex());
            }
        }
        return headers;
    }

    private int ensureColumn(Row headerRow, Map<String, Integer> headers, String columnName) {
        String key = normalize(columnName);
        Integer existingIndex = headers.get(key);
        if (existingIndex != null) {
            return existingIndex;
        }

        int index = headerRow.getLastCellNum() >= 0 ? headerRow.getLastCellNum() : 0;
        Cell cell = headerRow.createCell(index);
        cell.setCellValue(columnName);
        headers.put(key, index);
        return index;
    }

    private void writeCell(Row row, int columnIndex, String value) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            cell = row.createCell(columnIndex);
        }
        cell.setCellValue(value == null ? "" : value);
    }

    private String readCellAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        return dataFormatter.formatCellValue(cell).trim();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }

        return stripAccents(value)
                .trim()
                .toLowerCase()
                .replaceAll("\\s+", " ");
    }

    private String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
