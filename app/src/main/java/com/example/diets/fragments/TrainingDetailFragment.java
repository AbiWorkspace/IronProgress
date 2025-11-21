package com.example.diets.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.SeriesAdapter;

import com.example.diets.adapters.TrainingDetailAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;

import com.example.diets.fragments.MenuPopupFragmentAlt;
import com.example.diets.selection.OnExercisesSelectedListener;
import com.example.diets.prefs.Prefs;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class TrainingDetailFragment extends Fragment implements OnExercisesSelectedListener {

    private static final String TAG = "TrainingDetailFragment";
    private String realDayKey = null;

    /* UI */
    private RecyclerView recyclerViewExercises;
    private Chronometer chronometerTraining;
    private ImageView btnBackToPrevious;
    private Button btnFinishWorkout;

    /* Flags y argumentos */
    private boolean isFromInternal = false;
    private String exerciseIndex;
    private String dayKey;
    private String effectiveUid = null;

    /* Datos y adaptador */
    private TrainingDetailAdapter adapter;
    private List<UserExercise> exercises = new ArrayList<>();

    /* Estado cronómetro */
    private boolean isChronometerRunning = false;
    private long pauseOffset = 0;

    /* Guardado único */
    private boolean seriesSaved = false;

    /* Movimiento y sinergias */
    private MovementPattern movementPatternDelDia;
    private List<String> musculosSinergicosDelDia;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            exerciseIndex   = args.getString("exerciseIndex");
            dayKey          = args.getString("dayKey");
            isFromInternal  = args.getBoolean("isFromInternal", false);
            effectiveUid    = args.getString("effectiveUid", null);
        }
        if (effectiveUid == null && getActivity() != null && getActivity().getIntent() != null) {
            Bundle ex = getActivity().getIntent().getExtras();
            if (ex != null) effectiveUid = ex.getString("effectiveUid", null);
        }
        if (effectiveUid == null) {
            FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
            effectiveUid = (me != null) ? me.getUid() : null;
        }
        Log.i(TAG, "Args -> dayKey=" + dayKey + ", isFromInternal=" + isFromInternal + ", effectiveUid=" + effectiveUid);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: inflando layout…");
        View view = inflater.inflate(R.layout.fragment_training_detail, container, false);

        recyclerViewExercises = view.findViewById(R.id.recyclerViewExercises);
        recyclerViewExercises.setLayoutManager(new LinearLayoutManager(getContext()));

        Button btnAddExercise = view.findViewById(R.id.btnAddExercise);
        btnAddExercise.setEnabled(false);

        if (dayKey == null || dayKey.trim().isEmpty()) {
            dayKey = "Día 1";
            Log.w(TAG, "dayKey no recibido; usando por defecto: " + dayKey);
        }
        if (realDayKey == null) realDayKey = dayKey;

        if (effectiveUid == null) {
            Log.e(TAG, "effectiveUid NULL: no se pueden cargar datos.");
            return view;
        }

        // 1) Cargar ejercicios del día (fija realDayKey)
        cargarEjerciciosDelDiaConFallback(effectiveUid, dayKey, () -> {});

        // 2) Cargar MovementPattern del día
        obtenerMovementPatternDelDia(effectiveUid, dayKey, pattern -> {
            if (pattern != null) {
                movementPatternDelDia = pattern;
                musculosSinergicosDelDia = pattern.getSynergistMuscles();
                Log.d(TAG, "Patrón: " + movementPatternDelDia.getName() + " | sinergias=" + musculosSinergicosDelDia);
                btnAddExercise.setEnabled(true);
            } else {
                Log.w(TAG, "No se encontró patrón de movimiento para: " + dayKey);
            }
        });

        // Botón Agregar Ejercicio
        btnAddExercise.setOnClickListener(v -> {
            Log.d(TAG, "btnAddExercise: abrir selector con día=" + dayKey);
            if (movementPatternDelDia != null) {
                TrainingDay trainingDay = new TrainingDay();
                trainingDay.setDayIndex(0);
                trainingDay.setDayTitle(dayKey);
                trainingDay.setMovementPatterns(Collections.singletonList(movementPatternDelDia));

                Bundle args = new Bundle();
                args.putSerializable("trainingDay", trainingDay);

                MenuPopupFragmentAlt dialog = new MenuPopupFragmentAlt();
                dialog.setArguments(args);
                dialog.show(getChildFragmentManager(), "MenuPopupFragmentAlt");
            } else {
                Log.e(TAG, "No se puede abrir selector: movementPatternDelDia es NULL.");
            }
        });

        // Botón atrás
        btnBackToPrevious = view.findViewById(R.id.btnBackToPrevious);
        btnBackToPrevious.setOnClickListener(v -> finishWorkout());

        // Cronómetro
        chronometerTraining = view.findViewById(R.id.chronometerTraining);
        startChronometer();

        // Botón finalizar
        btnFinishWorkout = view.findViewById(R.id.btnFinishWorkout);
        btnFinishWorkout.setOnClickListener(v -> {
            if (adapter == null) return;
            adapter.commitAllEdits(() -> finishWorkout());
        });

        return view;
    }

    /* === Helpers estándar para diálogos === */
    private void showConfirmDialog(String title, String message,
                                   String positiveText, Runnable onPositive,
                                   String negativeText, Runnable onNegative) {
        new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positiveText, (d, w) -> { if (onPositive != null) onPositive.run(); })
                .setNegativeButton(negativeText, (d, w) -> { if (onNegative != null) onNegative.run(); })
                .show();
    }

    private void showInfoDialog(String title, String message,
                                String buttonText, Runnable onAccept) {
        new AlertDialog.Builder(requireContext(), R.style.CustomAlertDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(buttonText, (d, w) -> { if (onAccept != null) onAccept.run(); })
                .show();
    }

    private void cargarEjerciciosDelDiaConFallback(@NonNull String uid, @NonNull String dayKey, @NonNull Runnable onDone) {
        DatabaseReference mpRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("userMovementPatterns");

        DatabaseReference dayExercisesRef = mpRef.child(dayKey).child("userExercises");
        Log.d(TAG, "Intentando cargar ejercicios desde /userMovementPatterns/" + dayKey + "/userExercises");

        dayExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Log.d(TAG, "Encontrado nodo exacto (key): " + dayKey);
                    realDayKey = dayKey;
                    cargarEjerciciosDesdeNodo(uid, realDayKey, snapshot, onDone);
                } else {
                    Log.w(TAG, "No existe el nodo exacto. Buscando hijo con field name == " + dayKey);
                    mpRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot parentSnap) {
                            String tmpKey = null;
                            for (DataSnapshot child : parentSnap.getChildren()) {
                                String name = child.child("name").getValue(String.class);
                                if (name != null && name.equalsIgnoreCase(dayKey)) {
                                    tmpKey = child.getKey();
                                    break;
                                }
                            }
                            if (tmpKey == null) {
                                Log.e(TAG, "No se encontró ningún día con name=" + dayKey);
                                exercises = new ArrayList<>();
                                initOrNotifyAdapter();
                                onDone.run();
                                return;
                            }

                            Log.d(TAG, "🔁 Resuelto realKey=" + tmpKey + " para name=" + dayKey);

                            // ✅ Copias efectivamente finales para usarlas dentro del listener interno
                            final String realKeyFinal = tmpKey;
                            final String uidFinal = uid;
                            final Runnable onDoneFinal = onDone;

                            // Puedes asignar el campo de instancia aquí (no necesita ser final)
                            realDayKey = realKeyFinal;

                            parentSnap.getRef().child(realKeyFinal).child("userExercises")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override public void onDataChange(@NonNull DataSnapshot fixedSnap) {
                                            cargarEjerciciosDesdeNodo(uidFinal, realKeyFinal, fixedSnap, onDoneFinal);
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError e) {
                                            Log.e(TAG, "Error fallback userExercises: " + e.getMessage());
                                            onDoneFinal.run();
                                        }
                                    });
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {
                            Log.e(TAG, "Error leyendo userMovementPatterns: " + e.getMessage());
                            onDone.run();
                        }
                    });
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e(TAG, "Error dayExercisesRef: " + e.getMessage());
                onDone.run();
            }
        });
    }


    private void cargarEjerciciosDesdeNodo(@NonNull String uid, @NonNull String usedDayKey,
                                           @NonNull DataSnapshot dayExercisesSnap, @NonNull Runnable onDone) {

        exercises = new ArrayList<>();

        DatabaseReference globalExercisesRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("userExercises");

        globalExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot globalSnapshot) {

                for (DataSnapshot exerciseSnap : dayExercisesSnap.getChildren()) {
                    UserExercise ue = new UserExercise();
                    try { ue.hydrateFromSnapshot(exerciseSnap); } catch (Exception ignored) {}

                    String nombre = exerciseSnap.child("name").getValue(String.class);
                    ue.setName(nombre);

                    Boolean interno = exerciseSnap.child("isFromInternal").getValue(Boolean.class);
                    ue.setFromInternal(interno != null && interno);

                    ue.setSeries(getIntOrDefault(exerciseSnap.child("series").getValue(Integer.class), 3));
                    ue.setReps(getIntOrDefault(exerciseSnap.child("reps").getValue(Integer.class), 8));
                    ue.setRpe(getDoubleOrDefault(exerciseSnap.child("rpe").getValue(Double.class), 7.0));
                    ue.setWeight(getDoubleOrDefault(exerciseSnap.child("weight").getValue(Double.class), 0.0));
                    ue.setImage(exerciseSnap.child("image").getValue(String.class));
                    try { ue.setDayName(usedDayKey); } catch (Exception ignored) {}

                    // último peso (lastLoad) desde global
                    double lastLoad = 0;
                    for (DataSnapshot gSnap : globalSnapshot.getChildren()) {
                        String gName = gSnap.child("name").getValue(String.class);
                        if (nombre != null && nombre.equalsIgnoreCase(gName)) {
                            for (DataSnapshot s : gSnap.child("seriesData").getChildren()) {
                                Double w = s.child("weight").getValue(Double.class);
                                if (w != null) lastLoad = w;
                            }
                            break;
                        }
                    }
                    ue.setLastLoad(lastLoad);

                    exercises.add(ue);
                }

                // Crear/actualizar adapter
                if (adapter == null) {
                    adapter = new TrainingDetailAdapter(
                            exercises, recyclerViewExercises, usedDayKey, getContext(),
                            getChildFragmentManager(), effectiveUid
                    );
                    recyclerViewExercises.setAdapter(adapter);
                } else {
                    adapter.setExercises(exercises);
                    recyclerViewExercises.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                // Vincular claves Firebase y cargar historial antes de sugerencias
                AtomicInteger pendientes = new AtomicInteger(exercises.size());
                for (UserExercise ue : exercises) {
                    String exerciseName = ue.getName();
                    for (DataSnapshot gSnap : globalSnapshot.getChildren()) {
                        String gName = gSnap.child("name").getValue(String.class);
                        if (exerciseName != null && exerciseName.equalsIgnoreCase(gName)) {
                            String firebaseKey = gSnap.getKey();
                            adapter.setFirebaseKey(exerciseName, firebaseKey);

                            SeriesAdapter sa = adapter.getSeriesAdapterPorNombre(exerciseName);
                            if (sa != null) {
                                sa.cargarUltimasSeriesDesdeFirebase(firebaseKey, getContext(), () -> {
                                    if (pendientes.decrementAndGet() == 0) pedirRecomendacion();
                                });
                            } else {
                                if (pendientes.decrementAndGet() == 0) pedirRecomendacion();
                            }
                            break;
                        }
                    }
                }

                // Drag & drop
                ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder from,
                                          @NonNull RecyclerView.ViewHolder to) {
                        int f = from.getAdapterPosition();
                        int t = to.getAdapterPosition();
                        adapter.onItemMove(f, t);
                        return true;
                    }
                    @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) { }
                    @Override public boolean isLongPressDragEnabled() { return true; }
                };
                new ItemTouchHelper(callback).attachToRecyclerView(recyclerViewExercises);

                onDone.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e(TAG, "Error al leer userExercises globales: " + e.getMessage());
                onDone.run();
            }
        });
    }

    private int getIntOrDefault(Integer v, int def) { return v != null ? v : def; }
    private double getDoubleOrDefault(Double v, double def) { return v != null ? v : def; }

    private void pedirRecomendacion() {
        if (adapter != null) adapter.generarSugerencias();
    }

    /* ===== Selector de ejercicios ===== */
    @Override
    public void onExercisesSelected(List<Exercise> exercisesRecibidos, TrainingDay trainingDay) {
        Log.d(TAG, "onExercisesSelected: " + exercisesRecibidos.size() + " ejercicios.");
        for (Exercise ex : exercisesRecibidos) ex.setFromInternal(true);
        guardarEjerciciosEnFirebase(exercisesRecibidos, trainingDay.getDayTitle());

        for (Exercise ex : exercisesRecibidos) {
            boolean yaExiste = false;
            for (UserExercise u : exercises) {
                if (u.getName().equalsIgnoreCase(ex.getName())) { yaExiste = true; break; }
            }
            if (yaExiste) {
                Toast.makeText(getContext(), "Ya agregado: " + ex.getName(), Toast.LENGTH_SHORT).show();
                continue;
            }
            UserExercise ue = new UserExercise(ex);
            exercises.add(ue);
            if (adapter != null) {
                adapter.notifyItemInserted(exercises.size() - 1);
                recyclerViewExercises.smoothScrollToPosition(exercises.size() - 1);
            }
        }
    }

    /* ===== MovementPattern del día ===== */
    private void obtenerMovementPatternDelDia(@NonNull String uid, @NonNull String dayKey, @NonNull MovementPatternCallback callback) {
        DatabaseReference dayRef = FirebaseDatabase.getInstance()
                .getReference("users").child(uid).child("userMovementPatterns").child(dayKey);

        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    snapshot.getRef().getParent().addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot parent) {
                            for (DataSnapshot child : parent.getChildren()) {
                                String name = child.child("name").getValue(String.class);
                                if (name != null && name.equalsIgnoreCase(dayKey)) {
                                    buildPatternFrom(child, callback);
                                    return;
                                }
                            }
                            Log.w(TAG, "No se encontró patrón con name=" + dayKey);
                            callback.onPatternLoaded(null);
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error fallback patrón: " + error.getMessage());
                            callback.onPatternLoaded(null);
                        }
                    });
                    return;
                }
                buildPatternFrom(snapshot, callback);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error patrón día: " + error.getMessage());
                callback.onPatternLoaded(null);
            }
        });
    }

    private void buildPatternFrom(@NonNull DataSnapshot daySnap, @NonNull MovementPatternCallback callback) {
        String name = daySnap.child("name").getValue(String.class);
        Set<String> synergistMuscles = new HashSet<>();
        DataSnapshot exercisesSnapshot = daySnap.child("userExercises");
        for (DataSnapshot exerciseSnap : exercisesSnapshot.getChildren()) {
            DataSnapshot synergistSnap = exerciseSnap.child("synergistMuscles");
            for (DataSnapshot muscleSnap : synergistSnap.getChildren()) {
                String muscle = muscleSnap.getValue(String.class);
                if (muscle != null && !muscle.isEmpty()) synergistMuscles.add(muscle);
            }
        }
        MovementPattern pattern = new MovementPattern();
        pattern.setName(name != null ? name : daySnap.getKey());
        pattern.setSynergistMuscles(new ArrayList<>(synergistMuscles));
        callback.onPatternLoaded(pattern);
    }

    public interface MovementPatternCallback { void onPatternLoaded(MovementPattern pattern); }

    /* ===== Cronómetro ===== */
    private void startChronometer() {
        if (!isChronometerRunning) {
            chronometerTraining.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometerTraining.start();
            isChronometerRunning = true;
        }
    }

    private String formatElapsedTime(long elapsedMillis) {
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
        int hours   = (int) ((elapsedMillis / (1000 * 60 * 60)) % 24);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /* ===== Finalizar entrenamiento ===== */
    private void finishWorkout() {
        if (adapter != null && !adapter.getPendingDeletionExercises().isEmpty()) {
            showConfirmDialog(
                    "Eliminar ejercicios permanentemente",
                    "¿Deseas eliminar los ejercicios?",
                    "Sí, eliminar",
                    () -> borrarYReindexarEjerciciosEnFirebase(
                            adapter.getPendingDeletionExercises(), () -> finishWorkout(true)),
                    "No, mantener",
                    () -> finishWorkout(true)
            );
            return;
        }
        finishWorkout(true);
    }

    private void finishWorkout(boolean skipDeletionCheck) {
        if (isChronometerRunning) {
            pauseOffset = SystemClock.elapsedRealtime() - chronometerTraining.getBase();
            chronometerTraining.stop();
            isChronometerRunning = false;
            Log.d(TAG, "Tiempo total: " + formatElapsedTime(pauseOffset));
        }
        if (seriesSaved || adapter == null) return;

        adapter.commitAllEdits();
        Map<String, List<ExerciseSeries>> seriesPorEjercicio = adapter.getAllCompletedSeriesGroupedByExercise();

        boolean haySeriesIncompletas = false;
        for (UserExercise ej : exercises) {
            List<ExerciseSeries> total = adapter.getSeriesListForExercise(ej.getName());
            List<ExerciseSeries> comp  = seriesPorEjercicio.get(ej.getName());
            if (total != null && comp != null && comp.size() < total.size()) { haySeriesIncompletas = true; break; }
        }
        if (haySeriesIncompletas) { showIncompleteSeriesDialog(seriesPorEjercicio); return; }

        int totalComp = 0; for (List<ExerciseSeries> l : seriesPorEjercicio.values()) totalComp += l.size();
        if (totalComp == 0) { Toast.makeText(getContext(),"Marca al menos una serie completada",Toast.LENGTH_SHORT).show(); return; }

        if (!Prefs.isDynamicEnabled(requireContext())) {
            guardarSeriesYFinalizar(seriesPorEjercicio);
            return;
        }

        boolean hayCambiosSeries = false;
        for (UserExercise ex : exercises) {
            List<ExerciseSeries> lista = adapter.getSeriesListForExercise(ex.getName());
            int nActual = (lista != null) ? lista.size() : 0;
            if (nActual != ex.getSeries()) { hayCambiosSeries = true; break; }
        }

        if (!hayCambiosSeries) {
            guardarSeriesYFinalizar(seriesPorEjercicio);
            return;
        }

        showConfirmDialog(
                "¿Actualizar rutina?",
                "Cambiaste la cantidad de series en algún ejercicio.\n¿Deseas actualizar el número de series en tu rutina?",
                "Sí, actualizar",
                () -> { actualizarConteoSeriesEnFirebase(true); guardarSeriesYFinalizar(seriesPorEjercicio); },
                "No actualizar",
                () -> guardarSeriesYFinalizar(seriesPorEjercicio)
        );
    }

    // Estandarizado: sin layout personalizado
    private void showIncompleteSeriesDialog(Map<String, List<ExerciseSeries>> seriesPorEjercicio) {
        showConfirmDialog(
                "Series incompletas",
                "Hay series sin marcar como completadas. ¿Guardar de todos modos?",
                "Guardar de todos modos",
                () -> borrarYReindexarEjerciciosEnFirebase(
                        adapter.getPendingDeletionExercises(),
                        () -> guardarSeriesYFinalizar(seriesPorEjercicio)
                ),
                "Revisar",
                null
        );
    }

    private void actualizarConteoSeriesEnFirebase(boolean force) {
        if (effectiveUid == null || realDayKey == null) return;

        DatabaseReference exercisesRef = FirebaseDatabase.getInstance()
                .getReference("users").child(effectiveUid)
                .child("userMovementPatterns").child(realDayKey).child("userExercises");

        for (UserExercise exercise : exercises) {
            String exerciseName = exercise.getName();

            if (!force) {
                boolean progOn = Prefs.isDynamicEnabledForExercise(requireContext(), exerciseName);
                if (progOn) continue;
            }

            List<ExerciseSeries> lista = adapter.getSeriesListForExercise(exerciseName);
            int seriesCount = (lista != null && !lista.isEmpty()) ? lista.size() : exercise.getSeries();

            exercisesRef.orderByChild("name").equalTo(exerciseName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                exercisesRef.child(snap.getKey()).child("series").setValue(seriesCount);
                                Log.d(TAG, "Series actualizado " + exerciseName + " -> " + seriesCount + " (dayKey=" + realDayKey + ")");
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error actualizando series: " + error.getMessage());
                        }
                    });
        }
    }

    private void borrarYReindexarEjerciciosEnFirebase(List<UserExercise> toDelete, Runnable onComplete) {
        if (effectiveUid == null || realDayKey == null) { if (onComplete != null) onComplete.run(); return; }

        DatabaseReference userExercisesRef = FirebaseDatabase.getInstance()
                .getReference("users").child(effectiveUid)
                .child("userMovementPatterns").child(realDayKey).child("userExercises");

        userExercisesRef.removeValue().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error al eliminar ejercicios: ", task.getException());
                if (onComplete != null) onComplete.run();
                return;
            }
            List<UserExercise> remaining = adapter.getExercises();
            for (int i = 0; i < remaining.size(); i++) {
                UserExercise ex = remaining.get(i);
                Map<String,Object> data = new HashMap<>();
                data.put("name", ex.getName());
                data.put("description", ex.getDescription());
                data.put("equipmentType", ex.getEquipmentType());
                data.put("exerciseType", ex.getExerciseType());
                data.put("loadIntensity", ex.getLoadIntensity());
                data.put("movementType", ex.getMovementType());
                data.put("image", ex.getImage() != null ? ex.getImage() : "");
                data.put("musclesWorked", ex.getMusclesWorked());
                data.put("synergistMuscles", ex.getSynergistMuscles());
                data.put("series", ex.getSeries());
                data.put("weight", ex.getWeight());
                data.put("reps", ex.getReps());
                data.put("rpe", ex.getRpe());
                data.put("isFromInternal", ex.isFromInternal());

                String newKey = String.valueOf(i);
                userExercisesRef.child(newKey).setValue(data)
                        .addOnSuccessListener(a -> Log.d(TAG, "Reindex OK " + ex.getName() + " -> " + newKey))
                        .addOnFailureListener(e -> Log.e(TAG, "Reindex FAIL " + ex.getName() + " -> " + newKey, e));
            }
            if (onComplete != null) onComplete.run();
        });
    }

    private void guardarSeriesYFinalizar(Map<String, List<ExerciseSeries>> seriesPorEjercicio) {
        if (effectiveUid == null) return;

        Log.d(TAG, "Guardando series completadas…");
        DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference("users").child(effectiveUid).child("userExercises");

        for (Map.Entry<String, List<ExerciseSeries>> entry : seriesPorEjercicio.entrySet()) {
            String nombreEjercicio = entry.getKey();
            List<ExerciseSeries> nuevas = entry.getValue();

            baseRef.orderByChild("name").equalTo(nombreEjercicio)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                String key = snap.getKey();
                                DatabaseReference exerciseRef = baseRef.child(key);
                                DatabaseReference seriesRef = exerciseRef.child("seriesData");

                                seriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override public void onDataChange(@NonNull DataSnapshot prevSnap) {
                                        List<Map<String,Object>> todas = new ArrayList<>();
                                        if (prevSnap.exists()) {
                                            for (DataSnapshot s : prevSnap.getChildren()) {
                                                Object v = s.getValue();
                                                if (v instanceof Map) todas.add((Map<String, Object>) v);
                                            }
                                        }
                                        int nuevasComp = 0;
                                        for (ExerciseSeries serie : nuevas) {
                                            if (serie.isCompleted()) {
                                                Map<String,Object> nueva = new HashMap<>();
                                                nueva.put("seriesNumber", todas.size() + 1);
                                                nueva.put("reps", serie.getReps());
                                                nueva.put("weight", serie.getWeight());
                                                nueva.put("rpe", serie.getRpe());
                                                todas.add(nueva);
                                                nuevasComp++;
                                            }
                                        }
                                        if (nuevasComp > 0) {
                                            seriesRef.setValue(todas)
                                                    .addOnSuccessListener(a -> Log.d(TAG, "Series guardadas " + nombreEjercicio))
                                                    .addOnFailureListener(e -> Log.e(TAG, "Error guardando series", e));
                                            exerciseRef.child("series").setValue(todas.size())
                                                    .addOnSuccessListener(a -> Log.d(TAG, "Campo series actualizado a " + todas.size()))
                                                    .addOnFailureListener(e -> Log.e(TAG, "Error actualizando series", e));
                                        } else {
                                            Log.w(TAG, "Sin series completadas en " + nombreEjercicio);
                                        }
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "Error leyendo series previas", error.toException());
                                    }
                                });
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "Error consultando ejercicio", error.toException());
                        }
                    });
        }

        seriesSaved = true;

        // Estandarizado: diálogo de finalización (sin layout custom)
        showInfoDialog(
                "Entrenamiento finalizado",
                "Has completado tu entrenamiento en " + formatElapsedTime(pauseOffset) + ".",
                "Aceptar",
                () -> {
                    actualizarConteoSeriesEnFirebase(true);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        ((ViewPagerActivity) requireActivity()).switchToDashboardWorkoutFragment(new Bundle());
                    }, 1000);
                }
        );
    }

    // Ignoramos el parámetro recibido y usamos realDayKey
    private void guardarEjerciciosEnFirebase(List<Exercise> exercisesNuevos, String ignoredDayKey) {
        if (effectiveUid == null || realDayKey == null) { Log.e(TAG, "effectiveUid/realDayKey NULL al guardar ejercicios."); return; }

        DatabaseReference dayExercisesRef = FirebaseDatabase.getInstance()
                .getReference("users").child(effectiveUid)
                .child("userMovementPatterns").child(realDayKey).child("userExercises");

        DatabaseReference globalExercisesRef = FirebaseDatabase.getInstance()
                .getReference("users").child(effectiveUid).child("userExercises");

        globalExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot globalSnap) {
                Set<String> nombresGlobales = new HashSet<>();
                for (DataSnapshot snap : globalSnap.getChildren()) {
                    String n = snap.child("name").getValue(String.class);
                    if (n != null) nombresGlobales.add(n);
                }

                dayExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot daySnap) {
                        long index = daySnap.getChildrenCount();

                        for (Exercise ex : exercisesNuevos) {
                            Map<String,Object> data = new HashMap<>();
                            data.put("name", ex.getName());
                            data.put("description", ex.getDescription());
                            data.put("equipmentType", ex.getEquipmentType());
                            data.put("exerciseType", ex.getExerciseType());
                            data.put("loadIntensity", ex.getLoadIntensity());
                            data.put("movementType", ex.getMovementType());
                            data.put("image", ex.getImage() != null ? ex.getImage() : "");
                            data.put("musclesWorked", ex.getMusclesWorked());
                            data.put("synergistMuscles", ex.getSynergistMuscles());
                            data.put("series", ex.getSeries());
                            data.put("isFromInternal", ex.isFromInternal());

                            String childKey = String.valueOf(index++);
                            dayExercisesRef.child(childKey).setValue(data)
                                    .addOnSuccessListener(a -> Log.d(TAG, "Guardado en realDayKey=" + realDayKey + " key=" + childKey + " → " + ex.getName()))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error guardando en " + realDayKey, e));

                            if (!nombresGlobales.contains(ex.getName())) {
                                globalExercisesRef.push().setValue(data)
                                        .addOnSuccessListener(a -> Log.d(TAG, "Añadido a /userExercises → " + ex.getName()))
                                        .addOnFailureListener(e -> Log.e(TAG, "Error añadiendo a /userExercises", e));
                            } else {
                                Log.d(TAG, ex.getName() + " ya existe en /userExercises");
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e(TAG, "Error leyendo ejercicios del día: " + e.getMessage());
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e(TAG, "Error leyendo ejercicios globales: " + e.getMessage());
            }
        });
    }

    private void initOrNotifyAdapter() {
        if (adapter == null) {
            String keyParaUI = (realDayKey != null) ? realDayKey : dayKey;
            adapter = new TrainingDetailAdapter(
                    exercises,
                    recyclerViewExercises,
                    keyParaUI,
                    getContext(),
                    getChildFragmentManager(),
                    effectiveUid
            );
            recyclerViewExercises.setAdapter(adapter);
        } else {
            adapter.setExercises(exercises);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        // if (adapter != null) adapter.detachAllListeners();
    }
}
