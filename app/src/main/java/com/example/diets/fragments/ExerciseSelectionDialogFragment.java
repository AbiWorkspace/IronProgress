package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExerciseSelectionDialogFragment extends DialogFragment implements ExercisesAdapter.OnExerciseSelectionChangedListener {
    private static final String TAG = "ExerciseSelectionDialogFragment";
    private RecyclerView recyclerView;
    private Button doneButton;
    private BottomNavigationView bottomNavigationView;
    private List<Exercise> exercises = new ArrayList<>();
    private List<Exercise> selectedExercises = new ArrayList<>();
    private DatabaseReference mDatabase;
    private ExercisesAdapter adapter;
    private TrainingDay trainingDay; // ✅ Almacena el día al que pertenecen los ejercicios
    private SelectExercisesFragment.OnExercisesSelectedListener listener; // ✅ Listener para enviar datos

    public void setOnExercisesSelectedListener(SelectExercisesFragment.OnExercisesSelectedListener listener) {
        this.listener = listener;
    }
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_selection_dialog, container, false);
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        doneButton = view.findViewById(R.id.doneButton);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            Log.d(TAG, "onViewCreated: RecyclerView inicializado");
        }

        Bundle args = getArguments();
        if (args != null) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            if (trainingDay != null) {
                Log.d(TAG, "onViewCreated: TrainingDay recibido: " + trainingDay);
                loadExercises(trainingDay.getMovementPatterns());
            }
        }

        doneButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated: Botón doneButton presionado");

            if (listener != null && trainingDay != null) {
                for (Exercise exercise : selectedExercises) {
                    listener.onExercisesSelected(selectedExercises, trainingDay);

                    Log.d(TAG, "✅ Enviado ejercicio seleccionado: " + exercise.getName());
                }
            } else {
                Log.w(TAG, "⚠ Listener o TrainingDay es nulo al intentar enviar ejercicios.");
            }

            dismiss();
        });

    }

    private void loadExercises(List<MovementPattern> movementPatterns) {
        DatabaseReference exercisesRef = mDatabase.child("exercises");
        exercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                exercises.clear();
                Log.d(TAG, "📌 Número total de ejercicios en Firebase: " + snapshot.getChildrenCount());

                if (!snapshot.exists()) {
                    Log.e(TAG, "❌ ERROR: No hay ejercicios en la base de datos.");
                    return;
                }

                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    Object data = exerciseSnapshot.getValue();
                    if (data instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) data;
                        Exercise exercise = mapToExercise(dataMap);

                        if (exercise != null) {
                            // 🔥 Evaluar el ejercicio para cada patrón de movimiento
                            for (MovementPattern pattern : movementPatterns) {
                                Log.d(TAG, "-----------------------------");
                                Log.d(TAG, "🛠 Evaluando patrón de movimiento: " + pattern.getName());

                                if (hasMatchingMuscles(exercise, pattern)) {
                                    Log.d(TAG, "✅ Ejercicio ACEPTADO: " + exercise.getName() + " para el patrón " + pattern.getName());
                                    exercises.add(exercise);
                                    break; // Evita agregar duplicados si coincide con múltiples patrones
                                } else {
                                    Log.d(TAG, "❌ Ejercicio RECHAZADO: " + exercise.getName() + " para el patrón " + pattern.getName());
                                }
                            }
                        }
                    }
                }

                setupRecyclerView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ ERROR en Firebase: " + error.getMessage());
            }
        });
    }

    private boolean hasMatchingMuscles(Exercise exercise, MovementPattern pattern) {
        if ((exercise.getSynergistMuscles() == null || exercise.getSynergistMuscles().isEmpty()) &&
                (exercise.getMusclesWorked() == null || exercise.getMusclesWorked().isEmpty()) ||
                pattern.getSynergistMuscles() == null || pattern.getSynergistMuscles().isEmpty()) {
            Log.w(TAG, "⚠ hasMatchingMuscles: Datos insuficientes para comparar.");
            return false;
        }

        // 🔥 Normaliza todos los músculos en sets
        Set<String> patternMuscles = new HashSet<>();
        for (String muscle : pattern.getSynergistMuscles()) {
            patternMuscles.add(normalizeMuscleName(muscle));
        }

        Set<String> exerciseMuscles = new HashSet<>();
        exerciseMuscles.addAll(exercise.getSynergistMuscles()); // Músculos sinérgicos
        exerciseMuscles.addAll(extractMuscleList(String.join(",", exercise.getMusclesWorked()))); // Músculos trabajados

        // 📌 Logs detallados para depuración
        Log.d(TAG, "🔍 Evaluando ejercicio: " + exercise.getName());
        Log.d(TAG, "📌 Músculos del patrón (normalizados): " + patternMuscles);
        Log.d(TAG, "📌 Todos los músculos del ejercicio (normalizados): " + exerciseMuscles);

        // 🔄 Verificar coincidencias en los músculos normalizados
        for (String muscle : exerciseMuscles) {
            if (patternMuscles.contains(muscle)) {
                Log.d(TAG, "✅ Coincidencia encontrada para el músculo: " + muscle);
                return true;
            }
        }

        Log.d(TAG, "❌ No se encontraron coincidencias para el ejercicio " + exercise.getName());
        return false;
    }






    private Exercise mapToExercise(Map<String, Object> dataMap) {
        Exercise exercise = new Exercise();
        exercise.setName((String) dataMap.getOrDefault("name", ""));
        exercise.setEquipmentType((String) dataMap.getOrDefault("equipmentType", ""));
        exercise.setExerciseType((String) dataMap.getOrDefault("exerciseType", ""));
        exercise.setLoadIntensity((String) dataMap.getOrDefault("loadIntensity", ""));
        exercise.setImage((String) dataMap.getOrDefault("image", ""));
        exercise.setDescription((String) dataMap.getOrDefault("description", ""));
        exercise.setMovementType((String) dataMap.getOrDefault("movementType", ""));

        // ✅ Convertimos "Espalda, Bíceps" en ["Espalda", "Bíceps"]
        Object musclesWorkedData = dataMap.get("musclesWorked");
        if (musclesWorkedData instanceof String) {
            List<String> muscleList = extractMuscleList((String) musclesWorkedData);
            exercise.setMusclesWorked(String.join(", ", muscleList)); // 🔥 Convierte la lista en String
        } else {
            exercise.setMusclesWorked(""); // Si es nulo, lo dejamos vacío
        }

        // ✅ Extraer músculos sinérgicos correctamente
        exercise.setSynergistMuscles(extractMuscleNames(dataMap.get("synergistMuscles")));

        return exercise;
    }



    private List<String> extractMuscleNames(Object obj) {
        List<String> muscleNames = new ArrayList<>();

        if (obj instanceof List) {
            List<?> muscleList = (List<?>) obj;
            for (Object muscle : muscleList) {
                if (muscle instanceof Map) {
                    Map<String, Object> muscleMap = (Map<String, Object>) muscle;
                    String muscleName = (String) muscleMap.get("name"); // ✅ Extraer el nombre del músculo
                    if (muscleName != null) {
                        muscleNames.add(muscleName);
                    }
                }
            }
        }

        return muscleNames;
    }

    private List<String> extractMuscleList(String musclesWorked) {
        List<String> muscleList = new ArrayList<>();
        if (musclesWorked != null && !musclesWorked.isEmpty()) {
            // 🔥 Divide la cadena por comas y limpia los espacios en blanco
            String[] splitMuscles = musclesWorked.split(",");
            for (String muscle : splitMuscles) {
                muscleList.add(normalizeMuscleName(muscle.trim())); // 🔥 Normaliza nombres
            }
        }
        return muscleList;
    }


    // ✅ Convierte un objeto en List<MovementPattern>
    private List<MovementPattern> convertToMovementPatternList(Object obj) {
        if (obj instanceof List) {
            List<MovementPattern> movementPatterns = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof Map) {
                    movementPatterns.add(mapToMovementPattern((Map<String, Object>) item));
                }
            }
            return movementPatterns;
        }
        return new ArrayList<>();
    }

    // ✅ Convierte un Map en MovementPattern
    private MovementPattern mapToMovementPattern(Map<String, Object> dataMap) {
        MovementPattern pattern = new MovementPattern();
        pattern.setName((String) dataMap.getOrDefault("name", ""));
        return pattern;
    }
    private String normalizeMuscleName(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase()
                .replaceAll("[áÁ]", "a")
                .replaceAll("[éÉ]", "e")
                .replaceAll("[íÍ]", "i")
                .replaceAll("[óÓ]", "o")
                .replaceAll("[úÚüÜ]", "u")
                .replaceAll("[^a-zñ]", ""); // Elimina caracteres no alfabéticos, espacios y símbolos raros
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            List<String> selectedExerciseNames = selectedExercises.stream()
                    .map(Exercise::getName)
                    .collect(Collectors.toList());

            adapter = new ExercisesAdapter(exercises, selectedExerciseNames, exercise -> {
                addExerciseToSelection(exercise);
                Log.d(TAG, "onExerciseClick: Ejercicio seleccionado: " + exercise.getName());
                showExerciseDetails(exercise);
            }, this);

            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            Log.d(TAG, "setupRecyclerView: ✅ Adaptador de ejercicios configurado.");
        } else {
            Log.e(TAG, "setupRecyclerView: ❌ RecyclerView es nulo");
        }
    }





    private void showExerciseDetails(Exercise exercise) {
        ExerciseDetailsFragment detailsFragment = ExerciseDetailsFragment.newInstance(exercise);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }
    public void addExerciseToSelection(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
            Log.d(TAG, "addExerciseToSelection: ✅ Ejercicio añadido: " + exercise.getName());
        } else {
            selectedExercises.remove(exercise);
            Log.d(TAG, "addExerciseToSelection: ❌ Ejercicio eliminado: " + exercise.getName());
        }
        adapter.notifyDataSetChanged();
    }


    @Override
    public void onExerciseSelected(Exercise exercise) {
        addExerciseToSelection(exercise);
    }

    @Override
    public void onExerciseDeselected(Exercise exercise) {
        selectedExercises.remove(exercise);
        Log.d(TAG, "onExerciseDeselected: Ejercicio eliminado: " + exercise.getName());
        adapter.notifyDataSetChanged();
    }
}
