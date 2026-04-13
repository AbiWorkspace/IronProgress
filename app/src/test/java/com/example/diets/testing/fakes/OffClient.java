package com.example.diets.testing.fakes;

public interface OffClient {
    OffResponse getByBarcode(String barcode);
    OffResponse searchByText(String query);
}
