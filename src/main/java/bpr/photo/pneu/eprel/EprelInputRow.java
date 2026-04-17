package bpr.photo.pneu.eprel;

public record EprelInputRow(
        int rowNumber,
        String barcode,
        String labelCode
) {
}
