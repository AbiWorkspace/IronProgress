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
import java.util.List;
import java.util.Map;

public class DashboardWorkoutFragment extends Fragment {
    private RecyclerView recyclerView;
    private TrainingDayAdapter adapter;
    private List<TrainingDay> trainingDays;
    private static final String TAG = "DashboardWorkoutFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "🔹 onCreateView: Creando vista para DashboardWorkoutFragment.");
        View view = inflater.inflate(R.layout.fragment_dashboardfragment, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewTrainingDays);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        getTrainingDaysAndExercisesFromUser();

        return view;
    }

    private void getTrainingDaysAndExercisesFromUser() {
        Log.d("DashboardWorkoutFragment", "🔹 getTrainingDaysAndExercisesFromUser: Iniciando carga de datos desde Firebase.");

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
                        Log.e("DashboardWorkoutFragment", "❌ No se encontraron `userMovementPatterns` en Firebase.");
                    } else {
                        Log.d("DashboardWorkoutFragment", "✅ Datos recibidos de Firebase. Procesando...");
                    }

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        UserMovementPattern userMovementPattern = dataSnapshot.getValue(UserMovementPattern.class);

                        if (userMovementPattern == null) {
                            Log.e("DashboardWorkoutFragment", "❌ userMovementPattern es NULL. Omitiendo...");
                            continue;
                        }

                        Log.d("DashboardWorkoutFragment", "📅 Día de entrenamiento detectado: " + userMovementPattern.getName());

                        if (userMovementPattern.getUserExercises() == null || userMovementPattern.getUserExercises().isEmpty()) {
                            Log.e("DashboardWorkoutFragment", "⚠ Advertencia: Día " + userMovementPattern.getName() + " no tiene ejercicios asignados.");
                        }

                        TrainingDay trainingDay = new TrainingDay();
                        trainingDay.setDayIndex(trainingDays.size() + 1);
                        trainingDay.setDayTitle(userMovementPattern.getName());
                        trainingDay.setExercises(new ArrayList<>(userMovementPattern.getUserExercises()));

                        Log.d("DashboardWorkoutFragment", "📌 Se han agregado " + trainingDay.getExercises().size() + " ejercicios para este día.");
                        trainingDays.add(trainingDay);
                    }

                    Log.d("DashboardWorkoutFragment", "🔄 Total días de entrenamiento cargados: " + trainingDays.size());

                    // 🔥 Si `trainingDays` sigue vacío, intentar cargar desde `userExercises`
                    if (trainingDays.isEmpty()) {
                        loadTrainingDaysFromUserExercises(userId);
                    }

                    // Configurar el adaptador
                    adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
                        switchToTrainingDetailFragment(trainingDay);
                    });

                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("DashboardWorkoutFragment", "❌ Error al obtener los días de entrenamiento y ejercicios", error.toException());
                }
            });
        }
    }
    private void loadTrainingDaysFromUserExercises(String userId) {
        Log.w(TAG, "⚠ Intentando cargar los días desde `userExercises`...");

        DatabaseReference userExercisesRef = FirebaseDatabase.getInstance()
                .getReference()
                .child("users")
                .child(userId)
                .child("userExercises");

        userExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "❌ No se encontraron `userExercises` en Firebase.");
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

                    Log.d(TAG, "📌 Asignando ejercicio `" + exercise.getName() + "` al día `" + dayName + "`.");

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

                Log.d(TAG, "✅ Total días generados desde `userExercises`: " + trainingDays.size());

                // 🔥 Configurar y actualizar el adaptador
                adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
                    switchToTrainingDetailFragment(trainingDay);
                });

                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al obtener `userExercises`", error.toException());
            }
        });
    }


    private void switchToTrainingDetailFragment(TrainingDay trainingDay) {
        Log.d(TAG, "🔄 switchToTrainingDetailFragment: Cambiando a fragmento de detalles.");
        ViewPagerActivity activity = (ViewPagerActivity) getActivity();

        if (activity != null) {
            Bundle data = new Bundle();
            data.putSerializable("exercises", (Serializable) trainingDay.getExercises());
            activity.switchToTrainingDetailFragment(data);
        } else {
            Log.e(TAG, "❌ No se pudo obtener la referencia a ViewPagerActivity");
        }
    }
}
