package com.example.diets.fragments;

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
import androidx.appcompat.app.AlertDialog;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TrainingDetailFragment extends Fragment implements SelectExercisesFragment.OnExercisesSelectedListener {
    private static final String TAG = "TrainingDetailFragment";

    private RecyclerView recyclerViewExercises;
    private TrainingDetailAdapter adapter;
    private List<UserExercise> exercises;

    private ImageView btnBackToPrevious;
    private Chronometer chronometerTraining;
    private boolean isChronometerRunning = false;
    private long pauseOffset = 0;
    private Button btnFinishWorkout;
    private String dayKey;
    private boolean seriesSaved = false; // 🔐 Previene múltiples guardados
    private MovementPattern movementPatternDelDia;
    private List<String> musculosSinergicosDelDia;
    private TrainingDay trainingDayDelDia;
    private MovementPattern movementPattern; // 🔥

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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
        btnAddExercise.setEnabled(false); // 🔒 Desactivado hasta que se cargue el patrón

        if (getArguments() != null) {
            Log.d(TAG, "TrainingDetailFragment - onCreateView: Recuperando argumentos desde el Bundle.");
            exercises = (List<UserExercise>) getArguments().getSerializable("exercises");
            dayKey = getArguments().getString("dayKey", "Día 1");
            Log.d(TAG, "TrainingDetailFragment - onCreateView: dayKey recibido → " + dayKey);

            obtenerMovementPatternDelDia(dayKey, pattern -> {
                if (pattern != null) {
                    movementPatternDelDia = pattern;
                    musculosSinergicosDelDia = pattern.getSynergistMuscles();

                    Log.d(TAG, "📌 Nombre del patrón recuperado: " + movementPatternDelDia.getName());
                    Log.d(TAG, "💪 Músculos sinérgicos: " + musculosSinergicosDelDia);

                    // ✅ Activar botón cuando el patrón esté cargado
                    btnAddExercise.setEnabled(true);
                } else {
                    Log.w(TAG, "⚠ No se encontró patrón de movimiento para el día: " + dayKey);
                }
            });
        } else {
            Log.w(TAG, "TrainingDetailFragment - onCreateView: No se recibieron argumentos. Inicializando lista vacía.");
            exercises = new ArrayList<>();
        }

        btnAddExercise.setOnClickListener(v -> {
            Log.d(TAG, "btnAddExercise: Mostrando MenuPopupFragmentAlt con día: " + dayKey);

            if (movementPatternDelDia != null) {
                TrainingDay trainingDay = new TrainingDay();
                trainingDay.setDayIndex(0);
                trainingDay.setDayTitle(dayKey);
                trainingDay.setMovementPatterns(Collections.singletonList(movementPatternDelDia)); // ✅ Aquí va el patrón REAL

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
        btnBackToPrevious.setOnClickListener(v -> switchToDashboardWorkout());

        chronometerTraining = view.findViewById(R.id.chronometerTraining);
        startChronometer();

        btnFinishWorkout = view.findViewById(R.id.btnFinishWorkout);
        btnFinishWorkout.setOnClickListener(v -> finishWorkout());

        adapter = new TrainingDetailAdapter(exercises, recyclerViewExercises);
        if (recyclerViewExercises != null) {
            recyclerViewExercises.setAdapter(adapter);
        } else {
            Log.e(TAG, "TrainingDetailFragment - onCreateView: ERROR - RecyclerView es NULL al asignar adaptador.");
        }

        return view;
    }
    @Override
    public void onExercisesSelected(List<Exercise> exercises, TrainingDay trainingDay) {
        Log.d(TAG, "TrainingDetailFragment - onExercisesSelected: Recibidos " + exercises.size() + " ejercicios desde el selector.");

        for (Exercise exercise : exercises) {
            Log.d(TAG, "➕ Añadiendo ejercicio: " + exercise.getName());

            UserExercise userExercise = new UserExercise(exercise);
            userExercise.setSeries(3);
            this.exercises.add(userExercise);

            adapter.notifyItemInserted(this.exercises.size() - 1);
            recyclerViewExercises.smoothScrollToPosition(this.exercises.size() - 1);
        }
    }


    public void addSelectedExercise(Exercise exercise, TrainingDay day) {
        Log.d(TAG, "addSelectedExercise: Recibido ejercicio → " + exercise.getName());

        UserExercise userExercise = new UserExercise(exercise);
        userExercise.setSeries(3);
        exercises.add(userExercise);

        adapter.notifyItemInserted(exercises.size() - 1);
        recyclerViewExercises.smoothScrollToPosition(exercises.size() - 1);
    }


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

                // ✅ Obtener nombre del patrón
                String name = snapshot.child("name").getValue(String.class);
                Log.d(TAG, "📌 Nombre del patrón recuperado: " + name);

                // ✅ Recorrer userExercises para extraer músculos sinérgicos
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

                // ✅ Crear objeto MovementPattern completo
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




    private void finishWorkout() {
        Log.d(TAG, "TrainingDetailFragment - finishWorkout: Comenzando flujo de finalización del entrenamiento.");

        // ⏱️ Detener cronómetro
        if (isChronometerRunning) {
            pauseOffset = SystemClock.elapsedRealtime() - chronometerTraining.getBase();
            chronometerTraining.stop();
            isChronometerRunning = false;
            Log.d(TAG, "TrainingDetailFragment - finishWorkout: Cronómetro detenido. Tiempo total: " + formatElapsedTime(pauseOffset));
        }

        if (seriesSaved) {
            Log.w(TAG, "⚠️ Las series ya fueron guardadas previamente. Se omite guardado.");
            return;
        }

        Map<String, List<ExerciseSeries>> seriesPorEjercicio = adapter.getAllCompletedSeriesGroupedByExercise();
        boolean haySeriesIncompletas = false;

        // ✅ Verificar si hay series incompletas
        for (UserExercise ejercicio : exercises) {
            List<ExerciseSeries> listaTotal = adapter.getSeriesListForExercise(ejercicio.getName());
            List<ExerciseSeries> completadas = seriesPorEjercicio.get(ejercicio.getName());

            if (listaTotal != null && completadas != null && completadas.size() < listaTotal.size()) {
                Log.w(TAG, "⚠️ El ejercicio " + ejercicio.getName() + " tiene series incompletas.");
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

                                // Acumular series previas + nuevas completadas
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
                                                .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Series guardadas para " + nombreEjercicio))
                                                .addOnFailureListener(e -> Log.e(TAG, "❌ Error al guardar series", e));
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

        // 🔁 Actualizar cantidad de series
        actualizarConteoSeriesEnFirebase();
        seriesSaved = true;
// ✅ Mostrar diálogo personalizado de entrenamiento finalizado
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_training_finished, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

// Rellenar contenido dinámico
        TextView title = dialogView.findViewById(R.id.txtDialogTitle);
        TextView message = dialogView.findViewById(R.id.txtDialogMessage);
        Button btnAceptar = dialogView.findViewById(R.id.btnAceptarDialog);

        title.setText("Entrenamiento finalizado");
        message.setText("Has completado tu entrenamiento en " + formatElapsedTime(pauseOffset) + ".");

        btnAceptar.setOnClickListener(v -> {
            dialog.dismiss();

            // 🔄 Actualizar conteo de series
            actualizarConteoSeriesEnFirebase();
            seriesSaved = true;

            // 🔁 Regresar al Dashboard tras 1 segundo
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Log.d(TAG, "🏁 Redirigiendo al DashboardWorkoutFragment tras guardado.");
                ViewPagerActivity activity = (ViewPagerActivity) requireActivity();
                Bundle data = new Bundle();
                activity.switchToDashboardWorkoutFragment(data);
            }, 1000);
        });

        dialog.show();

    }

    private void actualizarConteoSeriesEnFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference baseRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(userId).child("userMovementPatterns");

            for (UserExercise exercise : exercises) {
                String exerciseName = exercise.getName();
                DatabaseReference exercisesRef = baseRef.child(dayKey).child("userExercises");

                exercisesRef.orderByChild("name").equalTo(exerciseName).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            String key = snap.getKey();
                            List<ExerciseSeries> currentSeriesList = adapter.getSeriesListForExercise(exerciseName);
                            int seriesCount = (currentSeriesList != null) ? currentSeriesList.size() : 0;
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


    private String formatElapsedTime(long elapsedMillis) {
        int seconds = (int) (elapsedMillis / 1000) % 60;
        int minutes = (int) ((elapsedMillis / (1000 * 60)) % 60);
        int hours = (int) ((elapsedMillis / (1000 * 60 * 60)) % 24);
        Log.d(TAG, "TrainingDetailFragment - formatElapsedTime: Tiempo formateado → " + String.format("%02d:%02d:%02d", hours, minutes, seconds));
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void switchToDashboardWorkout() {
        Log.d(TAG, "TrainingDetailFragment - switchToDashboardWorkout: Usuario presionó el botón de retroceso.");
        ViewPagerActivity activity = (ViewPagerActivity) requireActivity();
        Bundle data = new Bundle();
        activity.switchToDashboardWorkoutFragment(data);
        Log.d(TAG, "TrainingDetailFragment - switchToDashboardWorkout: Navegación a DashboardWorkoutFragment realizada.");
    }
}
