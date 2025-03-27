package com.example.diets.fragments;

import android.content.Context;
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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CreatedExercisesFragment extends Fragment implements AddExerciseDialogFragment.AddExerciseListener,
        ExercisesAdapter.OnExerciseClickListener, ExercisesAdapter.OnExerciseSelectionChangedListener {

    private static final String TAG = "CreatedExercisesFragment";
    private RecyclerView recyclerView;
    private Button doneButton, deleteExerciseButton, addExerciseButton;
    private List<Exercise> exercises = new ArrayList<>();
    private List<Exercise> selectedExercises = new ArrayList<>();
    private static final String FILE_NAME = "exercises.json";
    private DatabaseReference mDatabase;
    private ExercisesAdapter adapter;
    private MenuPopupFragment.OnExerciseSelectedListener listener;
    private TrainingDay trainingDay; // Para recibir el día de entrenamiento y su patrón de movimiento

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

        // 🔥 OBTENER `TrainingDay` SIN PERDER FUNCIONALIDADES
        Bundle args = getArguments();
        if (args != null && args.containsKey("trainingDay")) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            if (trainingDay != null) {
                Log.d(TAG, "📌 Día de entrenamiento recibido en CreatedExercisesFragment: " + trainingDay.getDayTitle());
                loadExercisesFromInternalStorage(trainingDay.getMovementPatterns()); // ✅ FILTRO POR PATRÓN DE MOVIMIENTO
            } else {
                Log.e(TAG, "❌ TrainingDay es NULL en CreatedExercisesFragment");
                loadExercisesFromInternalStorage(null);
            }
        } else {
            Log.e(TAG, "❌ No se recibió `TrainingDay`, cargando todos los ejercicios.");
            loadExercisesFromInternalStorage(null);
        }

        // ✅ BOTÓN PARA GUARDAR EJERCICIOS SELECCIONADOS (NO SE TOCA)
        doneButton.setOnClickListener(v -> {
            if (listener != null) {
                for (Exercise exercise : selectedExercises) {
                    listener.onExerciseSelected(exercise);
                }
            }
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        // ✅ BOTÓN PARA AGREGAR UN NUEVO EJERCICIO (NO SE TOCA)
        addExerciseButton.setOnClickListener(v -> {
            AddExerciseDialogFragment dialogFragment = new AddExerciseDialogFragment();
            dialogFragment.setAddExerciseListener(this); // 🔥 Asegurar que el listener está configurado
            dialogFragment.show(getParentFragmentManager(), "AddExerciseDialogFragment");
        });

        // ✅ BOTÓN PARA ELIMINAR EJERCICIOS (NO SE TOCA)
        deleteExerciseButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }


    public void setOnExerciseSelectedListener(MenuPopupFragment.OnExerciseSelectedListener listener) {
        this.listener = listener;
    }

    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Ejercicio")
                .setMessage("¿Estás seguro de que deseas eliminar los ejercicios seleccionados?")
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

    // 🔥 Método actualizado para cargar ejercicios filtrados por patrón de movimiento
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
                if (movementPatterns != null) {
                    for (Exercise exercise : loadedExercises) {
                        if (hasMatchingMuscles(exercise, movementPatterns)) {
                            exercises.add(exercise);
                        }
                    }
                } else {
                    exercises.addAll(loadedExercises); // Si no hay filtro, agregar todos
                }
            }

            setupRecyclerView();
        } catch (Exception e) {
            Log.e(TAG, "❌ Error al cargar ejercicios desde almacenamiento interno", e);
        }
    }

    // 🔥 Método para filtrar ejercicios según músculos sinérgicos
    private boolean hasMatchingMuscles(Exercise exercise, List<MovementPattern> movementPatterns) {
        if (exercise.getSynergistMuscles() == null || exercise.getSynergistMuscles().isEmpty()) {
            return false;
        }

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

    // 🔥 Extraer nombres de músculos de una lista de objetos
    private List<String> extractMuscleNames(Object obj) {
        List<String> muscleNames = new ArrayList<>();

        if (obj instanceof List) {
            List<?> muscleList = (List<?>) obj;
            for (Object muscle : muscleList) {
                if (muscle instanceof Map) {
                    Map<String, Object> muscleMap = (Map<String, Object>) muscle;
                    String muscleName = (String) muscleMap.get("name");
                    if (muscleName != null) {
                        muscleNames.add(muscleName);
                    }
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
