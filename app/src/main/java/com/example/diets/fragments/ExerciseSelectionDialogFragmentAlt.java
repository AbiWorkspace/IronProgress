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
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
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

public class ExerciseSelectionDialogFragmentAlt  extends DialogFragment implements ExercisesAdapter.OnExerciseSelectionChangedListener {
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
                loadExercises(trainingDay.getDayTitle());

            }
        }

        doneButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated: Botón doneButton presionado");

            if (listener != null && trainingDay != null) {
                listener.onExercisesSelected(selectedExercises, trainingDay);
                for (Exercise exercise : selectedExercises) {
                    Log.d(TAG, "✅ Enviado ejercicio seleccionado: " + exercise.getName());
                }
            } else {
                Log.w(TAG, "⚠ Listener o TrainingDay es nulo al intentar enviar ejercicios.");
            }

            dismiss();
        });

        if (trainingDay != null) {
            String dayKey = trainingDay.getDayTitle(); // Por ejemplo: "Día 1"

            obtenerMovementPatternDelDia(dayKey, pattern -> {
                if (pattern != null) {
                    Log.d(TAG, "📌 MovementPattern cargado:");
                    Log.d(TAG, "🧠 Nombre: " + pattern.getName());
                    Log.d(TAG, "💪 Músculos sinérgicos: " + pattern.getSynergistMuscles());

                    // ✅ Mostrar información básica en un AlertDialog
                    if (getContext() != null) {

                    }

                } else {
                    Log.w(TAG, "⚠ No se encontró el patrón de movimiento para el día " + dayKey);
                }
            });
        }





    }

    private void loadExercises(String dayKey) {
        obtenerMovementPatternDelDia(dayKey, pattern -> {
            if (pattern == null) {
                Log.w(TAG, "⚠ No se pudo cargar el patrón de movimiento para " + dayKey);
                return;
            }

            Log.d(TAG, "🔄 Cargando ejercicios usando patrón de movimiento completo: " + pattern.getName());

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
                                Log.d(TAG, "-----------------------------");
                                Log.d(TAG, "🛠 Evaluando patrón de movimiento: " + pattern.getName());

                                if (hasMatchingMuscles(exercise, pattern)) {
                                    Log.d(TAG, "✅ Ejercicio ACEPTADO: " + exercise.getName() + " para el patrón " + pattern.getName());
                                    exercises.add(exercise);
                                } else {
                                    Log.d(TAG, "❌ Ejercicio RECHAZADO: " + exercise.getName() + " para el patrón " + pattern.getName());
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
        });
    }


    private boolean hasMatchingMuscles(Exercise exercise, MovementPattern pattern) {
        boolean ejercicioIncompleto = (exercise.getSynergistMuscles() == null || exercise.getSynergistMuscles().isEmpty()) &&
                (exercise.getMusclesWorked() == null || exercise.getMusclesWorked().isEmpty());
        boolean patronIncompleto = (pattern.getSynergistMuscles() == null || pattern.getSynergistMuscles().isEmpty()) &&
                (pattern.getMusclesWorked() == null || pattern.getMusclesWorked().isEmpty());

        if (ejercicioIncompleto || patronIncompleto) {
            Log.w(TAG, "⚠ hasMatchingMuscles: Datos insuficientes para comparar.");
            return false;
        }

        // 🔄 Normalizamos músculos del patrón
        Set<String> patternMuscles = new HashSet<>();
        if (pattern.getSynergistMuscles() != null) {
            for (String muscle : pattern.getSynergistMuscles()) {
                patternMuscles.add(normalizeMuscleName(muscle));
            }
        }
        if (pattern.getMusclesWorked() != null) {
            for (String muscle : pattern.getMusclesWorked()) {
                patternMuscles.add(normalizeMuscleName(muscle));
            }
        }

        // 🔄 Normalizamos músculos del ejercicio
        Set<String> exerciseMuscles = new HashSet<>();
        if (exercise.getSynergistMuscles() != null) {
            for (String muscle : exercise.getSynergistMuscles()) {
                exerciseMuscles.add(normalizeMuscleName(muscle));
            }
        }
        if (exercise.getMusclesWorked() != null && !exercise.getMusclesWorked().isEmpty()) {
            List<String> worked = extractMuscleList(exercise.getMusclesWorked()); // Usa método que limpia y divide
            for (String muscle : worked) {
                exerciseMuscles.add(normalizeMuscleName(muscle));
            }
        }

        // 🧠 Logs detallados
        Log.d(TAG, "🔍 Evaluando ejercicio: " + exercise.getName());
        Log.d(TAG, "📌 Músculos del patrón (normalizados): " + patternMuscles);
        Log.d(TAG, "📌 Músculos del ejercicio (normalizados): " + exerciseMuscles);

        for (String muscle : exerciseMuscles) {
            if (patternMuscles.contains(muscle)) {
                Log.d(TAG, "✅ Coincidencia encontrada: " + muscle);
                return true;
            }
        }

        Log.d(TAG, "❌ No se encontraron coincidencias para el ejercicio: " + exercise.getName());
        return false;
    }




    private void obtenerMovementPatternDelDia(String dayKey, MovementPatternCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ Usuario no autenticado.");
            return;
        }

        DatabaseReference dayRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getUid())
                .child("userMovementPatterns")
                .child(dayKey);

        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "📥 Datos encontrados para " + dayKey);

                    String name = snapshot.child("name").getValue(String.class);
                    List<String> synergists = new ArrayList<>();
                    List<String> workedMuscles = new ArrayList<>();

                    for (DataSnapshot muscleSnap : snapshot.child("synergistMuscles").getChildren()) {
                        synergists.add(muscleSnap.getValue(String.class));
                    }

                    for (DataSnapshot muscleSnap : snapshot.child("musclesWorked").getChildren()) {
                        workedMuscles.add(muscleSnap.getValue(String.class));
                    }

                    MovementPattern pattern = new MovementPattern();
                    pattern.setName(name);
                    pattern.setSynergistMuscles(synergists);
                    pattern.setMusclesWorked(workedMuscles);

                    Log.d(TAG, "✅ MovementPattern creado: " + pattern.getName() +
                            " con sinérgicos → " + pattern.getSynergistMuscles() +
                            " y trabajados → " + pattern.getMusclesWorked());

                    callback.onPatternLoaded(pattern);
                } else {
                    Log.w(TAG, "⚠ No se encontraron datos para " + dayKey);
                    callback.onPatternLoaded(null);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al leer el patrón de movimiento: ", error.toException());
                callback.onPatternLoaded(null);
            }
        });
    }



    public interface MovementPatternCallback {
        void onPatternLoaded(MovementPattern pattern);
    }




    private Exercise mapToExercise(Map<String, Object> dataMap) {
        Exercise exercise = new Exercise();
        exercise.setName((String) dataMap.getOrDefault("name", ""));
        exercise.setEquipmentType((String) dataMap.getOrDefault("equipmentType", ""));
        exercise.setExerciseType((String) dataMap.getOrDefault("exerciseType", ""));
        exercise.setLoadIntensity((String) dataMap.getOrDefault("loadIntensity", ""));
        exercise.setDescription((String) dataMap.getOrDefault("description", ""));
        exercise.setMovementType((String) dataMap.getOrDefault("movementType", ""));

        // ✅ EXTRAER imagen en base64
        exercise.setImage((String) dataMap.getOrDefault("image", ""));

        // ✅ Convertimos "Espalda, Bíceps" en ["Espalda", "Bíceps"]
        Object musclesWorkedData = dataMap.get("musclesWorked");
        if (musclesWorkedData instanceof String) {
            List<String> muscleList = extractMuscleList((String) musclesWorkedData);
            exercise.setMusclesWorked(String.join(", ", muscleList));
        } else {
            exercise.setMusclesWorked("");
        }

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
