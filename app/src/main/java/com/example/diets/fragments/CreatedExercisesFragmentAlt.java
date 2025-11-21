package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.AddExerciseDialogFragment;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.Objects;
import java.util.Set;

public class CreatedExercisesFragmentAlt extends Fragment implements
        AddExerciseDialogFragment.AddExerciseListener,
        ExercisesAdapter.OnExerciseClickListener,
        ExercisesAdapter.OnExerciseSelectionChangedListener {

    private static final String TAG = "CreatedExercisesFragmentAlt";

    private RecyclerView recyclerView;
    private Button doneButton, deleteExerciseButton, addExerciseButton;
    private final List<Exercise> exercises = new ArrayList<>();
    private final List<Exercise> selectedExercises = new ArrayList<>();
    private ExercisesAdapter adapter;

    private SelectExercisesFragment.OnExercisesSelectedListener listener; // Interfaz global
    private TrainingDay trainingDay; // Día de entrenamiento recibido como argumento

    // Helpers de seguridad
    private @Nullable String uid() {
        var u = FirebaseAuth.getInstance().getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    //
    private File file() {
        return new File(requireContext().getFilesDir(), uid() + "_exercises.json");
    }
    private boolean ready() { return isAdded() && getContext() != null; }

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

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Inicializa adapter una vez
        setupRecyclerView();

        // Obtener el día actual y cargar ejercicios
        Bundle args = getArguments();
        if (args != null && args.containsKey("trainingDay")) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            if (trainingDay != null) {
                Log.d(TAG, "\uD83D\uDCCD Día recibido: " + trainingDay.getDayTitle());
                loadExercisesFromInternalStorage(trainingDay.getMovementPatterns());
            } else {
                Log.w(TAG, "\u26A0\uFE0F TrainingDay es NULL");
                loadExercisesFromInternalStorage(null);
            }
        } else {
            Log.w(TAG, "\u26A0\uFE0F No se recibió TrainingDay");
            loadExercisesFromInternalStorage(null);
        }

        // Enviar ejercicios seleccionados al fragmento padre
        doneButton.setOnClickListener(v -> {
            if (selectedExercises.size() < 1) {
                Toast.makeText(getContext(), "Debes seleccionar al menos un ejercicio", Toast.LENGTH_SHORT).show();
                return;
            }
            if (listener != null && trainingDay != null) {
                listener.onExercisesSelected(new ArrayList<>(selectedExercises), trainingDay);
                Log.d(TAG, "Enviados " + selectedExercises.size() + " ejercicios seleccionados.");
            } else {
                Log.w(TAG, "Listener o TrainingDay es null");
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

    // Método para recibir listener desde MenuPopupFragmentAlt
    public void setOnExercisesSelectedListener(SelectExercisesFragment.OnExercisesSelectedListener listener) {
        this.listener = listener;
    }

    private void showDeleteConfirmationDialog() {
        if (!ready()) return;
        new AlertDialog.Builder(requireContext())
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
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void saveExercisesToInternalStorage() {
        if (!ready()) return;
        String u = uid();
        if (u == null) { Log.w(TAG, "save: uid null"); return; }
        try (FileWriter writer = new FileWriter(file())) {
            new Gson().toJson(exercises, writer);
        } catch (IOException e) {
            Log.e(TAG, "\u274C Error al guardar ejercicios", e);
        }
    }

    private void loadExercisesFromInternalStorage(@Nullable List<MovementPattern> movementPatterns) {
        if (!ready()) return;
        String u = uid();
        if (u == null) { Log.w(TAG, "load: uid null"); return; }
        File f = file();
        if (!f.exists()) {
            Log.i(TAG, "Archivo de ejercicios no encontrado. Se inicia vacío.");
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Exercise>>() {}.getType();
            List<Exercise> loadedExercises = gson.fromJson(reader, listType);

            exercises.clear();
            if (loadedExercises != null) exercises.addAll(loadedExercises);

            // Normalización (si aplica)
            for (Exercise ex : exercises) {
                ex.setEquipmentType(ex.getEquipmentType());
                ex.setExerciseType(ex.getExerciseType());
                ex.setMovementType(ex.getMovementType());
            }
            saveExercisesToInternalStorage(); // sobre-escribe ya corregido

            if (adapter != null) adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, "\u274C Error al cargar ejercicios", e);
        }
    }

    private boolean hasMatchingMuscles(Exercise exercise, List<MovementPattern> movementPatterns) {
        if (exercise.getSynergistMuscles() == null || exercise.getSynergistMuscles().isEmpty() || movementPatterns == null) return false;
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
        if (recyclerView == null) return;
        if (adapter == null) {
            adapter = new ExercisesAdapter(
                    exercises,
                    new ArrayList<>(),
                    new ExercisesAdapter.OnExerciseClickListener() {
                        @Override
                        public void onExerciseClick(Exercise exercise) {
                            CreatedExercisesFragmentAlt.this.onExerciseClick(exercise);
                        }
                    },
                    this
            );
            recyclerView.setAdapter(adapter);
        }
        adapter.notifyDataSetChanged();
    }

    public void addExerciseToSelection(Exercise exercise) {
        if (exercise == null) return;
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
        }
    }

    @Override
    public void onExerciseAdded(Exercise exercise) {
        if (exercise == null) return;
        exercises.add(exercise);
        if (adapter != null) adapter.notifyItemInserted(exercises.size() - 1);
        saveExercisesToInternalStorage();
    }

    @Override
    public void onExerciseClick(Exercise exercise) {
        Log.d(TAG, "\uD83D\uDD79 Contenedor clickeado - ejercicio: " + (exercise != null ? exercise.getName() : "null"));
        if (exercise == null) { Log.e(TAG, "\u274C Exercise es null"); return; }
        try {
            ExerciseDetailsFragment fragment = ExerciseDetailsFragment.newInstance(exercise);
            Fragment parent = getParentFragment();
            if (parent == null) { Log.e(TAG, "\u274C getParentFragment() null"); return; }
            FragmentManager childManager = parent.getChildFragmentManager();
            Log.d(TAG, "\uD83D\uDD01 Iniciando transacción de fragmento...");
            childManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
            Log.d(TAG, "\u2705 Fragmento ExerciseDetailsFragment insertado correctamente.");
        } catch (Exception e) {
            Log.e(TAG, "\u274C Excepción al abrir detalles: " + e.getMessage(), e);
        }
    }

    @Override
    public void onExerciseSelected(Exercise exercise) {
        if (exercise == null) return;
        if (!selectedExercises.contains(exercise)) selectedExercises.add(exercise);
    }

    @Override
    public void onExerciseDeselected(Exercise exercise) {
        if (exercise == null) return;
        selectedExercises.remove(exercise);
    }
}
