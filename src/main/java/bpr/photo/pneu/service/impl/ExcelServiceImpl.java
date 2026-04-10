package bpr.photo.pneu.service.impl;

import bpr.photo.pneu.config.AppProperties;
import bpr.photo.pneu.service.ExcelService;
import bpr.photo.pneu.service.ImageLookupService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

@Service
public class ExcelServiceImpl implements ExcelService {

    private final AppProperties props;
    private final ImageLookupService imageLookupService;
    private final DataFormatter dataFormatter = new DataFormatter();
    private static final Logger log = LoggerFactory.getLogger(ExcelServiceImpl.class);

    public ExcelServiceImpl(AppProperties props, ImageLookupService imageLookupService) {
        this.props = props;
        this.imageLookupService = imageLookupService;
    }

    @Override
    public void processFile(Path inputPath, Path outputPath) throws Exception {
        try (InputStream is = Files.newInputStream(inputPath);
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(props.getExcel().getSheetIndex());
            if (sheet == null) {
                throw new IllegalStateException("Feuille introuvable");
            }

            log.info("Début du traitement du fichier: {}", inputPath);

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
            int secondaryImageCol = ensureColumn(headerRow, headers, "secondary_images");
            int statusCol = ensureColumn(headerRow, headers, props.getExcel().getOutputStatusColumnName());

            Path imagesDir = Path.of("output", "images");
            Files.createDirectories(imagesDir);

            for (int i = headerRow.getRowNum() + 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }

                String ean = readCellAsString(row.getCell(eanCol));
                if (ean.isBlank()) {
                    writeCell(row, statusCol, "EAN_EMPTY");
                    continue;
                }

                String safeEan = sanitizeFileName(ean.trim());
                log.info("EAN lu: {}", ean);

                ImageLookupService.LookupResult result = imageLookupService.findImages(ean.trim());
                String finalStatus = result.status();

                if ("OK".equals(result.status())) {
                    log.info("Image principale trouvée pour {} : {}", ean, result.imageUrl());

                    if (!result.imageUrl().isBlank()) {
                        try {
                            Path primaryPath = imagesDir.resolve(safeEan + ".jpg");
                            imageLookupService.downloadImage(result.imageUrl(), primaryPath);
                            log.info("Image principale téléchargée pour {} : {}", ean, primaryPath.toAbsolutePath());
                        } catch (Exception e) {
                            log.error("Téléchargement impossible pour l'image principale de {}", ean, e);
                            finalStatus = "DOWNLOAD_ERROR";
                        }
                    }

                    for (int j = 0; j < result.secondaryImages().size(); j++) {
                        String secondaryUrl = result.secondaryImages().get(j);

                        try {
                            Path secondaryPath = imagesDir.resolve(safeEan + "_secondary_" + (j + 1) + ".jpg");
                            imageLookupService.downloadImage(secondaryUrl, secondaryPath);
                            log.info("Image secondaire téléchargée pour {} : {}", ean, secondaryPath.toAbsolutePath());
                        } catch (Exception e) {
                            log.error("Téléchargement impossible pour une image secondaire de {}", ean, e);
                            finalStatus = "DOWNLOAD_ERROR";
                        }
                    }
                } else {
                    log.warn("Aucune image trouvée pour {}", ean);
                }

                writeCell(row, imageCol, result.imageUrl());

                StringJoiner secondaryJoiner = new StringJoiner(" | ");
                for (String secondary : result.secondaryImages()) {
                    secondaryJoiner.add(secondary);
                }
                writeCell(row, secondaryImageCol, secondaryJoiner.toString());

                writeCell(row, statusCol, finalStatus);
            }

            autosize(sheet, imageCol, secondaryImageCol, statusCol);

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

    private String sanitizeFileName(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }

    private void autosize(Sheet sheet, int... indexes) {
        for (int index : indexes) {
            sheet.autoSizeColumn(index);
        }
    }
}