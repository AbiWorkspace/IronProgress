package com.example.diets.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
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
import android.view.Window;
import android.widget.*;
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
import com.example.diets.model.User;   // 👈  AJUSTA el paquete


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.gson.GsonBuilder;
public class DashboardFragment extends Fragment {

    /* -------------------------------------------------- */
    /* ------------------- CONSTANTES ------------------- */
    /* -------------------------------------------------- */
    private static final String TAG = "DashboardFragment";   // ← filtra por esto en Logcat
    private static final String PREFS_NAME = "FoodPrefs";
    private static final int MAX_FOOD_BLOCKS = 10;
    private static final String[] DEFAULT_FOOD_NAMES =
            {"Desayuno", "Almuerzo", "Comida", "Snack", "Cena"};
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT =
            DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault());
    public RecyclerView recyclerViewFoodBlocks;
    /* -------------------------------------------------- */
    /* ---------------- ATRIBUTOS UI -------------------- */
    /* -------------------------------------------------- */
    private LinearProgressIndicator barCalories, barProtein, barCarbs, barFat;
    private TextView labelCalories, labelProtein, labelCarbs, labelFat, txtCurrentDate;
    private ImageButton btnPrevDay, btnNextDay;
    /* ---- metas persistentes ---- */
    private static final String PREF_GOALS = "MacroGoals";
    private static final String K_KCAL = "gKcal";
    private static final String K_PROT = "gProt";
    private static final String K_FAT = "gFat";
    private static final String K_CARB = "gCarb";
    private static final int MAX_DAYS_IN_FUTURE = 365;

    /* -------------------------------------------------- */
    /* --------------- FIREBASE / DATOS ----------------- */
    /* -------------------------------------------------- */
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private static final String[] RECOMMENDED_BLOCKS = {
            "Desayuno",
            "Media mañana",
            "Comida",
            "Merienda",
            "Cena"
    };

    private final List<FoodBlock> foodBlockList = new ArrayList<>();
    private FoodBlockAdapter foodBlockAdapter;

    private User user;
    private double goalsKcal, goalsP, goalsF, goalsC;

    private LocalDate currentDate = LocalDate.now();

    /* Listener hacia activity */
    private OnNavigationItemSelectedListener mListener;
    /* ---------- LISTENER DE CAMBIO DE METAS ---------- */
    private SharedPreferences goalPrefs;                // ref global
    private final SharedPreferences.OnSharedPreferenceChangeListener goalListener =
            (sp, key) -> {                              // λ -> refresca si cambian metas
                if (K_KCAL.equals(key) || K_PROT.equals(key) ||
                        K_CARB.equals(key) || K_FAT.equals(key)) {
                    loadGoalsFromPrefs();               // lee prefs
                    updateMacrosUI();                   // y actualiza la pantalla
                    recalculateDayTotals();
                }
            };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (goalPrefs != null)
            goalPrefs.unregisterOnSharedPreferenceChangeListener(goalListener);
    }

    /**  Refresca las barras y etiquetas de macros con las metas actuales. */
    private void updateMacrosUI() {
        if (!isAdded() || barCalories == null) return;   // UI no disponible

        barCalories.setMax((int) goalsKcal);
        barProtein .setMax((int) goalsP);
        barCarbs   .setMax((int) goalsC);
        barFat     .setMax((int) goalsF);

        labelCalories.setText(String.format(Locale.getDefault(),
                "Calorías: 0 / %.0f kcal", goalsKcal));
        labelProtein .setText(String.format(Locale.getDefault(),
                "Proteínas: 0 / %.0f g",  goalsP));
        labelCarbs   .setText(String.format(Locale.getDefault(),
                "Carbohidratos: 0 / %.0f g", goalsC));
        labelFat     .setText(String.format(Locale.getDefault(),
                "Grasas: 0 / %.0f g", goalsF));

        //  El progreso real lo aplicará recalculateDayTotals()
    }


    public interface OnNavigationItemSelectedListener {
        void onNavigationItemSelected(int position);
    }

    /* ================================================== */
    /* =============== CICLO DE VIDA  =================== */
    /* ================================================== */
    @Override
    public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        Log.d(TAG, "[onAttach] fragment attached");
        if (ctx instanceof OnNavigationItemSelectedListener)
            mListener = (OnNavigationItemSelectedListener) ctx;
        else
            throw new RuntimeException(ctx + " must implement OnNavigationItemSelectedListener");
    }

    public interface OnFoodChangedListener {
        void onFoodChanged();
    }

    private static final Gson GSON = new GsonBuilder()
            .serializeNulls()
            .setPrettyPrinting()  // opcional
            .create();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inf,
                             @Nullable ViewGroup cont,
                             @Nullable Bundle saved) {
        Log.d(TAG, "[onCreateView] inflating layout");

    /* ────────────────────────────────────────────────────────────────
       NUEVO: aseguramos que el fragmento quede suscrito a los cambios
       de metas (kcal / macros) ANTES de que se cree la vista.
       Solo se registra una vez:                             */
        if (goalPrefs == null) {   // ← (goalPrefs es el SharedPreferences global)
            goalPrefs = requireContext()
                    .getSharedPreferences(PREF_GOALS, Context.MODE_PRIVATE);
            goalPrefs.registerOnSharedPreferenceChangeListener(goalListener);
        }
        // ────────────────────────────────────────────────────────────────

        return inf.inflate(R.layout.activity_food, cont, false);
    }


    // ░░░░░░░░░░░ 1. MÉTODO onViewCreated() COMPLETO ░░░░░░░░░░░
    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
        super.onViewCreated(v, saved);
        cleanupOldEntries();
        migrateAllSavedDays();                     // ⚠️ Solo temporal

        Log.d(TAG, "[onViewCreated] ► inicio");

        /* --- Firebase --- */
        mAuth     = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Log.d(TAG, "[onViewCreated] Firebase refs OK");

        /* --- UI --- */
        bindUI(v);

        /* --- Metas (kcal/macros) ----------------------------- */
        loadGoalsFromPrefs();                      // carga (si existen) en los atributos
        updateMacrosUI();                          // pinta barras/etiquetas con las metas
        recalculateDayTotals();                    // calcula progreso del día

        /* --- Navegación / menús --- */
        setupDateNavigation();
        setupBottomNav(v);
        setupDrawer(v);

        /* --- Cargar usuario --- */
        FirebaseUser cur = mAuth.getCurrentUser();
        if (cur != null) {
            Log.d(TAG, "[onViewCreated] uid=" + cur.getUid());
            loadUserData(cur.getUid());
        } else {
            Log.w(TAG, "[onViewCreated] usuario null");
        }

        /* --- Cargar comidas del día actual --- */
        loadFoodBlocksForDate(currentDate);

        Log.d(TAG, "[onViewCreated] ► fin");
    }


    /**  Lee las metas guardadas en SharedPreferences.
     *   Devuelve true si existían, false si era la 1.ª vez
     *   (o si el fragmento ya no está adjunto).                                               */
    private boolean loadGoalsFromPrefs() {

        // ⚠️  Puede invocarse desde callbacks asíncronos después de navegar.
        if (!isAdded()) {
            Log.w(TAG, "[loadGoalsFromPrefs] ignorado — fragmento ya separado");
            return false;
        }

        // Usa la instancia global goalPrefs si ya la creamos; si no, créala.
        SharedPreferences p = (goalPrefs != null)
                ? goalPrefs
                : requireContext().getSharedPreferences(PREF_GOALS, Context.MODE_PRIVATE);

        if (!p.contains(K_KCAL)) return false;          // primera vez, nada que cargar

        goalsKcal = p.getFloat(K_KCAL, 0);
        goalsP    = p.getFloat(K_PROT, 0);
        goalsF    = p.getFloat(K_FAT , 0);
        goalsC    = p.getFloat(K_CARB, 0);

        Log.d(TAG, String.format(Locale.getDefault(),
                "[loadGoalsFromPrefs] kcal=%.0f P=%.0f C=%.0f F=%.0f",
                goalsKcal, goalsP, goalsC, goalsF));
        return true;
    }


    private void saveGoalsToPrefs() {
        SharedPreferences.Editor ed = requireContext()
                .getSharedPreferences(PREF_GOALS, Context.MODE_PRIVATE)
                .edit();
        ed.putFloat(K_KCAL, (float) goalsKcal);
        ed.putFloat(K_PROT, (float) goalsP);
        ed.putFloat(K_FAT, (float) goalsF);
        ed.putFloat(K_CARB, (float) goalsC);
        ed.apply();
        Log.d(TAG, "[saveGoalsToPrefs] metas guardadas localmente");
    }

    public LocalDate getCurrentDate() {
        Log.d(TAG, "[getCurrentDate] ⇢ " + currentDate);
        return currentDate;
    }

    /* ================================================== */
    /* ----------  CARGAR DATOS DEL USUARIO ------------- */
    /* ================================================== */

    /** -----------------------------------------------------------------
     *  Carga perfil y metas desde Firebase, pero **respeta / actualiza**
     *  las metas personalizadas guardadas en SharedPreferences.
     *-----------------------------------------------------------------*/
    private void loadUserData(String uid) {
        Log.d(TAG, "[loadUserData] uid=" + uid);

        mDatabase.child("users").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {

                    @Override public void onDataChange(@NonNull DataSnapshot snap) {

                        if (!isAdded()) {                         //  seguridad
                            Log.w(TAG, "[loadUserData] abortado — fragmento separado");
                            return;
                        }

                        if (!snap.exists()) { Log.w(TAG,"snapshot vacío"); return; }
                        user = snap.getValue(User.class);
                        if (user == null)   { Log.w(TAG,"user==null");     return; }

                        /* 1️⃣  leer / crear caloriesToday ------------------ */
                        DatabaseReference kcalRef = mDatabase.child("users")
                                .child(uid).child("caloriesToday");

                        kcalRef.get().addOnSuccessListener(calSnap -> {

                            if (!isAdded()) {
                                Log.w(TAG,"Listener abortado — fragmento separado");
                                return;
                            }

                            double kcalToday = calSnap.exists()
                                    ? calSnap.getValue(Double.class)
                                    : user.calculateCalories(
                                    user.getWeight(), user.getHeight(), user.getAge(),
                                    user.getGender(), user.getActivityLevel(), user.getGoal());

                            if (!calSnap.exists()) {
                                kcalRef.setValue(kcalToday);           // lo creamos en Firebase
                                Log.i(TAG,"caloriesToday creado → "+kcalToday);
                            }

                            /* 2️⃣  metas: primero intentamos prefs -------- */
                            boolean loadedPrefs = loadGoalsFromPrefs();   // seguro contra detached

                            if (!loadedPrefs) {                          // 1.ª vez → guarda perfil
                                goalsKcal = kcalToday;
                                goalsP    = user.calculateProtein();
                                goalsF    = user.calculateFat();
                                goalsC    = user.calculateCarbs();
                                saveGoalsToPrefs();
                                Log.i(TAG,"Metas base guardadas en prefs");
                            } else {
                            /* 👉 Si las kcal del servidor han cambiado
                               (p.ej. WeightFragment las ajustó) y difieren
                               ≥10 kcal de lo que hay en prefs, actualizamos
                               las metas locales para que la UI se refresque. */
                                if (Math.abs(kcalToday - goalsKcal) >= 10) {
                                    Log.i(TAG, String.format(Locale.getDefault(),
                                            " ↻ Detected kcal change %.0f → %.0f (prefs)",
                                            goalsKcal, kcalToday));

                                    goalsKcal = kcalToday;
                                    // mantenemos P/C/F proporcionales
                                    double factor = kcalToday /
                                            (goalsP*4 + goalsC*4 + goalsF*9);
                                    goalsP = Math.round(goalsP * factor);
                                    goalsC = Math.round(goalsC * factor);
                                    goalsF = Math.round(goalsF * factor);
                                    saveGoalsToPrefs();                 // escribe la nueva meta
                                }
                            }

                            /* 3️⃣  refrescar UI --------------------------- */
                            updateMacrosUI();
                            recalculateDayTotals();
                        });
                    }

                    @Override public void onCancelled(@NonNull DatabaseError err) {
                        Log.e(TAG,"[loadUserData] Firebase error: "+err.getMessage());
                    }
                });
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "[onPause] guardando día " + currentDate);
        guardarComidasDelDia(currentDate);
    }

    /* ================================================== */
    /* ===============  UI HELPERS  ===================== */
    /* ================================================== */
    private void bindUI(View v) {
        Log.d(TAG, "[bindUI] referenciando vistas");

        labelCalories = v.findViewById(R.id.labelCalories);
        labelProtein = v.findViewById(R.id.labelProtein);
        labelCarbs = v.findViewById(R.id.labelCarbs);
        labelFat = v.findViewById(R.id.labelFat);

        barCalories = v.findViewById(R.id.barCalories);
        barProtein = v.findViewById(R.id.barProtein);
        barCarbs = v.findViewById(R.id.barCarbs);
        barFat = v.findViewById(R.id.barFat);

        txtCurrentDate = v.findViewById(R.id.txtCurrentDate);
        btnPrevDay = v.findViewById(R.id.btnPrevDay);
        btnNextDay = v.findViewById(R.id.btnNextDay);
        txtCurrentDate.setText(currentDate.format(DISPLAY_FMT));

        // 👇 MODIFICACIÓN: ahora usamos la referencia pública
        recyclerViewFoodBlocks = v.findViewById(R.id.recyclerFoodBlocks);
        recyclerViewFoodBlocks.setLayoutManager(new LinearLayoutManager(getContext()));
        foodBlockAdapter = new FoodBlockAdapter(
                foodBlockList,
                getContext(),
                this,
                new FoodBlockAdapter.OnFoodBlockDeleteRequestedListener() {
                    @Override
                    public void onDeleteRequested(FoodBlock block, int position) {
                        showDeleteBlockDialog(block, position);  // Este método lo defines abajo
                    }
                }
        );
        recyclerViewFoodBlocks.setAdapter(foodBlockAdapter);


        v.findViewById(R.id.addFoodButton).setOnClickListener(view -> {
            Log.d(TAG, "[bindUI] addFoodButton clicked");
            if (foodBlockList.size() >= MAX_FOOD_BLOCKS) {
                Toast.makeText(getContext(), "Máximo de bloques alcanzado", Toast.LENGTH_SHORT).show();
                return;
            }
            foodBlockList.add(new FoodBlock("Nueva comida"));
            foodBlockAdapter.notifyItemInserted(foodBlockList.size() - 1);
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

        labelCalories.setOnClickListener(v1 -> {
            Log.d(TAG, "[bindUI] click labelCalories → editar macros");
            showEditMacrosDialog();
        });
    }

    private void setupDateNavigation() {
        Log.d(TAG, "[setupDateNavigation] listeners prev/next");
        btnPrevDay.setOnClickListener(v -> {
            Log.d(TAG, "[setupDateNavigation] ← día anterior");
            switchDate(currentDate.minusDays(1));
        });
        btnNextDay.setOnClickListener(v -> {
            LocalDate tomorrow = currentDate.plusDays(1);
            if (ChronoUnit.DAYS.between(LocalDate.now(), tomorrow) <= MAX_DAYS_IN_FUTURE) {
                Log.d(TAG, "[setupDateNavigation] → día siguiente");
                switchDate(tomorrow);
            } else {
                Toast.makeText(getContext(), "No puedes planificar tan lejos", Toast.LENGTH_SHORT).show();
            }
        });

    }
    private void showDeleteBlockDialog(FoodBlock block, int position) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar bloque de comida")
                .setMessage("¿Deseas eliminar este bloque solo para esta semana o para todas las semanas (mismo día)?")
                .setPositiveButton("Solo esta semana", (dialog, which) -> {
                    // Solo elimina del día actual
                    foodBlockList.remove(position);
                    foodBlockAdapter.notifyItemRemoved(position);
                    recalculateDayTotals();
                    guardarComidasDelDia(currentDate);
                })
                .setNegativeButton("Todas las semanas (mismo día)", (dialog, which) -> {
                    removeBlockFromAllMatchingDays(block.getTitle(), position, currentDate.getDayOfWeek().getValue());
                    foodBlockList.remove(position);
                    foodBlockAdapter.notifyItemRemoved(position);
                    recalculateDayTotals();
                    guardarComidasDelDia(currentDate);
                })
                .setNeutralButton("Cancelar", null)
                .show();
    }

    private void removeBlockFromAllMatchingDays(String blockTitle, int position, int dayOfWeek) {
        // dayOfWeek: 1=Lunes ... 7=Domingo (ISO)
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();

        Gson gson = new Gson();
        for (String key : all.keySet()) {
            if (!key.startsWith("food_")) continue;
            String dateStr = key.substring(5);
            try {
                LocalDate date = LocalDate.parse(dateStr);
                if (date.getDayOfWeek().getValue() == dayOfWeek) {
                    String json = (String) all.get(key);
                    if (json == null) continue;

                    Type t = new TypeToken<List<FoodBlock>>() {}.getType();
                    List<FoodBlock> blocks = gson.fromJson(json, t);
                    boolean changed = false;

                    // Verifica que exista el bloque en esa posición y sea el mismo título (opcional)
                    if (blocks.size() > position) {
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



    private void cleanupOldEntries() {
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Map<String, ?> all = prefs.getAll();
        LocalDate today = LocalDate.now();
        for (String k : all.keySet()) {
            if (k.startsWith("food_")) {
                String dateStr = k.substring(5); // "yyyy-MM-dd"
                LocalDate d = LocalDate.parse(dateStr, DATE_FMT);
                if (ChronoUnit.DAYS.between(d, today) > 365) { // más de 1 año
                    prefs.edit().remove(k).apply();
                }
            }
        }
    }


    private void setupBottomNav(View v) {
        BottomNavigationView bottom = v.findViewById(R.id.bottom_navigation);
        if (bottom == null) {
            Log.w(TAG, "[setupBottomNav] bottom nav null");
            return;
        }
        bottom.setOnItemSelectedListener(item -> {
            Log.d(TAG, "[setupBottomNav] click id=" + item.getItemId());
            if (item.getItemId() == R.id.navigation_home) {
                if (mListener != null) mListener.onNavigationItemSelected(1);
                return true;
            } else if (item.getItemId() == R.id.navigation_dashboard) {
                if (getActivity() instanceof ViewPagerActivity)
                    ((ViewPagerActivity) getActivity()).switchToWeightFragment();
                return true;
            }
            return false;
        });
    }

    /* ==================================================
     * ==========   DIÁLOGO DE MACROS  ==================
     * ================================================== */
    private void showEditMacrosDialog() {
        Log.d(TAG, "[showEditMacrosDialog] abrir diálogo");

        ContextThemeWrapper m3Ctx = new ContextThemeWrapper(
                requireContext(), R.style.ThemeOverlay_Diets_Material3Bridge);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(m3Ctx);


        View content = LayoutInflater.from(m3Ctx)
                .inflate(R.layout.dialog_edit_macros, null, false);
        builder.setView(content);
        AlertDialog dlg = builder.create();

        // ⬇️ Haz el fondo del AlertDialog completamente transparente
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));

        TextInputEditText eKcal = content.findViewById(R.id.inpKcal);
        TextInputEditText ePro = content.findViewById(R.id.inpProt);
        TextInputEditText eCarb = content.findViewById(R.id.inpCarb);
        TextInputEditText eFat = content.findViewById(R.id.inpFat);
        Button btnCancel = content.findViewById(R.id.btnCancel);
        Button btnApply = content.findViewById(R.id.btnApply);

        /* valores actuales */
        eKcal.setText(String.valueOf((int) goalsKcal));
        ePro.setText(String.valueOf((int) goalsP));
        eCarb.setText(String.valueOf((int) goalsC));
        eFat.setText(String.valueOf((int) goalsF));

        btnCancel.setOnClickListener(v -> {
            Log.d(TAG, "[showEditMacrosDialog] cancel");
            dlg.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            boolean hasError = false;
            // Validar Kcal
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

            // Validar proteína
            Double newP = parseOrNull(ePro);
            if (newP != null && (newP <= 0 || Double.isNaN(newP))) {
                ePro.setError("Valor inválido");
                hasError = true;
            } else {
                ePro.setError(null);
            }

            // Validar carbos
            Double newC = parseOrNull(eCarb);
            if (newC != null && (newC <= 0 || Double.isNaN(newC))) {
                eCarb.setError("Valor inválido");
                hasError = true;
            } else {
                eCarb.setError(null);
            }

            // Validar grasas
            Double newF = parseOrNull(eFat);
            if (newF != null && (newF <= 0 || Double.isNaN(newF))) {
                eFat.setError("Valor inválido");
                hasError = true;
            } else {
                eFat.setError(null);
            }

            // Si hay algún error, no continúes
            if (hasError) {
                Toast.makeText(getContext(), "Corrige los valores marcados", Toast.LENGTH_SHORT).show();
                return;
            }

            // Lógica previa, ahora con valores válidos
            if (newP != null && Math.round(newP) == Math.round(goalsP)) newP = null;
            if (newC != null && Math.round(newC) == Math.round(goalsC)) newC = null;
            if (newF != null && Math.round(newF) == Math.round(goalsF)) newF = null;

            Log.d(TAG, String.format(
                    "[showEditMacrosDialog] apply kcal=%.0f  P*=%s  C*=%s  F*=%s",
                    newKcal, newP, newC, newF));

            boolean kcalOnly = (newP == null && newC == null && newF == null) &&
                    (Math.round(newKcal) != Math.round(goalsKcal));

            if (!kcalOnly && newP == null && newC == null && newF == null) {
                Toast.makeText(getContext(),
                        "Introduce al menos un macronutriente",
                        Toast.LENGTH_SHORT).show();
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

    /* ==================================================
     * ----------- REBALANCEO DE MACROS v2 --------------
     * ================================================== */
    private void autoRebalanceMacros(double kcal,
                                     @Nullable Double pGr,
                                     @Nullable Double cGr,
                                     @Nullable Double fGr) {

        Log.d(TAG, "[autoRebalanceMacros] kcal=" + kcal +
                " P=" + pGr + " C=" + cGr + " F=" + fGr);

        /* ⓪ – Solo cambió la kcal (P C F == null) → escalamos proporcional */
        if (pGr == null && cGr == null && fGr == null) {
            double factor = kcal / goalsKcal;          // ratio nuevo / antiguo

            Map<String, MacroRange> range = getRanges(user.getGoal());

            double P = goalsP * factor;
            double C = goalsC * factor;
            double F = goalsF * factor;

            /* clamp para no salir de rangos científicos */
            P = clamp(P, range.get("P").minPct * kcal / 4, range.get("P").maxPct * kcal / 4);
            C = clamp(C, range.get("C").minPct * kcal / 4, range.get("C").maxPct * kcal / 4);
            F = clamp(F, range.get("F").minPct * kcal / 9, range.get("F").maxPct * kcal / 9);

            goalsKcal = Math.round(kcal);
            goalsP = Math.round(P);
            goalsC = Math.round(C);
            goalsF = Math.round(F);

            Log.d(TAG, String.format(Locale.getDefault(),
                    "[autoRebalanceMacros] sólo kcal → kcal=%.0f  P=%.0f  C=%.0f  F=%.0f",
                    goalsKcal, goalsP, goalsC, goalsF));

            barCalories.setMax((int) goalsKcal);
            barProtein.setMax((int) goalsP);
            barCarbs.setMax((int) goalsC);
            barFat.setMax((int) goalsF);

            mDatabase.child("users")
                    .child(FirebaseAuth.getInstance().getUid())
                    .child("caloriesToday")
                    .setValue(goalsKcal);

            saveGoalsToPrefs();
            recalculateDayTotals();
            return;           // ← terminamos aquí
        }

        /* ① – Si los 3 macros fueron editados → los respetamos */
        if (pGr != null && cGr != null && fGr != null) {
            goalsKcal = Math.round(kcal);
            goalsP = Math.round(pGr);
            goalsC = Math.round(cGr);
            goalsF = Math.round(fGr);
            saveGoalsToPrefs();
            recalculateDayTotals();
            Log.d(TAG, "[autoRebalanceMacros] early‑return (3 campos)");
            return;
        }

        /* ② – Rangos según meta ---------------------------------- */
        Map<String, MacroRange> range = getRanges(user.getGoal());

        /* ③ – Macros actuales como base -------------------------- */
        double P = (pGr != null) ? pGr : goalsP;
        double C = (cGr != null) ? cGr : goalsC;
        double F = (fGr != null) ? fGr : goalsF;

    /* ④ – Fijamos primero los macros que el usuario tocó
            (clamp para no salirse de rangos)                 */
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

        /* ⑤ – Calorías faltantes (o sobrantes) ------------------- */
        double usedKcal = P * 4 + C * 4 + F * 9;
        double remKcal = kcal - usedKcal;   // puede ser + o –

        /* ⑥ – Macro libre (no editado) absorbe el desfase -------- */
        if (pGr == null && cGr == null) {          // solo F editado
            double pctP = (range.get("P").minPct + range.get("P").maxPct) / 2;
            P = pctP * kcal / 4;
            C = (kcal - (P * 4 + F * 9)) / 4;
        } else if (pGr == null && fGr == null) {     // solo C editado
            double pctP = (range.get("P").minPct + range.get("P").maxPct) / 2;
            P = pctP * kcal / 4;
            F = (kcal - (P * 4 + C * 4)) / 9;
        } else if (cGr == null && fGr == null) {     // solo P editado
            double pctF = (range.get("F").minPct + range.get("F").maxPct) / 2;
            F = pctF * kcal / 9;
            C = (kcal - (P * 4 + F * 9)) / 4;
        } else {                               // se tocaron 2 macros
            if (pGr == null) {                   // se tocaron C y F -> P ajusta
                P = remKcal / 4;
            } else if (cGr == null) {            // P y F -> C ajusta
                C = remKcal / 4;
            } else {                         // P y C -> F ajusta
                F = remKcal / 9;
            }
        }

        /* ⑦ – Clamp final para respetar rangos ------------------- */
        P = clamp(P, range.get("P").minPct * kcal / 4, range.get("P").maxPct * kcal / 4);
        C = clamp(C, range.get("C").minPct * kcal / 4, range.get("C").maxPct * kcal / 4);
        F = clamp(F, range.get("F").minPct * kcal / 9, range.get("F").maxPct * kcal / 9);

        /* ⑧ – Redondear y refrescar ------------------------------ */
        goalsKcal = Math.round(kcal);
        goalsP = Math.round(P);
        goalsC = Math.round(C);
        goalsF = Math.round(F);

        Log.d(TAG, String.format(Locale.getDefault(),
                "[autoRebalanceMacros] finales -> kcal=%.0f  P=%.0f  C=%.0f  F=%.0f",
                goalsKcal, goalsP, goalsC, goalsF));

        barCalories.setMax((int) goalsKcal);
        barProtein.setMax((int) goalsP);
        barCarbs.setMax((int) goalsC);
        barFat.setMax((int) goalsF);

        mDatabase.child("users")
                .child(FirebaseAuth.getInstance().getUid())
                .child("caloriesToday")
                .setValue(goalsKcal);

        saveGoalsToPrefs();
        recalculateDayTotals();
    }

    private Map<String, MacroRange> getRanges(String goal) {
        goal = goal == null ? "" : goal.toLowerCase();
        Map<String, MacroRange> r = new HashMap<>();

        if (goal.contains("ganar")) {             // BULKING
            r.put("P", new MacroRange(0.25, 0.30));
            r.put("F", new MacroRange(0.15, 0.25));
            r.put("C", new MacroRange(0.45, 0.60));
        } else if (goal.contains("perder")) {     // CUTTING
            r.put("P", new MacroRange(0.25, 0.35));
            r.put("F", new MacroRange(0.20, 0.30));
            r.put("C", new MacroRange(0.25, 0.45));
        } else {                                  // MANTENIMIENTO
            r.put("P", new MacroRange(0.15, 0.25));
            r.put("F", new MacroRange(0.25, 0.30));
            r.put("C", new MacroRange(0.45, 0.55));
        }

        return r;
    }


    private static double clamp(double val, double min, double max) {
        return Math.max(min, Math.min(max, val));
    }

    /* ===== RANGOS RECOMENDADOS SEGÚN META ===================== */
    private static class MacroRange {
        final double minPct, maxPct;   // % de las kcal totales

        MacroRange(double min, double max) {
            this.minPct = min;
            this.maxPct = max;
        }
    }


    /* ================================================== */
    /* --------------- DRAWER / LOGOUT ------------------ */
    /* ================================================== */
    private void setupDrawer(View v) {
        NavigationView nav = v.findViewById(R.id.navigation_view);
        nav.setNavigationItemSelectedListener(item -> {
            Log.d(TAG, "[setupDrawer] click id=" + item.getItemId());
            if (item.getItemId() == R.id.nav_logout) {
                mAuth.signOut();
                startActivity(new Intent(getActivity(), MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
                return true;
            }
            return false;
        });
    }

    /* ================================================== */
    /* ------------- NAVEGAR ENTRE FECHAS --------------- */
    /* ================================================== */
    private void switchDate(LocalDate newDate) {
        Log.d(TAG, "[switchDate] " + currentDate + " → " + newDate);
        guardarComidasDelDia(currentDate);
        currentDate = newDate;
        txtCurrentDate.setText(currentDate.format(DISPLAY_FMT));
        loadFoodBlocksForDate(currentDate);
    }

    /* ================================================== */
    /* -----------  PERSISTENCIA POR FECHA -------------- */
    /* ================================================== */
    private String keyFood(LocalDate d) {
        return "food_" + d.format(DATE_FMT);
    }

    private String keyTotK(LocalDate d) {
        return "totKcal_" + d.format(DATE_FMT);
    }

    private String keyTotP(LocalDate d) {
        return "totProt_" + d.format(DATE_FMT);
    }

    private String keyTotF(LocalDate d) {
        return "totFat_" + d.format(DATE_FMT);
    }

    private String keyTotC(LocalDate d) {
        return "totCarb_" + d.format(DATE_FMT);
    }

    public void guardarComidasDelDia(LocalDate date) {
        if (getContext() == null) return;

        SharedPreferences.Editor ed = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit();

        ed.putString(keyFood(date), GSON.toJson(foodBlockList));

        Totales t = calcularTotalesActuales();
        ed.putFloat(keyTotK(date), (float) t.kcal);
        ed.putFloat(keyTotP(date), (float) t.prot);
        ed.putFloat(keyTotF(date), (float) t.fat);
        ed.putFloat(keyTotC(date), (float) t.carb);
        ed.apply();

        Log.d(TAG, "[guardarComidasDelDia] " + date + "  →  " + foodBlockList.size() + " bloques");
    }

    private void loadFoodBlocksForDate(LocalDate date) {
        if (getContext() == null) return;

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(keyFood(date), null);

        foodBlockList.clear();

        if (json == null) {
            Log.d(TAG, "[loadFoodBlocksForDate] " + date + " sin datos. Se crearán bloques recomendados.");
            for (String def : DEFAULT_FOOD_NAMES)
                foodBlockList.add(new FoodBlock(def));
            // Guarda los bloques base en preferencias
            guardarComidasDelDia(date);   // para que persista aunque no se agregue comida
        } else {
            try {
                Type type = new TypeToken<List<FoodBlock>>() {}.getType();
                foodBlockList.addAll(GSON.fromJson(json, type));
            } catch (Exception e) {
                Log.e(TAG, "[loadFoodBlocksForDate] Error al parsear JSON, se reinicia el día", e);
                foodBlockList.clear();
                for (String def : DEFAULT_FOOD_NAMES)
                    foodBlockList.add(new FoodBlock(def));
                guardarComidasDelDia(date);
            }

            boolean changed = false;
            for (FoodBlock b : foodBlockList) {
                for (FoodItem it : b.getItems()) {
                    if (!it.isRecipe()
                            && it.getIngredients() != null
                            && !it.getIngredients().isEmpty()) {
                        it.setRecipe(true);
                        Log.i(TAG, "⚙️  Migrado a receta → " + it.getName());
                        changed = true;
                    }
                }
            }

            // Si hay menos de 5 bloques, agrega los que falten con nombres por defecto
            if (foodBlockList.size() < DEFAULT_FOOD_NAMES.length) {
                Log.d(TAG, "[loadFoodBlocksForDate] Solo hay " + foodBlockList.size() + " bloques. Se añaden faltantes.");
                // Detecta nombres existentes para evitar duplicados
                Set<String> existentes = new HashSet<>();
                for (FoodBlock fb : foodBlockList) {
                    existentes.add(fb.getTitle().trim().toLowerCase());
                }
                for (String def : DEFAULT_FOOD_NAMES) {
                    if (!existentes.contains(def.trim().toLowerCase())) {
                        foodBlockList.add(new FoodBlock(def));
                    }
                }
                changed = true;  // Hubo cambio, hay que guardar
            }

            if (changed) guardarComidasDelDia(date);  // guarda solo si hubo cambios
        }

        foodBlockAdapter.notifyDataSetChanged();
        recalculateDayTotals();
    }


    private void migrateAllSavedDays() {
        SharedPreferences p = requireContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        Map<String, ?> all = p.getAll();
        for (String k : all.keySet()) {
            if (!k.startsWith("food_")) continue;

            String json = (String) all.get(k);
            if (json == null) continue;

            Type t = new TypeToken<List<FoodBlock>>() {
            }.getType();
            List<FoodBlock> blocks = GSON.fromJson(json, t);
            boolean changed = false;

            for (FoodBlock b : blocks) {
                for (FoodItem it : b.getItems()) {
                    if (!it.isRecipe()
                            && it.getIngredients() != null
                            && !it.getIngredients().isEmpty()) {
                        it.setRecipe(true);
                        changed = true;
                    }
                }
            }

            if (changed) {
                p.edit().putString(k, GSON.toJson(blocks)).apply();
                Log.i(TAG, "⚙️  Día migrado → " + k);
            }
        }
    }


    /* ================================================== */
    /* -------------  RECÁLCULO TOTALES ----------------- */
    /* ================================================== */
    private static class Totales {
        double kcal, prot, fat, carb;
    }

    private Totales calcularTotalesActuales() {
        Totales t = new Totales();

        for (FoodBlock b : foodBlockList) {
            for (FoodItem it : b.getItems()) {

                /* ───── NUEVO LOG DETALLADO ───── */
                Log.d("TOT-TRACE",
                        "block=" + b.getTitle() +
                                "  name="  + it.getName() +
                                "  grams=" + it.getGrams() +
                                "  kcal/100=" + it.getKcalPer100g() +
                                "  checked=" + it.isChecked());

                if (!it.isChecked()) continue;     // ← ¡filtro crítico!

                double f = it.getGrams() / 100.0;
                t.kcal += it.getKcalPer100g() * f;
                t.prot += it.getProteinPer100g() * f;
                t.fat  += it.getFatPer100g()    * f;
                t.carb += it.getCarbsPer100g()  * f;
            }
        }

        Log.d(TAG, String.format(Locale.getDefault(),
                "[calcularTotalesActuales] kcal=%.0f P=%.0f C=%.0f F=%.0f",
                t.kcal, t.prot, t.carb, t.fat));
        return t;
    }


    public void recalculateDayTotals() {
        Totales t = calcularTotalesActuales();
        labelCalories.setText(String.format(Locale.getDefault(),
                "Calorías: %.0f / %.0f kcal", t.kcal, goalsKcal));
        labelProtein.setText(String.format(Locale.getDefault(),
                "Proteínas: %.0f / %.0f g", t.prot, goalsP));
        labelFat.setText(String.format(Locale.getDefault(),
                "Grasas: %.0f / %.0f g", t.fat, goalsF));
        labelCarbs.setText(String.format(Locale.getDefault(),
                "Carbohidratos: %.0f / %.0f g", t.carb, goalsC));

        barCalories.setProgress((int) Math.min(t.kcal, goalsKcal));
        barProtein.setProgress((int) Math.min(t.prot, goalsP));
        barFat.setProgress((int) Math.min(t.fat, goalsF));
        barCarbs.setProgress((int) Math.min(t.carb, goalsC));

        guardarComidasDelDia(currentDate);
    }

    public void showRecipeDialog(FoodSearchFragment.FoodResult recipe) {
        Log.d(TAG, "[showRecipeDialog] mostrando diálogo con receta: " + recipe.name);

        RecipeDialog dialog = RecipeDialog.newInstance(recipe);

        dialog.setOnGramsConfirmedListener(result -> {
            Log.d(TAG, "[showRecipeDialog] onGramsConfirmed ejecutado para: " + result.name);

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
// 👇 AGREGA ESTO
            item.setChecked(true);
            Log.d("RECIPE-CREATE",
                    "name=" + item.getName() + " checked=" + item.isChecked());


            Log.d(TAG, "[showRecipeDialog] DEBUG persistencia → ingredients=" +
                    item.getIngredients() + ", instructions=" + item.getInstructions());

            Log.d(TAG, "[showRecipeDialog] nuevo FoodItem → " +
                    "name=" + item.getName() + ", grams=" + item.getGrams() +
                    ", kcal=" + item.getKcalPer100g() + ", isRecipe=" + item.isRecipe());

            if (!foodBlockList.isEmpty()) {
                foodBlockList.get(0).addItem(item);  // 👉 añade al primer bloque (ej. desayuno)
                foodBlockAdapter.notifyItemChanged(0);
                Log.d(TAG, "[showRecipeDialog] alimento añadido al primer bloque");

                guardarComidasDelDia(currentDate);  // 🔥 fuerza escritura
                recalculateDayTotals();
            } else {
                Log.w(TAG, "[showRecipeDialog] no hay bloques de comida para añadir el alimento");
                Toast.makeText(getContext(), "No hay bloques de comida", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show(getParentFragmentManager(), "RecipeDialog");
    }



}
