package com.example.diets.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.activitys.MainActivity;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.FoodBlockAdapter;
import com.example.diets.model.FoodBlock;
import com.example.diets.model.FoodItem;
import com.example.diets.model.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class DashboardFragment extends Fragment implements FoodBlockAdapter.OnFoodChangedListener {

    private static final String TAG = "DashboardFragment";
    /* --- UI --- */
    private LinearProgressIndicator barCalories, barProtein, barCarbs, barFat;
    private TextView labelCalories, labelProtein, labelCarbs, labelFat, txtCurrentDate;
    private ImageButton btnPrevDay, btnNextDay;

    /* --- Firebase / auth --- */
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    /* --- Datos & prefs --- */
    private static final String PREFS_NAME = "FoodPrefs";
    private static final int MAX_FOOD_BLOCKS = 10;
    private static final String[] DEFAULT_FOOD_NAMES = {"Desayuno", "Almuerzo", "Comida", "Snack", "Cena"};
    private final List<FoodBlock> foodBlockList = new ArrayList<>();
    private FoodBlockAdapter foodBlockAdapter;

    private User user;
    private double goalsKcal, goalsP, goalsF, goalsC;

    /* --- Navegación por fecha --- */
    private LocalDate currentDate = LocalDate.now();
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault());

    /* Listener hacia activity */
    private OnNavigationItemSelectedListener mListener;
    public interface OnNavigationItemSelectedListener { void onNavigationItemSelected(int position); }

    /* ===== Ciclo de vida ===== */
    @Override public void onAttach(@NonNull Context ctx) {
        super.onAttach(ctx);
        if (ctx instanceof OnNavigationItemSelectedListener) mListener = (OnNavigationItemSelectedListener) ctx;
        else throw new RuntimeException(ctx + " must implement OnNavigationItemSelectedListener");
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inf, @Nullable ViewGroup cont, @Nullable Bundle saved) {
        return inf.inflate(R.layout.activity_food, cont, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle saved) {
        super.onViewCreated(v, saved);
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        bindUI(v);
        setupDateNavigation();
        setupBottomNav(v);
        setupDrawer(v);

        /* Cargar usuario */
        FirebaseUser cur = mAuth.getCurrentUser();
//        if (cur != null) loadUserData(cur.getUid());

        /* Cargar comidas */
        loadFoodBlocksForDate(currentDate);
    }

    @Override public void onPause() {
        super.onPause();
        guardarComidasDelDia(currentDate);   // <-- guarda el día visible
    }
    public LocalDate getCurrentDate() {
        return currentDate;
    }

    /* ===== UI helpers ===== */
    private void bindUI(View v) {
        // Etiquetas y barras
        labelCalories = v.findViewById(R.id.labelCalories);
        labelProtein  = v.findViewById(R.id.labelProtein);
        labelCarbs    = v.findViewById(R.id.labelCarbs);
        labelFat      = v.findViewById(R.id.labelFat);
        barCalories = v.findViewById(R.id.barCalories);
        barProtein  = v.findViewById(R.id.barProtein);
        barCarbs    = v.findViewById(R.id.barCarbs);
        barFat      = v.findViewById(R.id.barFat);

        // Fecha + flechas
        txtCurrentDate = v.findViewById(R.id.txtCurrentDate);
        btnPrevDay     = v.findViewById(R.id.btnPrevDay);
        btnNextDay     = v.findViewById(R.id.btnNextDay);
        txtCurrentDate.setText(currentDate.format(DISPLAY_FMT));

        // Lista de bloques
        RecyclerView rv = v.findViewById(R.id.recyclerFoodBlocks);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        foodBlockAdapter = new FoodBlockAdapter(foodBlockList, getContext(), this);
        rv.setAdapter(foodBlockAdapter);

        // Botón agregar bloque
        v.findViewById(R.id.addFoodButton).setOnClickListener(view -> {
            if (foodBlockList.size() >= MAX_FOOD_BLOCKS) {
                Toast.makeText(getContext(), "Máximo de bloques alcanzado", Toast.LENGTH_SHORT).show();
                return;
            }
            foodBlockList.add(new FoodBlock("Nueva comida"));
            foodBlockAdapter.notifyItemInserted(foodBlockList.size() - 1);
        });
    }

    private void setupDateNavigation() {
        btnPrevDay.setOnClickListener(v -> switchDate(currentDate.minusDays(1)));
        btnNextDay.setOnClickListener(v -> {
            LocalDate tomorrow = currentDate.plusDays(1);
            if (!tomorrow.isAfter(LocalDate.now())) switchDate(tomorrow); // No permitir futuro
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
                if (getActivity() instanceof ViewPagerActivity)
                    ((ViewPagerActivity) getActivity()).switchToWeightFragment();
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

    /* ===== Navegar entre fechas ===== */
    private void switchDate(LocalDate newDate) {
        guardarComidasDelDia(currentDate);   // Guarda antes de cambiar
        currentDate = newDate;
        txtCurrentDate.setText(currentDate.format(DISPLAY_FMT));
        loadFoodBlocksForDate(currentDate);
    }

    /* ===== Persistencia por fecha ===== */
    private String keyFood(LocalDate date)   { return "food_"   + date.format(DATE_FMT); }
    private String keyTotK(LocalDate date)   { return "totKcal_"+ date.format(DATE_FMT); }
    private String keyTotP(LocalDate date)   { return "totProt_"+ date.format(DATE_FMT); }
    private String keyTotF(LocalDate date)   { return "totFat_" + date.format(DATE_FMT); }
    private String keyTotC(LocalDate date)   { return "totCarb_"+ date.format(DATE_FMT); }

    public void guardarComidasDelDia(LocalDate date) {
        if (getContext()==null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = prefs.edit();

        /* Serializar bloques */
        String json = new Gson().toJson(foodBlockList);
        ed.putString(keyFood(date), json);

        /* Guardar totales */
        Totales t = calcularTotalesActuales();
        ed.putFloat(keyTotK(date), (float) t.kcal);
        ed.putFloat(keyTotP(date), (float) t.prot);
        ed.putFloat(keyTotF(date), (float) t.fat);
        ed.putFloat(keyTotC(date), (float) t.carb);

        ed.apply();
        Log.d(TAG, "📝 Guardado de día " + date);
    }

    private void loadFoodBlocksForDate(LocalDate date) {
        if (getContext()==null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(keyFood(date), null);

        foodBlockList.clear();
        if (json == null) {
            Log.d(TAG, "📅 Día sin datos → bloques por defecto");
            for (String def : DEFAULT_FOOD_NAMES) foodBlockList.add(new FoodBlock(def));
        } else {
            Type type = new TypeToken<List<FoodBlock>>(){}.getType();
            foodBlockList.addAll(new Gson().fromJson(json, type));
            Log.d(TAG, "📦 Cargados bloques de " + date);
        }
        foodBlockAdapter.notifyDataSetChanged();
        recalculateDayTotals();
    }

//    /* ===== User data ===== */
//    private void loadUserData(String uid) {
//        mDatabase.child("users").child(uid)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
//                        if (!snap.exists()) return;
//                        user = snap.getValue(User.class);
//                        if (user!=null) updateNutritionalValues(user);
//                    }
//                    @Override public void onCancelled(@NonNull DatabaseError err) {
//                        Log.e(TAG,"Firebase error: "+err.getMessage());
//                    }
//                });
//    }

    /* ===== Actualizar metas ===== */
    private void updateNutritionalValues(User u) {
        goalsKcal = u.calculateCalories(u.getWeight(), u.getHeight(), u.getAge(),
                u.getGender(), u.getActivityLevel(), u.getGoal());
        goalsP = u.calculateProtein();
        goalsF = u.calculateFat();
        goalsC = u.calculateCarbs();

        barCalories.setMax((int) goalsKcal);
        barProtein .setMax((int) goalsP);
        barFat     .setMax((int) goalsF);
        barCarbs   .setMax((int) goalsC);

        recalculateDayTotals();
    }

    /* ===== Totales del día ===== */
    private static class Totales { double kcal, prot, fat, carb; }
    private Totales calcularTotalesActuales() {
        Totales t = new Totales();
        for (FoodBlock b : foodBlockList) {
            for (FoodItem item : b.getItems()) {
                if (!item.isChecked()) continue;
                double f = item.getGrams()/100.0;
                t.kcal += item.getKcalPer100g()*f;
                t.prot += item.getProteinPer100g()*f;
                t.fat  += item.getFatPer100g()*f;
                t.carb += item.getCarbsPer100g()*f;
            }
        }
        return t;
    }

    public void recalculateDayTotals() {
        Totales t = calcularTotalesActuales();
        labelCalories.setText(String.format(Locale.getDefault(),
                "Calorías: %.0f / %.0f kcal", t.kcal, goalsKcal));
        labelProtein .setText(String.format(Locale.getDefault(),
                "Proteínas: %.0f / %.0f g", t.prot, goalsP));
        labelFat     .setText(String.format(Locale.getDefault(),
                "Grasas: %.0f / %.0f g", t.fat, goalsF));
        labelCarbs   .setText(String.format(Locale.getDefault(),
                "Carbohidratos: %.0f / %.0f g", t.carb, goalsC));

        barCalories.setProgress((int) Math.min(t.kcal, goalsKcal));
        barProtein .setProgress((int) Math.min(t.prot, goalsP));
        barFat     .setProgress((int) Math.min(t.fat , goalsF));
        barCarbs   .setProgress((int) Math.min(t.carb, goalsC));
    }

    /* ===== Callback desde FoodBlockAdapter cuando cambian items ===== */
    @Override public void onFoodChanged() { recalculateDayTotals(); }
}