package com.example.diets.fragments;

import android.app.DatePickerDialog;
import android.content.Context;
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
import androidx.appcompat.app.AlertDialog;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.FoodBlockAdapter;
import com.example.diets.dialogs.RecipeDialog;
import com.example.diets.model.FoodBlock;
import com.example.diets.model.FoodItem;
import com.example.diets.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
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
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault());

    // Bloques por defecto
    private static final String[] DEFAULT_FOOD_NAMES =
            {"Desayuno", "Almuerzo", "Comida", "Snack", "Cena"};

    // Estado/carga
    private boolean dataLoaded = false;
    private LocalDate currentDate = LocalDate.now();

    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    // --- MODO COACH ---
    private boolean isCoachMode = false;
    private String effectiveUid;
    // --- FIN ---

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
                if (key == null || effectiveUid == null) return;
                String pfx = effectiveUid + "_";
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
        if (ctx instanceof OnNavigationItemSelectedListener) {
            mListener = (OnNavigationItemSelectedListener) ctx;
        } else {
            Log.w(TAG, ctx + " does not implement OnNavigationItemSelectedListener");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (goalPrefs != null) {
            goalPrefs.unregisterOnSharedPreferenceChangeListener(goalListener);
        }
    }

    // --- onCreate: determina UID efectivo ---
    @Override
    public void onCreate(@Nullable Bundle saved) {
        super.onCreate(saved);
        Log.d(TAG, "[onCreate] Iniciando DashboardFragment. Determinando UID efectivo...");

        mAuth = FirebaseAuth.getInstance();
        String loggedInUserUid = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getUid() : null;

        Bundle args = getArguments();
        if (args != null) {
            isCoachMode = args.getBoolean("coachMode", false);
            if (isCoachMode) {
                effectiveUid = args.getString("targetUid");
                Log.i(TAG, "MODO COACH activado. UID efectivo: " + effectiveUid);
            } else {
                effectiveUid = loggedInUserUid;
                Log.i(TAG, "MODO USUARIO (args presentes, coachMode=false). UID efectivo: " + effectiveUid);
            }
        } else {
            isCoachMode = false;
            effectiveUid = loggedInUserUid;
            Log.i(TAG, "MODO USUARIO (sin args). UID efectivo: " + effectiveUid);
        }

        if (effectiveUid == null) {
            Log.e(TAG, "¡ERROR FATAL! effectiveUid es null. LoggedInUID: " + (loggedInUserUid != null ? loggedInUserUid : "null") + " CoachMode: " + isCoachMode);
            if (isAdded()) Toast.makeText(getContext(), "Error: No se pudo determinar el usuario.", Toast.LENGTH_LONG).show();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup cont,
                             @Nullable Bundle saved) {
        Log.d(TAG, "[onCreateView] Inflating layout R.layout.activity_food");

        if (goalPrefs == null && getContext() != null) {
            try {
                goalPrefs = requireContext().getSharedPreferences(PREF_GOALS, Context.MODE_PRIVATE);
                goalPrefs.registerOnSharedPreferenceChangeListener(goalListener);
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error getting SharedPreferences in onCreateView", e);
            }
        }

        return inf.inflate(R.layout.activity_food, cont, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
        super.onViewCreated(v, saved);
        Log.d(TAG, "[onViewCreated] View creada. Iniciando limpieza, migración y carga de datos...");

        if (getContext() == null) {
            Log.e(TAG, "Contexto nulo en onViewCreated, saliendo.");
            return;
        }

        cleanupOldEntries();
        migrateAllSavedDays();

        mDatabase = FirebaseDatabase.getInstance().getReference();

        bindUI(v);

        if (effectiveUid != null) {
            loadGoalsFromPrefs();
            updateMacrosUI();
            loadFoodBlocksForDate(currentDate);
            recalculateDayTotals();
            loadUserData(effectiveUid);
        } else {
            Log.e(TAG, "[onViewCreated] effectiveUid es null. No se cargan datos principales.");
            Toast.makeText(getContext(), "Error al cargar datos del usuario.", Toast.LENGTH_SHORT).show();
        }

        setupDateNavigation();
        setupBottomNav(v);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (effectiveUid != null) {
            Log.d(TAG, "[onPause] Guardando estado actual del día " + currentDate + " para UID: " + effectiveUid);
            guardarComidasDelDia(currentDate);
        } else {
            Log.w(TAG, "[onPause] effectiveUid es null, no se guardan datos del día.");
        }
    }

    /* ==================== Helpers de UID y claves ==================== */


    @Nullable
    private String uid() { return effectiveUid; }

    // NUEVOS MÉTODOS: Obtienen el nombre del archivo de SharedPreferences con el UID
    private String getFoodPrefsFileName() {
        String u = uid();
        if (u == null) {
            Log.e(TAG, "¡UID nulo al generar nombre de archivo de FoodPrefs!");
            // Nombre de fallback para evitar fallos si el UID es null (aunque es un error)
            return PREFS_NAME + "_error";
        }
        return PREFS_NAME + "_" + u;
    }

    private String getGoalsPrefsFileName() {
        String u = uid();
        if (u == null) {
            Log.e(TAG, "¡UID nulo al generar nombre de archivo de GoalsPrefs!");
            return PREF_GOALS + "_error";
        }
        return PREF_GOALS + "_" + u;
    }
    private String keyFood(LocalDate d) {
        String u = uid();
        if (u == null) {
            Log.e(TAG, "¡UID nulo al generar keyFood!");
            return "food_error_" + DATE_FMT.format(d);
        }
        return "food_" + u + "_" + DATE_FMT.format(d);
    }
    private String keyTotK(LocalDate d) { String u = uid(); if (u==null) return "totK_error_"+DATE_FMT.format(d); return "totK_" + u + "_" + DATE_FMT.format(d); }
    private String keyTotP(LocalDate d) { String u = uid(); if (u==null) return "totP_error_"+DATE_FMT.format(d); return "totP_" + u + "_" + DATE_FMT.format(d); }
    private String keyTotC(LocalDate d) { String u = uid(); if (u==null) return "totC_error_"+DATE_FMT.format(d); return "totC_" + u + "_" + DATE_FMT.format(d); }
    private String keyTotF(LocalDate d) { String u = uid(); if (u==null) return "totF_error_"+DATE_FMT.format(d); return "totF_" + u + "_" + DATE_FMT.format(d); }

    /* ==================== Metas (load/save/UI) ==================== */

    private void saveGoalsToPrefs() {
        if (effectiveUid == null || !isAdded() || getContext() == null) return;

        Log.d(TAG, "Guardando metas en SharedPreferences (UID=" + effectiveUid + "). Kcal: " + goalsKcal);

        SharedPreferences.Editor ed;
        try {
            // 🛑 CORRECCIÓN: Usar getGoalsPrefsFileName()
            // NO usar PREF_GOALS aquí, o se guardará en el archivo equivocado.
            ed = requireContext().getSharedPreferences(getGoalsPrefsFileName(), Context.MODE_PRIVATE).edit();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting SharedPreferences editor", e);
            return;
        }

        String prefix = effectiveUid + "_";
        ed.putFloat(prefix + K_KCAL, (float) goalsKcal);
        ed.putFloat(prefix + K_PROT, (float) goalsP);
        ed.putFloat(prefix + K_FAT , (float) goalsF);
        ed.putFloat(prefix + K_CARB, (float) goalsC);
        ed.apply();

        Log.d(TAG, "Metas guardadas exitosamente en: " + getGoalsPrefsFileName());
    }

    private void updateMacrosUI() {
        if (!isAdded() || barCalories == null || labelCalories == null || goalsKcal < 0) {
            if (labelCalories != null) labelCalories.setText("Calorías: Cargando...");
            return;
        }


        barCalories.setMax((int) Math.max(1, goalsKcal));
        barProtein .setMax((int) Math.max(1, goalsP));
        barCarbs   .setMax((int) Math.max(1, goalsC));
        barFat     .setMax((int) Math.max(1, goalsF));

        Totales t = calcularTotalesActuales();

        labelCalories.setText(String.format(Locale.getDefault(),
                "Calorías: %.0f / %.0f kcal", t.kcal, goalsKcal));
        labelProtein .setText(String.format(Locale.getDefault(),
                "Proteínas: %.0f / %.0f g",  t.prot, goalsP));
        labelCarbs   .setText(String.format(Locale.getDefault(),
                "Carbohidratos: %.0f / %.0f g", t.carb, goalsC));
        labelFat     .setText(String.format(Locale.getDefault(),
                "Grasas: %.0f / %.0f g", t.fat, goalsF));

        barCalories.setProgress((int) Math.min(Math.max(0, t.kcal), goalsKcal));
        barProtein .setProgress((int) Math.min(Math.max(0, t.prot), goalsP));
        barCarbs   .setProgress((int) Math.min(Math.max(0, t.carb), goalsC));
        barFat     .setProgress((int) Math.min(Math.max(0, t.fat) , goalsF));
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
                effectiveUid, // ✅ NUEVO PARÁMETRO: Pasamos el UID del usuario
                (block, position) -> showDeleteBlockDialog(block, position)
        );
        recyclerViewFoodBlocks.setAdapter(foodBlockAdapter);

        v.findViewById(R.id.addFoodButton).setOnClickListener(view -> {
            if (foodBlockList.size() >= MAX_FOOD_BLOCKS) {
                Toast.makeText(getContext(), "Máximo de bloques de comida alcanzado.", Toast.LENGTH_SHORT).show();
                return;
            }
            FoodBlock newBlock = new FoodBlock("Nueva Comida " + (foodBlockList.size() + 1));
            foodBlockList.add(newBlock);
            if (foodBlockAdapter != null) {
                foodBlockAdapter.notifyItemInserted(foodBlockList.size() - 1);
                recyclerViewFoodBlocks.smoothScrollToPosition(foodBlockList.size() - 1);
            }
            guardarComidasDelDia(currentDate);
        });

        txtCurrentDate.setOnClickListener(v1 -> {
            if (getContext() == null) return;

            LocalDate date = currentDate;
            DatePickerDialog dpd = new DatePickerDialog(
                    requireContext(),
                    (view, year, month, dayOfMonth) -> {
                        LocalDate pickedDate = LocalDate.of(year, month + 1, dayOfMonth);
                        if (!pickedDate.equals(currentDate)) {
                            switchDate(pickedDate);
                        }
                    },
                    date.getYear(),
                    date.getMonthValue() - 1,
                    date.getDayOfMonth()
            );
            dpd.show();
        });

        labelCalories.setOnClickListener(v12 -> showEditMacrosDialog());
    }

    private void setupDateNavigation() {
        btnPrevDay.setOnClickListener(v -> {
            switchDate(currentDate.minusDays(1));
        });

        btnNextDay.setOnClickListener(v -> {
            LocalDate tomorrow = currentDate.plusDays(1);
            if (ChronoUnit.DAYS.between(LocalDate.now(), tomorrow) <= MAX_DAYS_IN_FUTURE) {
                switchDate(tomorrow);
            } else {
                Toast.makeText(getContext(), "No puedes planificar comidas tan a futuro.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupBottomNav(View v) {
        BottomNavigationView bottomNav = v.findViewById(R.id.bottom_navigation);
        if (bottomNav == null) return;

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            ViewPagerActivity activity = (getActivity() instanceof ViewPagerActivity) ? (ViewPagerActivity) getActivity() : null;
            if (activity == null) return false;

            if (itemId == R.id.navigation_home) {
                if (mListener != null) {
                    mListener.onNavigationItemSelected(1);
                    return true;
                }
                return false;
            } else if (itemId == R.id.navigation_dashboard) {
                activity.switchToWeightFragment();
                return true;
            } else {
                return false;
            }
        });
    }

    /* ==================== Usuario / metas desde Firebase ==================== */

    private void loadUserData(String uidToLoad) {
        if (!isAdded()) return;
        Log.d(TAG, "loadUserData: Solicitando datos a Firebase para UID: " + uidToLoad);

        mDatabase.child("users").child(uidToLoad)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        if (!isAdded() || getContext() == null) return;

                        if (!userSnapshot.exists()) {
                            Log.e(TAG, "loadUserData: Snapshot no existe para el usuario.");
                            Toast.makeText(getContext(), "Error: No se encontraron los datos del usuario.", Toast.LENGTH_LONG).show();
                            return;
                        }

                        user = userSnapshot.getValue(User.class);
                        if (user == null) {
                            Log.e(TAG, "loadUserData: El objeto User es null tras parsear.");
                            Toast.makeText(getContext(), "Error al procesar datos del usuario.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        Log.i(TAG, "Datos de usuario cargados. Peso: " + user.getWeight() + " Meta: " + user.getGoal());

                        DatabaseReference kcalRef = mDatabase.child("users").child(uidToLoad).child("caloriesToday");
                        kcalRef.get().addOnSuccessListener(kcalSnapshot -> {
                            if (!isAdded() || getContext() == null) return;

                            // 1. Cálculo de respaldo (por si no hay nada en Firebase)
                            double calculatedKcalToday;
                            if (user.getWeight() <= 0 || user.getHeight() <= 0 || user.getAge() <= 0) {
                                calculatedKcalToday = 0;
                            } else {
                                calculatedKcalToday = user.calculateCalories(
                                        user.getWeight(), user.getHeight(), user.getAge(),
                                        user.getGender(), user.getActivityLevel(), user.getGoal()
                                );
                            }

                            // 2. Obtener valor de Firebase
                            double kcalTodayValue = kcalSnapshot.exists()
                                    ? kcalSnapshot.getValue(Double.class)
                                    : calculatedKcalToday;

                            Log.d(TAG, "CaloriesToday de Firebase: " + (kcalSnapshot.exists() ? kcalTodayValue : "No existe (usando calc: " + calculatedKcalToday + ")"));

                            // Si no existía en Firebase, lo creamos
                            if (!kcalSnapshot.exists() && calculatedKcalToday > 0) {
                                Log.d(TAG, "Estableciendo caloriesToday inicial en Firebase.");
                                kcalRef.setValue(kcalTodayValue);
                            }

                            // Intentamos cargar lo que hay guardado localmente
                            boolean prefsExistentes = loadGoalsFromPrefs();
                            Log.d(TAG, "Prefs locales existentes: " + prefsExistentes + " | Meta actual en RAM: " + goalsKcal);

                            // REGLA: Solo sobrescribimos con datos de Firebase si NO hay datos locales
                            // o si los datos locales están vacíos/corruptos (goalsKcal <= 0).
                            if (!prefsExistentes || goalsKcal <= 0) {

                                goalsKcal = kcalTodayValue;
                                goalsP    = user.calculateProtein();
                                goalsF    = user.calculateFat();
                                goalsC    = user.calculateCarbs();

                                // Ajuste de escala si los macros no suman las kcal totales
                                double mk = goalsP*4 + goalsC*4 + goalsF*9;
                                if (kcalTodayValue > 0 && mk > 0 && Math.abs(mk - kcalTodayValue) > 10) {
                                    double f = kcalTodayValue / mk;
                                    Log.d(TAG, "Escalando macros iniciales por factor: " + f);
                                    goalsP = Math.round(goalsP * f);
                                    goalsC = Math.round(goalsC * f);
                                    goalsF = Math.round(goalsF * f);
                                }

                                // Guardamos inmediatamente para que la próxima vez 'prefsExistentes' sea true
                                saveGoalsToPrefs();
                            } else {
                                // Si ya existen preferencias, NO hacemos nada. Respetamos la edición manual del usuario.
                            }

                            // 4. Actualizar UI final
                            updateMacrosUI();
                            recalculateDayTotals();

                        }).addOnFailureListener(e -> {
                            Log.e(TAG, "[loadUserData] Error al obtener caloriesToday de Firebase", e);
                            // En caso de error de red, intentamos mostrar lo local si existe
                            loadGoalsFromPrefs();
                            updateMacrosUI();
                            recalculateDayTotals();
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (!isAdded()) return;
                        Log.e(TAG, "loadUserData Cancelled: " + databaseError.getMessage());
                        Toast.makeText(getContext(), "Error al cargar perfil: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                        // Fallback seguro
                        loadGoalsFromPrefs();
                        updateMacrosUI();
                        recalculateDayTotals();
                    }
                });
    }

    /* ==================== Cambiar de fecha ==================== */

    private void switchDate(LocalDate newDate) {
        if (effectiveUid != null) {
            guardarComidasDelDia(currentDate);
        }
        currentDate = newDate;

        if (txtCurrentDate != null) {
            txtCurrentDate.setText(currentDate.format(DISPLAY_FMT));
        }

        if (effectiveUid != null) {
            loadFoodBlocksForDate(currentDate);
        } else {
            foodBlockList.clear();
            if (foodBlockAdapter != null) foodBlockAdapter.notifyDataSetChanged();
            recalculateDayTotals();
        }
    }

    /* ==================== Guardar / Cargar ==================== */

    public void guardarComidasDelDia(LocalDate date) {
        if (!isAdded() || getContext() == null) return;
        if (foodBlockList == null) return;


        SharedPreferences.Editor ed;
        try {
            ed = requireContext().getSharedPreferences(getFoodPrefsFileName(), Context.MODE_PRIVATE).edit();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting SharedPreferences editor in guardarComidasDelDia", e);
            return;
        }

        String json = GSON.toJson(foodBlockList);
        ed.putString(keyFood(date), json);

        Totales t = calcularTotalesActuales();
        ed.putFloat(keyTotK(date), (float) t.kcal);
        ed.putFloat(keyTotP(date), (float) t.prot);
        ed.putFloat(keyTotF(date), (float) t.fat);
        ed.putFloat(keyTotC(date), (float) t.carb);

        ed.apply();
    }

    private boolean loadGoalsFromPrefs() {
        if (!isAdded() || getContext() == null) return false;
        if (effectiveUid == null) {
            goalsKcal = 0; goalsP = 0; goalsF = 0; goalsC = 0;
            return false;
        }

        SharedPreferences p;
        try {
            // 🛑 ¡CORRECCIÓN AQUÍ!
            // Usa el método que genera el nombre de archivo ÚNICO por UID
            p = requireContext().getSharedPreferences(getGoalsPrefsFileName(), Context.MODE_PRIVATE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting SharedPreferences in loadGoalsFromPrefs", e);
            return false;
        }

        String prefix = effectiveUid + "_";
        if (!p.contains(prefix + K_KCAL)) {
            goalsKcal = 0; goalsP = 0; goalsF = 0; goalsC = 0;
            return false;
        }

        goalsKcal = p.getFloat(prefix + K_KCAL, 0f);
        goalsP    = p.getFloat(prefix + K_PROT, 0f);
        goalsF    = p.getFloat(prefix + K_FAT , 0f);
        goalsC    = p.getFloat(prefix + K_CARB, 0f);

        return true;
    }

    private void loadFoodBlocksForDate(LocalDate date) {
        if (!isAdded() || getContext() == null) return;

        dataLoaded = false;

        SharedPreferences prefs;
        try {
            prefs = requireContext().getSharedPreferences(getFoodPrefsFileName(), Context.MODE_PRIVATE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting SharedPreferences in loadFoodBlocksForDate", e);
            foodBlockList.clear();
            if (foodBlockAdapter != null) foodBlockAdapter.notifyDataSetChanged();
            recalculateDayTotals();
            return;
        }

        String json = prefs.getString(keyFood(date), null);

        foodBlockList.clear();
        boolean debeGuardarCambios = false;

        if (json == null || json.isEmpty() || json.equals("[]")) {
            for (String defaultName : DEFAULT_FOOD_NAMES) {
                foodBlockList.add(new FoodBlock(defaultName));
            }
            debeGuardarCambios = true;
        } else {
            try {
                Type listType = new TypeToken<ArrayList<FoodBlock>>() {}.getType();
                List<FoodBlock> loadedBlocks = GSON.fromJson(json, listType);

                if (loadedBlocks != null && !loadedBlocks.isEmpty()) {
                    foodBlockList.addAll(loadedBlocks);

                    for (FoodBlock block : foodBlockList) {
                        if (block.getItems() != null) {
                            for (FoodItem item : block.getItems()) {
                                if (!item.isRecipe() && item.getIngredients() != null && !item.getIngredients().isEmpty()) {
                                    item.setRecipe(true);
                                    debeGuardarCambios = true;
                                }
                            }
                        } else {
                            // Requiere que FoodBlock tenga setItems(List<FoodItem>)
                            block.setItems(new ArrayList<FoodItem>());
                            debeGuardarCambios = true;
                        }
                    }

                    if (foodBlockList.size() < DEFAULT_FOOD_NAMES.length) {
                        Set<String> currentTitles = new HashSet<>();
                        for (FoodBlock block : foodBlockList) {
                            if (block.getTitle() != null)
                                currentTitles.add(block.getTitle().trim().toLowerCase());
                        }
                        for (String defaultName : DEFAULT_FOOD_NAMES) {
                            if (!currentTitles.contains(defaultName.trim().toLowerCase())) {
                                foodBlockList.add(new FoodBlock(defaultName));
                                debeGuardarCambios = true;
                            }
                        }
                    }

                } else {
                    for (String defaultName : DEFAULT_FOOD_NAMES) foodBlockList.add(new FoodBlock(defaultName));
                    debeGuardarCambios = true;
                }

            } catch (Exception e) {
                Log.e(TAG, "[loadFoodBlocksForDate] Error al parsear JSON. Usando bloques por defecto.", e);
                foodBlockList.clear();
                for (String defaultName : DEFAULT_FOOD_NAMES) foodBlockList.add(new FoodBlock(defaultName));
                debeGuardarCambios = true;
            }
        }

        if (debeGuardarCambios) {
            guardarComidasDelDia(date);
        }

        dataLoaded = true;

        if (foodBlockAdapter != null) {
            foodBlockAdapter.notifyDataSetChanged();
        }
        recalculateDayTotals();
    }

    private void cleanupOldEntries() {
        if (effectiveUid == null || !isAdded() || getContext() == null) return;

        SharedPreferences prefs;
        try {
            prefs = requireContext().getSharedPreferences(getFoodPrefsFileName(), Context.MODE_PRIVATE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting SharedPreferences in cleanupOldEntries", e);
            return;
        }

        Map<String, ?> allEntries = prefs.getAll();
        LocalDate today = LocalDate.now();
        SharedPreferences.Editor editor = prefs.edit();
        boolean removedAny = false;

        String foodPrefix = "food_" + effectiveUid + "_";
        String totKPrefix = "totK_" + effectiveUid + "_";
        String totPPrefix = "totP_" + effectiveUid + "_";
        String totCPrefix = "totC_" + effectiveUid + "_";
        String totFPrefix = "totF_" + effectiveUid + "_";

        int count = 0; // Variable local para log
        for (String key : allEntries.keySet()) {
            String dateStr = null;
            if (key.startsWith(foodPrefix)) dateStr = key.substring(foodPrefix.length());
            else if (key.startsWith(totKPrefix)) dateStr = key.substring(totKPrefix.length());
            else if (key.startsWith(totPPrefix)) dateStr = key.substring(totPPrefix.length());
            else if (key.startsWith(totCPrefix)) dateStr = key.substring(totCPrefix.length());
            else if (key.startsWith(totFPrefix)) dateStr = key.substring(totFPrefix.length());

            if (dateStr != null) {
                try {
                    LocalDate entryDate = LocalDate.parse(dateStr, DATE_FMT);
                    long daysBetween = ChronoUnit.DAYS.between(entryDate, today);
                    if (daysBetween > 365) {
                        editor.remove(key);
                        removedAny = true;
                        count++;
                    }
                } catch (Exception e) {
                    Log.w(TAG, "[cleanupOldEntries] No se pudo parsear fecha en clave: " + key, e);
                }
            }
        }

        if (removedAny) {
            editor.apply();
        } else {
        }
    }

    /** MIGRACIÓN: Marca como receta items antiguos que tengan ingredientes. */
    private void migrateAllSavedDays() {
        if (!isAdded() || getContext() == null) return;

        SharedPreferences p;
        try {
            p = requireContext().getSharedPreferences(getFoodPrefsFileName(), Context.MODE_PRIVATE);
        } catch (IllegalStateException e) {
            Log.e(TAG, "Error getting SharedPreferences in migrateAllSavedDays", e);
            return;
        }

        Map<String, ?> allEntries = p.getAll();
        SharedPreferences.Editor editor = p.edit();
        boolean madeChanges = false;

        for (String key : allEntries.keySet()) {
            if (!key.startsWith("food_")) continue;

            Object rawValue = allEntries.get(key);
            if (!(rawValue instanceof String)) continue;
            String json = (String) rawValue;
            if (json == null || json.isEmpty()) continue;

            try {
                Type listType = new TypeToken<ArrayList<FoodBlock>>() {}.getType();
                List<FoodBlock> blocks = GSON.fromJson(json, listType);
                boolean changedInThisKey = false;

                if (blocks != null) {
                    for (FoodBlock block : blocks) {
                        if (block == null || block.getItems() == null) continue;
                        for (FoodItem item : block.getItems()) {
                            if (item == null) continue;
                            if (!item.isRecipe()
                                    && item.getIngredients() != null
                                    && !item.getIngredients().isEmpty()) {
                                item.setRecipe(true);
                                changedInThisKey = true;
                            }
                        }
                    }
                }

                if (changedInThisKey) {
                    String updatedJson = GSON.toJson(blocks);
                    editor.putString(key, updatedJson);
                    madeChanges = true;
                }

            } catch (Exception e) {
                Log.e(TAG, "[migrateAllSavedDays] Error procesando/parseando JSON para clave " + key, e);
            }
        }

        if (madeChanges) {
            editor.apply();
        }
    }

    /* ==================== Diálogo de macros ==================== */

    private void showEditMacrosDialog() {
        if (getContext() == null) return;

        ContextThemeWrapper themedContext = new ContextThemeWrapper(
                requireContext(), R.style.ThemeOverlay_Diets_Material3Bridge);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(themedContext);
        View dialogView = LayoutInflater.from(themedContext).inflate(R.layout.dialog_edit_macros, null);
        builder.setView(dialogView);

        final AlertDialog dialog = builder.create(); // usa la de androidx

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextInputEditText etKcal = dialogView.findViewById(R.id.inpKcal);
        TextInputEditText etProt = dialogView.findViewById(R.id.inpProt);
        TextInputEditText etCarb = dialogView.findViewById(R.id.inpCarb);
        TextInputEditText etFat  = dialogView.findViewById(R.id.inpFat);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnApply  = dialogView.findViewById(R.id.btnApply);

        etKcal.setText(String.format(Locale.US, "%.0f", goalsKcal));
        etProt.setText(String.format(Locale.US, "%.0f", goalsP));
        etCarb.setText(String.format(Locale.US, "%.0f", goalsC));
        etFat.setText(String.format(Locale.US, "%.0f", goalsF));

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            // Límite de aumento (1.10 = 10%)
            final double MAX_CALORIE_INCREASE_FACTOR = 1.10;
            boolean hasInputError = false;

            // --- 1. Parsear todos los valores ---
            double newKcal = -1.0;
            try {
                String kcalStr = etKcal.getText().toString().trim();
                if (kcalStr.isEmpty()) throw new NumberFormatException("Kcal vacío");
                newKcal = Double.parseDouble(kcalStr);
                if (newKcal <= 0 || Double.isNaN(newKcal) || newKcal > 15000) {
                    throw new NumberFormatException("Kcal fuera de rango");
                }
                etKcal.setError(null);
            } catch (NumberFormatException ex) {
                etKcal.setError("Valor inválido (ej: 2000)");
                hasInputError = true;
            }

            // Parseamos lo que hay en las cajas
            Double parsedProt = parseOrNull(etProt);
            Double parsedCarb = parseOrNull(etCarb);
            Double parsedFat  = parseOrNull(etFat);

            if (hasInputError) {
                Toast.makeText(getContext(), "Corrige los valores inválidos.", Toast.LENGTH_SHORT).show();
                return;
            }

            // --- 2. Comprobar qué cambió REALMENTE ---
            boolean kcalChanged = Math.abs(newKcal - goalsKcal) > 0.1;


            // Un macro cambió SÓLO SI el valor parseado (parsedProt)
            // es diferente del valor original guardado (goalsP).
            // También cuenta si el usuario borró el número (parsedProt == null).
            boolean protChanged = (parsedProt != null && Math.abs(parsedProt - goalsP) > 0.1)
                    || (parsedProt == null && goalsP != 0);
            boolean carbChanged = (parsedCarb != null && Math.abs(parsedCarb - goalsC) > 0.1)
                    || (parsedCarb == null && goalsC != 0);
            boolean fatChanged  = (parsedFat  != null && Math.abs(parsedFat - goalsF) > 0.1)
                    || (parsedFat  == null && goalsF != 0);

            // Este booleano AHORA SÍ funciona bien
            boolean onlyKcalChanged = kcalChanged && !protChanged && !carbChanged && !fatChanged;


            // --- 3. Aplicar tu lógica ---
            if (onlyKcalChanged) {
                // ESCENARIO 1: El usuario SÓLO cambió las Kcal

                double maxAllowedKcal = goalsKcal * MAX_CALORIE_INCREASE_FACTOR;

                if (newKcal > maxAllowedKcal && goalsKcal > 0) {
                    // 🛑 ¡AQUÍ ESTÁ TU TOAST! 🛑
                    // Se pasó del 10% de AUMENTO
                    Toast.makeText(getContext(),
                            "El aumento excede el 10%. No se guardó.",
                            Toast.LENGTH_LONG).show();
                    // No cerramos el diálogo, el usuario debe corregir
                    return;

                } else {
                    // Está dentro del rango (o es una disminución)
                    // Guardamos SÓLO las Kcal. Los macros se quedan "tal cual".
                    Log.d(TAG, "Guardando sólo Kcal, macros se quedan 'tal cual'.");
                    goalsKcal = Math.round(newKcal);
                    // No tocamos goalsP, goalsC, goalsF

                    saveGoalsToPrefs();
                    updateMacrosUI();
                    recalculateDayTotals();
                    dialog.dismiss();
                }

            } else {
                // ESCENARIO 2: El usuario cambió macros (o no cambió nada)

                if (!kcalChanged && !protChanged && !carbChanged && !fatChanged) {
                    dialog.dismiss(); // No cambió nada en absoluto
                    return;
                }

                // El usuario SÍ cambió macros, así que usamos el rebalanceo
                Log.d(TAG, "Macros cambiados. Ejecutando rebalanceo automático.");
                autoRebalanceMacros(newKcal, parsedProt, parsedCarb, parsedFat);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private Double parseOrNull(TextInputEditText editText) {
        if (editText == null || editText.getText() == null) return null;
        String text = editText.getText().toString().trim();
        if (text.isEmpty()) return null;
        try {
            double value = Double.parseDouble(text);
            if (Double.isNaN(value) || value < 0) {
                editText.setError("Inválido");
                return null;
            }
            editText.setError(null);
            return value;
        } catch (NumberFormatException e) {
            editText.setError("Inválido");
            return null;
        }
    }

    /* ==================== Rebalanceo de macros ==================== */

    private void autoRebalanceMacros(double targetKcal,
                                     @Nullable Double fixedProtGr,
                                     @Nullable Double fixedCarbGr,
                                     @Nullable Double fixedFatGr) {

        // Caso 0: Solo cambió Kcal -> escalar distribuciones actuales
        if (fixedProtGr == null && fixedCarbGr == null && fixedFatGr == null) {
            double currentTotalKcal = goalsKcal;
            if (currentTotalKcal <= 0) {
                if (user != null) {
                    goalsKcal = Math.round(targetKcal);
                    goalsP = Math.round(user.calculateProtein());
                    goalsC = Math.round(user.calculateCarbs());
                    goalsF = Math.round(user.calculateFat());
                    double mk = goalsP*4 + goalsC*4 + goalsF*9;
                    if (mk > 0) {
                        double f = targetKcal / mk;
                        goalsP = Math.round(goalsP * f);
                        goalsC = Math.round(goalsC * f);
                        goalsF = Math.round(goalsF * f);
                    }
                } else {
                    goalsKcal = Math.round(targetKcal); goalsP = 0; goalsC = 0; goalsF = 0;
                }
            } else {
                double scaleFactor = targetKcal / currentTotalKcal;
                goalsKcal = Math.round(targetKcal);
                goalsP = Math.round(goalsP * scaleFactor);
                goalsC = Math.round(goalsC * scaleFactor);
                goalsF = Math.round(goalsF * scaleFactor);
            }

            saveGoalsToPrefs();
            updateMacrosUI();
            recalculateDayTotals();
            return;
        }

        Map<String, MacroRange> ranges = getRanges(user != null ? user.getGoal() : null);

        double finalP, finalC, finalF;
        double kcalFromFixedMacros = 0;
        if (fixedProtGr != null) kcalFromFixedMacros += fixedProtGr * 4;
        if (fixedCarbGr != null) kcalFromFixedMacros += fixedCarbGr * 4;
        if (fixedFatGr != null) kcalFromFixedMacros += fixedFatGr * 9;

        double remainingKcal = targetKcal - kcalFromFixedMacros;

        if (fixedProtGr != null && fixedCarbGr != null && fixedFatGr != null) {
            finalP = fixedProtGr; finalC = fixedCarbGr; finalF = fixedFatGr;
        } else if ((fixedProtGr != null && fixedCarbGr != null) ||
                (fixedProtGr != null && fixedFatGr  != null) ||
                (fixedCarbGr != null && fixedFatGr  != null)) {
            if (fixedProtGr == null) { // C y F fijos
                finalP = Math.max(0, remainingKcal / 4);
                finalC = fixedCarbGr; finalF = fixedFatGr;
            } else if (fixedCarbGr == null) { // P y F fijos
                finalP = fixedProtGr;
                finalC = Math.max(0, remainingKcal / 4);
                finalF = fixedFatGr;
            } else { // P y C fijos
                finalP = fixedProtGr; finalC = fixedCarbGr;
                finalF = Math.max(0, remainingKcal / 9);
            }
        } else {
            double prevNonFixedKcal = 0;
            double prevProtRatio = 0, prevCarbRatio = 0, prevFatRatio = 0;

            if (fixedProtGr == null) prevNonFixedKcal += goalsP * 4;
            if (fixedCarbGr == null) prevNonFixedKcal += goalsC * 4;
            if (fixedFatGr == null) prevNonFixedKcal += goalsF * 9;

            if (prevNonFixedKcal > 0) {
                if (fixedProtGr == null) prevProtRatio = (goalsP * 4) / prevNonFixedKcal;
                if (fixedCarbGr == null) prevCarbRatio = (goalsC * 4) / prevNonFixedKcal;
                if (fixedFatGr == null) prevFatRatio = (goalsF * 9) / prevNonFixedKcal;
            } else {
                if (fixedProtGr != null) { prevCarbRatio = 0.5; prevFatRatio = 0.5; }
                else if (fixedCarbGr != null) { prevProtRatio = 0.5; prevFatRatio = 0.5; }
                else { prevProtRatio = 0.5; prevCarbRatio = 0.5; }
            }

            if (fixedProtGr != null) {
                finalP = fixedProtGr;
                double sum = prevCarbRatio + prevFatRatio;
                if (sum <= 0) { prevCarbRatio = 0.5; prevFatRatio = 0.5; sum = 1.0; }
                finalC = Math.max(0, (remainingKcal * (prevCarbRatio / sum)) / 4);
                finalF = Math.max(0, (remainingKcal * (prevFatRatio  / sum)) / 9);
            } else if (fixedCarbGr != null) {
                finalC = fixedCarbGr;
                double sum = prevProtRatio + prevFatRatio;
                if (sum <= 0) { prevProtRatio = 0.5; prevFatRatio = 0.5; sum = 1.0; }
                finalP = Math.max(0, (remainingKcal * (prevProtRatio / sum)) / 4);
                finalF = Math.max(0, (remainingKcal * (prevFatRatio  / sum)) / 9);
            } else {
                finalF = fixedFatGr;
                double sum = prevProtRatio + prevCarbRatio;
                if (sum <= 0) { prevProtRatio = 0.5; prevCarbRatio = 0.5; sum = 1.0; }
                finalP = Math.max(0, (remainingKcal * (prevProtRatio / sum)) / 4);
                finalC = Math.max(0, (remainingKcal * (prevCarbRatio / sum)) / 4);
            }
        }

        double minProtG = ranges.get("P").minPct * targetKcal / 4.0;
        double maxProtG = ranges.get("P").maxPct * targetKcal / 4.0;
        double minCarbG = ranges.get("C").minPct * targetKcal / 4.0;
        double maxCarbG = ranges.get("C").maxPct * targetKcal / 4.0;
        double minFatG  = ranges.get("F").minPct * targetKcal / 9.0;
        double maxFatG  = ranges.get("F").maxPct * targetKcal / 9.0;

        finalP = clamp(finalP, minProtG, maxProtG);
        finalC = clamp(finalC, minCarbG, maxCarbG);
        finalF = clamp(finalF, minFatG, maxFatG);

        double finalKcal = finalP * 4 + finalC * 4 + finalF * 9;

        goalsKcal = Math.round(finalKcal);
        goalsP = Math.round(finalP);
        goalsC = Math.round(finalC);
        goalsF = Math.round(finalF);

        saveGoalsToPrefs();
        updateMacrosUI();
        recalculateDayTotals();
    }

    private Map<String, MacroRange> getRanges(@Nullable String goalString) {
        String normalizedGoal = (goalString == null) ? "mantenimiento" : goalString.trim().toLowerCase();

        Map<String, MacroRange> ranges = new HashMap<>();
        ranges.put("P", new MacroRange(0.15, 0.25));
        ranges.put("C", new MacroRange(0.45, 0.55));
        ranges.put("F", new MacroRange(0.25, 0.35));

        if (normalizedGoal.contains("ganar") || normalizedGoal.contains("volumen") || normalizedGoal.contains("bulk")) {
            ranges.put("P", new MacroRange(0.25, 0.35));
            ranges.put("C", new MacroRange(0.40, 0.60));
            ranges.put("F", new MacroRange(0.15, 0.25));
        } else if (normalizedGoal.contains("perder") || normalizedGoal.contains("definir") || normalizedGoal.contains("cut")) {
            ranges.put("P", new MacroRange(0.30, 0.40));
            ranges.put("C", new MacroRange(0.30, 0.50));
            ranges.put("F", new MacroRange(0.20, 0.30));
        }

        return ranges;
    }

    private static double clamp(double value, double min, double max) {
        if (min > max) {
            double temp = min; min = max; max = temp;
        }
        return Math.max(min, Math.min(value, max));
    }

    private static class MacroRange {
        final double minPct;
        final double maxPct;
        MacroRange(double minPercentage, double maxPercentage) {
            this.minPct = Math.max(0.0, Math.min(1.0, minPercentage));
            this.maxPct = Math.max(0.0, Math.min(1.0, maxPercentage));
        }
    }

    /* ==================== Borrado de bloques ==================== */

    private void showDeleteBlockDialog(FoodBlock blockToDelete, int position) {
        if (getContext() == null || blockToDelete == null) return;

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Eliminar Bloque de Comida")
                .setMessage("¿Eliminar '" + blockToDelete.getTitle() + "'? Esta acción no se puede deshacer.")
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    try {
                        if (position >= 0 && position < foodBlockList.size()) {
                            foodBlockList.remove(position);
                            if (foodBlockAdapter != null) {
                                foodBlockAdapter.notifyItemRemoved(position);
                                foodBlockAdapter.notifyItemRangeChanged(position, foodBlockList.size() - position);
                            }
                            recalculateDayTotals();
                            guardarComidasDelDia(currentDate);
                        } else {
                            Toast.makeText(getContext(), "Posición inválida.", Toast.LENGTH_SHORT).show();
                        }
                    } catch (IndexOutOfBoundsException e) {
                        Toast.makeText(getContext(), "Error al eliminar.", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    /* ==================== Totales y UI ==================== */

    private static class Totales { double kcal = 0, prot = 0, fat = 0, carb = 0; }

    private Totales calcularTotalesActuales() {
        Totales t = new Totales();
        if (foodBlockList == null || foodBlockList.isEmpty()) return t;

        for (FoodBlock block : foodBlockList) {
            if (block == null || block.getItems() == null || block.getItems().isEmpty()) continue;
            for (FoodItem item : block.getItems()) {
                if (item == null || !item.isChecked()) continue;
                double factor = item.getGrams() / 100.0;
                if (factor < 0) factor = 0;
                t.kcal += item.getKcalPer100g() * factor;
                t.prot += item.getProteinPer100g() * factor;
                t.carb += item.getCarbsPer100g() * factor;
                t.fat  += item.getFatPer100g() * factor;
            }
        }
        return t;
    }

    public void recalculateDayTotals() {
        if (!isAdded() || labelCalories == null || barCalories == null || goalsKcal < 0) return;

        Totales t = calcularTotalesActuales();

        // Log Verbose para ver el cálculo exacto si lo necesitas

        labelCalories.setText(String.format(Locale.getDefault(), "Calorías: %.0f / %.0f kcal", t.kcal, goalsKcal));
        labelProtein.setText(String.format(Locale.getDefault(),  "Proteínas: %.0f / %.0f g",  t.prot, goalsP));
        labelCarbs.setText(String.format(Locale.getDefault(),    "Carbohidratos: %.0f / %.0f g", t.carb, goalsC));
        labelFat.setText(String.format(Locale.getDefault(),      "Grasas: %.0f / %.0f g",    t.fat, goalsF));

        barCalories.setMax((int) Math.max(1, goalsKcal));
        barProtein.setMax((int) Math.max(1, goalsP));
        barCarbs.setMax((int) Math.max(1, goalsC));
        barFat.setMax((int) Math.max(1, goalsF));

        barCalories.setProgress((int) clamp(t.kcal, 0, goalsKcal));
        barProtein.setProgress((int) clamp(t.prot, 0, goalsP));
        barCarbs.setProgress((int) clamp(t.carb, 0, goalsC));
        barFat.setProgress((int) clamp(t.fat, 0, goalsF));

        if (dataLoaded && effectiveUid != null) {
            guardarComidasDelDia(currentDate);
        }
    }

    /* ==================== Recetas ==================== */

    public void showRecipeDialog(FoodSearchFragment.FoodResult recipeResult) {
        if (!isAdded() || getParentFragmentManager() == null) return;

        RecipeDialog dialog = RecipeDialog.newInstance(recipeResult, effectiveUid); // ✅ Pasar effectiveUid

        dialog.setOnGramsConfirmedListener(confirmedResult -> {
            FoodItem newItem = new FoodItem(
                    confirmedResult.name,
                    confirmedResult.grams,
                    confirmedResult.kcalPer100g,
                    confirmedResult.protein,
                    confirmedResult.carbs,
                    confirmedResult.fat,
                    confirmedResult.imageUrl,
                    true
            );
            if (confirmedResult.ingredients != null) {
                newItem.setIngredients(new ArrayList<>(confirmedResult.ingredients));
            }
            if (confirmedResult.instructions != null) {
                newItem.setInstructions(confirmedResult.instructions);
            }

            newItem.setChecked(true);

            if (foodBlockList != null && !foodBlockList.isEmpty()) {
                FoodBlock targetBlock = foodBlockList.get(0);
                targetBlock.addItem(newItem);

                if (foodBlockAdapter != null) {
                    foodBlockAdapter.notifyItemChanged(0);
                }

                guardarComidasDelDia(currentDate);
                recalculateDayTotals();

            } else {
                Toast.makeText(getContext(), "No hay bloques de comida disponibles.", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show(getParentFragmentManager(), "RecipeDialogTag");
    }

}
