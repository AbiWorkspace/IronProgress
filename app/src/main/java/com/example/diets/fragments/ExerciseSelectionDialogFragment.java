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
import java.util.List;
import java.util.Map;
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
                listener.onExercisesSelected(new ArrayList<>(selectedExercises), trainingDay);
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
                Log.d(TAG, "loadExercises: Número de ejercicios recuperados: " + snapshot.getChildrenCount());

                if (!snapshot.exists()) {
                    Log.e(TAG, "loadExercises: No hay ejercicios en la base de datos.");
                    return;
                }

                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    Object data = exerciseSnapshot.getValue();

                    if (data instanceof Map) {
                        // Convertimos el HashMap en un objeto Exercise
                        Map<String, Object> dataMap = (Map<String, Object>) data;
                        Exercise exercise = mapToExercise(dataMap);

                        if (exercise != null) {
                            Log.d(TAG, "loadExercises: ✅ Ejercicio recuperado: " + exercise.getName());
                            exercises.add(exercise);
                        }
                    } else {
                        Log.e(TAG, "loadExercises: ❌ Tipo de datos incorrecto: " + (data != null ? data.getClass().getSimpleName() : "null"));
                    }
                }

                setupRecyclerView();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "loadExercises: Error de base de datos: " + error.getMessage());
            }
        });
    }

    private Exercise mapToExercise(Map<String, Object> dataMap) {
        Exercise exercise = new Exercise();
        exercise.setName((String) dataMap.getOrDefault("name", ""));
        exercise.setEquipmentType((String) dataMap.getOrDefault("equipmentType", ""));
        exercise.setExerciseType((String) dataMap.getOrDefault("exerciseType", ""));
        exercise.setMusclesWorked((String) dataMap.getOrDefault("musclesWorked", ""));
        exercise.setLoadIntensity((String) dataMap.getOrDefault("loadIntensity", ""));
        exercise.setImage((String) dataMap.getOrDefault("image", ""));
        exercise.setDescription((String) dataMap.getOrDefault("description", ""));
        exercise.setMovementType((String) dataMap.getOrDefault("movementType", ""));

        // ✅ Conversión de listas
        exercise.setSynergistMuscles(convertToStringList(dataMap.get("synergistMuscles")));
        exercise.setGoals(convertToStringList(dataMap.get("goals")));
        exercise.setMovementPatterns(convertToMovementPatternList(dataMap.get("movementPatterns")));

        return exercise;
    }

    // ✅ Convierte un objeto en List<String>
    private List<String> convertToStringList(Object obj) {
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        return new ArrayList<>();
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
