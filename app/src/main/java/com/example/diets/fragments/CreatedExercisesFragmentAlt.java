package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.AddExerciseDialogFragment;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.google.common.reflect.TypeToken;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
public class CreatedExercisesFragmentAlt extends Fragment implements AddExerciseDialogFragment.AddExerciseListener,
        ExercisesAdapter.OnExerciseClickListener, ExercisesAdapter.OnExerciseSelectionChangedListener {

    private static final String TAG = "CreatedExercisesFragmentAlt";
    private RecyclerView recyclerView;
    private Button doneButton, deleteExerciseButton, addExerciseButton;
    private List<Exercise> exercises = new ArrayList<>();
    private List<Exercise> selectedExercises = new ArrayList<>();
    private static final String FILE_NAME = "exercises.json";
    private DatabaseReference mDatabase;
    private ExercisesAdapter adapter;

    private SelectExercisesFragment.OnExercisesSelectedListener listener; // ✅ Interfaz global
    private TrainingDay trainingDay; // ✅ Día de entrenamiento recibido como argumento

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_created_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        doneButton = view.findViewById(R.id.doneButton);
        addExerciseButton = view.findViewById(R.id.add_exercise_button);
        deleteExerciseButton = view.findViewById(R.id.delete_exercise_button);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // 🔥 Obtener el día actual y cargar ejercicios
        Bundle args = getArguments();
        if (args != null && args.containsKey("trainingDay")) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            if (trainingDay != null) {
                Log.d(TAG, "📌 Día recibido: " + trainingDay.getDayTitle());
                loadExercisesFromInternalStorage(trainingDay.getMovementPatterns());
            } else {
                Log.w(TAG, "⚠️ TrainingDay es NULL");
                loadExercisesFromInternalStorage(null);
            }
        } else {
            Log.w(TAG, "⚠️ No se recibió TrainingDay");
            loadExercisesFromInternalStorage(null);
        }

        // ✅ Enviar ejercicios seleccionados al fragmento padre
        doneButton.setOnClickListener(v -> {
            if (listener != null && trainingDay != null) {
                listener.onExercisesSelected(selectedExercises, trainingDay);
                Log.d(TAG, "✅ Enviados " + selectedExercises.size() + " ejercicios seleccionados.");
            } else {
                Log.w(TAG, "⚠️ Listener o TrainingDay es null");
            }
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        addExerciseButton.setOnClickListener(v -> {
            AddExerciseDialogFragment dialogFragment = new AddExerciseDialogFragment();
            dialogFragment.setAddExerciseListener(this);
            dialogFragment.show(getParentFragmentManager(), "AddExerciseDialogFragment");
        });

        deleteExerciseButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    // ✅ Método para recibir listener desde MenuPopupFragmentAlt
    public void setOnExercisesSelectedListener(SelectExercisesFragment.OnExercisesSelectedListener listener) {
        this.listener = listener;
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Ejercicio")
                .setMessage("¿Deseas eliminar los ejercicios seleccionados?")
                .setPositiveButton("Sí", (dialog, which) -> deleteSelectedExercises())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteSelectedExercises() {
        exercises.removeAll(selectedExercises);
        selectedExercises.clear();
        saveExercisesToInternalStorage();
        adapter.notifyDataSetChanged();
    }

    private void saveExercisesToInternalStorage() {
        try {
            File file = new File(getContext().getFilesDir(), FILE_NAME);
            FileWriter writer = new FileWriter(file);
            Gson gson = new Gson();
            String json = gson.toJson(exercises);
            writer.write(json);
            writer.close();
        } catch (IOException e) {
            Log.e(TAG, "❌ Error al guardar ejercicios", e);
        }
    }

    private void loadExercisesFromInternalStorage(List<MovementPattern> movementPatterns) {
        try {
            File file = new File(getContext().getFilesDir(), FILE_NAME);
            if (!file.exists()) {
                Log.e(TAG, "❌ Archivo de ejercicios no encontrado.");
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Exercise>>() {}.getType();
            List<Exercise> loadedExercises = gson.fromJson(reader, listType);
            reader.close();

            if (loadedExercises != null) {
                exercises.clear();
                exercises.addAll(loadedExercises);
            }

            setupRecyclerView();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error al cargar ejercicios", e);
        }
    }

    private boolean hasMatchingMuscles(Exercise exercise, List<MovementPattern> movementPatterns) {
        if (exercise.getSynergistMuscles() == null || exercise.getSynergistMuscles().isEmpty()) return false;

        for (MovementPattern pattern : movementPatterns) {
            Set<String> patternMuscles = new HashSet<>(extractMuscleNames(pattern.getSynergistMuscles()));
            for (String muscle : exercise.getSynergistMuscles()) {
                if (patternMuscles.contains(muscle)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> extractMuscleNames(Object obj) {
        List<String> muscleNames = new ArrayList<>();
        if (obj instanceof List) {
            List<?> muscleList = (List<?>) obj;
            for (Object muscle : muscleList) {
                if (muscle instanceof Map) {
                    Map<String, Object> muscleMap = (Map<String, Object>) muscle;
                    String name = (String) muscleMap.get("name");
                    if (name != null) muscleNames.add(name);
                }
            }
        }
        return muscleNames;
    }

    private void setupRecyclerView() {
        if (recyclerView != null) {
            adapter = new ExercisesAdapter(exercises, new ArrayList<>(), exercise -> {
                addExerciseToSelection(exercise);
            }, this);
            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
        }
    }

    public void addExerciseToSelection(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
        }
    }

    @Override
    public void onExerciseAdded(Exercise exercise) {
        exercises.add(exercise);
        adapter.notifyItemInserted(exercises.size() - 1);
        saveExercisesToInternalStorage();
    }

    @Override
    public void onExerciseClick(Exercise exercise) {
        addExerciseToSelection(exercise);
    }

    @Override
    public void onExerciseSelected(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
        }
    }

    @Override
    public void onExerciseDeselected(Exercise exercise) {
        selectedExercises.remove(exercise);
    }
}
