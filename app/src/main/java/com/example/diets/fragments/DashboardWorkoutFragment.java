package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.TrainingDayAdapter;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DashboardWorkoutFragment extends Fragment {

    private static final String TAG = "DashboardWorkoutFragment";

    private RecyclerView recyclerView;
    private TrainingDayAdapter adapter;
    private final List<TrainingDay> trainingDays = new ArrayList<>();
    private String effectiveUid = null; // <- clave para coach mode

    // ===========================
    // Ciclo de vida
    // ===========================
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "DashboardWorkoutFragment - onCreateView: Inflando vista.");
        View view = inflater.inflate(R.layout.fragment_dashboardfragment, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewTrainingDays);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        resolveEffectiveUid();              // <- determina cliente vs yo
        getTrainingDaysAndExercises();      // <- carga los datos del UID efectivo

        return view;
    }

    // ===========================
    // Resolución de UID efectivo
    // ===========================
    private void resolveEffectiveUid() {
        // 1) Primero, argumentos del Fragment (si ViewPager los puso)
        Bundle args = getArguments();
        boolean coachModeFromArgs = args != null && args.getBoolean("coachMode", false);
        String targetFromArgs = args != null ? args.getString("targetUid", null) : null;

        // 2) Luego, Intent del Activity (si UsersListActivity -> ViewPagerActivity los pasó)
        Bundle intentExtras = getActivity() != null ? getActivity().getIntent().getExtras() : null;
        boolean coachModeFromIntent = intentExtras != null && intentExtras.getBoolean("coachMode", false);
        String targetFromIntent = intentExtras != null ? intentExtras.getString("targetUid", null) : null;

        // 3) Lógica de prioridad: args > intent > usuario actual
        if (coachModeFromArgs && targetFromArgs != null && !targetFromArgs.trim().isEmpty()) {
            effectiveUid = targetFromArgs;
            Log.i(TAG, "resolveEffectiveUid: coachMode (args) ON -> effectiveUid=" + effectiveUid);
            return;
        }
        if (coachModeFromIntent && targetFromIntent != null && !targetFromIntent.trim().isEmpty()) {
            effectiveUid = targetFromIntent;
            Log.i(TAG, "resolveEffectiveUid: coachMode (intent) ON -> effectiveUid=" + effectiveUid);
            return;
        }
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        effectiveUid = (current != null) ? current.getUid() : null;
        Log.i(TAG, "resolveEffectiveUid: coachMode OFF -> effectiveUid=" + effectiveUid);
    }

    // ===========================
    // Carga de datos
    // ===========================
    private void getTrainingDaysAndExercises() {
        Log.d(TAG, "getTrainingDaysAndExercises: Iniciando carga. effectiveUid=" + effectiveUid);
        if (effectiveUid == null || effectiveUid.trim().isEmpty()) {
            Log.e(TAG, "getTrainingDaysAndExercises: ❌ effectiveUid es NULL/empty. Abortando.");
            return;
        }

        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(effectiveUid)
                .child("userMovementPatterns");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                trainingDays.clear();

                if (!snapshot.exists()) {
                    Log.w(TAG, "getTrainingDaysAndExercises: ⚠ No hay userMovementPatterns. Probando fallback userExercises.");
                    loadTrainingDaysFromUserExercises(effectiveUid);
                    return;
                }

                Map<String, TrainingDay> tempDays = new LinkedHashMap<>();
                int dayIndex = 1;

                for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                    String dayName = daySnapshot.child("name").getValue(String.class);
                    if (dayName == null || dayName.trim().isEmpty()) {
                        dayName = "Día " + dayIndex;
                        Log.w(TAG, "getTrainingDaysAndExercises: ⚠ Día sin nombre. Usando " + dayName);
                    }

                    String uniqueKey = dayIndex + "_" + dayName;
                    if (!tempDays.containsKey(uniqueKey)) {
                        TrainingDay td = new TrainingDay();
                        td.setDayTitle(dayName);
                        td.setDayIndex(dayIndex);
                        td.setExercises(new ArrayList<>());
                        tempDays.put(uniqueKey, td);
                        Log.d(TAG, "getTrainingDaysAndExercises: ✅ Día creado: " + uniqueKey);
                    }

                    TrainingDay currentDay = tempDays.get(uniqueKey);
                    DataSnapshot exercisesSnapshot = daySnapshot.child("userExercises");
                    int exerciseCount = 0;

                    for (DataSnapshot exSnap : exercisesSnapshot.getChildren()) {
                        UserExercise ex = exSnap.getValue(UserExercise.class);
                        if (ex == null) {
                            Log.e(TAG, "getTrainingDaysAndExercises: ❌ Ejercicio NULL. Omitiendo...");
                            continue;
                        }
                        // Asegura el nombre del día en el modelo (si tu POJO lo soporta)
                        try {
                            ex.setDayName(dayName);
                        } catch (Exception ignored) { /* compat */ }

                        currentDay.getExercises().add(ex);
                        exerciseCount++;
                    }
                    Log.d(TAG, "getTrainingDaysAndExercises: 📅 " + dayName + " -> " + exerciseCount + " ejercicios");
                    dayIndex++;
                }

                trainingDays.addAll(tempDays.values());
                actualizarRecyclerView();

                // Log final
                for (TrainingDay d : trainingDays) {
                    Log.d(TAG, "RV Día: " + d.getDayTitle() + " | ejercicios=" + d.getExercises().size());
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "getTrainingDaysAndExercises: ❌ Error Firebase", error.toException());
            }
        });
    }

    // Fallback si no existe userMovementPatterns
    private void loadTrainingDaysFromUserExercises(@NonNull String uid) {
        Log.w(TAG, "loadTrainingDaysFromUserExercises: Fallback desde /users/" + uid + "/userExercises");

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(uid)
                .child("userExercises");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                trainingDays.clear();
                if (!snapshot.exists()) {
                    Log.e(TAG, "loadTrainingDaysFromUserExercises: ❌ No hay userExercises.");
                    actualizarRecyclerView();
                    return;
                }

                Map<String, TrainingDay> map = new HashMap<>();
                for (DataSnapshot exSnap : snapshot.getChildren()) {
                    UserExercise ex = exSnap.getValue(UserExercise.class);
                    if (ex == null) continue;

                    String dayName = exSnap.child("dayName").getValue(String.class);
                    if (dayName == null || dayName.trim().isEmpty()) dayName = "Día Desconocido";

                    TrainingDay td = map.get(dayName);
                    if (td == null) {
                        td = new TrainingDay();
                        td.setDayIndex(map.size() + 1);
                        td.setDayTitle(dayName);
                        td.setExercises(new ArrayList<>());
                        map.put(dayName, td);
                    }

                    try { ex.setDayName(dayName); } catch (Exception ignored) {}
                    td.getExercises().add(ex);
                }

                trainingDays.addAll(map.values());
                actualizarRecyclerView();
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadTrainingDaysFromUserExercises: ❌ Error Firebase", error.toException());
            }
        });
    }

    // ===========================
    // UI helpers
    // ===========================
    private void actualizarRecyclerView() {
        Log.d(TAG, "actualizarRecyclerView: días=" + trainingDays.size());
        if (adapter == null) {
            adapter = new TrainingDayAdapter(trainingDays, this::switchToTrainingDetailFragment);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged();
        }
    }

    private void switchToTrainingDetailFragment(@NonNull TrainingDay trainingDay) {
        Log.d(TAG, "switchToTrainingDetailFragment: Abriendo detalle del día " + trainingDay.getDayTitle());
        ViewPagerActivity activity = (ViewPagerActivity) getActivity();
        if (activity == null) {
            Log.e(TAG, "switchToTrainingDetailFragment: ❌ activity NULL");
            return;
        }

        Bundle data = new Bundle();
        data.putSerializable("exercises", (Serializable) trainingDay.getExercises());
        data.putString("dayKey", "Día " + trainingDay.getDayIndex());
        // MUY IMPORTANTE: pasar también el effectiveUid para que el detalle escriba/lea del cliente correcto
        data.putString("effectiveUid", effectiveUid);

        activity.switchToTrainingDetailFragment(data);
    }
}
