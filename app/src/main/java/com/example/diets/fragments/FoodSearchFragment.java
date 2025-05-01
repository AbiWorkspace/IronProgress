package com.example.diets.fragments; // Cambio lógico en el paquete

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.example.diets.dialogs.BarcodeScannerDialog;
import com.example.diets.dialogs.GramsInputDialog;
import com.example.diets.network.OkHttpSingleton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FoodSearchFragment extends Fragment {

    private static final String TAG = "FoodSearchFragment";

    private final List<FoodResult> resultObjects = new ArrayList<>();
    private final List<String> foodResults = new ArrayList<>();

    private EditText searchFoodEditText;
    private ListView foodResultsListView;
    private ArrayAdapter<String> adapter;
    private Consumer<FoodResult> onFoodSelected;
    private String currentFilterType = ""; // etiqueta/tag seleccionada
    private String currentFilterTagType = ""; // tagtype_0 como "labels", "ingredients_analysis", etc.

    public void setOnFoodSelected(Consumer<FoodResult> listener) {
        Log.d(TAG, "FoodSearchFragment - setOnFoodSelected: Listener establecido");
        this.onFoodSelected = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_food_search, container, false);
        ImageButton barcodeBtn = view.findViewById(R.id.barcodeButton);

        barcodeBtn.setOnClickListener(v -> {
            BarcodeScannerDialog.newInstance(code -> {
                Log.d(TAG, "Código de barras leído → " + code);
                searchFoodByBarcode(code);
            }).show(getParentFragmentManager(), "BarcodeScannerDialog");
        });
        view.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        Spinner filterSpinner = view.findViewById(R.id.filterSpinner);
        List<String> filterNames = new ArrayList<>(getAvailableFilters().keySet());

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, filterNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = filterNames.get(position);
                String[] filter = getAvailableFilters().get(selected);
                currentFilterTagType = filter[0];
                currentFilterType = filter[1];
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        searchFoodEditText = view.findViewById(R.id.searchFoodEditText);
        foodResultsListView = view.findViewById(R.id.foodResultsListView);
        ImageButton searchButton = view.findViewById(R.id.searchButton);

        adapter = new ArrayAdapter<>(requireContext(),
                R.layout.item_food_result,
                R.id.textFoodResult,
                foodResults);

        foodResultsListView.setAdapter(adapter);

        searchFoodEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchFoodEditText.getText().toString().trim();
            Log.d(TAG, "FoodSearchFragment - onEditorAction: Búsqueda por teclado → " + query);
            if (!query.isEmpty()) searchFood(query);
            return true;
        });

        searchButton.setOnClickListener(v -> {
            String query = searchFoodEditText.getText().toString().trim();
            Log.d(TAG, "FoodSearchFragment - onClick: Búsqueda por botón → " + query);
            if (!query.isEmpty()) searchFood(query);
        });

        foodResultsListView.setOnItemClickListener((parent, v, position, id) -> {
            if (onFoodSelected == null || position >= resultObjects.size()) return;

            FoodResult selected = resultObjects.get(position);
            GramsInputDialog dialog = GramsInputDialog.newInstance(
                    selected,
                    result -> {
                        if (onFoodSelected != null) {
                            onFoodSelected.accept(result);
                        }
                    }
            );
            dialog.show(getParentFragmentManager(), "GramsInputDialog");

        });


        return view;
    }
    private Map<String, String[]> getAvailableFilters() {
        Map<String, String[]> filters = new LinkedHashMap<>();
        filters.put("Todos", new String[]{"", ""});
        filters.put("Vegano", new String[]{"labels", "en:vegan"});
        filters.put("Vegetariano", new String[]{"labels", "en:vegetarian"});
        filters.put("Orgánico", new String[]{"labels", "en:organic"});
        filters.put("Sin gluten", new String[]{"labels", "en:gluten-free"});
        filters.put("Sin azúcar", new String[]{"labels", "en:low-sugar"});
        filters.put("Sin aceite de palma", new String[]{"labels", "en:palm-oil-free"});
        filters.put("NutriScore A", new String[]{"nutrition_grades_tags", "a"});
        filters.put("NOVA Grupo 1", new String[]{"nova_groups_tags", "1"});
        filters.put("EcoScore A", new String[]{"ecoscore_tags", "a"});
        return filters;
    }

    private void searchFoodByBarcode(String barcode) {
        Log.d(TAG, "searchFoodByBarcode: " + barcode);

        foodResults.clear();
        resultObjects.clear();
        adapter.notifyDataSetChanged();

        new Thread(() -> {
            try {
                String url = "https://world.openfoodfacts.org/api/v0/product/" +
                        URLEncoder.encode(barcode, "UTF-8") + ".json";

                OkHttpClient client = OkHttpSingleton.get();
                Response res = client.newCall(
                        new Request.Builder().url(url).build()).execute();

                if (!res.isSuccessful())
                    throw new IOException("HTTP " + res.code());

                JSONObject root = new JSONObject(res.body().string());
                if (root.optInt("status") != 1)
                    throw new Exception("Producto no encontrado");

                JSONObject p = root.getJSONObject("product");
                JSONObject n = p.optJSONObject("nutriments");
                if (n == null) throw new Exception("Sin nutriments");

                String name = p.optString("product_name_es", p.optString("product_name", ""));
                String brand = p.optString("brands", "Sin marca");
                String img = p.optString("image_front_url", "");

                double kcal = n.optDouble("energy-kcal_100g", -1);
                double protein = n.optDouble("proteins_100g", 0);
                double carbs = n.optDouble("carbohydrates_100g", 0);
                double fat = n.optDouble("fat_100g", 0);

                if (name.isEmpty() || kcal <= 0)
                    throw new Exception("Datos insuficientes");

                // 🔥 Crear el objeto alimento
                FoodResult food = new FoodResult(name, kcal, 0, brand, img, protein, carbs, fat);

                // 🔥 intentar leer porción estándar
                String servingStr = p.optString("serving_size", "");
                if (servingStr.endsWith("g") || servingStr.endsWith(" g")) {
                    try {
                        double g = Double.parseDouble(servingStr.replace("g", "").trim());
                        food.portions.add(new FoodResult.Portion("Porción estándar", g));
                    } catch (NumberFormatException ignore) {}
                }

                foodResults.add(name + " (" + kcal + " kcal/100g)");
                resultObjects.add(food);

                if (isAdded()) requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                Log.e(TAG, "❌ Error buscando por código", e);
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "No se encontró información para ese código.",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void searchFood(String query) {
        Log.d(TAG, "searchFood: Iniciando búsqueda → " + query);

        foodResults.clear();
        resultObjects.clear();
        adapter.notifyDataSetChanged();

        new Thread(() -> {
            try {
                String baseUrl = "https://world.openfoodfacts.org/cgi/search.pl";
                String urlStr = baseUrl +
                        "?search_terms=" + URLEncoder.encode(query, "UTF-8") +
                        "&search_simple=1&action=process&json=1&lc=es&page_size=40" +
                        "&fields=product_name_es,product_name,generic_name," +
                        "brands,image_front_url,categories_tags,ingredients_text," +
                        "nutriments.energy-kcal_100g,nutriments.proteins_100g," +
                        "nutriments.carbohydrates_100g,nutriments.fat_100g";

                if (!TextUtils.isEmpty(currentFilterType) && !TextUtils.isEmpty(currentFilterTagType)) {
                    urlStr += "&tagtype_0=" + currentFilterTagType +
                            "&tag_contains_0=contains&tag_0=" + currentFilterType;
                }

                OkHttpClient client = OkHttpSingleton.get();
                Request req = new Request.Builder()
                        .url(urlStr)
                        .header("Accept-Encoding", "gzip")
                        .build();

                Response res;
                try {
                    res = client.newCall(req).execute();
                } catch (java.net.SocketTimeoutException e) {
                    Log.w(TAG, "🌐 Timeout en host global, intentando fallback a mirror ES...");
                    String fallbackUrl = urlStr.replace("world.", "es.");
                    res = client.newCall(req.newBuilder().url(fallbackUrl).build()).execute();
                }

                if (!res.isSuccessful())
                    throw new IOException("HTTP " + res.code());

                JSONObject json = new JSONObject(res.body().string());
                JSONArray products = json.optJSONArray("products");

                if (products == null || products.length() == 0) {
                    if (isAdded()) requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "No se encontraron alimentos para esa búsqueda.",
                                    Toast.LENGTH_SHORT).show());
                    return;
                }

                List<FoodResult> direct = new ArrayList<>();
                List<FoodResult> related = new ArrayList<>();

                String q = query.toLowerCase(Locale.ROOT);

                for (int i = 0; i < products.length(); i++) {
                    JSONObject p = products.getJSONObject(i);

                    String name = p.optString("product_name_es", p.optString("product_name", ""));
                    String nameLower = name.toLowerCase(Locale.ROOT);

                    // Filtrar productos que expresamente son "sin huevo", "egg-free", etc.
                    if (nameLower.contains("sin " + q) || nameLower.contains("without " + q) ||
                            nameLower.contains("egg-free") || nameLower.contains("sin huevo")) {
                        continue;
                    }

                    String brand = p.optString("brands", "Sin marca");
                    String imageUrl = p.optString("image_front_url", "");
                    JSONObject n = p.optJSONObject("nutriments");

                    if (name.isEmpty() || n == null) continue;

                    double kcal = n.optDouble("energy-kcal_100g", -1);
                    double protein = n.optDouble("proteins_100g", 0);
                    double carbs = n.optDouble("carbohydrates_100g", 0);
                    double fat = n.optDouble("fat_100g", 0);
                    if (kcal <= 0) continue;

                    // Evaluar coincidencia en categorías e ingredientes
                    JSONArray catArr = p.optJSONArray("categories_tags");
                    String categories = catArr != null ? catArr.toString().toLowerCase(Locale.ROOT) : "";
                    String ingredientsText = p.optString("ingredients_text", "").toLowerCase(Locale.ROOT);

                    boolean strongMatch = nameLower.contains(q) || nameLower.contains("egg") || nameLower.contains("eggs");
                    boolean relatedMatch = categories.contains(q) || categories.contains("egg") ||
                            ingredientsText.contains(q) || ingredientsText.contains("egg");

                    if (!strongMatch && !relatedMatch) continue;

                    FoodResult food = new FoodResult(name, kcal, 0, brand, imageUrl, protein, carbs, fat);

                    String servingStr = p.optString("serving_size", "");
                    if (servingStr.endsWith("g") || servingStr.endsWith(" g")) {
                        try {
                            double g = Double.parseDouble(servingStr.replace("g", "").trim());
                            food.portions.add(new FoodResult.Portion("Porción estándar", g));
                        } catch (NumberFormatException ignore) {}
                    }

                    if (strongMatch) {
                        direct.add(food);
                    } else {
                        related.add(food);
                    }
                }

                resultObjects.addAll(direct);
                resultObjects.addAll(related);
                for (FoodResult f : direct) {
                    foodResults.add(f.name + " (" + f.kcalPer100g + " kcal/100g)");
                }
                for (FoodResult f : related) {
                    foodResults.add(f.name + " (" + f.kcalPer100g + " kcal/100g)");
                }

                if (isAdded()) requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());


            } catch (Exception e) {
                Log.e(TAG, "❌ Error buscando alimento", e);
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(),
                                "Error de conexión. Intenta de nuevo.",
                                Toast.LENGTH_SHORT).show());
            }
        }).start();
    }


    private void parseAndPopulate(String jsonStr) throws Exception {
        JSONObject json = new JSONObject(jsonStr);
        JSONArray products = json.optJSONArray("products");

        if (products == null || products.length() == 0) {
            if (isAdded()) requireActivity().runOnUiThread(() ->
                    Toast.makeText(getContext(),
                            "No se encontraron alimentos para esa búsqueda.",
                            Toast.LENGTH_SHORT).show());
            return;
        }

        for (int i = 0; i < products.length(); i++) {
            JSONObject p = products.getJSONObject(i);
            String name = p.optString("product_name_es", p.optString("product_name", ""));
            String brand = p.optString("brands", "Sin marca");
            String imageUrl = p.optString("image_front_url", "");

            JSONObject n = p.optJSONObject("nutriments");

            if (name.isEmpty() || n == null) {
                Log.w(TAG, "parseAndPopulate: Producto sin nombre o sin 'nutriments', omitido.");
                continue;
            }

            double kcal = n.optDouble("energy-kcal_100g", -1);
            double protein = n.optDouble("proteins_100g", 0);
            double carbs = n.optDouble("carbohydrates_100g", 0);
            double fat = n.optDouble("fat_100g", 0);

            if (kcal <= 0) {
                Log.w(TAG, "parseAndPopulate: Producto con kcal <= 0, omitido → " + name);
                continue;
            }

            // 🔥 Crear el objeto alimento
            FoodResult food = new FoodResult(name, kcal, 0, brand, imageUrl, protein, carbs, fat);

            // 🔥 intentar leer porción estándar
            String servingStr = p.optString("serving_size", "");
            if (servingStr.endsWith("g") || servingStr.endsWith(" g")) {
                try {
                    double g = Double.parseDouble(servingStr.replace("g", "").trim());
                    food.portions.add(new FoodResult.Portion("Porción estándar", g));
                } catch (NumberFormatException ignore) {}
            }

            foodResults.add(name + " (" + kcal + " kcal/100g)");
            resultObjects.add(food);
        }

        if (isAdded()) requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
    }

    public static class FoodResult implements java.io.Serializable {

        public static class Portion implements java.io.Serializable {
            public final String label;
            public final double grams;

            public Portion(String label, double grams) {
                this.label = label;
                this.grams = grams;
            }
        }

        public String name;
        public double kcalPer100g;
        public double grams;
        public String brand;
        public String imageUrl;
        public double protein;
        public double carbs;
        public double fat;
        public List<Portion> portions = new ArrayList<>();

        public FoodResult() {
            // Constructor vacío para asignaciones posteriores
        }

        public FoodResult(String name,
                          double kcalPer100g,
                          double grams,
                          String brand,
                          String imageUrl,
                          double protein,
                          double carbs,
                          double fat) {
            this.name = name;
            this.kcalPer100g = kcalPer100g;
            this.grams = grams;
            this.brand = brand;
            this.imageUrl = imageUrl;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
        }
    }


}
