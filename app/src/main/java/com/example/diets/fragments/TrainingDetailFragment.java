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
import androidx.fragment.app.FragmentManager;
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
import com.example.diets.model.ProgresionDinamica;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.example.diets.fragments.SelectExercisesFragment.OnExercisesSelectedListener;
import com.example.diets.viewmodel.TrainingDetailViewModel;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class TrainingDetailFragment extends Fragment implements OnExercisesSelectedListener {

    private static final String TAG = "TrainingDetailFragment";

    /* UI */
    private RecyclerView recyclerViewExercises;
    private Chronometer chronometerTraining;
    private ImageView btnBackToPrevious;
    private Button btnFinishWorkout;
    private boolean isFromInternal = false;
    private String exerciseIndex; // 🔑 ID del ejercicio de Firebase

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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            exerciseIndex = getArguments().getString("exerciseIndex");
            dayKey = getArguments().getString("dayKey");
            isFromInternal = getArguments().getBoolean("isFromInternal", false); // ✅ NUEVO
        }
    }

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
                @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                    exercises = new ArrayList<>();

                    // Leemos también /userExercises globales para el lastLoad...
                    DatabaseReference globalExercisesRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(userId)
                            .child("userExercises");

                    globalExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot globalSnapshot) {
                            for (DataSnapshot exerciseSnap : snapshot.getChildren()) {
                                UserExercise ue = new UserExercise();
                                ue.hydrateFromSnapshot(exerciseSnap);
                                // Nombre
                                String nombre = exerciseSnap.child("name").getValue(String.class);
                                ue.setName(nombre);

                                Boolean interno = exerciseSnap.child("isFromInternal")
                                        .getValue(Boolean.class);
                                ue.setFromInternal(interno != null && interno);

                                // Series / reps / rpe / weight / image
                                ue.setSeries(exerciseSnap.child("series").getValue(Integer.class) != null
                                        ? exerciseSnap.child("series").getValue(Integer.class) : 3);
                                ue.setReps(exerciseSnap.child("reps").getValue(Integer.class) != null
                                        ? exerciseSnap.child("reps").getValue(Integer.class) : 8);
                                ue.setRpe(exerciseSnap.child("rpe").getValue(Double.class) != null
                                        ? exerciseSnap.child("rpe").getValue(Double.class) : 7.0);
                                ue.setWeight(exerciseSnap.child("weight").getValue(Double.class) != null
                                        ? exerciseSnap.child("weight").getValue(Double.class) : 0.0);
                                ue.setImage(exerciseSnap.child("image").getValue(String.class));

                                // último peso (lastLoad) buscándolo en la rama global:
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

                            adapter = new TrainingDetailAdapter(
                                    exercises,
                                    recyclerViewExercises,
                                    dayKey,
                                    getContext(),
                                    getChildFragmentManager()   // ← nuevo parámetro
                            );

                            AtomicInteger pendientes = new AtomicInteger(exercises.size());   // ← AÑADE ESTA LÍNEA

                            recyclerViewExercises.setAdapter(adapter);
                            for (UserExercise ue : exercises) {
                                String exerciseName = ue.getName();

                                // 🔍 Buscar la key real desde la rama global
                                for (DataSnapshot gSnap : globalSnapshot.getChildren()) {
                                    String gName = gSnap.child("name").getValue(String.class);
                                    if (exerciseName.equalsIgnoreCase(gName)) {

                                        String firebaseKey = gSnap.getKey();
                                        adapter.setFirebaseKey(exerciseName, firebaseKey);

                                        /* ↓↓↓ Cargamos historial; al terminar descontamos ‘pendientes’ */
                                        SeriesAdapter sa = adapter.getSeriesAdapterPorNombre(exerciseName);
                                        if (sa != null) {
                                            sa.cargarUltimasSeriesDesdeFirebase(firebaseKey, getContext(), () -> {
                                                if (pendientes.decrementAndGet() == 0) {
                                                    Log.d(TAG, "Historial cargado → ahora sí pedimos sugerencia");
                                                    pedirRecomendacion();       // ⬅️ llama a ProgresionDinamica
                                                }
                                            });
                                        } else {
                                            // Fallback si aún no hay SeriesAdapter
                                            if (pendientes.decrementAndGet() == 0) {
                                                pedirRecomendacion();
                                            }
                                        }
                                        break;      // salimos del for gSnap
                                    }
                                }
                            }


                            ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                                    ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                                @Override
                                public boolean onMove(@NonNull RecyclerView recyclerView,
                                                      @NonNull RecyclerView.ViewHolder viewHolder,
                                                      @NonNull RecyclerView.ViewHolder target) {
                                    int fromPosition = viewHolder.getAdapterPosition();
                                    int toPosition = target.getAdapterPosition();
                                    adapter.onItemMove(fromPosition, toPosition); // ✅ este método es el que acabamos de definir
                                    return true;
                                }

                                @Override
                                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                                    // No usado aquí
                                }

                                @Override
                                public boolean isLongPressDragEnabled() {
                                    return true;
                                }
                            };

                            ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
                            touchHelper.attachToRecyclerView(recyclerViewExercises);



                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {
                            Log.e(TAG, "❌ Error al leer userExercises globales: " + e.getMessage());
                        }
                    });
                }
                @Override public void onCancelled(@NonNull DatabaseError e) {
                    Log.e(TAG, "❌ Error al cargar ejercicios de Firebase: " + e.getMessage());
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
        btnFinishWorkout.setOnClickListener(v -> {
            adapter.commitAllEdits(() -> {

                finishWorkout();        // cierra el entrenamiento y muestra el diálogo
            });
        });

        return view;
    }
    private void pedirRecomendacion() {
        if (adapter != null) {
            adapter.generarSugerencias();   // o el método que creaste para llamar a ProgresionDinamica
        }
    }


    @Override
    public void onExercisesSelected(List<Exercise> exercisesRecibidos, TrainingDay trainingDay) {
        Log.d(TAG, "onExercisesSelected: Recibidos " + exercisesRecibidos.size() + " ejercicios.");

        // ① Marco TODOS como “internos”
        for (Exercise ex : exercisesRecibidos) {
            ex.setFromInternal(true);
        }

        // ② Ahora guardo: el método incluirá isFromInternal en Firebase
        guardarEjerciciosEnFirebase(exercisesRecibidos, trainingDay.getDayTitle());

        // ③ Refresco UI
        for (Exercise ex : exercisesRecibidos) {
            boolean yaExiste = false;
            for (UserExercise u : exercises) {
                if (u.getName().equalsIgnoreCase(ex.getName())) {
                    yaExiste = true;
                    break;
                }
            }
            if (yaExiste) {
                Toast.makeText(getContext(),
                        "Este ejercicio ya está agregado: " + ex.getName(),
                        Toast.LENGTH_SHORT).show();
                continue;
            }
            UserExercise ue = new UserExercise(ex);
            exercises.add(ue);
            adapter.notifyItemInserted(exercises.size() - 1);
            recyclerViewExercises.smoothScrollToPosition(exercises.size() - 1);
        }
    }



    public static ExerciseDetailsDialogFragment newInstance(String exerciseId, String dayKey, boolean isFromInternal) {
        ExerciseDetailsDialogFragment fragment = new ExerciseDetailsDialogFragment();
        Bundle args = new Bundle();
        args.putString("exerciseIndex", exerciseId);
        args.putString("dayKey", dayKey);
        args.putBoolean("isFromInternal", isFromInternal); // ✅ NUEVO
        fragment.setArguments(args);
        return fragment;
    }





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

        adapter.commitAllEdits();
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


            Map<String, List<ExerciseSeries>> completadasPorEjercicio =
                    adapter.getAllCompletedSeriesGroupedByExercise();   // ① ya viene filtrado

            int totalCompletadas = 0;
            for (List<ExerciseSeries> l : completadasPorEjercicio.values())
                totalCompletadas += l.size();

            if (totalCompletadas == 0) {
                Toast.makeText(getContext(),
                        "Marca al menos una serie como completada antes de finalizar",
                        Toast.LENGTH_SHORT).show();
                return;                               // ⛔ ¡salimos!
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("¿Actualizar rutina?")
                    .setMessage("¿Deseas que el número de series se actualice en tu rutina según los cambios hechos hoy?")
                    .setPositiveButton("Sí, actualizar", (dialog, which) -> {
                        actualizarConteoSeriesEnFirebase(); // ✅ actualiza series en userMovementPatterns
                        guardarSeriesYFinalizar(seriesPorEjercicio);
                    })
                    .setNegativeButton("No actualizar", (dialog, which) -> {
                        guardarSeriesYFinalizar(seriesPorEjercicio);
                    })
                    .show();
        }

    }

    // ───────────── TrainingDetailFragment.java ─────────────
    /**
     * Pinta las series de un ejercicio sin crear la “serie fantasma”.
     * Se muestra un mensaje o un botón para que el usuario añada la primera serie.
     */
    private void mostrarSeries(UserExercise ex) {
        List<ExerciseSeries> listaSeries = new ArrayList<>();

        // ─── sin paso 2: no buscamos txtSinSeries ───

        SeriesAdapter sa = new SeriesAdapter(
                listaSeries,
                ex.getName(),
                adapter
        );
        recyclerViewExercises.setAdapter(sa);
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

            // NUEVO: reindexar antes de guardar series
            borrarYReindexarEjerciciosEnFirebase(adapter.getPendingDeletionExercises(), () -> {
                guardarSeriesYFinalizar(seriesPorEjercicio);
            });

            dialog.dismiss();
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
                final int seriesCount = (listaSeries != null && !listaSeries.isEmpty())
                        ? listaSeries.size()
                        : exercise.getSeries();  // Mantener el valor original si no hay series creadas


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

            // 2) Reindexar subiendo los ejercicios que permanecen en el adapter
            List<UserExercise> remainingExercises = adapter.getExercises(); // 🔄 ya está reordenado

            Log.d(TAG, "📦 Reindexando ejercicios en nuevo orden:");
            for (int i = 0; i < remainingExercises.size(); i++) {
                UserExercise ex = remainingExercises.get(i);

                Map<String, Object> exerciseData = new HashMap<>();
                exerciseData.put("name", ex.getName());
                exerciseData.put("description", ex.getDescription());
                exerciseData.put("equipmentType", ex.getEquipmentType());
                exerciseData.put("exerciseType", ex.getExerciseType());
                exerciseData.put("loadIntensity", ex.getLoadIntensity());
                exerciseData.put("movementType", ex.getMovementType());
                exerciseData.put("image", ex.getImage() != null ? ex.getImage() : "");
                exerciseData.put("musclesWorked", ex.getMusclesWorked());
                exerciseData.put("synergistMuscles", ex.getSynergistMuscles());
                exerciseData.put("series", ex.getSeries());
                exerciseData.put("weight", ex.getWeight());
                exerciseData.put("reps", ex.getReps());
                exerciseData.put("rpe", ex.getRpe());
                exerciseData.put("isFromInternal", ex.isFromInternal()); // ✅ persistimos si es interno

                String newIndexKey = String.valueOf(i);
                Log.d(TAG, "🔢 Posición " + i + ": " + ex.getName() + " (isFromInternal=" + ex.isFromInternal() + ")");

                userExercisesRef.child(newIndexKey).setValue(exerciseData)
                        .addOnSuccessListener(aVoid ->
                                Log.d(TAG, "✅ Reindex: agregado " + ex.getName() + " con key=" + newIndexKey))
                        .addOnFailureListener(e ->
                                Log.e(TAG, "❌ Error al reindexar " + ex.getName() + " con key=" + newIndexKey, e));
            }

            Log.d(TAG, "✅ Reindexación finalizada exitosamente.");
            if (onComplete != null) onComplete.run();
        });
    }


    // ************************************************************************
// Lógica para guardar las series completadas y cerrar el entrenamiento
// ************************************************************************
    private void guardarSeriesYFinalizar(Map<String, List<ExerciseSeries>> seriesPorEjercicio) {

        Log.d(TAG, "TrainingDetailFragment - guardarSeriesYFinalizar: Iniciando guardado de series…");

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        DatabaseReference baseRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("userExercises");

        // --------------------------------------------------------------------
        // 1. Subir todas las series completadas a Firebase
        // --------------------------------------------------------------------
        for (Map.Entry<String, List<ExerciseSeries>> entry : seriesPorEjercicio.entrySet()) {

            String nombreEjercicio      = entry.getKey();
            List<ExerciseSeries> nuevas = entry.getValue();

            baseRef.orderByChild("name").equalTo(nombreEjercicio)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapshot) {

                            for (DataSnapshot snap : snapshot.getChildren()) {

                                String key = snap.getKey();
                                DatabaseReference exerciseRef = baseRef.child(key);
                                DatabaseReference seriesRef   = exerciseRef.child("seriesData");

                                seriesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override public void onDataChange(@NonNull DataSnapshot prevSnap) {

                                        List<Map<String, Object>> todas = new ArrayList<>();

                                        if (prevSnap.exists()) {
                                            for (DataSnapshot s : prevSnap.getChildren()) {
                                                todas.add((Map<String, Object>) s.getValue());
                                            }
                                        }

                                        int nuevasCompletadas = 0;
                                        for (ExerciseSeries serie : nuevas) {
                                            if (serie.isCompleted()) {
                                                Map<String, Object> nueva = new HashMap<>();
                                                nueva.put("seriesNumber", todas.size() + 1);
                                                nueva.put("reps",   serie.getReps());
                                                nueva.put("weight", serie.getWeight());
                                                nueva.put("rpe",    serie.getRpe());
                                                todas.add(nueva);
                                                nuevasCompletadas++;
                                            }
                                        }

                                        if (nuevasCompletadas > 0) {

                                            Log.d(TAG, "✅ " + nuevasCompletadas + " nuevas series para "
                                                    + nombreEjercicio + " (total " + todas.size() + ")");

                                            // ⬇️ guardar lista completa
                                            seriesRef.setValue(todas)
                                                    .addOnSuccessListener(a -> Log.d(TAG, "📤 Series guardadas"))
                                                    .addOnFailureListener(e -> Log.e(TAG, "❌ Error guardando series", e));

                                            // ⬇️ actualizar campo ‘series’ en el nodo del ejercicio
                                            exerciseRef.child("series").setValue(todas.size())
                                                    .addOnSuccessListener(a -> Log.d(TAG, "🔥 Campo 'series' actualizado a "
                                                            + todas.size()))
                                                    .addOnFailureListener(e -> Log.e(TAG, "❌ Error actualizando 'series'", e));

                                        } else {
                                            Log.w(TAG, "⚠️ No hubo series completadas en " + nombreEjercicio);
                                        }
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e(TAG, "❌ Error al leer series previas", error.toException());
                                    }
                                });
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError error) {
                            Log.e(TAG, "❌ Error al consultar ejercicio en Firebase", error.toException());
                        }
                    });
        }

        // --------------------------------------------------------------------
        // 2. Marcamos estado como guardado y reseteamos el contador de RIR-target
        // --------------------------------------------------------------------
        seriesSaved = true;
        Log.d(TAG, "··· pctCounter reseteado ···");

        // --------------------------------------------------------------------
        // 3. Mostrar diálogo de “Entrenamiento finalizado”
        // --------------------------------------------------------------------
        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_training_finished, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        TextView title       = dialogView.findViewById(R.id.txtDialogTitle);
        TextView message     = dialogView.findViewById(R.id.txtDialogMessage);
        Button   btnAceptar  = dialogView.findViewById(R.id.btnAceptarDialog);

        title.setText("Entrenamiento finalizado");
        message.setText("Has completado tu entrenamiento en "
                + formatElapsedTime(pauseOffset) + ".");

        btnAceptar.setOnClickListener(v -> {
            dialog.dismiss();

            // Actualiza nº de series en userMovementPatterns
            actualizarConteoSeriesEnFirebase();

            // Ya estaba marcado como guardado arriba; no es necesario repetir
            // seriesSaved = true;

            // Volvemos al dashboard tras 1 s para dar tiempo al usuario a ver el diálogo
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "🏁 Redirigiendo al DashboardWorkoutFragment tras guardado.");
                ((ViewPagerActivity) requireActivity())
                        .switchToDashboardWorkoutFragment(new Bundle());
            }, 1000);
        });

        dialog.show();
    }

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

        // 1⃣ Leemos los nombres ya existentes en /userExercises
        globalExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot globalSnap) {
                Set<String> nombresGlobales = new HashSet<>();
                for (DataSnapshot snap : globalSnap.getChildren()) {
                    String n = snap.child("name").getValue(String.class);
                    if (n != null) nombresGlobales.add(n);
                }

                // 2⃣ Leemos cuántos ejercicios ya hay en la rama del día
                dayExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot daySnap) {
                        long index = daySnap.getChildrenCount();

                        // 3⃣ Recorremos y escribimos cada Exercise
                        for (Exercise ex : exercisesNuevos) {
                            Map<String,Object> exerciseData = new HashMap<>();
                            exerciseData.put("name",             ex.getName());
                            exerciseData.put("description",      ex.getDescription());
                            exerciseData.put("equipmentType",    ex.getEquipmentType());
                            exerciseData.put("exerciseType",     ex.getExerciseType());
                            exerciseData.put("loadIntensity",    ex.getLoadIntensity());
                            exerciseData.put("movementType",     ex.getMovementType());
                            exerciseData.put("image",            ex.getImage() != null ? ex.getImage() : "");
                            exerciseData.put("musclesWorked",    ex.getMusclesWorked());
                            exerciseData.put("synergistMuscles", ex.getSynergistMuscles());
                            // ← ahora usamos el valor real de series
                            exerciseData.put("series",           ex.getSeries());
                            // ← y persistimos la bandera interna
                            exerciseData.put("isFromInternal",   ex.isFromInternal());

                            // 3.a) Guardar en userMovementPatterns/<day>/userExercises
                            String childKey = String.valueOf(index++);
                            dayExercisesRef.child(childKey)
                                    .setValue(exerciseData)
                                    .addOnSuccessListener(a ->
                                            Log.d(TAG, "✅ Guardado en " + dayKey
                                                    + " key=" + childKey + " → " + ex.getName()))
                                    .addOnFailureListener(e ->
                                            Log.e(TAG, "❌ Error al guardar en " + dayKey, e));

                            // 3.b) Si no existía en /userExercises, lo añadimos
                            if (!nombresGlobales.contains(ex.getName())) {
                                globalExercisesRef.push()
                                        .setValue(exerciseData)
                                        .addOnSuccessListener(a ->
                                                Log.d(TAG, "✅ Añadido a /userExercises → " + ex.getName()))
                                        .addOnFailureListener(e ->
                                                Log.e(TAG, "❌ Error al añadir a /userExercises", e));
                            } else {
                                Log.d(TAG, "↪ " + ex.getName() + " ya existe en /userExercises");
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {
                        Log.e(TAG, "❌ Error al leer ejercicios del día: " + e.getMessage());
                    }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Log.e(TAG, "❌ Error al leer ejercicios globales: " + e.getMessage());
            }
        });
    }
    @Override
    public void onStop() {
        super.onStop();
//        if (adapter != null) adapter.detachAllListeners();
    }


}
