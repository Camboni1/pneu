package bpr.photo.pneu.eprel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EprelInputReader {

    private final DataFormatter dataFormatter = new DataFormatter();

    public List<EprelInputRow> read(Path inputPath) throws Exception {
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
            int barcodeColumn = findRequiredColumn(headers, "code barre", "code-barre", "barcode", "code barre article");
            int labelCodeColumn = findRequiredColumn(headers, "code etiquette", "code étiquette", "etiquette", "étiquette");

            List<EprelInputRow> rows = new ArrayList<>();
            for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null) {
                    continue;
                }

                String barcode = readCellAsString(row.getCell(barcodeColumn));
                String labelCode = readCellAsString(row.getCell(labelCodeColumn));

                if (barcode.isBlank() && labelCode.isBlank()) {
                    continue;
                }

                rows.add(new EprelInputRow(rowIndex + 1, barcode, labelCode));
            }

            return rows;
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

    private int findRequiredColumn(Map<String, Integer> headers, String... candidates) {
        for (String candidate : candidates) {
            Integer index = headers.get(normalize(candidate));
            if (index != null) {
                return index;
            }
        }

        throw new IllegalStateException("Colonne introuvable. Valeurs attendues: " + String.join(", ", candidates));
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
