package bpr.photo.pneu.service;

import java.nio.file.Path;
import java.util.List;

public interface ImageLookupService {

    LookupResult findImages(String ean);

    void downloadImage(String imageUrl, Path outputPath) throws Exception;

    record LookupResult(
            String ean,
            String imageUrl,
            List<String> secondaryImages,
            String status
    ) {}
}