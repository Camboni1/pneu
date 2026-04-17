package bpr.photo.pneu.eprel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EprelLabelDownloaderMain {

    public static void main(String[] args) throws Exception {
        Path inputFile = Path.of("input", "input.xlsx");
        Path outputDir = Path.of("output", "eprel-labels");
        Path outputExcel = Path.of("output", "eprel-resultat.xlsx");
        int maxRowsToProcess = parseMaxRows(args);

        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException("Fichier introuvable : " + inputFile.toAbsolutePath());
        }

        Files.createDirectories(outputDir);

        EprelInputReader inputReader = new EprelInputReader();
        EprelLabelDownloader downloader = new EprelLabelDownloader();
        EprelOutputExcelWriter outputExcelWriter = new EprelOutputExcelWriter();

        List<EprelInputRow> rows = inputReader.read(inputFile);
        Map<Integer, String> imageNamesByRow = new HashMap<>();
        Map<Integer, String> imageUrlsByRow = new HashMap<>();
        int downloadedCount = 0;
        int skippedCount = 0;
        int processedCount = 0;

        for (EprelInputRow row : rows) {
            if (maxRowsToProcess > 0 && processedCount >= maxRowsToProcess) {
                break;
            }

            String barcode = row.barcode().trim();
            String labelCode = row.labelCode().trim();
            processedCount++;

            if (barcode.isBlank() || labelCode.isBlank()) {
                imageNamesByRow.put(row.rowNumber(), "");
                imageUrlsByRow.put(row.rowNumber(), "");
                skippedCount++;
                System.out.println("Ligne " + row.rowNumber() + " ignoree : code barre ou code etiquette vide");
                continue;
            }

            String imageName = sanitizeFileName(barcode) + ".png";
            Path outputFile = outputDir.resolve(imageName);

            try {
                String imageUrl = downloader.downloadLabel(labelCode, outputFile);
                imageNamesByRow.put(row.rowNumber(), imageName);
                imageUrlsByRow.put(row.rowNumber(), imageUrl);
                downloadedCount++;
                System.out.println("OK ligne " + row.rowNumber() + " -> " + outputFile.toAbsolutePath());
            } catch (Exception exception) {
                imageNamesByRow.put(row.rowNumber(), "");
                imageUrlsByRow.put(row.rowNumber(), "");
                System.err.println("ERREUR ligne " + row.rowNumber() + " (code etiquette " + labelCode + ") : " + exception.getMessage());
            }
        }

        outputExcelWriter.write(inputFile, outputExcel, imageNamesByRow, imageUrlsByRow);

        System.out.println();
        System.out.println("Traitement termine");
        System.out.println("Lignes traitees      : " + processedCount);
        System.out.println("Fichiers telecharges : " + downloadedCount);
        System.out.println("Lignes ignorees      : " + skippedCount);
        System.out.println("Dossier de sortie    : " + outputDir.toAbsolutePath());
        System.out.println("Excel de sortie      : " + outputExcel.toAbsolutePath());
    }

    private static int parseMaxRows(String[] args) {
        for (String arg : args) {
            if (arg != null && arg.startsWith("--limit=")) {
                return Integer.parseInt(arg.substring("--limit=".length()));
            }
        }
        return 0;
    }

    private static String sanitizeFileName(String value) {
        return value.replaceAll("[\\\\/:*?\"<>|\\s]+", "_");
    }
}
