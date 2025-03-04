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
import com.example.diets.adapters.TrainingDayAdapter;
import com.example.diets.adapters.TrainingDayDetailAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.Muscle;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class TrainingDayDetailFragment2 extends Fragment {
    private static final String ARG_TRAINING_DAY = "training_day";
    private TrainingDay trainingDay;
    private RecyclerView recyclerView;
    private TrainingDayAdapter adapter;
    private List<TrainingDay> trainingDays;

    public static TrainingDayDetailFragment2 newInstance(TrainingDay trainingDay) {
        TrainingDayDetailFragment2 fragment = new TrainingDayDetailFragment2();
        Bundle args = new Bundle();
        args.putSerializable(ARG_TRAINING_DAY, trainingDay);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            trainingDay = (TrainingDay) getArguments().getSerializable(ARG_TRAINING_DAY);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_training_day_detail2, container, false);
        recyclerView = view.findViewById(R.id.recyclerViewTrainingDayDetails);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Cargar la información del día de entrenamiento
        loadTrainingDayDetails();

        return view;
    }

    private void loadTrainingDayDetails() {
        Log.d("TrainingDayDetailFragment2", "loadTrainingDayDetails: Cargando detalles del día de entrenamiento");
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            Log.d("TrainingDayDetailFragment2", "loadTrainingDayDetails: Usuario autenticado con userId " + userId);
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                    .child("users")
                    .child(userId)
                    .child("userMovementPatterns")
                    .child(String.valueOf(trainingDay.getDayIndex()));

            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Log.d("TrainingDayDetailFragment2", "loadTrainingDayDetails: onDataChange: Datos recibidos de Firebase");
                    trainingDays = new ArrayList<>();
                    UserMovementPattern userMovementPattern = snapshot.getValue(UserMovementPattern.class);

                    if (userMovementPattern != null) {
                        // Convertir UserMovementPattern a MovementPattern
                        MovementPattern movementPattern = convertToMovementPattern(userMovementPattern);

                        trainingDay.setMovementPatterns(Collections.singletonList(movementPattern));
                        trainingDays.add(trainingDay);

                        Log.d("TrainingDayDetailFragment2", "loadTrainingDayDetails: onDataChange: Día de entrenamiento añadido: " + trainingDay.getDayIndex());
                    } else {
                        Log.e("TrainingDayDetailFragment2", "loadTrainingDayDetails: onDataChange: userMovementPattern es nulo");
                    }

                    // Configurar el adaptador del RecyclerView
                    adapter = new TrainingDayAdapter(trainingDays, trainingDay -> {
                        // Manejar el clic en un día de entrenamiento
                        Log.d("TrainingDayDetailFragment2", "loadTrainingDayDetails: onDataChange: Día de entrenamiento clicado: " + trainingDay);
                    });
                    recyclerView.setAdapter(adapter);
                    Log.d("TrainingDayDetailFragment2", "loadTrainingDayDetails: onDataChange: Adapter configurado con los días de entrenamiento");
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("TrainingDayDetailFragment2", "loadTrainingDayDetails: onCancelled: Error al obtener los días de entrenamiento", error.toException());
                }
            });
        } else {
            Log.e("TrainingDayDetailFragment2", "loadTrainingDayDetails: Usuario no autenticado");
        }
    }

    /**
     * Convierte un `UserMovementPattern` en un `MovementPattern`, asegurando la conversión de ID de String a int.
     */
    private MovementPattern convertToMovementPattern(UserMovementPattern userMovementPattern) {
        int id = 0; // ID predeterminado en caso de error
        try {
            // ✅ Convertimos el ID a int si es necesario
            Object idObject = userMovementPattern.getId();
            if (idObject instanceof String) {
                id = Integer.parseInt((String) idObject);
            } else if (idObject instanceof Number) {
                id = ((Number) idObject).intValue();
            } else {
                Log.e("TrainingDayDetailFragment2", "convertToMovementPattern: ID en formato inesperado: " + idObject);
            }
        } catch (Exception e) {
            Log.e("TrainingDayDetailFragment2", "Error al convertir ID a int", e);
        }

        // ✅ Convertir manualmente UserExercise a Exercise
        List<Exercise> convertedExercises = new ArrayList<>();
        for (UserExercise userExercise : userMovementPattern.getUserExercises()) {
            Exercise exercise = new Exercise();
            exercise.setName(userExercise.getName());
            exercise.setEquipmentType(userExercise.getEquipmentType());
            exercise.setExerciseType(userExercise.getExerciseType());
            exercise.setMusclesWorked(userExercise.getMusclesWorked());
            exercise.setLoadIntensity(userExercise.getLoadIntensity());
            exercise.setImage(userExercise.getImage());
            exercise.setDescription(userExercise.getDescription());
            exercise.setMovementType(userExercise.getMovementType());

            // ✅ Convertir List<String> a List<Muscle>
            List<Muscle> muscles = new ArrayList<>();
            if (userExercise.getSynergistMuscles() != null) {
                for (Object obj : userExercise.getSynergistMuscles()) {
                    if (obj instanceof String) {
                        try {
                            muscles.add(new Muscle(id, (String) obj)); // Convertimos ID a int correctamente
                        } catch (Exception e) {
                            Log.e("TrainingDayDetailFragment2", "Error al crear Muscle: " + e.getMessage());
                        }
                    } else {
                        Log.e("TrainingDayDetailFragment2", "convertToMovementPattern: SynergistMuscles en formato inesperado");
                    }
                }
            }
            List<String> muscleNames = new ArrayList<>();
            if (userExercise.getSynergistMuscles() != null) {
                for (Object obj : userExercise.getSynergistMuscles()) {
                    if (obj instanceof String) {
                        muscleNames.add((String) obj);
                    } else if (obj instanceof Muscle) {
                        muscleNames.add(((Muscle) obj).getName());
                    }
                }
            }
            exercise.setSynergistMuscles(muscleNames);


            convertedExercises.add(exercise);
        }

        return new MovementPattern(
                String.valueOf(id), // ✅ Convertimos el ID a String
                userMovementPattern.getName(),
                new ArrayList<>(), // Lista de músculos sinergistas vacía (si es necesario, poblarla)
                new ArrayList<>()
        );
    }
}
