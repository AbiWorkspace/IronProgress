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
import com.google.firebase.auth.FirebaseUser;
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

public class CreatedExercisesFragment extends Fragment implements
        AddExerciseDialogFragment.AddExerciseListener,
        ExercisesAdapter.OnExerciseClickListener,
        ExercisesAdapter.OnExerciseSelectionChangedListener {

    private static final String TAG = "CreatedExercisesFragment";

    private RecyclerView recyclerView;
    private Button doneButton, deleteExerciseButton, addExerciseButton;

    /** Catálogo local de ejercicios personalizados (origen: JSON en almacenamiento interno privado por UID). */
    private final List<Exercise> exercises = new ArrayList<>();
    private final List<Exercise> selectedExercises = new ArrayList<>();
    // IDs/keys que el adapter espera
    private final List<String> selectedExerciseIds = new ArrayList<>();

    public interface OnExerciseSelectedListener {
        void onExerciseSelected(Exercise exercise);
    }

    private ExercisesAdapter adapter;
    private OnExerciseSelectedListener listener;
    private TrainingDay trainingDay; // Día de entrenamiento y su patrón

    // ===== Helpers de seguridad y archivo por usuario =====
    private @Nullable String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return (u != null) ? u.getUid() : null;
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

        // Inicializar SIEMPRE el adapter antes de cargar datos
        Log.d(TAG, "onViewCreated: Inicializando adapter y asignándolo al RecyclerView.");
        adapter = new ExercisesAdapter(
                exercises,
                selectedExerciseIds,
                this,
                this
        );
        recyclerView.setAdapter(adapter);

        // Obtener `TrainingDay` y cargar ejercicios
        Bundle args = getArguments();
        if (args != null && args.containsKey("trainingDay")) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            if (trainingDay != null && trainingDay.getDayTitle() == null) {
                Log.w(TAG, "onViewCreated: TrainingDay sin dayTitle; asignando genérico.");
                trainingDay.setDayTitle("Día sin nombre");
            }
            if (trainingDay != null) {
                Log.d(TAG, "onViewCreated: Día recibido -> " + trainingDay.getDayTitle());
                loadExercisesFromInternalStorage(trainingDay.getMovementPatterns());
            } else {
                Log.e(TAG, "onViewCreated: TrainingDay es NULL; cargando todos.");
                loadExercisesFromInternalStorage(null);
            }
        } else {
            Log.e(TAG, "onViewCreated: No se recibió TrainingDay; cargando todos.");
            loadExercisesFromInternalStorage(null);
        }

        // Botón guardar selección
        doneButton.setOnClickListener(v -> {
            if (selectedExercises.size() < 1) {
                Toast.makeText(getContext(), "Debes seleccionar al menos un ejercicio", Toast.LENGTH_SHORT).show();
                return;
            }
            Log.d(TAG, "doneButton.onClick: Enviando " + selectedExercises.size() + " ejercicios al listener.");
            if (listener != null) {
                for (Exercise exercise : selectedExercises) listener.onExerciseSelected(exercise);
            }
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        // Botón agregar ejercicio
        addExerciseButton.setOnClickListener(v -> {
            Log.d(TAG, "addExerciseButton.onClick: Abriendo AddExerciseDialogFragment y fijando listener.");
            AddExerciseDialogFragment dialogFragment = new AddExerciseDialogFragment();
            dialogFragment.setAddExerciseListener(this);
            dialogFragment.show(getParentFragmentManager(), "AddExerciseDialogFragment");
        });

        // Botón eliminar seleccionados
        deleteExerciseButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    public void setOnExerciseSelectedListener(OnExerciseSelectedListener listener) {
        this.listener = listener;
    }

    private void showDeleteConfirmationDialog() {
        if (!ready()) return;
        new AlertDialog.Builder(requireContext())
                .setTitle("Eliminar Ejercicio")
                .setMessage("¿Estás seguro de que deseas eliminar los ejercicios seleccionados?")
                .setPositiveButton("Sí", (dialog, which) -> deleteSelectedExercises())
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteSelectedExercises() {
        Log.d(TAG, "deleteSelectedExercises: Eliminando " + selectedExercises.size() + " ejercicios seleccionados.");

        // ⚠️ AUDIT_TRAIL: regla de independencia entre JSON local y planes en Firebase
        Log.w(TAG, "AUDIT_TRAIL: Eliminando " + selectedExercises.size()
                + " ítems del catálogo local. Los planes en Firebase NO se ven afectados (Regla de Independencia).");

        exercises.removeAll(selectedExercises);
        selectedExercises.clear();
        selectedExerciseIds.clear(); // mantener en sync con el adapter
        saveExercisesToInternalStorage();
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void saveExercisesToInternalStorage() {
        Log.d(TAG, "saveExercisesToInternalStorage: Guardando " + exercises.size() + " ejercicios en JSON.");
        if (!ready()) return;
        String u = uid();
        if (u == null) { Log.w(TAG, "save: uid null"); return; }

        // 📁 AUDIT_TRAIL: énfasis en el archivo JSON local por usuario
        Log.i(TAG, "AUDIT_TRAIL: Escribiendo " + exercises.size()
                + " ejercicios en archivo local privado: " + file().getName());

        try (FileWriter writer = new FileWriter(file())) {
            new Gson().toJson(exercises, writer);
        } catch (IOException e) {
            Log.e(TAG, "Error al guardar ejercicios", e);
        }
    }

    /**
     * ============================
     *  CATÁLOGO LOCAL POR USUARIO
     * ============================
     * Cada usuario tiene su propio archivo JSON en almacenamiento interno privado:
     *
     *      <UID>_exercises.json
     *
     * Ubicación (solo app): getFilesDir() / <UID>_exercises.json
     * - No es accesible por otras apps.
     * - Sirve como catálogo OFFLINE de ejercicios creados por el usuario.
     */
    private File file() {
        return new File(requireContext().getFilesDir(), uid() + "_exercises.json");
    }

    // Cargar ejercicios (si no hay archivo, el adapter YA está listo → no hay NPE)
    private void loadExercisesFromInternalStorage(@Nullable List<MovementPattern> movementPatterns) {
        Log.d(TAG, "loadExercisesFromInternalStorage: Iniciando carga desde almacenamiento interno.");

        // 📖 AUDIT_TRAIL: lectura explícita del catálogo offline
        Log.d(TAG, "AUDIT_TRAIL: Leyendo catálogo local. Origen: Almacenamiento Interno (Offline).");

        if (!ready()) return;
        String u = uid();
        if (u == null) { Log.w(TAG, "load: uid null"); return; }
        File f = file();
        if (!f.exists()) {
            Log.w(TAG, "loadExercisesFromInternalStorage: Archivo no encontrado; lista vacía. Archivo esperado: " + f.getName());
            exercises.clear();
            if (adapter != null) adapter.notifyDataSetChanged();
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Exercise>>() {}.getType();
            List<Exercise> loadedExercises = gson.fromJson(reader, listType);

            exercises.clear();
            if (loadedExercises != null) {
                // (Se mantiene este AUDIT_TRAIL si quieres detalle de conteo)
                Log.i(TAG, "AUDIT_TRAIL: Cargando ejercicios personalizados desde JSON local. Total: " + loadedExercises.size());

                exercises.addAll(loadedExercises); // por ahora sin filtro
                Log.d(TAG, "loadExercisesFromInternalStorage: Cargados " + exercises.size()
                        + " ejercicios desde " + f.getName());
            } else {
                Log.w(TAG, "loadExercisesFromInternalStorage: JSON vacío o inválido; lista vacía.");
            }

            if (adapter != null) adapter.notifyDataSetChanged();
        } catch (Exception e) {
            Log.e(TAG, " loadExercisesFromInternalStorage: Error al cargar ejercicios", e);
        }
    }

    // --- Utilidades de selección/keys -----------------------------------------

    private String keyOf(Exercise e) {
        if (e == null) return "null";
        try {
            int id = e.getId();
            if (id > 0) return "id:" + id;
        } catch (Throwable ignore) { }
        if (e.getName() != null && !e.getName().trim().isEmpty()) return "name:" + e.getName().trim();
        return "hash:" + System.identityHashCode(e);
    }

    private void syncSelectAdd(Exercise e) {
        if (e == null) return;
        if (!selectedExercises.contains(e)) selectedExercises.add(e);
        String k = keyOf(e);
        if (!selectedExerciseIds.contains(k)) selectedExerciseIds.add(k);
        Log.d(TAG, "syncSelectAdd: añadido " + k + " (total sel=" + selectedExercises.size() + ")");
    }

    private void syncSelectRemove(Exercise e) {
        if (e == null) return;
        selectedExercises.remove(e);
        String k = keyOf(e);
        selectedExerciseIds.remove(k);
        Log.d(TAG, "syncSelectRemove: removido " + k + " (total sel=" + selectedExercises.size() + ")");
    }

    // --------------------------------------------------------------------------

    public void addExerciseToSelection(Exercise exercise) { syncSelectAdd(exercise); }

    @Override
    public void onExerciseAdded(Exercise exercise) {
        Log.d(TAG, "onExerciseAdded: Recibido nuevo ejercicio: " + (exercise != null ? exercise.getName() : "null"));
        if (exercise == null) {
            Log.e(TAG, "onExerciseAdded: Exercise es null; no se inserta.");
            return;
        }
        exercises.add(exercise);
        int pos = exercises.size() - 1;
        if (adapter != null) adapter.notifyItemInserted(pos);
        saveExercisesToInternalStorage();
        Log.d(TAG, "onExerciseAdded: Insertado en posición " + pos + "; total ejercicios = " + exercises.size());
    }

    @Override
    public void onExerciseClick(Exercise exercise) {
        Log.d(TAG, "onExerciseClick: Click en " + (exercise != null ? exercise.getName() : "null"));
        if (exercise == null) {
            Log.e(TAG, "onExerciseClick: Exercise es null; no se puede abrir detalles.");
            return;
        }
        try {
            ExerciseDetailsFragment fragment = ExerciseDetailsFragment.newInstance(exercise);
            Fragment parent = getParentFragment(); // Debe ser MenuPopupFragment
            if (parent == null) { Log.e(TAG, "getParentFragment() es null."); return; }
            FragmentManager childManager = parent.getChildFragmentManager();
            Log.d(TAG, "Iniciando transacción hacia el contenedor del padre.");
            childManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit();
            Log.d(TAG, "Fragmento de detalles insertado correctamente.");
        } catch (Exception e) {
            Log.e(TAG, "onExerciseClick: Excepción al abrir detalles: " + e.getMessage(), e);
        }
    }

    @Override
    public void onExerciseSelected(Exercise exercise) { syncSelectAdd(exercise); }

    @Override
    public void onExerciseDeselected(Exercise exercise) { syncSelectRemove(exercise); }
}
