package com.example.diets.testing.fakes;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FakeOffClient implements OffClient {

    private final Map<String, OffResponse> byBarcode = new HashMap<>();
    private final Map<String, OffResponse> byText    = new HashMap<>();

    public void seedBarcode(String barcode, OffResponse resp){
        byBarcode.put(barcode, resp);
    }

    public void seedText(String key, OffResponse resp){
        byText.put(key.toLowerCase(Locale.ROOT), resp);
    }

    @Override public OffResponse getByBarcode(String barcode) {
        return byBarcode.getOrDefault(barcode, OffResponse.fail());
    }

    @Override public OffResponse searchByText(String query) {
        return byText.getOrDefault(query.toLowerCase(Locale.ROOT), OffResponse.fail());
    }
}
