package com.example.diets.fragments;

import static com.example.diets.R.id.fragment_container;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

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

public class ExerciseSelectionDialogFragment extends DialogFragment
        implements ExercisesAdapter.OnExerciseSelectionChangedListener {

    /* ---------- CONSTANTES ---------- */
    private static final String TAG = "ExerciseSelectionDialog";

    /* ---------- UI ---------- */
    private RecyclerView recyclerView;
    private Button       doneButton;

    /* ---------- DATOS ---------- */
    private final List<Exercise> exercises         = new ArrayList<>();
    private final List<Exercise> selectedExercises = new ArrayList<>();

    private DatabaseReference     dbRef;            // nodo /exercises
    private ValueEventListener    exercisesListener;

    private ExercisesAdapter adapter;
    private TrainingDay      trainingDay;
    private SelectExercisesFragment.OnExercisesSelectedListener listener;

    /* ---------- SETTERS ---------- */
    public void setOnExercisesSelectedListener(SelectExercisesFragment.OnExercisesSelectedListener l) {
        this.listener = l;
    }

    /* ---------- CICLO DE VIDA ---------- */

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_selection_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle s) {
        super.onViewCreated(v, s);

        recyclerView = v.findViewById(R.id.recyclerView);
        doneButton   = v.findViewById(R.id.doneButton);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        dbRef = FirebaseDatabase.getInstance().getReference("exercises");

        /* ---------- Recuperar argumentos ---------- */
        Bundle args = getArguments();
        if (args != null) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            if (trainingDay != null) loadExercises(trainingDay.getMovementPatterns());
        }

        doneButton.setOnClickListener(btn -> {
            if (selectedExercises.isEmpty()) {
                Toast.makeText(getContext(), "Debes seleccionar al menos un ejercicio", Toast.LENGTH_SHORT).show();
                return;
            }

            if (listener != null && trainingDay != null) {
                listener.onExercisesSelected(selectedExercises, trainingDay);
                Log.d(TAG, " Enviados " + selectedExercises.size() + " ejercicios.");
            } else {
                Log.w(TAG, " Listener o TrainingDay es nulo.");
            }
            dismiss();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 🔒 Evitar callbacks tardíos a un fragment destruido
        if (exercisesListener != null) dbRef.removeEventListener(exercisesListener);
        adapter = null;
    }

    /* ---------- CARGA DE EJERCICIOS ---------- */

    private void loadExercises(List<MovementPattern> patterns) {
        Log.d(TAG, " Cargando ejercicios…");

        /* Musculatura del día normalizada */
        Set<String> patternMuscles = new HashSet<>();
        for (MovementPattern p : patterns) {
            if (p.getMusclesWorked() != null)
                p.getMusclesWorked().forEach(m -> patternMuscles.add(norm(m)));
            if (p.getSynergistMuscles() != null)
                p.getSynergistMuscles().forEach(m -> patternMuscles.add(norm(m)));
        }

        exercisesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!isAdded()) {                         //   Fragment todavía vivo?
                    Log.w(TAG, "Callback recibido tras cerrarse el diálogo → ignorado.");
                    return;
                }

                List<Exercise> filtered = new ArrayList<>();
                for (DataSnapshot child : snap.getChildren()) {
                    Map<String, Object> map = (Map<String, Object>) child.getValue();
                    if (map == null) continue;
                    Exercise ex = mapToExercise(map);

                    /* Filtrado rápido */
                    if (!Collections.disjoint(patternMuscles, getAllMuscles(ex))) {
                        filtered.add(ex);
                    }
                }

                /* Refrescar UI */
                requireActivity().runOnUiThread(() -> {
                    exercises.clear();
                    exercises.addAll(filtered);
                    setupRecyclerView();
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError err) {
                if (isAdded())
                    Log.e(TAG, " Firebase error: " + err.getMessage());
            }
        };

        dbRef.addListenerForSingleValueEvent(exercisesListener);
    }

    /* ---------- ADAPTADOR ---------- */

    private void setupRecyclerView() {
        List<String> preselected = selectedExercises.stream()
                .map(Exercise::getName).toList();

        adapter = new ExercisesAdapter(
                exercises,
                preselected,
                // Click rápido → detalles + toggle
                ex -> { addExerciseToSelection(ex); showExerciseDetails(ex); },
                this         // listener de selección para checkboxes
        );

        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        Log.d(TAG, " RecyclerView listo (" + exercises.size() + " ítems).");
    }

    /* ---------- OPERACIONES ---------- */

    private void addExerciseToSelection(Exercise ex) {
        if (selectedExercises.contains(ex)) {
            selectedExercises.remove(ex);
            Log.d(TAG, " Eliminado: " + ex.getName());
        } else {
            selectedExercises.add(ex);
            Log.d(TAG, " Añadido: " + ex.getName());
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void showExerciseDetails(Exercise ex) {
        ExerciseDetailsFragment d = ExerciseDetailsFragment.newInstance(ex);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, d)
                .addToBackStack(null)
                .commit();
    }

    /* ---------- INTERFAZ DEL ADAPTADOR ---------- */

    @Override public void onExerciseSelected  (Exercise ex) { addExerciseToSelection(ex); }
    @Override public void onExerciseDeselected(Exercise ex) { addExerciseToSelection(ex); }

    /* ---------- UTILIDADES ---------- */

    private Exercise mapToExercise(Map<String, Object> m) {
        Exercise ex = new Exercise();
        ex.setName         ((String) m.getOrDefault("name", ""));
        ex.setEquipmentType((String) m.getOrDefault("equipmentType", ""));
        ex.setExerciseType ((String) m.getOrDefault("exerciseType", ""));
        ex.setLoadIntensity((String) m.getOrDefault("loadIntensity", ""));
        ex.setDescription  ((String) m.getOrDefault("description", ""));
        ex.setMovementType ((String) m.getOrDefault("movementType", ""));
        ex.setImage        ((String) m.getOrDefault("image", ""));

        /* musclesWorked puede venir como String */
        Object worked = m.get("musclesWorked");
        ex.setMusclesWorked(worked instanceof String ? (String) worked : "");

        ex.setSynergistMuscles(extractSynergistNames(m.get("synergistMuscles")));
        return ex;
    }

    private List<String> extractSynergistNames(Object obj) {
        List<String> out = new ArrayList<>();
        if (obj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?,?> map) {
                    Object n = map.get("name");
                    if (n != null) out.add(n.toString());
                }
            }
        }
        return out;
    }

    private Set<String> getAllMuscles(Exercise ex) {
        Set<String> set = new HashSet<>(splitAndNorm(ex.getMusclesWorked()));
        ex.getSynergistMuscles().forEach(m -> set.add(norm(m)));
        return set;
    }

    private List<String> splitAndNorm(String csv) {
        if (csv == null || csv.isEmpty()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .map(this::norm)
                .collect(Collectors.toList());
    }

    private String norm(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[áàä]", "a")
                .replaceAll("[éèë]", "e")
                .replaceAll("[íìï]", "i")
                .replaceAll("[óòö]", "o")
                .replaceAll("[úùü]", "u")
                .replaceAll("[^a-zñ]", "");
    }
}
