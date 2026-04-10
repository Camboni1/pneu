package bpr.photo.pneu.service.impl;

import bpr.photo.pneu.config.AppProperties;
import bpr.photo.pneu.service.ExcelService;
import bpr.photo.pneu.service.ImageLookupService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Service
public class ExcelServiceImpl implements ExcelService {
    private final AppProperties props;
    private final DataFormatter dataFormatter = new DataFormatter();


    public ExcelServiceImpl(AppProperties props) {
        this.props = props;
    }

    public void processFile(Path inputPath, Path outputPath, ImageLookupService imageLookupService) throws Exception {
        try (InputStream is = Files.newInputStream(inputPath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(props.getExcel().getSheetIndex());
            if (sheet == null) {
                throw new IllegalStateException("Feuille introuvable");
            }

            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IllegalStateException("Header introuvable");
            }

            Map<String, Integer> headers = getHeaderMap(headerRow);

            Integer eanCol = headers.get(normalize(props.getExcel().getEanColumnName()));
            if (eanCol == null) {
                throw new IllegalStateException("Colonne EAN introuvable : " + props.getExcel().getEanColumnName());
            }

            int imageCol = ensureColumn(headerRow, headers, props.getExcel().getOutputImageColumnName());
            int statusCol = ensureColumn(headerRow, headers, props.getExcel().getOutputStatusColumnName());

            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String ean = readCellAsString(row.getCell(eanCol));
                if (ean == null || ean.isBlank()) {
                    writeCell(row, statusCol, "EAN_EMPTY");
                    continue;
                }

                ImageLookupService.LookupResult result = imageLookupService.findImageUrl(ean.trim());
                writeCell(row, imageCol, result.imageUrl());
                writeCell(row, statusCol, result.status());
            }

            autosize(sheet, imageCol, statusCol);

            try (OutputStream os = Files.newOutputStream(outputPath)) {
                workbook.write(os);
            }
        }
    }

    private Map<String, Integer> getHeaderMap(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            map.put(normalize(readCellAsString(cell)), cell.getColumnIndex());
        }
        return map;
    }

    private int ensureColumn(Row headerRow, Map<String, Integer> headers, String columnName) {
        String key = normalize(columnName);
        if (headers.containsKey(key)) {
            return headers.get(key);
        }
        int index = headerRow.getLastCellNum() >= 0 ? headerRow.getLastCellNum() : 0;
        Cell cell = headerRow.createCell(index);
        cell.setCellValue(columnName);
        headers.put(key, index);
        return index;
    }

    private void writeCell(Row row, int colIndex, String value) {
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            cell = row.createCell(colIndex);
        }
        cell.setCellValue(value == null ? "" : value);
    }

    private String readCellAsString(Cell cell) {
        if (cell == null) return "";
        return dataFormatter.formatCellValue(cell).trim();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private void autosize(Sheet sheet, int... indexes) {
        for (int index : indexes) {
            sheet.autoSizeColumn(index);
        }
    }
}

