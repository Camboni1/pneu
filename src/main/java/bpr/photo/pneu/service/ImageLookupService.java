package bpr.photo.pneu.service;

public interface ImageLookupService {

    LookupResult findImageUrl(String ean);

    record LookupResult(String ean, String imageUrl, String status) {}
}