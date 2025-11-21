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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExerciseSelectionDialogFragmentAlt extends DialogFragment
        implements ExercisesAdapter.OnExerciseSelectionChangedListener {

    private static final String TAG = "ExerciseSelectionDialog";

    /* ─────────── UI ─────────── */
    private RecyclerView recyclerView;
    private Button doneButton;

    /* ─────────── Datos ─────────── */
    private final List<Exercise> exercises = new ArrayList<>();
    private final List<Exercise> selectedExercises = new ArrayList<>();
    private DatabaseReference mDatabase;
    private ExercisesAdapter adapter;
    private TrainingDay trainingDay;
    private SelectExercisesFragment.OnExercisesSelectedListener listener;

    /* ─────────── Coach Mode ─────────── */
    private boolean coachMode = false;
    private String effectiveUid = null;   // UID del cliente si coachMode, o del usuario actual
    private String dayKey = null;

    /* ─────────── Listener setter ─────────── */
    public void setOnExercisesSelectedListener(SelectExercisesFragment.OnExercisesSelectedListener l) {
        this.listener = l;
    }

    /* ─────────── Ciclo de vida ─────────── */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_selection_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        doneButton   = view.findViewById(R.id.doneButton);
        mDatabase    = FirebaseDatabase.getInstance().getReference();

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            Log.d(TAG, "RecyclerView configurado.");
        }

        /* Recuperar argumentos */
        Bundle args = getArguments();
        String argTargetUid = null;
        if (args != null) {
            trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            coachMode   = args.getBoolean("coachMode", false);
            argTargetUid= safeTrim(args.getString("targetUid", null));
        }

        // Resolver UID efectivo (cliente si coach, si no el actual)
        effectiveUid = resolveEffectiveUid(coachMode, argTargetUid);
        dayKey = (trainingDay != null) ? trainingDay.getDayTitle() : null;

        Log.d(TAG, "✔ coachMode=" + coachMode + " | effectiveUid=" + effectiveUid + " | dayKey=" + dayKey);

        if (trainingDay != null) {
            loadExercises(dayKey);
        } else {
            // Sin trainingDay → mostrar todo
            Log.w(TAG, "⚠ trainingDay nulo: se listarán todos los ejercicios.");
            loadAllExercisesNoFilter();
        }

        /* Botón «Hecho» */
        doneButton.setOnClickListener(v -> {
            if (listener != null && trainingDay != null) {
                listener.onExercisesSelected(selectedExercises, trainingDay);
            } else {
                Log.w(TAG, "⚠ Listener o TrainingDay nulo.");
            }
            dismiss();
        });
    }

    /* ──────────────────────────────────────── */
    /*             CARGA DE EJERCICIOS          */
    /* ──────────────────────────────────────── */
    private void loadExercises(String key) {
        // Coach: si no hay UID efectivo o key, listar todo sin romper flujo
        if (coachMode && (effectiveUid == null || isEmpty(key))) {
            Log.w(TAG, "⚠ Coach sin effectiveUid o dayKey → listar TODO sin filtro.");
            loadAllExercisesNoFilter();
            return;
        }

        obtenerMovementPatternDelDia(effectiveUid, key, pattern -> {
            if (pattern == null) {
                Log.w(TAG, "⚠ No se encontró patrón para " + key + " en uid=" + effectiveUid);
                // mostrar todo para no bloquear flujo
                loadAllExercisesNoFilter();
                return;
            }

            // Con patrón → filtrar por músculos
            mDatabase.child("exercises")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot ds) {
                            exercises.clear();
                            for (DataSnapshot child : ds.getChildren()) {
                                Object data = child.getValue();
                                if (!(data instanceof Map)) continue;

                                Exercise ex = mapToExercise((Map<String, Object>) data);
                                if (ex != null && hasMatchingMuscles(ex, pattern)) {
                                    exercises.add(ex);
                                }
                            }
                            setupRecyclerView();
                            Log.d(TAG, "✅ ejercicios filtrados=" + exercises.size());
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {
                            Log.e(TAG, "❌ Firebase error: " + e.getMessage());
                        }
                    });
        });
    }

    /** Lista TODOS los ejercicios sin filtrar (bypass). */
    private void loadAllExercisesNoFilter() {
        mDatabase.child("exercises")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        exercises.clear();
                        for (DataSnapshot child : ds.getChildren()) {
                            Object data = child.getValue();
                            if (data instanceof Map) {
                                Exercise ex = mapToExercise((Map<String, Object>) data);
                                if (ex != null) exercises.add(ex);
                            }
                        }
                        setupRecyclerView();
                        Log.d(TAG, "✅ ejercicios (sin filtro)=" + exercises.size());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e(TAG, "❌ Firebase error: " + e.getMessage());
                    }
                });
    }

    /* ──────────────────────────────────────── */
    /*       COMPARACIÓN DE MÚSCULOS            */
    /* ──────────────────────────────────────── */
    private boolean hasMatchingMuscles(Exercise ex, MovementPattern pattern) {
        if ((ex.getMusclesWorked() == null || ex.getMusclesWorked().isEmpty()) &&
                (ex.getSynergistMuscles() == null || ex.getSynergistMuscles().isEmpty()))
            return false;

        if ((pattern.getMusclesWorked() == null || pattern.getMusclesWorked().isEmpty()) &&
                (pattern.getSynergistMuscles() == null || pattern.getSynergistMuscles().isEmpty()))
            return false;

        /* Normalizar patrón */
        Set<String> patternMuscles = new HashSet<>();
        if (pattern.getMusclesWorked() != null)
            patternMuscles.addAll(pattern.getMusclesWorked()
                    .stream().map(this::normalizeMuscleName).collect(Collectors.toSet()));

        if (pattern.getSynergistMuscles() != null)
            patternMuscles.addAll(pattern.getSynergistMuscles()
                    .stream().map(this::normalizeMuscleName).collect(Collectors.toSet()));

        /* Normalizar ejercicio */
        Set<String> exerciseMuscles = new HashSet<>();
        if (ex.getMusclesWorked() != null)
            exerciseMuscles.addAll(
                    extractMuscleList(ex.getMusclesWorked())
                            .stream().map(this::normalizeMuscleName).collect(Collectors.toSet()));

        if (ex.getSynergistMuscles() != null)
            exerciseMuscles.addAll(ex.getSynergistMuscles()
                    .stream().map(this::normalizeMuscleName).collect(Collectors.toSet()));

        for (String m : exerciseMuscles)
            if (patternMuscles.contains(m)) return true;

        return false;
    }

    /* ──────────────────────────────────────── */
    /*         OBTENER MOVEMENT PATTERN         */
    /* ──────────────────────────────────────── */
    private void obtenerMovementPatternDelDia(@Nullable String uid, String key, MovementPatternCallback cb) {
        // Si no pasaron UID (usuario normal) se usa el actual
        String finalUid = uid;
        if (isEmpty(finalUid)) {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            finalUid = (u != null) ? u.getUid() : null;
        }

        if (isEmpty(finalUid) || isEmpty(key)) {
            Log.w(TAG, "⚠ obtenerMovementPatternDelDia: uid/key nulos.");
            cb.onPatternLoaded(null);
            return;
        }

        String path = "users/" + finalUid + "/userMovementPatterns/" + key;
        Log.d(TAG, "🔎 fetch patrón → " + path);

        FirebaseDatabase.getInstance().getReference("users")
                .child(finalUid)
                .child("userMovementPatterns")
                .child(key)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot ds) {
                        if (!ds.exists()) {
                            cb.onPatternLoaded(null);
                            return;
                        }

                        MovementPattern p = new MovementPattern();
                        p.setName(ds.child("name").getValue(String.class));

                        List<String> syn = new ArrayList<>();
                        for (DataSnapshot m : ds.child("synergistMuscles").getChildren()) {
                            String val = m.getValue(String.class);
                            if (val != null) syn.add(val);
                        }
                        p.setSynergistMuscles(syn);

                        List<String> w = new ArrayList<>();
                        for (DataSnapshot m : ds.child("musclesWorked").getChildren()) {
                            String val = m.getValue(String.class);
                            if (val != null) w.add(val);
                        }
                        p.setMusclesWorked(w);

                        cb.onPatternLoaded(p);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        Log.e(TAG, "❌ obtenerMovementPatternDelDia:onCancelled " + e.getMessage());
                        cb.onPatternLoaded(null);
                    }
                });
    }

    /* ──────────────────────────────────────── */
    /*           MAPEO A OBJETO MODELO          */
    /* ──────────────────────────────────────── */
    private Exercise mapToExercise(Map<String, Object> data) {
        Exercise ex = new Exercise();
        ex.setName((String) data.getOrDefault("name", ""));
        ex.setDescription((String) data.getOrDefault("description", ""));
        ex.setEquipmentType((String) data.getOrDefault("equipmentType", ""));
        ex.setExerciseType((String) data.getOrDefault("exerciseType", ""));
        ex.setLoadIntensity((String) data.getOrDefault("loadIntensity", ""));
        ex.setMovementType((String) data.getOrDefault("movementType", ""));
        ex.setImage((String) data.getOrDefault("image", ""));

        /* Músculos trabajados → de cadena a 1 sola cadena limpia */
        Object mwObj = data.get("musclesWorked");
        if (mwObj instanceof String) {
            List<String> clean = extractMuscleList((String) mwObj);
            ex.setMusclesWorked(String.join(", ", clean));
        }

        /* Sinérgicos puede ser lista simple o lista de mapas */
        ex.setSynergistMuscles(extractMuscleNames(data.get("synergistMuscles")));

        ex.setFromInternal(false);
        return ex;
    }

    /* ──────────────────────────────────────── */
    /*           UTILIDADES STRING              */
    /* ──────────────────────────────────────── */
    private String normalizeMuscleName(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase(Locale.ROOT)
                .replace("á", "a").replace("é", "e").replace("í", "i")
                .replace("ó", "o").replace("ú", "u").replace("ü", "u")
                .replaceAll("[^a-zñ]", "");
    }

    private List<String> extractMuscleList(String text) {
        List<String> out = new ArrayList<>();
        if (text == null) return out;
        for (String part : text.split(",")) {
            String n = normalizeMuscleName(part);
            if (!n.isEmpty()) out.add(n);
        }
        return out;
    }

    private List<String> extractMuscleNames(Object obj) {
        List<String> names = new ArrayList<>();
        if (obj instanceof List<?>) {
            for (Object o : (List<?>) obj) {
                if (o instanceof String) names.add((String) o);
                else if (o instanceof Map) {
                    Object n = ((Map<?, ?>) o).get("name");
                    if (n instanceof String) names.add((String) n);
                }
            }
        }
        return names;
    }

    /* ──────────────────────────────────────── */
    /*       RECYCLERVIEW & SELECCIÓN           */
    /* ──────────────────────────────────────── */
    private void setupRecyclerView() {
        if (recyclerView == null) {
            Log.e(TAG, "❌ RecyclerView nulo");
            return;
        }

        List<String> already = selectedExercises.stream()
                .map(Exercise::getName)
                .collect(Collectors.toList());

        adapter = new ExercisesAdapter(
                exercises,
                already,
                ex -> {
                    addOrRemoveSelection(ex);
                    showExerciseDetails(ex);
                },
                this);

        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    private void addOrRemoveSelection(Exercise ex) {
        if (selectedExercises.contains(ex)) selectedExercises.remove(ex);
        else selectedExercises.add(ex);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    /* ─────────────── Callbacks de adapter ─────────────── */
    @Override
    public void onExerciseSelected(Exercise exercise) {
        addOrRemoveSelection(exercise);
    }

    @Override
    public void onExerciseDeselected(Exercise exercise) {
        addOrRemoveSelection(exercise);
    }

    /* ──────────────────────────────────────── */
    /*   MOSTRAR DETALLES (CON GALERÍA IMG)     */
    /* ──────────────────────────────────────── */
    private void showExerciseDetails(Exercise exercise) {
        loadExerciseImagesFromFirebase(exercise, () -> {
            // Usa tu factory existente y luego injerta argumentos extra sin romper compatibilidad
            ExerciseDetailsFragment f = ExerciseDetailsFragment.newInstance(exercise);

            // Inyecta args para modo coach y para evitar el primer filtro de RPE
            Bundle b = (f.getArguments() != null) ? f.getArguments() : new Bundle();
            b.putBoolean("coachMode", coachMode);
            if (!isEmpty(effectiveUid)) b.putString("uidForQueries", effectiveUid);
            if (!isEmpty(dayKey))      b.putString("dayKey", dayKey);

            // >>> IMPORTANTE: el detalle debe leer este flag y **ignorar el primer onItemSelected del Spinner**
            // para no borrar la gráfica al iniciar (caso coach).
            b.putBoolean("preventInitialRpeFilter", true);

            f.setArguments(b);

            getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, f)
                    .addToBackStack(null)
                    .commit();
        });
    }

    private void loadExerciseImagesFromFirebase(Exercise ex, Runnable done) {
        FirebaseDatabase.getInstance().getReference("exerciseImages")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot s) {
                        List<String> imgs = new ArrayList<>();
                        for (DataSnapshot child : s.getChildren()) {
                            String n = child.child("name").getValue(String.class);
                            if (n != null && n.equalsIgnoreCase(ex.getName())) {
                                for (int i = 1; i <= 3; i++) {
                                    String img = child.child("images")
                                            .child("image" + i).getValue(String.class);
                                    if (img != null) imgs.add(img);
                                }
                                break;
                            }
                        }
                        ex.setImageGallery(imgs);
                        done.run();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        done.run();
                    }
                });
    }

    /* ─────────────── Helpers ─────────────── */
    private String resolveEffectiveUid(boolean isCoachMode, @Nullable String target) {
        if (isCoachMode && !isEmpty(target)) return target;
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    private boolean isEmpty(@Nullable String s) {
        return s == null || s.trim().isEmpty();
    }

    private String safeTrim(@Nullable String s) {
        return s == null ? null : s.trim();
    }

    /* ─────────────── Callback interfaz ─────────────── */
    public interface MovementPatternCallback {
        void onPatternLoaded(MovementPattern p);
    }
}
