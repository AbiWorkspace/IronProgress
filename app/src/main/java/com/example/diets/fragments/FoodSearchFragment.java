package com.example.diets.fragments; // Cambio lógico en el paquete

import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
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
import com.example.diets.adapters.SearchResultAdapter;
import com.example.diets.dialogs.BarcodeScannerDialog;
import com.example.diets.dialogs.GramsInputDialog;
import com.example.diets.dialogs.RecipeDialog;
import com.example.diets.network.OkHttpSingleton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

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


    public boolean isRecipe = false;

    private EditText searchFoodEditText;
    private ListView foodResultsListView;
    private final List<FoodResult> resultObjects = new ArrayList<>();
    private SearchResultAdapter adapter;

    private Consumer<FoodResult> onFoodSelected;
    private String currentFilterType = ""; // etiqueta/tag seleccionada
    private String currentFilterTagType = ""; // tagtype_0 como "labels", "ingredients_analysis", etc.
    private final List<FoodResult> productResults = new ArrayList<>();
    private final List<FoodResult> recipeResults = new ArrayList<>();

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

        adapter = new SearchResultAdapter(requireContext(), resultObjects);
        foodResultsListView.setAdapter(adapter);


        foodResultsListView.setAdapter(adapter);

        searchFoodEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {          // ⬅️ solo esta acción
                lanzarBusqueda();
                return true;
            }
            return false;
        });
        searchButton.setOnClickListener(v -> lanzarBusqueda());

        foodResultsListView.setOnItemClickListener((parent, vw, pos, id) -> {
            FoodResult sel = resultObjects.get(pos);

            /* ===================================================== */
            /* ================   CASO  RECETA   ==================== */
            /* ===================================================== */
            if (sel.isRecipe()) {

                /* ①  Preparamos el diálogo y le conectamos el listener  */
                RecipeDialog dialog = RecipeDialog.newInstance(sel);
                dialog.setOnGramsConfirmedListener(result -> {
                    Log.d(TAG, "[FoodSearch] ✅ onGramsConfirmed (recipe) → " + result.name);
                    if (onFoodSelected != null) onFoodSelected.accept(result);   // <-- redirige al Dashboard
                });

        /* ②  Mostrar directamente si ya tenemos todos los datos
              o esperar a que se completen los detalles de TheMealDB */
                Runnable showDialog = () ->
                        dialog.show(getParentFragmentManager(), "RecipeDialog");

                if ("Spoonacular".equals(sel.brand) || sel.hasFullDetails()) {
                    showDialog.run();
                }

                return;   // 👉 terminamos (no seguimos con producto)
            }

            /* ===================================================== */
            /* ================   CASO  PRODUCTO   ================== */
            /* ===================================================== */
            GramsInputDialog.newInstance(sel, result -> {
                // ✅ Detectar si es receta por ingredientes
                if (result.ingredients != null && !result.ingredients.isEmpty()) {
                    Log.d(TAG, "✅ Es receta, abriendo showRecipeDialog");

                    Fragment parentFragment = getParentFragment();
                    if (parentFragment instanceof DashboardFragment) {
                        ((DashboardFragment) parentFragment).showRecipeDialog(result);
                    } else {
                        Log.w(TAG, "⚠️ Parent fragment no es DashboardFragment");
                    }

                    return; // ⛔️ No continuar con flujo normal
                }

                // ✅ Si no es receta, se continúa con flujo normal
                if (onFoodSelected != null) {
                    onFoodSelected.accept(result);
                }
            }).show(getParentFragmentManager(), "GramsInputDialog");


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



    private void lanzarBusqueda() {
        String q = searchFoodEditText.getText().toString().trim();
        if (!q.isEmpty()) searchFood(q);
    }
    private void searchFoodByBarcode(String barcode) {
        Log.d(TAG, "searchFoodByBarcode: " + barcode);

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

                FoodResult food = new FoodResult(name, kcal, 0, brand, img, protein, carbs, fat);

                String servingStr = p.optString("serving_size", "");
                if (servingStr.endsWith("g") || servingStr.endsWith(" g")) {
                    try {
                        double g = Double.parseDouble(servingStr.replace("g", "").trim());
                        food.portions.add(new FoodResult.Portion("Porción estándar", g));
                    } catch (NumberFormatException ignore) {}
                }

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
        Log.d(TAG, "🔍 searchFood → " + query);

        // limpiar listas y UI
        resultObjects.clear();
        adapter.notifyDataSetChanged();
        fetchProductsFromOpenFoodFacts(query);
        /* 1) RECETAS (Spoonacular) */
        fetchRecipesFromSpoonacular(query);



    }
    private void fetchRecipesFromSpoonacular(String query) {
        new Thread(() -> {
            try {
                final String apiKey = "04e23d7cf29c4324bbba0799fdbfba61";
                String url = "https://api.spoonacular.com/recipes/complexSearch" +
                        "?query=" + URLEncoder.encode(query, "UTF-8") +
                        "&number=30" +
                        "&addRecipeNutrition=true" +   // 👈 ESTO incluye macros
                        "&instructionsRequired=true" +
                        "&fillIngredients=true" +
                        "&apiKey=" + apiKey;

                Log.d(TAG, "🌐 Spoonacular RECIPES → " + url);

                OkHttpClient client = OkHttpSingleton.get();
                Response rsp = client.newCall(new Request.Builder().url(url).build()).execute();
                Log.d(TAG, "↩️ HTTP " + rsp.code());

                if (!rsp.isSuccessful()) throw new IOException("HTTP " + rsp.code());

                JSONArray results = new JSONObject(rsp.body().string()).optJSONArray("results");
                Log.d(TAG, "🍽 recetas recibidas: " + (results != null ? results.length() : 0));
                if (results == null || results.length() == 0) return;

                for (int i = 0; i < results.length(); i++) {
                    JSONObject r = results.getJSONObject(i);

                    String id = r.optString("id", "");
                    String name = r.optString("title", "Receta sin nombre");
                    String img = r.optString("image", "");
                    String instr = r.optString("instructions", "");

                    if (instr.isEmpty()) {
                        JSONArray analyzed = r.optJSONArray("analyzedInstructions");
                        if (analyzed != null && analyzed.length() > 0) {
                            JSONArray steps = analyzed.getJSONObject(0).optJSONArray("steps");
                            if (steps != null) {
                                StringBuilder sb = new StringBuilder();
                                for (int s = 0; s < steps.length(); s++) {
                                    JSONObject st = steps.getJSONObject(s);
                                    sb.append(st.optInt("number"))
                                            .append(". ")
                                            .append(st.optString("step"))
                                            .append("\n");
                                }
                                instr = sb.toString().trim();
                            }
                        }
                    }
                    if (!instr.isEmpty()) {
                        instr = Html.fromHtml(instr, Html.FROM_HTML_MODE_LEGACY).toString();
                    }

                    // 🔸 Ingredientes
                    List<String> ings = new ArrayList<>();
                    JSONArray extIng = r.optJSONArray("extendedIngredients");
                    if (extIng != null) {
                        for (int j = 0; j < extIng.length(); j++) {
                            String ing = extIng.getJSONObject(j).optString("original", "");
                            if (!ing.isEmpty()) ings.add(ing);
                        }
                    }

                    // 🔸 Macros
                    double kcal = -1, protein = -1, carbs = -1, fat = -1;
                    JSONObject nutrition = r.optJSONObject("nutrition");
                    if (nutrition != null) {
                        JSONArray nutrients = nutrition.optJSONArray("nutrients");
                        if (nutrients != null) {
                            for (int n = 0; n < nutrients.length(); n++) {
                                JSONObject nut = nutrients.getJSONObject(n);
                                String nutName = nut.optString("name", "").toLowerCase(Locale.ROOT);
                                double amount = nut.optDouble("amount", -1);
                                String unit = nut.optString("unit", "").toLowerCase(Locale.ROOT);

                                if (amount < 0 || unit.isEmpty()) continue;

                                switch (nutName) {
                                    case "calories":
                                        kcal = amount;
                                        break;
                                    case "protein":
                                        if (unit.equals("g")) protein = amount;
                                        break;
                                    case "fat":
                                        if (unit.equals("g")) fat = amount;
                                        break;
                                    case "carbohydrates":
                                        if (unit.equals("g")) carbs = amount;
                                        break;
                                }
                            }
                        }
                    }

                    if (kcal <= 0) {
                        Log.w(TAG, "⚠️ Receta ignorada (sin kcal): " + name);
                        continue;
                    }

                    // Garantiza que no vayan nulos
                    protein = Math.max(0, protein);
                    carbs = Math.max(0, carbs);
                    fat = Math.max(0, fat);

                    FoodResult rec = new FoodResult(id, name, kcal, "Spoonacular", img, instr, ings);
                    rec.type = "receta";
                    rec.protein = protein;
                    rec.carbs = carbs;
                    rec.fat = fat;
                    rec.idMeal = id;

                    Log.d(TAG, String.format(Locale.getDefault(),
                            "✅ Receta: %s | kcal=%.0f | P=%.1f | C=%.1f | F=%.1f",
                            name, kcal, protein, carbs, fat));

                    translateRecipeFields(rec, () -> {
                        resultObjects.add(rec);
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                        }
                    });
                }

            } catch (Exception e) {
                Log.e(TAG, "❌ Error recetas Spoonacular", e);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(), "Error al buscar recetas en Spoonacular", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }


    private void translateRecipeFields(FoodResult recipe, Runnable onDone) {
        TranslatorOptions opts = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH)
                .build();
        Translator translator = Translation.getClient(opts);

        DownloadConditions cond = new DownloadConditions.Builder()
                .requireWifi()
                .build();

        translator.downloadModelIfNeeded(cond)
                .addOnSuccessListener(unused -> {
                    List<String> translatedIngredients = new ArrayList<>();

                    // Traducir nombre
                    translator.translate(recipe.name)
                            .addOnSuccessListener(translatedName -> recipe.name = translatedName);

                    // Traducir instrucciones
                    translator.translate(recipe.instructions)
                            .addOnSuccessListener(translatedInstr -> recipe.instructions = translatedInstr);

                    // Traducir ingredientes uno a uno
                    for (String ing : recipe.ingredients) {
                        translator.translate(ing)
                                .addOnSuccessListener(translated -> {
                                    translatedIngredients.add(translated);
                                    if (translatedIngredients.size() == recipe.ingredients.size()) {
                                        recipe.ingredients = translatedIngredients;
                                        onDone.run();
                                        translator.close();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    translatedIngredients.add(ing); // fallback
                                    if (translatedIngredients.size() == recipe.ingredients.size()) {
                                        recipe.ingredients = translatedIngredients;
                                        onDone.run();
                                        translator.close();
                                    }
                                });
                    }

                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "⚠️ Traducción fallida, se mantiene en EN");
                    onDone.run();
                    translator.close();
                });
    }





    private void fetchProductsFromOpenFoodFacts(String query) {
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

                if (products == null || products.length() == 0) return;

                String q = query.toLowerCase(Locale.ROOT);
                for (int i = 0; i < products.length(); i++) {
                    JSONObject p = products.getJSONObject(i);
                    String name = p.optString("product_name_es", p.optString("product_name", ""));
                    String nameLower = name.toLowerCase(Locale.ROOT);

                    if (nameLower.contains("sin " + q) || nameLower.contains("without " + q) ||
                            nameLower.contains("egg-free") || nameLower.contains("sin huevo")) continue;

                    String brand = p.optString("brands", "Sin marca");
                    String imageUrl = p.optString("image_front_url", "");
                    JSONObject n = p.optJSONObject("nutriments");

                    if (name.isEmpty() || n == null) continue;

                    double kcal = n.optDouble("energy-kcal_100g", -1);
                    double protein = n.optDouble("proteins_100g", 0);
                    double carbs = n.optDouble("carbohydrates_100g", 0);
                    double fat = n.optDouble("fat_100g", 0);
                    if (kcal <= 0) continue;

                    String ingredientsText = p.optString("ingredients_text", "").toLowerCase(Locale.ROOT);

                    boolean strongMatch = nameLower.contains(q);
                    boolean relatedMatch = ingredientsText.contains(q);

                    if (!strongMatch && !relatedMatch) continue;

                    FoodResult food = new FoodResult(
                            name, kcal, 0, brand, imageUrl, protein, carbs, fat);

                    /* NUEVO  ✅ */
                    food.type = "producto";     // asegura que siempre sea tratado como producto

                    String servingStr = p.optString("serving_size", "");
                    if (servingStr.endsWith("g") || servingStr.endsWith(" g")) {
                        try {
                            double g = Double.parseDouble(servingStr.replace("g", "").trim());
                            food.portions.add(new FoodResult.Portion("Porción estándar", g));
                        } catch (NumberFormatException ignore) {}
                    }

                    resultObjects.add(food);
                }

                if (isAdded()) requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                Log.e(TAG, "❌ Error buscando producto", e);
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error al buscar productos.", Toast.LENGTH_SHORT).show());
            }
        }).start();
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

        public String idMeal = "";
        public String name;
        public double kcalPer100g;
        public double grams;
        public String brand;
        public String imageUrl;
        public double protein;
        public double carbs;
        public double fat;
        public String type = "producto"; // 🔥 "producto" o "receta"
        public List<Portion> portions = new ArrayList<>();
        public String instructions = "";
        public List<String> ingredients = new ArrayList<>();

        public FoodResult() {}

        public FoodResult(String idMeal,
                          String name,
                          double kcalPer100g,
                          String brand,
                          String imageUrl,
                          String instructions,
                          List<String> ingredients) {
            this.idMeal = idMeal;
            this.name = name;
            this.kcalPer100g = kcalPer100g;
            this.brand = brand;
            this.imageUrl = imageUrl;
            this.instructions = instructions;
            this.ingredients = ingredients;
            this.type = "receta";
        }

        public FoodResult(String name,
                          double kcalPer100g,
                          double grams,
                          String brand,
                          String imageUrl,
                          double protein,
                          double carbs,
                          double fat,
                          String type) {
            this.name = name;
            this.kcalPer100g = kcalPer100g;
            this.grams = grams;
            this.brand = brand;
            this.imageUrl = imageUrl;
            this.protein = protein;
            this.carbs = carbs;
            this.fat = fat;
            this.type = type;
        }

        public FoodResult(String name,
                          double kcalPer100g,
                          double grams,
                          String brand,
                          String imageUrl,
                          double protein,
                          double carbs,
                          double fat) {
            this(name, kcalPer100g, grams, brand, imageUrl, protein, carbs, fat, "producto");
        }
        public boolean hasFullDetails() {
            return !TextUtils.isEmpty(instructions) && !ingredients.isEmpty();
        }


        public static FoodResult fromSearch(JSONObject meal) {
            String id = meal.optString("idMeal");
            String name = meal.has("strMealES") && !meal.isNull("strMealES")
                    ? meal.optString("strMealES")
                    : meal.optString("strMeal");

            String image = meal.optString("strMealThumb");
            return new FoodResult(id, name, 250, "TheMealDB", image, "", new ArrayList<>());
        }

        public boolean isRecipe() {
            return "receta".equalsIgnoreCase(type);
        }
    }


}
