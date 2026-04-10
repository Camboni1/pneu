package bpr.photo.pneu.runner;

import bpr.photo.pneu.service.ExcelService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class BatchRunner implements CommandLineRunner {

    private final ExcelService excelService;

    public BatchRunner(ExcelService excelService) {
        this.excelService = excelService;
    }

    @Override
    public void run(String... args) throws Exception {
        Path inputDir = Path.of("input");
        Path outputDir = Path.of("output");

        Files.createDirectories(inputDir);
        Files.createDirectories(outputDir);

        Path inputFile = inputDir.resolve("input.xlsx");
        Path outputFile = outputDir.resolve("resultat.xlsx");

        if (!Files.exists(inputFile)) {
            throw new IllegalArgumentException(
                    "Fichier introuvable : " + inputFile.toAbsolutePath()
            );
        }

        excelService.processFile(inputFile, outputFile);

        System.out.println("Terminé. Fichier généré : " + outputFile.toAbsolutePath());
    }
}