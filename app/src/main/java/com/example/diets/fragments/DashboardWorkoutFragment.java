package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.TrainingDayAdapter;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.example.diets.model.UserMovementPattern;
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
    private RecyclerView recyclerView;
    private TrainingDayAdapter adapter;
    private List<TrainingDay> trainingDays = new ArrayList<>();
    private static final String TAG = "DashboardWorkoutFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "🔹 onCreateView: Creando vista para DashboardWorkoutFragment.");
        View view = inflater.inflate(R.layout.fragment_dashboardfragment, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewTrainingDays);

        // 🔥 Configurar RecyclerView para que los días se muestren HORIZONTALMENTE
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        getTrainingDaysAndExercisesFromUser();
        return view;
    }

    private void getTrainingDaysAndExercisesFromUser() {
        Log.d(TAG, "🔹 getTrainingDaysAndExercisesFromUser: Iniciando carga de datos desde Firebase.");

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference()
                    .child("users")
                    .child(userId)
                    .child("userMovementPatterns");

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    trainingDays = new ArrayList<>();

                    if (!snapshot.exists()) {
                        Log.e(TAG, "❌ No se encontraron `userMovementPatterns` en Firebase.");
                        return;
                    }

                    Log.d(TAG, "✅ Datos recibidos de Firebase. Procesando...");

                    Map<String, TrainingDay> tempDays = new LinkedHashMap<>();
                    int dayIndex = 1;

                    for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                        Log.d(TAG, "🔍 Procesando nodo: " + daySnapshot.getKey());

                        String dayName = daySnapshot.child("name").getValue(String.class);
                        if (dayName == null || dayName.isEmpty()) {
                            Log.w(TAG, "⚠️ Día sin nombre en Firebase. Asignando nombre genérico.");
                            dayName = "Día " + dayIndex;
                        }

                        String uniqueKey = dayIndex + "_" + dayName;
                        Log.d(TAG, "📅 Día detectado: " + dayName + " | Clave única: " + uniqueKey);

                        if (!tempDays.containsKey(uniqueKey)) {
                            TrainingDay trainingDay = new TrainingDay();
                            trainingDay.setDayTitle(dayName);
                            trainingDay.setDayIndex(dayIndex);
                            trainingDay.setExercises(new ArrayList<>());
                            tempDays.put(uniqueKey, trainingDay);
                            Log.d(TAG, "✅ Nuevo día creado: " + uniqueKey);
                        }

                        TrainingDay currentDay = tempDays.get(uniqueKey);
                        if (currentDay == null) {
                            Log.e(TAG, "❌ ERROR: No se pudo recuperar el contenedor del día: " + uniqueKey);
                            continue;
                        }

                        DataSnapshot exercisesSnapshot = daySnapshot.child("userExercises");
                        if (!exercisesSnapshot.exists()) {
                            Log.w(TAG, "⚠️ No hay ejercicios para el día: " + uniqueKey);
                        }

                        int exerciseCount = 0;
                        for (DataSnapshot exerciseSnapshot : exercisesSnapshot.getChildren()) {
                            Log.d(TAG, "🔍 Procesando ejercicio...");
                            UserExercise exercise = exerciseSnapshot.getValue(UserExercise.class);
                            if (exercise == null) {
                                Log.e(TAG, "❌ ERROR: Ejercicio NULL encontrado. Omitiendo...");
                                continue;
                            }

                            Log.d(TAG, "🏋️‍♂️ Ejercicio detectado: " + exercise.getName());
                            exercise.setDayName(dayName);  // 🔥 Se usa `setDayName()` en vez de `setDayAssigned()`

                            currentDay.getExercises().add(exercise);
                            exerciseCount++;
                        }

                        Log.d(TAG, "📅 Día `" + uniqueKey + "` contiene " + exerciseCount + " ejercicios.");
                        dayIndex++;
                    }

                    trainingDays.clear();
                    trainingDays.addAll(tempDays.values());

                    Log.d(TAG, "🔄 Total días de entrenamiento cargados: " + trainingDays.size());

                    if (adapter == null) {
                        adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
                            switchToTrainingDetailFragment(trainingDay);
                        });
                        recyclerView.setAdapter(adapter);
                    } else {
                        adapter.notifyDataSetChanged(); // 🔥 Asegurar actualización
                    }

                    // 🔍 DEPURACIÓN FINAL
                    for (TrainingDay day : trainingDays) {
                        Log.d(TAG, "📋 Día en RecyclerView: " + day.getDayTitle() + " | Ejercicios: " + day.getExercises().size());
                        for (UserExercise exercise : day.getExercises()) {
                            Log.d(TAG, "   🏋️‍♂️ Ejercicio: " + exercise.getName() + " | Día asignado: " + exercise.getDayName());
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "❌ Error al obtener los días de entrenamiento y ejercicios", error.toException());
                }
            });
        }
    }




    private void loadTrainingDaysFromUserExercises(String userId) {
        Log.w(TAG, "⚠ Intentando cargar los días desde userExercises...");

        DatabaseReference userExercisesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(userId)
                .child("userExercises");

        userExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "❌ No se encontraron userExercises en Firebase.");
                    return;
                }

                Map<String, TrainingDay> tempDays = new HashMap<>();

                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    UserExercise exercise = exerciseSnapshot.getValue(UserExercise.class);

                    if (exercise == null) {
                        Log.e(TAG, "❌ Ejercicio NULL encontrado. Omitiendo...");
                        continue;
                    }

                    String dayName = exerciseSnapshot.child("dayName").getValue(String.class);
                    if (dayName == null || dayName.isEmpty()) {
                        dayName = "Día Desconocido";
                    }

                    Log.d(TAG, "📌 Asignando ejercicio " + exercise.getName() + " al día " + dayName + ".");

                    if (!tempDays.containsKey(dayName)) {
                        TrainingDay trainingDay = new TrainingDay();
                        trainingDay.setDayIndex(tempDays.size() + 1);
                        trainingDay.setDayTitle(dayName);
                        trainingDay.setExercises(new ArrayList<>());
                        tempDays.put(dayName, trainingDay);
                    }

                    tempDays.get(dayName).getExercises().add(exercise);
                }

                trainingDays.clear();
                trainingDays.addAll(tempDays.values());

                Log.d(TAG, "✅ Total días generados desde userExercises: " + trainingDays.size());
                actualizarRecyclerView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al obtener userExercises", error.toException());
            }
        });
    }

    private void actualizarRecyclerView() {
        Log.d(TAG, "🔄 Actualizando RecyclerView con " + trainingDays.size() + " días.");

        if (adapter == null) {
            adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
                switchToTrainingDetailFragment(trainingDay);
            });
            recyclerView.setAdapter(adapter);
        } else {
            adapter.notifyDataSetChanged(); // 🔥 Asegurar actualización correcta
        }
    }

    private void switchToTrainingDetailFragment(TrainingDay trainingDay) {
        Log.d(TAG, "🔄 switchToTrainingDetailFragment: Cambiando a fragmento de detalles.");
        ViewPagerActivity activity = (ViewPagerActivity) getActivity();

        if (activity != null) {
            Bundle data = new Bundle();
            data.putSerializable("exercises", (Serializable) trainingDay.getExercises());

            // 🔥 NUEVO: Pasamos la clave del día, como "Día 1"
            String dayKey = "Día " + trainingDay.getDayIndex();
            data.putString("dayKey", dayKey);
            Log.d(TAG, "📦 Agregando dayKey al Bundle → " + dayKey);

            activity.switchToTrainingDetailFragment(data);
        } else {
            Log.e(TAG, "❌ No se pudo obtener la referencia a ViewPagerActivity");
        }
    }

}