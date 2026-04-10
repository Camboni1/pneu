package bpr.photo.pneu.service;

import bpr.photo.pneu.service.impl.ImageLookupServiceImpl;

public interface ImageLookupService {
    ImageLookupServiceImpl.LookupResult findImageUrl(String ean);

}
