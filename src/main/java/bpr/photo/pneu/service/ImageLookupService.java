package bpr.photo.pneu.service;
import java.nio.file.Path;

public interface ImageLookupService {

    LookupResult findImageUrl(String ean);
    void downloadImage(String imageUrl, Path outputPath) throws Exception;
    record LookupResult(String ean, String imageUrl, String status) {}
}