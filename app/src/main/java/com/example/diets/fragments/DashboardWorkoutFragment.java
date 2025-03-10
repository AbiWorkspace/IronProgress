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
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
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
                        Log.e(TAG, "❌ No se encontraron userMovementPatterns en Firebase.");
                        return;
                    }

                    Log.d(TAG, "✅ Datos recibidos de Firebase. Procesando...");

                    // 🔥 RECORRER userMovementPatterns (que es un HashMap)
                    for (DataSnapshot daySnapshot : snapshot.getChildren()) {
                        String dayName = daySnapshot.child("name").getValue(String.class);
                        if (dayName == null || dayName.isEmpty()) {
                            Log.w(TAG, "⚠️ Día sin nombre en Firebase. Se asignará un nombre genérico.");
                            dayName = "Día " + (trainingDays.size() + 1);
                        }

                        TrainingDay trainingDay = new TrainingDay();
                        trainingDay.setDayTitle(dayName);
                        trainingDay.setDayIndex(trainingDays.size() + 1);

                        // ✅ EXTRAER movementPatterns
                        List<MovementPattern> movementPatterns = new ArrayList<>();
                        DataSnapshot patternsSnapshot = daySnapshot.child("movementPatterns");
                        if (patternsSnapshot.exists()) {
                            for (DataSnapshot patternSnapshot : patternsSnapshot.getChildren()) {
                                MovementPattern pattern = patternSnapshot.getValue(MovementPattern.class);
                                if (pattern != null) {
                                    movementPatterns.add(pattern);
                                }
                            }
                        }
                        trainingDay.setMovementPatterns(movementPatterns);

                        // ✅ EXTRAER userExercises (SOLUCIÓN AL ERROR)
                        List<UserExercise> userExercises = new ArrayList<>();
                        DataSnapshot exercisesSnapshot = daySnapshot.child("userExercises");
                        if (exercisesSnapshot.exists()) {
                            for (DataSnapshot exerciseSnapshot : exercisesSnapshot.getChildren()) {
                                UserExercise exercise = exerciseSnapshot.getValue(UserExercise.class);
                                if (exercise != null) {
                                    userExercises.add(exercise);
                                }
                            }
                        }
                        trainingDay.setExercises(userExercises);

                        Log.d(TAG, "📅 Día detectado: " + dayName + " con " + userExercises.size() + " ejercicios.");
                        trainingDays.add(trainingDay);
                    }

                    Log.d(TAG, "🔄 Total días de entrenamiento cargados: " + trainingDays.size());

                    // 🔥 ACTUALIZAR ADAPTADOR
                    adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
                        switchToTrainingDetailFragment(trainingDay);
                    });

                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
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
        // 🔥 Configurar y actualizar el adaptador con todos los días cargados
        adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
            switchToTrainingDetailFragment(trainingDay);
        });

        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
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