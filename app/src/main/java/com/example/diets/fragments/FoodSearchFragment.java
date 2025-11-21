package com.example.diets.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;
import com.example.diets.network.OkHttpSingleton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.example.diets.adapters.SearchResultAdapter;
import com.example.diets.dialogs.BarcodeScannerDialog;
import com.example.diets.dialogs.GramsInputDialog;
import com.example.diets.dialogs.RecipeDialog;
import com.example.diets.model.FoodBlock;          //  se mantiene para compatibilidad con otras clases
import com.example.diets.model.FoodItem;          //  se mantiene
import com.example.diets.network.OkHttpSingleton;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;



public class FoodSearchFragment extends Fragment {

    // ==================== CONSTANTES ======================
    private static final String TAG = "FoodSearchFragment";

    // ==================== UI WIDGETS ======================
    private EditText searchFoodEditText;
    private ListView foodResultsListView;
    private SearchResultAdapter adapter;

    // ==================== CALLBACK ========================
    private Consumer<FoodResult> onFoodSelected;

    // ==================== LISTAS DE DATOS =================
    private final List<FoodResult> resultObjects = new ArrayList<>();
    private final List<FoodResult> productResults = new ArrayList<>();
    private final List<FoodResult> recipeResults  = new ArrayList<>();

    // -------------------- FILTROS -------------------------
    private final Map<String, String[]> quickFilterMap = getQuickFilters();
    private final List<String> quickFilterNames        = new ArrayList<>(quickFilterMap.keySet());
    private final List<String> quickSelectedNames      = new ArrayList<>();
    private final List<String[]> quickSelectedPairs    = new ArrayList<>();

    // FILTROS MACRO NUEVOS (más parámetros: azúcar, sodio, fibra)
    private final Map<String, MacroSpec> macroFilterMap = getMacroFilters();
    private final List<String> macroFilterNames         = new ArrayList<>(macroFilterMap.keySet());
    private final List<String> macroSelectedNames       = new ArrayList<>();

    // ==================== CICLO DE VIDA ===================
    public void setOnFoodSelected(Consumer<FoodResult> listener) {
        Log.d(TAG, "setOnFoodSelected: Listener establecido");
        this.onFoodSelected = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.dialog_food_search, container, false);
        view.setBackgroundColor(Color.TRANSPARENT);

        ImageButton barcodeBtn = view.findViewById(R.id.barcodeButton);
        barcodeBtn.setOnClickListener(v -> BarcodeScannerDialog
                .newInstance(code -> {
                    Log.d(TAG, "Código leído → " + code);
                    searchFoodByBarcode(code);
                })
                .show(getParentFragmentManager(), "BarcodeScannerDialog"));

        ImageButton extraBtn = view.findViewById(R.id.extraFilterButton);
        extraBtn.setOnClickListener(this::showMacroFilterPopup);

        Spinner filterSpinner = view.findViewById(R.id.filterSpinner);
        ArrayAdapter<String> spAdapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, quickFilterNames);
        spAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                clearQuickFilters();
                addQuickFilter(quickFilterNames.get(pos));
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
        filterSpinner.setOnLongClickListener(v -> { showQuickFilterPopup(v); return true; });

        searchFoodEditText  = view.findViewById(R.id.searchFoodEditText);
        foodResultsListView = view.findViewById(R.id.foodResultsListView);
        ImageButton searchButton = view.findViewById(R.id.searchButton);

        adapter = new SearchResultAdapter(requireContext(), resultObjects);
        foodResultsListView.setAdapter(adapter);

        searchFoodEditText.setOnEditorActionListener((v, actId, evt) -> {
            if (actId == EditorInfo.IME_ACTION_SEARCH) { lanzarBusqueda(); return true; }
            return false;
        });
        searchButton.setOnClickListener(v -> lanzarBusqueda());

        foodResultsListView.setOnItemClickListener(
                (parent, vw, pos, id) -> handleResultClick(resultObjects.get(pos)));

        clearQuickFilters();
        addQuickFilter("Todos");

        return view;
    }

    // ===================== POPUP QUICK FILTER =====================
    private void showQuickFilterPopup(View anchor) {
        ContextThemeWrapper cw = new ContextThemeWrapper(requireContext(), R.style.PopupMenu_Dark);
        PopupMenu pm = new PopupMenu(cw, anchor);

        for (int i = 0; i < quickFilterNames.size(); i++) {
            String n = quickFilterNames.get(i);
            boolean sel = quickSelectedNames.contains(n);
            pm.getMenu().add(0, i, i, (sel ? "✓ " : "") + n);
        }
        pm.setOnMenuItemClickListener(mi -> {
            toggleQuickFilter(quickFilterNames.get(mi.getItemId()), mi);
            Log.i(TAG, "AUDIT_TRAIL: Usuario modificó filtros de búsqueda. Recargando parámetros.");
            return true;
        });
        pm.show();
    }

    private void toggleQuickFilter(String name, MenuItem mi) {
        if (quickSelectedNames.contains(name)) removeQuickFilter(name);
        else addQuickFilter(name);
        mi.setTitle((quickSelectedNames.contains(name) ? "✓ " : "") + name);
    }

    // ===================== POPUP MACRO FILTER =====================
    private void showMacroFilterPopup(View anchor) {
        ContextThemeWrapper cw = new ContextThemeWrapper(requireContext(), R.style.PopupMenu_Dark);
        PopupMenu pm = new PopupMenu(cw, anchor);

        for (int i = 0; i < macroFilterNames.size(); i++) {
            String n = macroFilterNames.get(i);
            boolean sel = macroSelectedNames.contains(n);
            pm.getMenu().add(1, i, i, (sel ? "✓ " : "") + n);
        }
        pm.setOnMenuItemClickListener(mi -> {
            String n = macroFilterNames.get(mi.getItemId());
            if (macroSelectedNames.contains(n)) macroSelectedNames.remove(n);
            else macroSelectedNames.add(n);
            mi.setTitle((macroSelectedNames.contains(n) ? "✓ " : "") + n);
            Log.i(TAG, "AUDIT_TRAIL: Usuario modificó filtros de búsqueda. Recargando parámetros.");
            return true;
        });
        pm.show();
    }

    // ================== GESTIÓN QUICK FILTER ======================
    private void addQuickFilter(String name) {
        if (quickSelectedNames.contains(name)) return;
        String[] pair = quickFilterMap.get(name);
        if (pair == null) return;
        quickSelectedNames.add(name);
        quickSelectedPairs.add(pair);
    }

    private void removeQuickFilter(String name) {
        int idx = quickSelectedNames.indexOf(name);
        if (idx >= 0) {
            quickSelectedNames.remove(idx);
            quickSelectedPairs.remove(idx);
        }
    }

    private void clearQuickFilters() { quickSelectedNames.clear(); quickSelectedPairs.clear(); }

    // ====================== BÚSQUEDAS ===========================
    private void lanzarBusqueda() {
        String q = searchFoodEditText.getText().toString().trim();
        if (!q.isEmpty()) searchFood(q);
    }

    /** Consulta OpenFoodFacts por código de barras sin bloquear UI
     *  y con búsqueda textual de respaldo si no se encuentra el producto. */
    private void searchFoodByBarcode(String barcode) {
        Log.d(TAG, "🔍 searchFoodByBarcode: " + barcode);

        // Limpiamos resultados visibles
        resultObjects.clear();
        adapter.notifyDataSetChanged();

        new Thread(() -> {
            final long t0 = SystemClock.elapsedRealtime(); // ⏱ inicio medición
            try {
                String url = "https://world.openfoodfacts.org/api/v0/product/" +
                        URLEncoder.encode(barcode, "UTF-8") + ".json";

                OkHttpClient cli = OkHttpSingleton.get();
                Response rsp = cli.newCall(new Request.Builder().url(url).build()).execute();
                if (!rsp.isSuccessful()) throw new IOException("HTTP " + rsp.code());

                JSONObject root = new JSONObject(rsp.body().string());
                int status = root.optInt("status", 0);

                /* ───── producto NO encontrado ───── */
                if (status != 1) {
                    final long t1 = SystemClock.elapsedRealtime();
                    Log.i("OFF_TIMING", "term=" + barcode + " ms=" + (t1 - t0) +
                            " success=false source=barcode not_found=true");
                    Log.w(TAG, "Código no encontrado (" + barcode + "), status=" + status);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(),
                                        "Código no encontrado. Mostrando resultados relacionados…",
                                        Toast.LENGTH_SHORT).show());
                    }
                    // Fallback: ejecuta una búsqueda normal con el mismo texto
                    searchFood(barcode);
                    return;
                }

                /* ───── producto SÍ encontrado ───── */
                JSONObject p = root.getJSONObject("product");
                JSONObject n = p.optJSONObject("nutriments");
                if (n == null) {
                    final long t1 = SystemClock.elapsedRealtime();
                    Log.i("OFF_TIMING", "term=" + barcode + " ms=" + (t1 - t0) +
                            " success=false source=barcode reason=no_nutriments");
                    Log.w(TAG, "Producto sin nutriments → " + barcode);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(),
                                        "El producto no tiene información nutricional.",
                                        Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                String name = p.optString("product_name_es",
                        p.optString("product_name", "Producto sin nombre"));

                double kcal = n.optDouble("energy-kcal_100g", -1);
                if (kcal <= 0) kcal = n.optDouble("energy_100g", -1);   // respaldo

                if (kcal <= 0) {
                    final long t1 = SystemClock.elapsedRealtime();
                    Log.i("OFF_TIMING", "term=" + barcode + " ms=" + (t1 - t0) +
                            " success=false source=barcode reason=no_kcal");
                    Log.w(TAG, "Producto sin kcal válido → " + name);

                    if (isAdded()) {
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(),
                                        "El producto carece de calorías válidas.",
                                        Toast.LENGTH_SHORT).show());
                    }
                    return;
                }

                // Macros
                double protein = n.optDouble("proteins_100g", 0);
                double carbs   = n.optDouble("carbohydrates_100g", 0);
                double fat     = n.optDouble("fat_100g", 0);
                double sugar   = n.optDouble("sugars_100g", -1);
                double sodium  = n.optDouble("sodium_100g", -1);
                double fiber   = n.optDouble("fiber_100g", -1);

                if (!passesMacroFilters(kcal, protein, carbs, fat, sugar, sodium, fiber)) {
                    final long t1 = SystemClock.elapsedRealtime();
                    Log.i("OFF_TIMING", "term=" + barcode + " ms=" + (t1 - t0) +
                            " success=false source=barcode filtered=true reason=macro_filters");
                    Log.d(TAG, "Producto filtrado por macros → " + name);
                    return;   // se respeta lógica de filtros
                }

                String brand = p.optString("brands", "Sin marca");
                String img   = p.optString("image_front_url", "");
                String servingSizeStr = p.optString("serving_size", "");

                FoodResult food = new FoodResult(name, kcal, 0, brand, img, protein, carbs, fat);

                // Porciones: siempre 100 g + porción OFF si existe
                food.portions.add(new FoodResult.Portion("100 g", 100));
                double servingGrams = parseServingSizeToGrams(servingSizeStr);
                if (servingGrams > 0) {
                    String label = "1 porción (" + servingSizeStr + ")";
                    food.portions.add(new FoodResult.Portion(label, servingGrams));
                    Log.d(TAG, "Porción OFF (barcode): " + label + " = " + servingGrams + " g");
                } else {
                    Log.d(TAG, "Sin porción OFF válida (barcode) para " + name +
                            " – serving_size='" + servingSizeStr + "'");
                }

                resultObjects.add(food);

                final long t1 = SystemClock.elapsedRealtime(); // ⏱ fin medición
                Log.i("OFF_TIMING", "term=" + barcode + " ms=" + (t1 - t0) +
                        " success=true source=barcode count=1 name=\"" + name.replace("\"","'") + "\"");

                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                }

            } catch (Exception e) {
                final long t1 = SystemClock.elapsedRealtime(); // ⏱ fin con error
                Log.i("OFF_TIMING", "term=" + barcode + " ms=" + (t1 - t0) +
                        " success=false source=barcode error=" + e.getClass().getSimpleName());
                Log.e(TAG, "searchFoodByBarcode: error", e);

                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(getContext(),
                                    "Error al consultar el código de barras.",
                                    Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void searchFood(String query) {
        Log.d(TAG, "searchFood: " + query);
        Log.i(TAG, "AUDIT_TRAIL: Iniciando búsqueda híbrida para: '" + query + "'. Disparando hilos OFF y Spoonacular.");
        resultObjects.clear(); adapter.notifyDataSetChanged();
        productResults.clear(); recipeResults.clear();
        fetchProductsFromOpenFoodFacts(query);
        fetchRecipesFromSpoonacular(query); // ← SEGURO
    }

    // Si falta, agrega esto:
    private void fetchRecipesFromSpoonacular(String query) {
        fetchRecipesFromSpoonacularRetry(query, 0);
    }

    private void fetchRecipesFromSpoonacularRetry(String query, int intento) {
        new Thread(() -> {
            try {
                // CLAVE DOCUMENTADA (Coincide con Manual Técnico)
                final String key = "04e23d7cf29c4324bbba0799fdbfba61";

                StringBuilder url = new StringBuilder("https://api.spoonacular.com/recipes/complexSearch")
                        .append("?query=").append(URLEncoder.encode(query, "UTF-8"))
                        .append("&number=30")
                        .append("&addRecipeNutrition=true")
                        .append("&instructionsRequired=true")
                        .append("&fillIngredients=true")
                        .append("&apiKey=").append(key);

                for (String n : quickSelectedNames)  url.append(getSpoonacularQuickQuery(n));
                for (String n : macroSelectedNames) url.append(getSpoonacularMacroQuery(n));

                Log.d(TAG, "AUDIT_TRAIL: Spoonacular filtros server-side aplicados. query=" + query +
                        " quickFilters=" + quickSelectedNames +
                        " macroFilters=" + macroSelectedNames);


                OkHttpClient cli = OkHttpSingleton.get();
                Response rsp = cli.newCall(new Request.Builder().url(url.toString()).build()).execute();

                // --- CORRECCIÓN AUDITORÍA: MANEJO ESPECÍFICO DE ERRORES (Doc: SPOON-03) ---
                if (!rsp.isSuccessful()) {
                    // Caso Cuota Excedida (Documentado en Integración Técnica)
                    if (rsp.code() == 402) {
                        Log.e(TAG, "Spoonacular 402: Cuota diaria excedida.");
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(),
                                            "Límite de recetas alcanzado (Cuota diaria). Intenta mañana.",
                                            Toast.LENGTH_LONG).show()
                            );
                        }
                        return; // NO reintentar si es error de cuota
                    }
                    // Otros errores HTTP
                    throw new IOException("HTTP " + rsp.code());
                }
                // ---------------------------------------------------------------------------

                JSONArray arr = new JSONObject(rsp.body().string()).optJSONArray("results");
                if (arr == null) return;

                Log.i(TAG, "AUDIT_TRAIL: Spoonacular retornó " + arr.length() + " recetas.");

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject r = arr.getJSONObject(i);

                    /* ---------- MACROS ---------- */
                    double kcal = -1, protein = -1, carbs = -1, fat = -1, sugar = -1, sodium = -1, fiber = -1;
                    JSONArray nutr = r.optJSONObject("nutrition").optJSONArray("nutrients");
                    if (nutr != null)
                        for (int j = 0; j < nutr.length(); j++) {
                            JSONObject nt   = nutr.getJSONObject(j);
                            String name     = nt.optString("name", "").toLowerCase(Locale.ROOT);
                            double amount   = nt.optDouble("amount", -1);
                            String unit     = nt.optString("unit", "").toLowerCase(Locale.ROOT);
                            switch (name) {
                                case "calories":       kcal = amount; break;
                                case "protein":        if ("g".equals(unit)) protein = amount; break;
                                case "fat":            if ("g".equals(unit)) fat     = amount; break;
                                case "carbohydrates":  if ("g".equals(unit)) carbs   = amount; break;
                                case "sugar": case "sugars":
                                    if ("g".equals(unit)) sugar = amount; break;
                                case "fiber":          if ("g".equals(unit)) fiber  = amount; break;
                                case "sodium":         sodium = amount / 1000.0; break; // mg → g
                            }
                        }
                    if (kcal <= 0) continue;
                    if (!passesMacroFilters(kcal, protein, carbs, fat, sugar, sodium, fiber)) continue;

                    /* ---------- CAMPOS BÁSICOS ---------- */
                    String id     = r.optString("id", "");
                    String name   = r.optString("title", "Receta");
                    String img    = r.optString("image", "");

                    /* ---------- INSTRUCCIONES (Lógica de Fallbacks Documentada) ---------- */
                    String instr = r.optString("instructions", "");
                    if (!instr.isEmpty()) {
                        instr = Html.fromHtml(instr, Html.FROM_HTML_MODE_LEGACY).toString();
                    } else {
                        // Fallback 1: analyzedInstructions
                        JSONArray analyzed = r.optJSONArray("analyzedInstructions");
                        if (analyzed != null && analyzed.length() > 0) {
                            JSONArray steps = analyzed.getJSONObject(0).optJSONArray("steps");
                            if (steps != null && steps.length() > 0) {
                                StringBuilder sb = new StringBuilder();
                                for (int k = 0; k < steps.length(); k++)
                                    sb.append(steps.getJSONObject(k).optString("step", "")).append(". ");
                                instr = sb.toString().trim();
                            }
                        }
                        // Fallback 2: Summary
                        if (instr.isEmpty()) {
                            String summary = r.optString("summary", "");
                            if (!summary.isEmpty()) {
                                instr = Html.fromHtml(summary, Html.FROM_HTML_MODE_LEGACY).toString();
                            }
                        }
                    }

                    /* ---------- INGREDIENTES ---------- */
                    List<String> ings = new ArrayList<>();
                    JSONArray ext = r.optJSONArray("extendedIngredients");
                    if (ext != null)
                        for (int j = 0; j < ext.length(); j++)
                            ings.add(ext.getJSONObject(j).optString("original", ""));

                    /* ---------- OBJETO FINAL ---------- */
                    FoodResult rec = new FoodResult(id, name, kcal, "Spoonacular", img, instr, ings);
                    rec.type    = "receta";
                    rec.protein = protein;
                    rec.carbs   = carbs;
                    rec.fat     = fat;
                    rec.idMeal  = id;

                    translateRecipeFields(rec, () -> {
                        recipeResults.add(rec);
                        resultObjects.add(rec);
                        if (isAdded())
                            requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
                    });
                }
            } catch (Exception e) {
                Log.e(TAG, "Error Spoonacular: " + e.getMessage()); // Log seguro (no imprime API Key)

                // Retry automático SOLO si es IOException y no se ha excedido el máximo
                if (e instanceof IOException && intento < 3) {
                    try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
                    fetchRecipesFromSpoonacularRetry(query, intento + 1);
                } else {
                    // Solo mostrar Toast si ya fallaron todos los intentos y NO fue error 402 (ya manejado arriba)
                    if (isAdded())
                        requireActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Error de conexión con recetas", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    private void fetchProductsFromOpenFoodFacts(String query) {
        new Thread(() -> {
            final long t0 = SystemClock.elapsedRealtime(); // ⏱ inicio medición
            try {
                StringBuilder url = new StringBuilder("https://world.openfoodfacts.org/cgi/search.pl")
                        .append("?search_terms=").append(URLEncoder.encode(query, "UTF-8"))
                        .append("&search_simple=1&action=process&json=1&lc=es&page_size=40")
                        .append("&fields=product_name_es,product_name,brands,countries_tags,")
                        .append("image_front_url,ingredients_text,")
                        .append("nutriments.energy-kcal_100g,nutriments.proteins_100g,")
                        .append("nutriments.carbohydrates_100g,nutriments.fat_100g,")
                        .append("nutriments.sugars_100g,nutriments.sodium_100g,")
                        .append("nutriments.fiber_100g,")
                        .append("serving_size"); // 👈 añadimos serving_size

                for (int i = 0; i < quickSelectedPairs.size(); i++) {
                    String[] pr = quickSelectedPairs.get(i);
                    if (TextUtils.isEmpty(pr[0]) || TextUtils.isEmpty(pr[1])) continue;
                    url.append("&tagtype_").append(i)
                            .append("=").append(pr[0])
                            .append("&tag_contains_").append(i).append("=contains")
                            .append("&tag_").append(i).append("=").append(pr[1]);
                }

                OkHttpClient cli = OkHttpSingleton.get();
                Response rsp = cli.newCall(new Request.Builder().url(url.toString()).build()).execute();
                if (!rsp.isSuccessful()) throw new IOException("HTTP " + rsp.code());

                JSONArray products = new JSONObject(rsp.body().string()).optJSONArray("products");
                if (products == null) {
                    final long t1 = SystemClock.elapsedRealtime();
                    Log.i("OFF_TIMING", "term=" + query + " ms=" + (t1 - t0) + " success=false source=red reason=no_products");
                    return;
                }

                String qLow = query.toLowerCase(Locale.ROOT);
                List<FoodResult> tempProducts = new ArrayList<>();

                for (int i = 0; i < products.length(); i++) {
                    JSONObject p = products.getJSONObject(i);
                    String name = p.optString("product_name_es", p.optString("product_name", ""));
                    if (name.isEmpty()) continue;
                    String nameLow = name.toLowerCase(Locale.ROOT);
                    if (nameLow.contains("sin " + qLow) || nameLow.contains("without " + qLow)) continue;

                    JSONObject n = p.optJSONObject("nutriments");
                    if (n == null) continue;

                    double kcal = n.optDouble("energy-kcal_100g", -1);
                    double protein = n.optDouble("proteins_100g", 0);
                    double carbs   = n.optDouble("carbohydrates_100g", 0);
                    double fat     = n.optDouble("fat_100g", 0);
                    double sugar   = n.optDouble("sugars_100g", -1);
                    double sodium  = n.optDouble("sodium_100g", -1);
                    double fiber   = n.optDouble("fiber_100g", -1);

                    if (kcal <= 0) continue;

                    if (!passesMacroFilters(kcal, protein, carbs, fat, sugar, sodium, fiber)) {
                        Log.d(TAG, "AUDIT_TRAIL: OFF Producto descartado por filtros de Macros: " + name);
                        continue;
                    }

                    Log.v(TAG, "AUDIT_TRAIL: OFF Producto aceptado: " + name);

                    String brand = p.optString("brands", "Sin marca");
                    String img   = p.optString("image_front_url", "");
                    String servingSizeStr = p.optString("serving_size", "");

                    FoodResult food = new FoodResult(name, kcal, 0, brand, img, protein, carbs, fat);

                    // Porciones: siempre 100 g + porción OFF si existe
                    food.portions.add(new FoodResult.Portion("100 g", 100));
                    double servingGrams = parseServingSizeToGrams(servingSizeStr);
                    if (servingGrams > 0) {
                        String label = "1 porción (" + servingSizeStr + ")";
                        food.portions.add(new FoodResult.Portion(label, servingGrams));
                        Log.d(TAG, "Porción OFF (search): " + label + " = " + servingGrams + " g");
                    } else {
                        Log.d(TAG, "Sin porción OFF válida (search) para " + name +
                                " – serving_size='" + servingSizeStr + "'");
                    }

                    foodResultsAddWithRanking(tempProducts, food, query);
                }

                // Ordenamos por relevancia calculada previamente
                Collections.sort(tempProducts, (a, b) ->
                        Integer.compare(relevanceScore(a.name, query), relevanceScore(b.name, query)));

                productResults.addAll(tempProducts);
                resultObjects.addAll(tempProducts);

                final long t1 = SystemClock.elapsedRealtime(); // ⏱ fin medición
                boolean success = !tempProducts.isEmpty();
                Log.i("OFF_TIMING",
                        "term=" + query +
                                " ms=" + (t1 - t0) +
                                " success=" + success +
                                " source=red" +
                                " count=" + tempProducts.size());

                if (isAdded()) requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());

            } catch (Exception e) {
                final long t1 = SystemClock.elapsedRealtime(); // ⏱ fin medición con error
                Log.i("OFF_TIMING",
                        "term=" + query +
                                " ms=" + (t1 - t0) +
                                " success=false source=red error=" + e.getClass().getSimpleName());
                Log.e(TAG, "Error OFF", e);
                if (isAdded()) requireActivity().runOnUiThread(() ->
                        Toast.makeText(getContext(), "Error productos", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // ================ FILTROS AVANZADOS (SIN romper compatibilidad) ================
    private boolean passesMacroFilters(double kcal, double protein, double carbs, double fat) {
        return passesMacroFilters(kcal, protein, carbs, fat, -1, -1, -1);
    }

    private boolean passesMacroFilters(double kcal, double protein, double carbs, double fat,
                                       double sugar, double sodium, double fiber) {
        for (String name : macroSelectedNames) {
            MacroSpec m = macroFilterMap.get(name);
            if (m == null) continue;
            if (m.maxKcal    > 0 && kcal    > m.maxKcal   ) return false;
            if (m.minProtein > 0 && protein < m.minProtein) return false;
            if (m.maxCarbs   > 0 && carbs   > m.maxCarbs  ) return false;
            if (m.maxFat     > 0 && fat     > m.maxFat    ) return false;
            if (m.maxSugar   > 0 && sugar   > m.maxSugar  ) return false;
            if (m.maxSodium  > 0 && sodium  > m.maxSodium ) return false;
            if (m.minFiber   > 0 && fiber   < m.minFiber  ) return false;
        }
        return true;
    }

    // ================= UTILIDAD DE RANKING =================
    private static int relevanceScore(String name, String query) {
        String n = name.toLowerCase(Locale.ROOT);
        String q = query.toLowerCase(Locale.ROOT);

        if (n.equals(q)) return 0;                             // Coincidencia exacta
        if (n.startsWith(q)) return 1;                         // Comienza con la consulta
        if (n.contains(" " + q + " ")) return 2;               // Palabra completa interna
        int idx = n.indexOf(q);
        if (idx >= 0) return 3 + idx;                          // Contiene en posición <idx>
        return 1000;                                           // Poco relevante
    }

    /** Inserta en la lista temporal evitando duplicados visibles y conservando la referencia */
    private void foodResultsAddWithRanking(List<FoodResult> list, FoodResult item, String query) {
        for (FoodResult f : list) { // evitar duplicados por nombre y marca
            if (f.name.equalsIgnoreCase(item.name) && f.brand.equalsIgnoreCase(item.brand)) return;
        }
        list.add(item);
    }

    // ============= QUICK Y MACRO FILTERS MAPS AMPLIADOS =====================
    private Map<String, String[]> getQuickFilters() {
        Map<String, String[]> m = new LinkedHashMap<>();
        m.put("Todos",               new String[]{"", ""});
        m.put("Vegano",              new String[]{"labels", "en:vegan"});
        m.put("Vegetariano",         new String[]{"labels", "en:vegetarian"});
        m.put("Orgánico",            new String[]{"labels", "en:organic"});
        m.put("Sin gluten",          new String[]{"labels", "en:gluten-free"});
        m.put("Sin azúcar",          new String[]{"labels", "en:low-sugar"});
        m.put("Sin aceite de palma", new String[]{"labels", "en:palm-oil-free"});
        m.put("NutriScore A",        new String[]{"nutrition_grades_tags", "a"});
        m.put("NOVA Grupo 1",        new String[]{"nova_groups_tags", "1"});
        m.put("EcoScore A",          new String[]{"ecoscore_tags", "a"});
        m.put("Mexicano",            new String[]{"countries_tags", "en:mexico"});
        return m;
    }

    private String getSpoonacularQuickQuery(String name) {
        Map<String, String[]> map = new HashMap<>();
        map.put("Vegano",              new String[]{"diet",            "vegan"});
        map.put("Vegetariano",         new String[]{"diet",            "vegetarian"});
        map.put("Sin gluten",          new String[]{"intolerances",    "gluten"});
        map.put("Sin aceite de palma", new String[]{"excludeIngredients", "palm oil"});
        map.put("Sin azúcar",          new String[]{"maxSugar",        "5"});
        String[] p = map.get(name); if (p == null) return "";
        return "&" + p[0] + "=" + URLEncoder.encode(p[1], java.nio.charset.StandardCharsets.UTF_8);
    }

    private static class MacroSpec {
        double maxKcal, minProtein, maxCarbs, maxFat, maxSugar, maxSodium, minFiber;
        MacroSpec(double maxKcal, double minProt, double maxCarbs, double maxFat) {
            this.maxKcal = maxKcal; this.minProtein = minProt; this.maxCarbs = maxCarbs; this.maxFat = maxFat;
            this.maxSugar = -1; this.maxSodium = -1; this.minFiber = -1;
        }
        MacroSpec(double maxKcal, double minProt, double maxCarbs, double maxFat,
                  double maxSugar, double maxSodium, double minFiber) {
            this.maxKcal = maxKcal; this.minProtein = minProt; this.maxCarbs = maxCarbs; this.maxFat = maxFat;
            this.maxSugar = maxSugar; this.maxSodium = maxSodium; this.minFiber = minFiber;
        }
    }

    private Map<String, MacroSpec> getMacroFilters() {
        Map<String, MacroSpec> m = new LinkedHashMap<>();
        m.put("Bajo en grasa",         new MacroSpec(-1, -1, -1, 10));
        m.put("Bajo en carbohidratos", new MacroSpec(-1, -1, 10, -1));
        m.put("Alto en proteína",      new MacroSpec(-1, 20, -1, -1));
        m.put("Bajo en calorías",      new MacroSpec(100, -1, -1, -1));
        m.put("Sin calorías",          new MacroSpec(5,   -1, -1, -1)); // <= 5 kcal
        m.put("Bajo en azúcar",        new MacroSpec(-1, -1, -1, -1, 5,   -1, -1));   // <= 5g azúcar
        m.put("Bajo en sodio",         new MacroSpec(-1, -1, -1, -1, -1, 0.15, -1));  // <= 0.15g Na (150mg)
        m.put("Alto en fibra",         new MacroSpec(-1, -1, -1, -1, -1, -1, 5));     // >= 5g fibra
        return m;
    }

    private String getSpoonacularMacroQuery(String name) {
        switch (name) {
            case "Bajo en grasa":         return "&maxFat=10";
            case "Bajo en carbohidratos": return "&maxCarbs=10";
            case "Alto en proteína":      return "&minProtein=20";
            case "Bajo en calorías":      return "&maxCalories=100";
            case "Sin calorías":          return "&maxCalories=5";
            case "Bajo en azúcar":        return "&maxSugar=5";
            case "Bajo en sodio":         return "&maxSodium=150";
            case "Alto en fibra":         return "&minFiber=5";
            default: return "";
        }
    }

    // ========= PARSEO DE serving_size → gramos (para porciones OFF) =========
    private double parseServingSizeToGrams(String serving) {
        if (TextUtils.isEmpty(serving)) return -1;
        String s = serving.replace(',', '.').toLowerCase(Locale.ROOT);

        // Busca el primer número con unidad g / gramos / ml / l
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("(\\d+(?:\\.\\d+)?)\\s*(g|gramos|ml|l)")
                .matcher(s);

        if (!m.find()) return -1;

        double value;
        try {
            value = Double.parseDouble(m.group(1));
        } catch (NumberFormatException e) {
            return -1;
        }

        String unit = m.group(2);
        if ("ml".equals(unit)) {
            // Aproximación densidad ≈ agua
            return value;
        } else if ("l".equals(unit)) {
            return value * 1000.0;
        }
        // g / gramos
        return value;
    }

    // =================== RESTO DEL CÓDIGO IGUAL (sin tocar nada) ==============
    private void translateRecipeFields(FoodResult recipe, Runnable onDone) {
        TranslatorOptions opts = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.SPANISH).build();
        Translator tr = com.google.mlkit.nl.translate.Translation.getClient(opts);
        tr.downloadModelIfNeeded(new DownloadConditions.Builder().requireWifi().build())
                .addOnSuccessListener(v -> {
                    tr.translate(recipe.name).addOnSuccessListener(t -> recipe.name = t);
                    tr.translate(recipe.instructions).addOnSuccessListener(t -> recipe.instructions = t);
                    List<String> transIng = new ArrayList<>();
                    for (String ing : recipe.ingredients) {
                        tr.translate(ing)
                                .addOnSuccessListener(t -> {
                                    transIng.add(t);
                                    if (transIng.size() == recipe.ingredients.size()) {
                                        recipe.ingredients = transIng; onDone.run(); tr.close();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    transIng.add(ing);
                                    if (transIng.size() == recipe.ingredients.size()) {
                                        recipe.ingredients = transIng; onDone.run(); tr.close();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> { onDone.run(); tr.close(); });
    }

    private void handleResultClick(FoodResult sel) {
        if (sel.isRecipe()) {
            RecipeDialog dlg = RecipeDialog.newInstance(sel);
            dlg.setOnGramsConfirmedListener(r -> { if (onFoodSelected != null) onFoodSelected.accept(r); });
            dlg.show(getParentFragmentManager(), "RecipeDialog");
            return;
        }
        GramsInputDialog.newInstance(sel, r -> {
            if (r.ingredients != null && !r.ingredients.isEmpty()) {
                RecipeDialog dlg = RecipeDialog.newInstance(r);
                dlg.setOnGramsConfirmedListener(rr -> { if (onFoodSelected != null) onFoodSelected.accept(rr); });
                dlg.show(getParentFragmentManager(), "RecipeDialog");
                return;
            }
            if (onFoodSelected != null) onFoodSelected.accept(r);
        }).show(getParentFragmentManager(), "GramsInputDialog");
    }

    public static class FoodResult implements java.io.Serializable {
        public static class Portion implements java.io.Serializable {
            public final String label; public final double grams;
            public Portion(String l, double g) { label = l; grams = g; }
        }
        public String idMeal = "";
        public String name;
        public double kcalPer100g;
        public double grams;
        public String brand;
        public String imageUrl = "";
        public double protein;
        public double carbs;
        public double fat;
        public String type = "producto";         // "producto" o "receta"
        public List<Portion> portions = new ArrayList<>();
        public String instructions = "";
        public List<String> ingredients = new ArrayList<>();

        public FoodResult() {}

        public FoodResult(String idMeal, String name, double kcalPer100g,
                          String brand, String imageUrl, String instructions,
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

        public FoodResult(String name, double kcalPer100g, double grams,
                          String brand, String imageUrl,
                          double protein, double carbs, double fat) {
            this.name = name; this.kcalPer100g = kcalPer100g; this.grams = grams;
            this.brand = brand; this.imageUrl = imageUrl;
            this.protein = protein; this.carbs = carbs; this.fat = fat;
            this.type = "producto";
        }

        public boolean isRecipe() { return "receta".equalsIgnoreCase(type); }
    }
}
