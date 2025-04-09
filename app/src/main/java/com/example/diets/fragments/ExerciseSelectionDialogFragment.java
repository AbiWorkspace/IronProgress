package com.example.diets.fragments;

import static com.example.diets.R.id.fragment_container;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.google.firebase.database.*;

import java.util.*;
import java.util.stream.Collectors;

public class ExerciseSelectionDialogFragment extends DialogFragment implements ExercisesAdapter.OnExerciseSelectionChangedListener {

    private static final String TAG = "ExerciseSelectionDialog";
    private RecyclerView recyclerView;
    private Button doneButton;
    private List<Exercise> exercises = new ArrayList<>();
    private List<Exercise> selectedExercises = new ArrayList<>();
    private DatabaseReference mDatabase;
    private ExercisesAdapter adapter;
    private TrainingDay trainingDay;
    private SelectExercisesFragment.OnExercisesSelectedListener listener;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Bundle args = getArguments();
        if (args != null) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            if (trainingDay != null) {
                loadExercises(trainingDay.getMovementPatterns());
            }
        }

        doneButton.setOnClickListener(v -> {
            if (listener != null && trainingDay != null) {
                listener.onExercisesSelected(selectedExercises, trainingDay);
                Log.d(TAG, "✅ Ejercicios seleccionados enviados: " + selectedExercises.size());
            } else {
                Log.w(TAG, "⚠ Listener o TrainingDay es nulo.");
            }
            dismiss();
        });
    }

    private void loadExercises(List<MovementPattern> movementPatterns) {
        Log.d(TAG, "🔄 Iniciando carga optimizada de ejercicios...");
        Set<String> normalizedPatternMuscles = new HashSet<>();
        for (MovementPattern pattern : movementPatterns) {
            if (pattern.getMusclesWorked() != null)
                for (String m : pattern.getMusclesWorked())
                    normalizedPatternMuscles.add(normalizeMuscleName(m));
            if (pattern.getSynergistMuscles() != null)
                for (String m : pattern.getSynergistMuscles())
                    normalizedPatternMuscles.add(normalizeMuscleName(m));
        }

        mDatabase.child("exercises").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.e(TAG, "❌ No hay ejercicios en Firebase.");
                    return;
                }

                List<Exercise> filtered = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Map<String, Object> dataMap = (Map<String, Object>) child.getValue();
                    if (dataMap == null) continue;
                    Exercise ex = mapToExercise(dataMap);
                    Set<String> exMuscles = getAllNormalizedMuscles(ex);
                    if (!Collections.disjoint(normalizedPatternMuscles, exMuscles)) {
                        filtered.add(ex);
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    exercises.clear();
                    exercises.addAll(filtered);
                    setupRecyclerView();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error Firebase: " + error.getMessage());
            }
        });
    }

    private Set<String> getAllNormalizedMuscles(Exercise exercise) {
        Set<String> result = new HashSet<>();
        result.addAll(extractMuscleList(exercise.getMusclesWorked()));
        for (Object item : exercise.getSynergistMuscles()) {
            if (item instanceof Map) {
                Object name = ((Map<?, ?>) item).get("name");
                if (name != null) result.add(normalizeMuscleName(name.toString()));
            } else if (item instanceof String) {
                result.add(normalizeMuscleName((String) item));
            }
        }
        return result;
    }

    private Exercise mapToExercise(Map<String, Object> dataMap) {
        Exercise ex = new Exercise();
        ex.setName((String) dataMap.getOrDefault("name", ""));
        ex.setEquipmentType((String) dataMap.getOrDefault("equipmentType", ""));
        ex.setExerciseType((String) dataMap.getOrDefault("exerciseType", ""));
        ex.setLoadIntensity((String) dataMap.getOrDefault("loadIntensity", ""));
        ex.setDescription((String) dataMap.getOrDefault("description", ""));
        ex.setMovementType((String) dataMap.getOrDefault("movementType", ""));

        Object musclesWorkedData = dataMap.get("musclesWorked");
        if (musclesWorkedData instanceof String) {
            ex.setMusclesWorked((String) musclesWorkedData);
        } else {
            ex.setMusclesWorked("");
        }

        // ✅ Aquí está la clave para que funcione:
        ex.setImage((String) dataMap.getOrDefault("image", ""));

        ex.setSynergistMuscles(extractMuscleNames(dataMap.get("synergistMuscles")));
        return ex;
    }


    private List<String> extractMuscleNames(Object obj) {
        List<String> names = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                if (item instanceof Map) {
                    Object name = ((Map<?, ?>) item).get("name");
                    if (name != null) names.add(name.toString());
                }
            }
        }
        return names;
    }

    private List<String> extractMuscleList(String str) {
        if (str == null || str.isEmpty()) return new ArrayList<>();
        return Arrays.stream(str.split(","))
                .map(String::trim)
                .map(this::normalizeMuscleName)
                .collect(Collectors.toList());
    }

    private String normalizeMuscleName(String input) {
        if (input == null) return "";
        return input.trim().toLowerCase()
                .replaceAll("[áÁ]", "a")
                .replaceAll("[éÉ]", "e")
                .replaceAll("[íÍ]", "i")
                .replaceAll("[óÓ]", "o")
                .replaceAll("[úÚüÜ]", "u")
                .replaceAll("[^a-zñ]", "");
    }

    private void setupRecyclerView() {
        List<String> selectedNames = selectedExercises.stream()
                .map(Exercise::getName)
                .collect(Collectors.toList());

        adapter = new ExercisesAdapter(exercises, selectedNames, exercise -> {
            addExerciseToSelection(exercise);
            showExerciseDetails(exercise);
        }, this);

        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        Log.d(TAG, "✅ RecyclerView configurado.");
    }

    private void showExerciseDetails(Exercise exercise) {
        ExerciseDetailsFragment detailsFragment = ExerciseDetailsFragment.newInstance(exercise);
        getParentFragmentManager().beginTransaction()
                .replace(fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }

    public void addExerciseToSelection(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
            Log.d(TAG, "✅ Ejercicio añadido: " + exercise.getName());
        } else {
            selectedExercises.remove(exercise);
            Log.d(TAG, "❌ Ejercicio eliminado: " + exercise.getName());
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
        adapter.notifyDataSetChanged();
        Log.d(TAG, "❌ Ejercicio deseleccionado: " + exercise.getName());
    }
}
