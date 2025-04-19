package com.example.diets.fragments;

import android.app.AlertDialog;
import android.content.Context;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.TrainingDetailAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.example.diets.fragments.SelectExercisesFragment.OnExercisesSelectedListener;
import com.example.diets.viewmodel.TrainingDetailViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrainingDetailFragment extends Fragment implements OnExercisesSelectedListener {

    private static final String TAG = "TrainingDetailFragment";

    /* UI */
    private RecyclerView recyclerViewExercises;
    private Chronometer chronometerTraining;
    private ImageView btnBackToPrevious;
    private Button btnFinishWorkout;

    /* Datos y adaptador */
    private TrainingDetailAdapter adapter;
    private List<UserExercise> exercises = new ArrayList<>();

    /* ViewModel (persiste la lista) */
    private TrainingDetailViewModel viewModel;

    /* Estado para cronómetro */
    private boolean isChronometerRunning = false;
    private long   pauseOffset = 0;

    /* Día actual */
    private String dayKey;

    /* Bandera de guardado único */
    private boolean seriesSaved = false;

    /* Movimiento y sinergias */
    private MovementPattern movementPatternDelDia;
    private List<String> musculosSinergicosDelDia;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "TrainingDetailFragment - onCreateView: Inicio de creación de la vista del fragmento.");

        View view = inflater.inflate(R.layout.fragment_training_detail, container, false);
        Log.d(TAG, "TrainingDetailFragment - onCreateView: Layout del fragmento inflado correctamente.");

        recyclerViewExercises = view.findViewById(R.id.recyclerViewExercises);
        if (recyclerViewExercises != null) {
            recyclerViewExercises.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            Log.e(TAG, "TrainingDetailFragment - onCreateView: ERROR - recyclerViewExercises es NULL.");
        }

        Button btnAddExercise = view.findViewById(R.id.btnAddExercise);
        btnAddExercise.setEnabled(false); // Se habilitará cuando cargue el MovementPattern

        // ***********************************************************
        // Recuperar argumentos (dayKey = "Día X") y cargar ejercicios desde Firebase
        // ***********************************************************
        if (getArguments() != null) {
            dayKey = getArguments().getString("dayKey", "Día 1");
            Log.d(TAG, "TrainingDetailFragment - onCreateView: dayKey recibido → " + dayKey);

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) {
                Log.e(TAG, "❌ Usuario no autenticado.");
                return view;
            }
            String userId = user.getUid();

            // 1) Leer ejercicios desde Firebase
            DatabaseReference dayExercisesRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("userMovementPatterns")
                    .child(dayKey)
                    .child("userExercises");

            dayExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    exercises = new ArrayList<>();

                    FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                    if (user == null) {
                        Log.e(TAG, "❌ Usuario no autenticado.");
                        return;
                    }

                    String userId = user.getUid();
                    DatabaseReference globalExercisesRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId)
                            .child("userExercises");

                    globalExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot globalSnapshot) {
                            for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                                UserExercise ue = new UserExercise();

                                String nombreEjercicio = exerciseSnap.child("name").getValue(String.class);
                                ue.setName(nombreEjercicio);

                                // Buscar en userExercises (globales) el nodo que coincide por nombre
                                boolean foundMatch = false;
                                for (DataSnapshot globalSnap : globalSnapshot.getChildren()) {
                                    String nombreGlobal = globalSnap.child("name").getValue(String.class);
                                    if (nombreEjercicio != null && nombreEjercicio.equals(nombreGlobal)) {

                                        // Recolectar último peso desde seriesData
                                        DataSnapshot seriesDataSnap = globalSnap.child("seriesData");
                                        double lastWeight = 0.0;

                                        for (DataSnapshot serieSnap : seriesDataSnap.getChildren()) {
                                            Double weight = serieSnap.child("weight").getValue(Double.class);
                                            Log.d(TAG, "📦 Serie encontrada para " + nombreEjercicio + ": peso = " + weight);
                                            if (weight != null) lastWeight = weight;
                                        }

                                        ue.setLastLoad(lastWeight);
                                        Log.d(TAG, "TrainingDetailFragment - onDataChange: Carga previa recuperada desde seriesData para "
                                                + ue.getName() + " → " + ue.getLastLoad());

                                        foundMatch = true;
                                        break;
                                    }
                                }

                                if (!foundMatch) {
                                    Log.w(TAG, "⚠ No se encontró seriesData para " + nombreEjercicio + ", usando carga 0.0");
                                    ue.setLastLoad(0.0);
                                }

                                // Asignar series, reps, rpe, etc.
                                ue.setSeries(exerciseSnap.child("series").getValue(Integer.class) != null
                                        ? exerciseSnap.child("series").getValue(Integer.class)
                                        : 3);
                                ue.setReps(exerciseSnap.child("reps").getValue(Integer.class) != null
                                        ? exerciseSnap.child("reps").getValue(Integer.class)
                                        : 8);
                                ue.setRpe(exerciseSnap.child("rpe").getValue(Double.class) != null
                                        ? exerciseSnap.child("rpe").getValue(Double.class)
                                        : 7.0);
                                ue.setWeight(exerciseSnap.child("weight").getValue(Double.class) != null
                                        ? exerciseSnap.child("weight").getValue(Double.class)
                                        : 0.0);

                                // Imagen, si existe
                                String imageBase64 = exerciseSnap.child("image").getValue(String.class);
                                ue.setImage(imageBase64 != null ? imageBase64 : "");

                                exercises.add(ue);
                            }

                            Log.d(TAG, "✔ Total ejercicios cargados con lastLoad desde seriesData: " + exercises.size());

                            adapter = new TrainingDetailAdapter(exercises, recyclerViewExercises, dayKey);
                            recyclerViewExercises.setAdapter(adapter);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "❌ Error al leer userExercises globales: " + error.getMessage());
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "❌ Error al cargar ejercicios de Firebase: " + error.getMessage());
                }
            });

            // 2) Cargar MovementPattern de Firebase según dayKey
            obtenerMovementPatternDelDia(dayKey, pattern -> {
                if (pattern != null) {
                    movementPatternDelDia = pattern;
                    musculosSinergicosDelDia = pattern.getSynergistMuscles();

                    Log.d(TAG, "📌 Nombre del patrón recuperado: " + movementPatternDelDia.getName());
                    Log.d(TAG, "💪 Músculos sinérgicos: " + musculosSinergicosDelDia);

                    btnAddExercise.setEnabled(true); // habilitar botón
                } else {
                    Log.w(TAG, "⚠ No se encontró patrón de movimiento para el día: " + dayKey);
                }
            });
        } else {
            Log.w(TAG, "TrainingDetailFragment - onCreateView: No se recibieron argumentos.");
            exercises = new ArrayList<>();
        }

        // ************************************************************************
        // Botón "Agregar Ejercicio" - Abre MenuPopupFragmentAlt con el dayKey actual
        // ************************************************************************
        btnAddExercise.setOnClickListener(v -> {
            Log.d(TAG, "btnAddExercise: Mostrando MenuPopupFragmentAlt con día: " + dayKey);

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
                Log.e(TAG, "btnAddExercise: ❌ No se puede abrir selector porque movementPatternDelDia es NULL.");
            }
        });

        btnBackToPrevious = view.findViewById(R.id.btnBackToPrevious);
        btnBackToPrevious.setOnClickListener(v -> finishWorkout());

        chronometerTraining = view.findViewById(R.id.chronometerTraining);
        startChronometer();

        btnFinishWorkout = view.findViewById(R.id.btnFinishWorkout);
        btnFinishWorkout.setOnClickListener(v -> finishWorkout());

        return view;
    }


    // ****************************************************************
    // Implementación de la interfaz OnExercisesSelectedListener:
    // Llamado cuando se agregan ejercicios en ExerciseSelectionDialog/CreatedExercisesFragment
    // ****************************************************************
    @Override
    public void onExercisesSelected(List<Exercise> exercisesRecibidos, TrainingDay trainingDay) {
        Log.d(TAG, "TrainingDetailFragment - onExercisesSelected: Recibidos "
                + exercisesRecibidos.size() + " ejercicios desde el selector.");

        for (Exercise exercise : exercisesRecibidos) {
            Log.d(TAG, "➕ Intentando añadir ejercicio: " + exercise.getName());

            // ✅ Validación de duplicado
            boolean yaExiste = false;
            for (UserExercise existente : exercises) {
                if (existente.getName().equalsIgnoreCase(exercise.getName())) {
                    yaExiste = true;
                    break;
                }
            }

            if (yaExiste) {
                Log.w(TAG, "⚠ El ejercicio '" + exercise.getName() + "' ya está en la lista. No se añadirá duplicado.");
                Toast.makeText(getContext(), "Este ejercicio ya está agregado: " + exercise.getName(), Toast.LENGTH_SHORT).show();
                continue;
            }

            // Si no existe, lo añadimos normalmente
            UserExercise userExercise = new UserExercise(exercise);
            userExercise.setSeries(3);
            exercises.add(userExercise);

            adapter.notifyItemInserted(exercises.size() - 1);
            recyclerViewExercises.smoothScrollToPosition(exercises.size() - 1);
        }

        guardarEjerciciosEnFirebase(exercisesRecibidos, trainingDay.getDayTitle());
    }

    // ****************************************************************
    // Guarda una lista de nuevos ejercicios en "userMovementPatterns/<dayKey>/userExercises"
    // usando índices incrementales en vez de push().
    // ****************************************************************
    /**
     * Guarda la lista de ejercicios nuevos tanto en la rama del día (index 0,1,2…)
     * como en la colección global /userExercises si aún no existen allí.
     */
    private void guardarEjerciciosEnFirebase(List<Exercise> exercisesNuevos, String dayKey) {

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.e(TAG, "❌ Usuario no autenticado. No se guardarán ejercicios en Firebase.");
            return;
        }

        String userId = user.getUid();

        DatabaseReference dayExercisesRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("userMovementPatterns")
                .child(dayKey)
                .child("userExercises");

        DatabaseReference globalExercisesRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("userExercises");

    /* =========================================================
       1⃣  Lee UNA VEZ los nombres que ya existen en la rama global
       ========================================================= */
        globalExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot globalSnap) {

                //  ➡  Conjunto de nombres para evitar duplicados en /userExercises
                Set<String> nombresGlobales = new HashSet<>();
                for (DataSnapshot snap : globalSnap.getChildren()) {
                    String n = snap.child("name").getValue(String.class);
                    if (n != null) nombresGlobales.add(n);
                }

            /* =========================================================
               2⃣  Lee cuántos ejercicios hay ya guardados en el DÍA
               ========================================================= */
                dayExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot daySnap) {
                        long indice = daySnap.getChildrenCount();   // arranca en 0‑based

                    /* =====================================================
                       3⃣  Recorre los ejercicios recibidos y los escribe
                       ===================================================== */
                        for (Exercise ex : exercisesNuevos) {

                            // --- Datos a grabar (usa tu utilitario si lo prefieres) ---
                            Map<String, Object> exerciseData = new HashMap<>();
                            exerciseData.put("name",             ex.getName());
                            exerciseData.put("description",      ex.getDescription());
                            exerciseData.put("equipmentType",    ex.getEquipmentType());
                            exerciseData.put("exerciseType",     ex.getExerciseType());
                            exerciseData.put("loadIntensity",    ex.getLoadIntensity());
                            exerciseData.put("movementType",     ex.getMovementType());
                            exerciseData.put("image",            ex.getImage() != null ? ex.getImage() : "");
                            exerciseData.put("musclesWorked",    ex.getMusclesWorked());
                            exerciseData.put("synergistMuscles", ex.getSynergistMuscles());
                            exerciseData.put("series",           3);        // valor por defecto

                            /* ---- 3.a) RAMA DEL DÍA -------------------------------- */
                            String childKey = String.valueOf(indice);
                            dayExercisesRef.child(childKey).setValue(exerciseData)
                                    .addOnSuccessListener(aVoid ->
                                            Log.d(TAG, "✅ Guardado en " + dayKey
                                                    + " con key=" + childKey + " → " + ex.getName()))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "❌ Error al guardar en " + dayKey, e));
                            indice++;

                            /* ---- 3.b) RAMA GLOBAL (/userExercises) ---------------- */
                            if (!nombresGlobales.contains(ex.getName())) {
                                globalExercisesRef.push().setValue(exerciseData)
                                        .addOnSuccessListener(aVoid ->
                                                Log.d(TAG, "✅ Añadido a /userExercises → " + ex.getName()))
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "❌ Error al añadir a /userExercises", e));
                            } else {
                                Log.d(TAG, "↪ " + ex.getName()
                                        + " ya existe en /userExercises, no se duplica.");
                            }
                        }
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "❌ Error al leer ejercicios del día: " + error.getMessage());
                    }
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al leer ejercicios globales: " + error.getMessage());
            }
        });
    }


    // ****************************************************************
    // Método legacy (por si se usa en otra parte)
    // ****************************************************************
    public void addSelectedExercise(Exercise exercise, TrainingDay day) {
        Log.d(TAG, "addSelectedExercise: Recibido ejercicio → " + exercise.getName());

        UserExercise userExercise = new UserExercise(exercise);
        userExercise.setSeries(3);
        exercises.add(userExercise);

        adapter.notifyItemInserted(exercises.size() - 1);
        recyclerViewExercises.smoothScrollToPosition(exercises.size() - 1);

        // Si quisieras también guardarlo con índice incremental, puedes:
        // guardarEjerciciosEnFirebase(Collections.singletonList(exercise), day.getDayTitle());
    }

    // ****************************************************************
    // Cargar el MovementPattern (músculos sinérgicos) según el dayKey
    // ****************************************************************
    private void obtenerMovementPatternDelDia(String dayKey, MovementPatternCallback callback) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "❌ Usuario no autenticado.");
            return;
        }

        String userId = currentUser.getUid();

        DatabaseReference dayRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("userMovementPatterns")
                .child(dayKey);

        dayRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Log.w(TAG, "⚠ No se encontraron datos para " + dayKey);
                    callback.onPatternLoaded(null);
                    return;
                }

                // Nombre del patrón
                String name = snapshot.child("name").getValue(String.class);
                Log.d(TAG, "📌 Nombre del patrón recuperado: " + name);

                // Recorrer "userExercises" y extraer "synergistMuscles" para formar la sinergia
                Set<String> synergistMuscles = new HashSet<>();
                DataSnapshot exercisesSnapshot = snapshot.child("userExercises");

                for (DataSnapshot exerciseSnap : exercisesSnapshot.getChildren()) {
                    DataSnapshot synergistSnap = exerciseSnap.child("synergistMuscles");
                    for (DataSnapshot muscleSnap : synergistSnap.getChildren()) {
                        String muscle = muscleSnap.getValue(String.class);
                        if (muscle != null && !muscle.isEmpty()) {
                            synergistMuscles.add(muscle);
                            Log.d(TAG, "🧩 Músculo sinérgico leído de ejercicio: " + muscle);
                        }
                    }
                }

                // Crear MovementPattern
                MovementPattern pattern = new MovementPattern();
                pattern.setName(name != null ? name : dayKey);
                pattern.setSynergistMuscles(new ArrayList<>(synergistMuscles));

                Log.d(TAG, "✅ Movimiento reconstruido con " + synergistMuscles.size() + " músculos sinérgicos.");
                callback.onPatternLoaded(pattern);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al leer patrón de movimiento: " + error.getMessage());
                callback.onPatternLoaded(null);
            }
        });
    }

    public interface MovementPatternCallback {
        void onPatternLoaded(MovementPattern pattern);
    }

    // ****************************************************************
    // Cronómetro
    // ****************************************************************
    private void startChronometer() {
        Log.d(TAG, "TrainingDetailFragment - startChronometer: Verificando si el cronómetro ya está corriendo.");
        if (!isChronometerRunning) {
            chronometerTraining.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometerTraining.start();
            isChronometerRunning = true;
            Log.d(TAG, "TrainingDetailFragment - startChronometer: Cronómetro iniciado correctamente.");
        } else {
            Log.d(TAG, "TrainingDetailFragment - startChronometer: El cronómetro ya estaba corriendo.");
        }
    }

    // ************************************************************************
    // Lógica para finalizar entrenamiento y guardar series completadas globalmente
    // ************************************************************************
    private void finishWorkout() {
        // ================================================================================
        // = INICIO MOD NUEVA FUNCIONALIDAD: Confirmar borrado en Firebase si hay borrados =
        // ================================================================================
        if (!adapter.getPendingDeletionExercises().isEmpty()) {
            Log.d(TAG, "TrainingDetailFragment - finishWorkout: Existen ejercicios pendientes de borrar en Firebase.");

            new AlertDialog.Builder(requireContext())
                    .setTitle("Eliminar ejercicios permanentemente")
                    .setMessage("¿Deseas eliminar de tu rutina en Firebase los ejercicios que marcaste para borrar?")
                    .setPositiveButton("Sí, eliminar", (dialog, which) -> {
                        Log.d(TAG, "TrainingDetailFragment - finishWorkout: Usuario confirmó eliminación en Firebase.");
                        borrarYReindexarEjerciciosEnFirebase(adapter.getPendingDeletionExercises(), () -> {
                            // Después de reindexar, continuamos con el flujo normal
                            Log.d(TAG, "TrainingDetailFragment - finishWorkout: Reindexación completada. Continuando.");
                            // Llamamos de nuevo para entrar al flujo original (pero ya sin la alerta)
                            finishWorkout(true);
                        });
                    })
                    .setNegativeButton("No, mantener", (dialog, which) -> {
                        Log.d(TAG, "TrainingDetailFragment - finishWorkout: Usuario eligió NO eliminar en Firebase.");
                        // Continuamos el flujo original de finishWorkout
                        finishWorkout(true);
                    })
                    .show();

            // Salimos para no ejecutar dos veces el mismo flujo
            return;
        }
        // ================================================================================
        // = FIN MOD NUEVA FUNCIONALIDAD                                                   =
        // ================================================================================

        // ----------------------------------------------------------------------------
        // Las líneas que ya estaban en este método se ejecutan si NO hay ejercicios
        // pendientes, O si ya pasamos por la confirmación y volvimos con finishWorkout(true).
        // ----------------------------------------------------------------------------
        Log.d(TAG, "TrainingDetailFragment - finishWorkout: Comenzando flujo de finalización del entrenamiento.");

        // Detener cronómetro
        if (isChronometerRunning) {
            pauseOffset = SystemClock.elapsedRealtime() - chronometerTraining.getBase();
            chronometerTraining.stop();
            isChronometerRunning = false;
            Log.d(TAG, "TrainingDetailFragment - finishWorkout: Cronómetro detenido. Tiempo total: "
                    + formatElapsedTime(pauseOffset));
        }

        if (seriesSaved) {
            Log.w(TAG, "⚠ Las series ya fueron guardadas previamente. Se omite guardado.");
            return;
        }

        Map<String, List<ExerciseSeries>> seriesPorEjercicio = adapter.getAllCompletedSeriesGroupedByExercise();
        boolean haySeriesIncompletas = false;

        // Verificar si hay series incompletas
        for (UserExercise ejercicio : exercises) {
            List<ExerciseSeries> listaTotal = adapter.getSeriesListForExercise(ejercicio.getName());
            List<ExerciseSeries> completadas = seriesPorEjercicio.get(ejercicio.getName());

            if (listaTotal != null && completadas != null && completadas.size() < listaTotal.size()) {
                Log.w(TAG, "⚠ El ejercicio " + ejercicio.getName() + " tiene series incompletas.");
                haySeriesIncompletas = true;
                break;
            }
        }

        if (haySeriesIncompletas) {
            Log.d(TAG, "TrainingDetailFragment - finishWorkout: Mostrando diálogo por series incompletas.");
            showIncompleteSeriesDialog(seriesPorEjercicio);
        } else {
            guardarSeriesYFinalizar(seriesPorEjercicio);
        }
    }

    // ===========================================================================
    // = INICIO MOD NUEVA FUNCIONALIDAD: Sobrecarga para continuar el flujo     =
    // = después de la confirmación de borrado (NO se elimina ninguna línea)     =
    // ===========================================================================
    private void finishWorkout(boolean skipDeletionCheck) {
        // Este método se llama cuando YA se manejó la confirmación de borrado,
        // para que no volvamos a mostrar el diálogo ni hagamos return.
        Log.d(TAG, "TrainingDetailFragment - finishWorkout(boolean): Re-entrando al flujo sin verificar borrados.");

        // Se mantiene exactamente el mismo bloque de código que tu método original
        // a partir de aquí (copiado tal cual). No eliminamos nada:
        Log.d(TAG, "TrainingDetailFragment - finishWorkout: Comenzando flujo de finalización del entrenamiento.");

        // Detener cronómetro
        if (isChronometerRunning) {
            pauseOffset = SystemClock.elapsedRealtime() - chronometerTraining.getBase();
            chronometerTraining.stop();
            isChronometerRunning = false;
            Log.d(TAG, "TrainingDetailFragment - finishWorkout: Cronómetro detenido. Tiempo total: "
                    + formatElapsedTime(pauseOffset));
        }

        if (seriesSaved) {
            Log.w(TAG, "⚠ Las series ya fueron guardadas previamente. Se omite guardado.");
            return;
        }

        Map<String, List<ExerciseSeries>> seriesPorEjercicio = adapter.getAllCompletedSeriesGroupedByExercise();
        boolean haySeriesIncompletas = false;

        // Verificar si hay series incompletas
        for (UserExercise ejercicio : exercises) {
            List<ExerciseSeries> listaTotal = adapter.getSeriesListForExercise(ejercicio.getName());
            List<ExerciseSeries> completadas = seriesPorEjercicio.get(ejercicio.getName());

            if (listaTotal != null && completadas != null && completadas.size() < listaTotal.size()) {
                Log.w(TAG, "⚠ El ejercicio " + ejercicio.getName() + " tiene series incompletas.");
                haySeriesIncompletas = true;
                break;
            }
        }

        if (haySeriesIncompletas) {
            Log.d(TAG, "TrainingDetailFragment - finishWorkout: Mostrando diálogo por series incompletas.");
            showIncompleteSeriesDialog(seriesPorEjercicio);
        } else {
            guardarSeriesYFinalizar(seriesPorEjercicio);
        }
    }
    // ===========================================================================
    // = FIN MOD NUEVA FUNCIONALIDAD ==============================================
    // ===========================================================================

    private void showIncompleteSeriesDialog(Map<String, List<ExerciseSeries>> seriesPorEjercicio) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_incomplete_series, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
        Button btnGuardar = dialogView.findViewById(R.id.btnGuardarDeTodosModos);

        btnCancelar.setOnClickListener(v -> {
            Log.d(TAG, "TrainingDetailFragment - showIncompleteSeriesDialog: Usuario canceló guardado.");
            dialog.dismiss();
        });

        btnGuardar.setOnClickListener(v -> {
            Log.d(TAG, "TrainingDetailFragment - showIncompleteSeriesDialog: Usuario confirmó guardado incompleto.");
            guardarSeriesYFinalizar(seriesPorEjercicio);
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * Guarda las series completadas globalmente (users/<uid>/userExercises)
     * y finaliza el flujo de entrenamiento.
     */
    private void guardarSeriesYFinalizar(Map<String, List<ExerciseSeries>> seriesPorEjercicio) {
        Log.d(TAG, "TrainingDetailFragment - guardarSeriesYFinalizar: Iniciando guardado de series...");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("userExercises");

        for (Map.Entry<String, List<ExerciseSeries>> entry : seriesPorEjercicio.entrySet()) {
            String nombreEjercicio = entry.getKey();
            List<ExerciseSeries> nuevasSeries = entry.getValue();

            baseRef.orderByChild("name").equalTo(nombreEjercicio)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            for (DataSnapshot snap : snapshot.getChildren()) {
                                String key = snap.getKey();
                                DatabaseReference seriesRef = baseRef.child(key).child("seriesData");

                                // Añadir series completadas a la data existente
                                seriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshotPrevias) {
                                        List<Map<String, Object>> todasLasSeries = new ArrayList<>();

                                        if (snapshotPrevias.exists()) {
                                            for (DataSnapshot s : snapshotPrevias.getChildren()) {
                                                todasLasSeries.add((Map<String, Object>) s.getValue());
                                            }
                                        }

                                        for (ExerciseSeries serie : nuevasSeries) {
                                            if (serie.isCompleted()) {
                                                Map<String, Object> nueva = new HashMap<>();
                                                nueva.put("seriesNumber", todasLasSeries.size() + 1);
                                                nueva.put("reps", serie.getReps());
                                                nueva.put("weight", serie.getWeight());
                                                nueva.put("rpe", serie.getRpe());
                                                todasLasSeries.add(nueva);
                                            }
                                        }

                                        seriesRef.setValue(todasLasSeries)
                                                .addOnSuccessListener(aVoid ->
                                                        Log.d(TAG, "✅ Series guardadas para " + nombreEjercicio))
                                                .addOnFailureListener(e ->
                                                        Log.e(TAG, "❌ Error al guardar series", e));
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "❌ Error al leer series previas", error.toException());
                                    }
                                });
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "❌ Error al consultar ejercicio en Firebase", error.toException());
                        }
                    });
        }

        // Actualizar campo "series" en userMovementPatterns/dayKey/userExercises/<exerciseKey>/series
        actualizarConteoSeriesEnFirebase();
        seriesSaved = true;

        // Mostrar diálogo final de entrenamiento completado
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_training_finished, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        TextView title = dialogView.findViewById(R.id.txtDialogTitle);
        TextView message = dialogView.findViewById(R.id.txtDialogMessage);
        Button btnAceptar = dialogView.findViewById(R.id.btnAceptarDialog);

        title.setText("Entrenamiento finalizado");
        message.setText("Has completado tu entrenamiento en " + formatElapsedTime(pauseOffset) + ".");

        btnAceptar.setOnClickListener(v -> {
            dialog.dismiss();

            // Por si acaso, volver a actualizar
            actualizarConteoSeriesEnFirebase();
            seriesSaved = true;

            // Regresar al Dashboard tras 1 segundo
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "🏁 Redirigiendo al DashboardWorkoutFragment tras guardado.");
                ViewPagerActivity activity = (ViewPagerActivity) requireActivity();
                Bundle data = new Bundle();
                activity.switchToDashboardWorkoutFragment(data);
            }, 1000);
        });

        dialog.show();
    }

    /**
     * Actualiza el campo "series" de cada ejercicio en userMovementPatterns/<dayKey>/userExercises
     * para reflejar cuántas series tiene localmente en adapter.getSeriesListForExercise(...).
     */
    private void actualizarConteoSeriesEnFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference exercisesRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(userId)
                    .child("userMovementPatterns")
                    .child(dayKey)
                    .child("userExercises");

            for (UserExercise exercise : exercises) {
                final String exerciseName = exercise.getName();
                final List<ExerciseSeries> listaSeries = adapter.getSeriesListForExercise(exerciseName);
                final int seriesCount = (listaSeries != null) ? listaSeries.size() : 0;

                exercisesRef.orderByChild("name").equalTo(exerciseName)


                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot snap : snapshot.getChildren()) {
                                    String key = snap.getKey();
                                    exercisesRef.child(key).child("series").setValue(seriesCount);
                                    Log.d(TAG, "🔥 Actualizado series para " + exerciseName + ": " + seriesCount);
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "❌ Error al actualizar series totales: " + error.getMessage());
                            }
                        });
            }
        }
    }


    // Formatea el tiempo en HH:MM:SS
    private String formatElapsedTime(long elapsedMillis) {
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
        int hours = (int) ((elapsedMillis / (1000 * 60 * 60)) % 24);
        Log.d(TAG, "TrainingDetailFragment - formatElapsedTime: Tiempo formateado → "
                + String.format("%02d:%02d:%02d", hours, minutes, seconds));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Elimina de Firebase los ejercicios marcados como borrados localmente y luego reindexa
     * los ejercicios restantes (0, 1, 2, ...). Al finalizar, ejecuta el callback onComplete.
     */
    private void borrarYReindexarEjerciciosEnFirebase(List<UserExercise> exercisesToDelete, Runnable onComplete) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, "borrarYReindexarEjerciciosEnFirebase: Usuario no autenticado, no se puede reindexar.");
            if (onComplete != null) onComplete.run();
            return;
        }

        String userId = currentUser.getUid();
        DatabaseReference userExercisesRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("userMovementPatterns")
                .child(dayKey)
                .child("userExercises");

        // 1) Borrar por completo la rama userExercises del día actual
        userExercisesRef.removeValue().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "❌ Error al eliminar ejercicios en Firebase: " + task.getException());
                if (onComplete != null) onComplete.run();
                return;
            }

            // 2) Reindexar subiendo únicamente los ejercicios "exercises" que queden vivos en el adapter
            List<UserExercise> remainingExercises = adapter.getExercises();
            // => estos ya no incluyen los localmente borrados (que están en pendingDeletion)

            for (int i = 0; i < remainingExercises.size(); i++) {
                UserExercise ex = remainingExercises.get(i);

                Map<String, Object> exerciseData = new HashMap<>();
                exerciseData.put("name", ex.getName());
                exerciseData.put("description", ex.getDescription());
                exerciseData.put("equipmentType", ex.getEquipmentType());
                exerciseData.put("exerciseType", ex.getExerciseType());
                exerciseData.put("loadIntensity", ex.getLoadIntensity());
                exerciseData.put("movementType", ex.getMovementType());
                exerciseData.put("image", (ex.getImage() != null ? ex.getImage() : ""));
                exerciseData.put("musclesWorked", ex.getMusclesWorked());
                exerciseData.put("synergistMuscles", ex.getSynergistMuscles());
                exerciseData.put("series", ex.getSeries());
                exerciseData.put("weight", ex.getWeight());
                exerciseData.put("reps", ex.getReps());
                exerciseData.put("rpe", ex.getRpe());

                String newIndexKey = String.valueOf(i);
                userExercisesRef.child(newIndexKey).setValue(exerciseData)
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "✅ Reindex: agregado " + ex.getName() + " con key=" + newIndexKey))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "❌ Error al reindexar " + ex.getName() + " con key=" + newIndexKey, e));
            }

            Log.d(TAG, "borrarYReindexarEjerciciosEnFirebase: Reindexación finalizada con éxito.");
            if (onComplete != null) onComplete.run();
        });
    }
    // ===============================================================================================
    // == FIN MOD NUEVA FUNCIONALIDAD ================================================================
    // ===============================================================================================
}
