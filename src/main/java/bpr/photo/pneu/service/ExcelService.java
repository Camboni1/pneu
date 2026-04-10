package bpr.photo.pneu.service;

import java.nio.file.Path;

public interface ExcelService {
    void processFile(Path inputPath, Path outputPath, ImageLookupService imageLookupService) throws Exception;


}
