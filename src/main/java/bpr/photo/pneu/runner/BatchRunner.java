package bpr.photo.pneu.runner;

import bpr.photo.pneu.service.ExcelService;
import bpr.photo.pneu.service.ImageLookupService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class BatchRunner implements CommandLineRunner {

    private final ExcelService excelService;
    private final ImageLookupService imageLookupService;

    public BatchRunner(ExcelService excelService, ImageLookupService imageLookupService) {
        this.excelService = excelService;
        this.imageLookupService = imageLookupService;
    }

    @Override
    public void run(String... args) throws Exception {
        Map<String, String> params = parseArgs(args);

        String input = params.get("input");
        String output = params.get("output");

        if (input == null || output == null) {
            throw new IllegalArgumentException(
                    "Utilisation: java -jar app.jar --input=/chemin/in.xlsx --output=/chemin/out.xlsx"
            );
        }

        excelService.processFile(Path.of(input), Path.of(output), imageLookupService);
        System.out.println("Terminé. Fichier généré : " + output);
    }

    private Map<String, String> parseArgs(String[] args) {
        Map<String, String> map = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--") && arg.contains("=")) {
                String[] parts = arg.substring(2).split("=", 2);
                map.put(parts[0], parts[1]);
            }
        }
        return map;
    }
}