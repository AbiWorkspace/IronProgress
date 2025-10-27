package com.example.diets.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.MainActivity;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.FoodBlockAdapter;
import com.example.diets.dialogs.RecipeDialog;
import com.example.diets.model.FoodBlock;
import com.example.diets.model.FoodItem;
import com.example.diets.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";

    // SharedPreferences
    private static final String PREFS_NAME = "FoodPrefs";
    private static final String PREF_GOALS = "MacroGoals";
    private static final String K_KCAL = "gKcal";
    private static final String K_PROT = "gProt";
    private static final String K_FAT  = "gFat";
    private static final String K_CARB = "gCarb";

    private static final int MAX_FOOD_BLOCKS = 10;
    private static final int MAX_DAYS_IN_FUTURE = 365;

    // Fechas
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");                  // para claves
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()); // UI

    // Bloques por defecto
    private static final String[] DEFAULT_FOOD_NAMES =
            {"Desayuno", "Almuerzo", "Comida", "Snack", "Cena"};

    // Estado/carga
    private boolean dataLoaded = false;
    private LocalDate currentDate = LocalDate.now();

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    // Datos/usuario
    private final List<FoodBlock> foodBlockList = new ArrayList<>();
    private FoodBlockAdapter foodBlockAdapter;
    private User user;
    private double goalsKcal, goalsP, goalsF, goalsC;

    // UI
    public RecyclerView recyclerViewFoodBlocks;
    private LinearProgressIndicator barCalories, barProtein, barCarbs, barFat;
    private TextView labelCalories, labelProtein, labelCarbs, labelFat, txtCurrentDate;
    private ImageButton btnPrevDay, btnNextDay;

    // Listener de navegación
    public interface OnNavigationItemSelectedListener { void onNavigationItemSelected(int position); }
    private OnNavigationItemSelectedListener mListener;

    // Metas: prefs + listener
    private SharedPreferences goalPrefs;
    private final SharedPreferences.OnSharedPreferenceChangeListener goalListener =
            (sp, key) -> {
                if (key == null) return;
                FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
                if (u == null) return;
                String pfx = u.getUid() + "_";
                if (key.equals(pfx + K_KCAL) || key.equals(pfx + K_PROT) ||
                        key.equals(pfx + K_CARB) || key.equals(pfx + K_FAT)) {
                    if (loadGoalsFromPrefs()) {
                        updateMacrosUI();
                        recalculateDayTotals();
                    }
                }
            };

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()
            .create();

    /* ==================== Ciclo de vida ==================== */

    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        Log.d(TAG, "[onAttach] fragment attached");
        if (ctx instanceof OnNavigationItemSelectedListener) {
            mListener = (OnNavigationItemSelectedListener) ctx;
        } else {
            throw new RuntimeException(ctx + " must implement OnNavigationItemSelectedListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (goalPrefs != null) {
            goalPrefs.unregisterOnSharedPreferenceChangeListener(goalListener);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup cont,
                             @Nullable Bundle saved) {
        Log.d(TAG, "[onCreateView] inflating layout");

        if (goalPrefs == null) {
            goalPrefs = requireContext().getSharedPreferences(PREF_GOALS, Context.MODE_PRIVATE);
            goalPrefs.registerOnSharedPreferenceChangeListener(goalListener);
        }

        return inf.inflate(R.layout.activity_food, cont, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
        super.onViewCreated(v, saved);

        cleanupOldEntries();
        migrateAllSavedDays(); // si es temporal, muévelo a una pantalla de mantenimiento

        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        bindUI(v);

        // 1) Carga metas
        loadGoalsFromPrefs();
        updateMacrosUI();

        // 2) Carga comida del día
        loadFoodBlocksForDate(currentDate);

        // 3) Recalcula
        recalculateDayTotals();

        // Navegación/menús
        setupDateNavigation();
        setupBottomNav(v);
        setupDrawer(v);

        // Cargar usuario (para goal ranges y caloriesToday)
        FirebaseUser cur = mAuth.getCurrentUser();
        if (cur != null) {
            loadUserData(cur.getUid());
        } else {
            Log.w(TAG, "[onViewCreated] usuario null");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "[onPause] guardando día " + currentDate);
        guardarComidasDelDia(currentDate);
    }

    /* ==================== Helpers de UID y claves ==================== */

    @Nullable
    private String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null ? u.getUid() : null;
    }

    private String keyFood(LocalDate d) {
        String u = uid();
        return "food_" + (u == null ? "nouid" : u) + "_" + DATE_FMT.format(d);
    }
    private String keyTotK(LocalDate d) {
        String u = uid();
        return "totK_" + (u == null ? "nouid" : u) + "_" + DATE_FMT.format(d);
    }
    private String keyTotP(LocalDate d) {
        String u = uid();
        return "totP_" + (u == null ? "nouid" : u) + "_" + DATE_FMT.format(d);
    }
    private String keyTotC(LocalDate d) {
        String u = uid();
        return "totC_" + (u == null ? "nouid" : u) + "_" + DATE_FMT.format(d);
    }
    private String keyTotF(LocalDate d) {
        String u = uid();
        return "totF_" + (u == null ? "nouid" : u) + "_" + DATE_FMT.format(d);
    }

    /* ==================== Metas (load/save/UI) ==================== */

    private boolean loadGoalsFromPrefs() {
        if (!isAdded()) return false;
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return false;

        SharedPreferences p = (goalPrefs != null)
                ? goalPrefs
                : requireContext().getSharedPreferences(PREF_GOALS, Context.MODE_PRIVATE);

        String prefix = u.getUid() + "_";
        if (!p.contains(prefix + K_KCAL)) return false;

        goalsKcal = p.getFloat(prefix + K_KCAL, 0f);
        goalsP    = p.getFloat(prefix + K_PROT, 0f);
        goalsF    = p.getFloat(prefix + K_FAT , 0f);
        goalsC    = p.getFloat(prefix + K_CARB, 0f);

        Log.d(TAG, String.format(Locale.getDefault(),
                "[loadGoalsFromPrefs] kcal=%.0f P=%.0f C=%.0f F=%.0f",
                goalsKcal, goalsP, goalsC, goalsF));
        return true;
    }

    private void saveGoalsToPrefs() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || !isAdded()) return;

        String prefix = u.getUid() + "_";
        SharedPreferences.Editor ed = requireContext()
                .getSharedPreferences(PREF_GOALS, Context.MODE_PRIVATE)
                .edit();

        ed.putFloat(prefix + K_KCAL, (float) goalsKcal);
        ed.putFloat(prefix + K_PROT, (float) goalsP);
        ed.putFloat(prefix + K_FAT , (float) goalsF);
        ed.putFloat(prefix + K_CARB, (float) goalsC);
        ed.apply();

        Log.d(TAG, "[saveGoalsToPrefs] metas guardadas localmente");
    }

    private void updateMacrosUI() {
        if (!isAdded() || barCalories == null) return;

        barCalories.setMax((int) Math.max(0, goalsKcal));
        barProtein .setMax((int) Math.max(0, goalsP));
        barCarbs   .setMax((int) Math.max(0, goalsC));
        barFat     .setMax((int) Math.max(0, goalsF));

        labelCalories.setText(String.format(Locale.getDefault(),
                "Calorías: 0 / %.0f kcal", goalsKcal));
        labelProtein .setText(String.format(Locale.getDefault(),
                "Proteínas: 0 / %.0f g",  goalsP));
        labelCarbs   .setText(String.format(Locale.getDefault(),
                "Carbohidratos: 0 / %.0f g", goalsC));
        labelFat     .setText(String.format(Locale.getDefault(),
                "Grasas: 0 / %.0f g", goalsF));
    }

    /* ==================== UI wiring ==================== */

    private void bindUI(View v) {
        labelCalories = v.findViewById(R.id.labelCalories);
        labelProtein  = v.findViewById(R.id.labelProtein);
        labelCarbs    = v.findViewById(R.id.labelCarbs);
        labelFat      = v.findViewById(R.id.labelFat);

        barCalories = v.findViewById(R.id.barCalories);
        barProtein  = v.findViewById(R.id.barProtein);
        barCarbs    = v.findViewById(R.id.barCarbs);
        barFat      = v.findViewById(R.id.barFat);

        txtCurrentDate = v.findViewById(R.id.txtCurrentDate);
        btnPrevDay     = v.findViewById(R.id.btnPrevDay);
        btnNextDay     = v.findViewById(R.id.btnNextDay);
        txtCurrentDate.setText(currentDate.format(DISPLAY_FMT));

        recyclerViewFoodBlocks = v.findViewById(R.id.recyclerFoodBlocks);
        recyclerViewFoodBlocks.setLayoutManager(new LinearLayoutManager(getContext()));
        foodBlockAdapter = new FoodBlockAdapter(
                foodBlockList,
                getContext(),
                this,
                (block, position) -> showDeleteBlockDialog(block, position)
        );
        recyclerViewFoodBlocks.setAdapter(foodBlockAdapter);

        v.findViewById(R.id.addFoodButton).setOnClickListener(view -> {
            if (foodBlockList.size() >= MAX_FOOD_BLOCKS) {
                Toast.makeText(getContext(), "Máximo de bloques alcanzado", Toast.LENGTH_SHORT).show();
                return;
            }
            foodBlockList.add(new FoodBlock("Nueva comida"));
            if (foodBlockAdapter != null) foodBlockAdapter.notifyItemInserted(foodBlockList.size() - 1);
        });

        txtCurrentDate.setOnClickListener(v1 -> {
            LocalDate date = currentDate;
            DatePickerDialog dpd = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        LocalDate picked = LocalDate.of(year, month + 1, dayOfMonth);
                        switchDate(picked);
                    },
                    date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth()
            );
            dpd.show();
        });

        labelCalories.setOnClickListener(v12 -> showEditMacrosDialog());
    }

    private void setupDateNavigation() {
        btnPrevDay.setOnClickListener(v -> switchDate(currentDate.minusDays(1)));
        btnNextDay.setOnClickListener(v -> {
            LocalDate tomorrow = currentDate.plusDays(1);
            if (ChronoUnit.DAYS.between(LocalDate.now(), tomorrow) <= MAX_DAYS_IN_FUTURE) {
                switchDate(tomorrow);
            } else {
                Toast.makeText(getContext(), "No puedes planificar tan lejos", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav(View v) {
        BottomNavigationView bottom = v.findViewById(R.id.bottom_navigation);
        if (bottom == null) return;
        bottom.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.navigation_home) {
                if (mListener != null) mListener.onNavigationItemSelected(1);
                return true;
            } else if (item.getItemId() == R.id.navigation_dashboard) {
                if (getActivity() instanceof ViewPagerActivity) {
                    ((ViewPagerActivity) getActivity()).switchToWeightFragment();
                }
                return true;
            }
            return false;
        });
    }

    private void setupDrawer(View v) {
        NavigationView nav = v.findViewById(R.id.navigation_view);
        nav.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                mAuth.signOut();
                startActivity(new Intent(getActivity(), MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                return true;
            }
            return false;
        });
    }

    /* ==================== Usuario / metas desde Firebase ==================== */

    private void loadUserData(String uid) {
        mDatabase.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!isAdded()) return;

                        if (!snap.exists()) { Log.w(TAG, "snapshot vacío"); return; }
                        user = snap.getValue(User.class);
                        if (user == null) { Log.w(TAG, "user==null"); return; }

                        DatabaseReference kcalRef = mDatabase.child("users").child(uid).child("caloriesToday");
                        kcalRef.get().addOnSuccessListener(calSnap -> {
                            if (!isAdded()) return;

                            double kcalToday = calSnap.exists()
                                    ? calSnap.getValue(Double.class)
                                    : user.calculateCalories(
                                    user.getWeight(), user.getHeight(), user.getAge(),
                                    user.getGender(), user.getActivityLevel(), user.getGoal());

                            if (!calSnap.exists()) {
                                kcalRef.setValue(kcalToday);
                                Log.i(TAG, "caloriesToday creado → " + kcalToday);
                            }

                            boolean loadedPrefs = loadGoalsFromPrefs();
                            if (!loadedPrefs) {
                                goalsKcal = kcalToday;
                                goalsP    = user.calculateProtein();
                                goalsF    = user.calculateFat();
                                goalsC    = user.calculateCarbs();
                                saveGoalsToPrefs();
                            } else {
                                if (Math.abs(kcalToday - goalsKcal) >= 10) {
                                    double factor = kcalToday / (goalsP * 4 + goalsC * 4 + goalsF * 9);
                                    goalsKcal = kcalToday;
                                    goalsP = Math.round(goalsP * factor);
                                    goalsC = Math.round(goalsC * factor);
                                    goalsF = Math.round(goalsF * factor);
                                    saveGoalsToPrefs();
                                }
                            }

                            updateMacrosUI();
                            recalculateDayTotals();
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError err) {
                        Log.e(TAG, "[loadUserData] Firebase error: " + err.getMessage());
                    }
                });
    }

    /* ==================== Cambiar de fecha ==================== */

    private void switchDate(LocalDate newDate) {
        guardarComidasDelDia(currentDate);
        currentDate = newDate;
        txtCurrentDate.setText(currentDate.format(DISPLAY_FMT));
        loadFoodBlocksForDate(currentDate);
    }

    /* ==================== Guardar / Cargar ==================== */

    public void guardarComidasDelDia(LocalDate date) {
        if (!isAdded() || getContext() == null) return;
        if (foodBlockList == null) return;

        SharedPreferences.Editor ed = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit();

        String json = GSON.toJson(foodBlockList);
        ed.putString(keyFood(date), json);

        Totales t = calcularTotalesActuales();
        ed.putFloat(keyTotK(date), (float) t.kcal);
        ed.putFloat(keyTotP(date), (float) t.prot);
        ed.putFloat(keyTotF(date), (float) t.fat);
        ed.putFloat(keyTotC(date), (float) t.carb);
        ed.apply();

        Log.d(TAG, String.format(Locale.getDefault(),
                "[guardarComidasDelDia] %s bloques=%d  K=%.1f P=%.1f C=%.1f F=%.1f",
                date, foodBlockList.size(), t.kcal, t.prot, t.carb, t.fat));
    }

    private void loadFoodBlocksForDate(LocalDate date) {
        if (!isAdded()) return;

        dataLoaded = false;

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(keyFood(date), null);

        foodBlockList.clear();
        boolean debeGuardar = false;

        if (json == null) {
            Log.d(TAG, "[loadFoodBlocksForDate] " + date + " sin datos → bloques por defecto");
            for (String def : DEFAULT_FOOD_NAMES) foodBlockList.add(new FoodBlock(def));
            debeGuardar = true;
        } else {
            try {
                Type type = new TypeToken<List<FoodBlock>>() {}.getType();
                List<FoodBlock> loaded = GSON.fromJson(json, type);
                if (loaded != null) foodBlockList.addAll(loaded);
            } catch (Exception e) {
                Log.e(TAG, "[loadFoodBlocksForDate] JSON inválido → día reiniciado", e);
                for (String def : DEFAULT_FOOD_NAMES) foodBlockList.add(new FoodBlock(def));
                debeGuardar = true;
            }

            // Migración de recetas antiguas
            for (FoodBlock b : foodBlockList) {
                for (FoodItem it : b.getItems()) {
                    if (!it.isRecipe()
                            && it.getIngredients() != null
                            && !it.getIngredients().isEmpty()) {
                        it.setRecipe(true);
                        debeGuardar = true;
                    }
                }
            }

            // Asegurar bloques básicos
            if (foodBlockList.size() < DEFAULT_FOOD_NAMES.length) {
                Set<String> existentes = new HashSet<>();
                for (FoodBlock fb : foodBlockList) {
                    existentes.add(fb.getTitle().trim().toLowerCase());
                }
                for (String def : DEFAULT_FOOD_NAMES) {
                    if (!existentes.contains(def.trim().toLowerCase())) {
                        foodBlockList.add(new FoodBlock(def));
                        debeGuardar = true;
                    }
                }
            }
        }

        if (debeGuardar) guardarComidasDelDia(date);

        dataLoaded = true;

        if (foodBlockAdapter != null) foodBlockAdapter.notifyDataSetChanged();
        recalculateDayTotals();
    }

    private void cleanupOldEntries() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || !isAdded()) return;

        String uid = u.getUid();
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        LocalDate today = LocalDate.now();

        String prefix = "food_" + uid + "_";
        for (String k : all.keySet()) {
            if (!k.startsWith(prefix)) continue;
            String dateStr = k.substring(prefix.length());
            try {
                LocalDate d = LocalDate.parse(dateStr, DATE_FMT);
                if (ChronoUnit.DAYS.between(d, today) > 365) {
                    prefs.edit().remove(k).apply();
                    Log.d(TAG, "[cleanupOldEntries] Entrada eliminada: " + k);
                }
            } catch (Exception e) {
                Log.w(TAG, "[cleanupOldEntries] Fecha inválida en clave: " + k, e);
            }
        }
    }

    /** MIGRACIÓN: recorre todas las claves food_* (con/sin UID) y marca como receta
     *  cualquier FoodItem que tenga ingredientes pero recipe=false. */
    private void migrateAllSavedDays() {
        if (!isAdded()) return;
        SharedPreferences p = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = p.getAll();
        for (String k : all.keySet()) {
            if (!k.startsWith("food_")) continue; // incluye legacy y con UID
            Object raw = all.get(k);
            if (!(raw instanceof String)) continue;
            String json = (String) raw;
            if (json == null) continue;

            try {
                Type t = new TypeToken<List<FoodBlock>>() {}.getType();
                List<FoodBlock> blocks = GSON.fromJson(json, t);
                boolean changed = false;

                if (blocks != null) {
                    for (FoodBlock b : blocks) {
                        if (b == null || b.getItems() == null) continue;
                        for (FoodItem it : b.getItems()) {
                            if (!it.isRecipe()
                                    && it.getIngredients() != null
                                    && !it.getIngredients().isEmpty()) {
                                it.setRecipe(true);
                                changed = true;
                            }
                        }
                    }
                }

                if (changed) {
                    p.edit().putString(k, GSON.toJson(blocks)).apply();
                    Log.i(TAG, "⚙️  Día migrado → " + k);
                }
            } catch (Exception e) {
                Log.e(TAG, "[migrateAllSavedDays] error parseando " + k, e);
            }
        }
    }

    /* ==================== Diálogo de macros ==================== */

    private void showEditMacrosDialog() {
        ContextThemeWrapper m3Ctx = new ContextThemeWrapper(
                requireContext(), R.style.ThemeOverlay_Diets_Material3Bridge);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(m3Ctx);
        View content = LayoutInflater.from(m3Ctx).inflate(R.layout.dialog_edit_macros, null, false);
        builder.setView(content);
        AlertDialog dlg = builder.create();

        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextInputEditText eKcal = content.findViewById(R.id.inpKcal);
        TextInputEditText ePro  = content.findViewById(R.id.inpProt);
        TextInputEditText eCarb = content.findViewById(R.id.inpCarb);
        TextInputEditText eFat  = content.findViewById(R.id.inpFat);
        Button btnCancel = content.findViewById(R.id.btnCancel);
        Button btnApply  = content.findViewById(R.id.btnApply);

        eKcal.setText(String.valueOf((int) goalsKcal));
        ePro .setText(String.valueOf((int) goalsP));
        eCarb.setText(String.valueOf((int) goalsC));
        eFat .setText(String.valueOf((int) goalsF));

        btnCancel.setOnClickListener(v -> dlg.dismiss());

        btnApply.setOnClickListener(v -> {
            boolean hasError = false;

            double newKcal = -1;
            try {
                String kcalStr = eKcal.getText().toString().trim();
                if (kcalStr.isEmpty()) throw new Exception();
                newKcal = Double.parseDouble(kcalStr);
                if (newKcal <= 0 || Double.isNaN(newKcal)) throw new Exception();
                eKcal.setError(null);
            } catch (Exception ex) {
                eKcal.setError("Valor inválido");
                hasError = true;
            }

            Double newP = parseOrNull(ePro);
            if (newP != null && (newP <= 0 || Double.isNaN(newP))) { ePro.setError("Valor inválido"); hasError = true; }
            else ePro.setError(null);

            Double newC = parseOrNull(eCarb);
            if (newC != null && (newC <= 0 || Double.isNaN(newC))) { eCarb.setError("Valor inválido"); hasError = true; }
            else eCarb.setError(null);

            Double newF = parseOrNull(eFat);
            if (newF != null && (newF <= 0 || Double.isNaN(newF))) { eFat.setError("Valor inválido"); hasError = true; }
            else eFat.setError(null);

            if (hasError) {
                Toast.makeText(getContext(), "Corrige los valores marcados", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newP != null && Math.round(newP) == Math.round(goalsP)) newP = null;
            if (newC != null && Math.round(newC) == Math.round(goalsC)) newC = null;
            if (newF != null && Math.round(newF) == Math.round(goalsF)) newF = null;

            boolean kcalOnly = (newP == null && newC == null && newF == null) &&
                    (Math.round(newKcal) != Math.round(goalsKcal));

            if (!kcalOnly && newP == null && newC == null && newF == null) {
                Toast.makeText(getContext(), "Introduce al menos un macronutriente", Toast.LENGTH_SHORT).show();
                return;
            }

            autoRebalanceMacros(newKcal, newP, newC, newF);
            dlg.dismiss();
        });

        dlg.show();
    }

    private Double parseOrNull(TextInputEditText et) {
        String s = et.getText().toString().trim();
        return s.isEmpty() ? null : Double.parseDouble(s);
    }

    /* ==================== Rebalanceo de macros ==================== */

    private void autoRebalanceMacros(double kcal,
                                     @Nullable Double pGr,
                                     @Nullable Double cGr,
                                     @Nullable Double fGr) {
        // ⓪ – solo cambió la kcal → escalar proporcional respetando rangos
        if (pGr == null && cGr == null && fGr == null) {
            Map<String, MacroRange> range = getRanges(user != null ? user.getGoal() : null);

            double P = goalsP * (kcal / Math.max(1.0, goalsKcal));
            double C = goalsC * (kcal / Math.max(1.0, goalsKcal));
            double F = goalsF * (kcal / Math.max(1.0, goalsKcal));

            P = clamp(P, range.get("P").minPct * kcal / 4, range.get("P").maxPct * kcal / 4);
            C = clamp(C, range.get("C").minPct * kcal / 4, range.get("C").maxPct * kcal / 4);
            F = clamp(F, range.get("F").minPct * kcal / 9, range.get("F").maxPct * kcal / 9);

            goalsKcal = Math.round(kcal);
            goalsP = Math.round(P);
            goalsC = Math.round(C);
            goalsF = Math.round(F);

            barCalories.setMax((int) goalsKcal);
            barProtein .setMax((int) goalsP);
            barCarbs   .setMax((int) goalsC);
            barFat     .setMax((int) goalsF);

            String id = uid();
            if (id != null) {
                mDatabase.child("users").child(id).child("caloriesToday").setValue(goalsKcal);
            }

            saveGoalsToPrefs();
            recalculateDayTotals();
            return;
        }

        // ① – los 3 macros fueron editados → respetar
        if (pGr != null && cGr != null && fGr != null) {
            goalsKcal = Math.round(kcal);
            goalsP = Math.round(pGr);
            goalsC = Math.round(cGr);
            goalsF = Math.round(fGr);
            saveGoalsToPrefs();
            recalculateDayTotals();
            return;
        }

        Map<String, MacroRange> range = getRanges(user != null ? user.getGoal() : null);

        double P = (pGr != null) ? pGr : goalsP;
        double C = (cGr != null) ? cGr : goalsC;
        double F = (fGr != null) ? fGr : goalsF;

        if (pGr != null) {
            double pct = (pGr * 4) / kcal;
            if (pct < range.get("P").minPct) P = range.get("P").minPct * kcal / 4;
            if (pct > range.get("P").maxPct) P = range.get("P").maxPct * kcal / 4;
        }
        if (cGr != null) {
            double pct = (cGr * 4) / kcal;
            if (pct < range.get("C").minPct) C = range.get("C").minPct * kcal / 4;
            if (pct > range.get("C").maxPct) C = range.get("C").maxPct * kcal / 4;
        }
        if (fGr != null) {
            double pct = (fGr * 9) / kcal;
            if (pct < range.get("F").minPct) F = range.get("F").minPct * kcal / 9;
            if (pct > range.get("F").maxPct) F = range.get("F").maxPct * kcal / 9;
        }

        double usedKcal = P * 4 + C * 4 + F * 9;
        double remKcal = kcal - usedKcal;

        if ((pGr != null && cGr != null) || (pGr != null && fGr != null) || (cGr != null && fGr != null)) {
            if (pGr == null)      P = remKcal / 4;
            else if (cGr == null) C = remKcal / 4;
            else                  F = remKcal / 9;
        } else {
            if (cGr == null && fGr == null) { // solo P editado
                double kcalFijas = P * 4;
                double kcalAjustables = kcal - kcalFijas;
                double prev = goalsC * 4 + goalsF * 9;
                if (prev > 0) {
                    double ratioC = (goalsC * 4) / prev;
                    C = (kcalAjustables * ratioC) / 4;
                    F = (kcalAjustables * (1.0 - ratioC)) / 9;
                } else {
                    C = (kcalAjustables * 0.5) / 4;
                    F = (kcalAjustables * 0.5) / 9;
                }
            } else if (pGr == null && fGr == null) { // solo C editado
                double kcalFijas = C * 4;
                double kcalAjustables = kcal - kcalFijas;
                double prev = goalsP * 4 + goalsF * 9;
                if (prev > 0) {
                    double ratioP = (goalsP * 4) / prev;
                    P = (kcalAjustables * ratioP) / 4;
                    F = (kcalAjustables * (1.0 - ratioP)) / 9;
                } else {
                    P = (kcalAjustables * 0.5) / 4;
                    F = (kcalAjustables * 0.5) / 9;
                }
            } else if (pGr == null && cGr == null) { // solo F editado
                double kcalFijas = F * 9;
                double kcalAjustables = kcal - kcalFijas;
                double prev = goalsP * 4 + goalsC * 4;
                if (prev > 0) {
                    double ratioP = (goalsP * 4) / prev;
                    P = (kcalAjustables * ratioP) / 4;
                    C = (kcalAjustables * (1.0 - ratioP)) / 4;
                } else {
                    P = (kcalAjustables * 0.5) / 4;
                    C = (kcalAjustables * 0.5) / 4;
                }
            }
        }

        P = clamp(P, range.get("P").minPct * kcal / 4, range.get("P").maxPct * kcal / 4);
        C = clamp(C, range.get("C").minPct * kcal / 4, range.get("C").maxPct * kcal / 4);
        F = clamp(F, range.get("F").minPct * kcal / 9, range.get("F").maxPct * kcal / 9);

        goalsKcal = Math.round(kcal);
        goalsP = Math.round(P);
        goalsC = Math.round(C);
        goalsF = Math.round(F);

        barCalories.setMax((int) goalsKcal);
        barProtein .setMax((int) goalsP);
        barCarbs   .setMax((int) goalsC);
        barFat     .setMax((int) goalsF);

        String id = uid();
        if (id != null) {
            mDatabase.child("users").child(id).child("caloriesToday").setValue(goalsKcal);
        }
        saveGoalsToPrefs();
        recalculateDayTotals();
    }

    private Map<String, MacroRange> getRanges(@Nullable String goal) {
        String g = goal == null ? "" : goal.toLowerCase();
        Map<String, MacroRange> r = new HashMap<>();
        if (g.contains("ganar")) {             // BULKING
            r.put("P", new MacroRange(0.25, 0.30));
            r.put("F", new MacroRange(0.15, 0.25));
            r.put("C", new MacroRange(0.45, 0.60));
        } else if (g.contains("perder")) {     // CUTTING
            r.put("P", new MacroRange(0.25, 0.35));
            r.put("F", new MacroRange(0.20, 0.30));
            r.put("C", new MacroRange(0.25, 0.45));
        } else {                                // MANTENIMIENTO
            r.put("P", new MacroRange(0.15, 0.25));
            r.put("F", new MacroRange(0.25, 0.30));
            r.put("C", new MacroRange(0.45, 0.55));
        }
        return r;
    }

    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    private static class MacroRange {
        final double minPct, maxPct;
        MacroRange(double min, double max) { this.minPct = min; this.maxPct = max; }
    }

    /* ==================== Borrado de bloques ==================== */

    private void showDeleteBlockDialog(FoodBlock block, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar bloque de comida")
                .setMessage("¿Deseas eliminar este bloque?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    foodBlockList.remove(position);
                    if (foodBlockAdapter != null) foodBlockAdapter.notifyItemRemoved(position);
                    recalculateDayTotals();
                    guardarComidasDelDia(currentDate);
                })
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void removeBlockFromAllMatchingDays(String blockTitle, int position, int dayOfWeek) {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();

        String u = uid();
        if (u == null) return;
        String prefix = "food_" + u + "_";

        Gson gson = GSON;
        for (String key : all.keySet()) {
            if (!key.startsWith(prefix)) continue;
            String dateStr = key.substring(prefix.length()); // yyyy-MM-dd
            try {
                LocalDate date = LocalDate.parse(dateStr, DATE_FMT);
                if (date.getDayOfWeek().getValue() == dayOfWeek) {
                    String json = (String) all.get(key);
                    if (json == null) continue;

                    Type t = new TypeToken<List<FoodBlock>>() {}.getType();
                    List<FoodBlock> blocks = gson.fromJson(json, t);
                    boolean changed = false;

                    if (position >= 0 && position < blocks.size()) {
                        FoodBlock candidate = blocks.get(position);
                        if (candidate.getTitle().equalsIgnoreCase(blockTitle)) {
                            blocks.remove(position);
                            changed = true;
                        }
                    }
                    if (changed) {
                        prefs.edit().putString(key, gson.toJson(blocks)).apply();
                        Log.i(TAG, "[removeBlockFromAllMatchingDays] Eliminado '" + blockTitle + "' en " + key);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "[removeBlockFromAllMatchingDays] Fecha inválida: " + dateStr, e);
            }
        }
    }

    /* ==================== Totales y UI ==================== */

    private static class Totales { double kcal, prot, fat, carb; }

    private Totales calcularTotalesActuales() {
        Totales t = new Totales();
        if (foodBlockList == null || foodBlockList.isEmpty()) return t;

        int blockIdx = 0;
        for (FoodBlock b : foodBlockList) {
            int itemIdx = 0;
            for (FoodItem it : b.getItems()) {
                if (!it.isChecked()) { itemIdx++; continue; }

                double factor   = it.getGrams() / 100.0;
                double thisKcal = it.getKcalPer100g()   * factor;
                double thisProt = it.getProteinPer100g()* factor;
                double thisCarb = it.getCarbsPer100g()  * factor;
                double thisFat  = it.getFatPer100g()    * factor;

                t.kcal += thisKcal;
                t.prot += thisProt;
                t.carb += thisCarb;
                t.fat  += thisFat;
                itemIdx++;
            }
            blockIdx++;
        }
        return t;
    }

    public void recalculateDayTotals() {
        Totales t = calcularTotalesActuales();

        labelCalories.setText(String.format(Locale.getDefault(),
                "Calorías: %.0f / %.0f kcal", t.kcal, goalsKcal));
        labelProtein.setText(String.format(Locale.getDefault(),
                "Proteínas: %.0f / %.0f g", t.prot, goalsP));
        labelFat.setText(String.format(Locale.getDefault(),
                "Grasas: %.0f / %.0f g", t.fat, goalsF));
        labelCarbs.setText(String.format(Locale.getDefault(),
                "Carbohidratos: %.0f / %.0f g", t.carb, goalsC));

        barCalories.setProgress((int) Math.min(t.kcal, goalsKcal));
        barProtein .setProgress((int) Math.min(t.prot, goalsP));
        barFat     .setProgress((int) Math.min(t.fat , goalsF));
        barCarbs   .setProgress((int) Math.min(t.carb, goalsC));

        if (dataLoaded) {
            guardarComidasDelDia(currentDate);
        }
    }

    /* ==================== Recetas ==================== */

    public void showRecipeDialog(FoodSearchFragment.FoodResult recipe) {
        RecipeDialog dialog = RecipeDialog.newInstance(recipe);
        dialog.setOnGramsConfirmedListener(result -> {
            FoodItem item = new FoodItem(
                    result.name,
                    result.grams,
                    result.kcalPer100g,
                    result.protein,
                    result.carbs,
                    result.fat,
                    result.imageUrl,
                    result.isRecipe()
            );
            item.setIngredients(result.ingredients);
            item.setInstructions(result.instructions);
            item.setChecked(true);

            if (!foodBlockList.isEmpty()) {
                foodBlockList.get(0).addItem(item);
                if (foodBlockAdapter != null) foodBlockAdapter.notifyItemChanged(0);
                guardarComidasDelDia(currentDate);
                recalculateDayTotals();
            } else {
                Toast.makeText(getContext(), "No hay bloques de comida", Toast.LENGTH_SHORT).show();
            }
        });
        dialog.show(getParentFragmentManager(), "RecipeDialog");
    }
}
