package com.example.diets.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.fragments.ExerciseDetailsDialogFragment;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.UserExercise;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

import java.util.*;

public class TrainingDetailAdapter extends RecyclerView.Adapter<TrainingDetailAdapter.ExerciseViewHolder> {
    private static final String TAG = "TrainingDetailAdapter";
    private List<UserExercise> exercises;
    private boolean hasUnsavedChanges = false;
    private RecyclerView recyclerView;
    private final List<ExerciseViewHolder> viewHolders = new ArrayList<>();
    private final Map<String, Integer> exerciseSeriesCountMap = new HashMap<>();
    private boolean seriesSaved = false;
    private String dayKey; // día actual
    ImageView iconAction;

    // ===================================================================================
    // === INICIO MOD NUEVA FUNCIONALIDAD: Lista auxiliar para "pendientes de eliminar" ===
    // ===================================================================================
    /**
     * Mantendrá los ejercicios que se eliminan localmente del RecyclerView,
     * pero aún no eliminados permanentemente de Firebase.
     */
    private final List<UserExercise> pendingDeletionExercises = new ArrayList<>();

    /**
     * Método público para obtener la lista de ejercicios marcados para eliminar.
     */
    public List<UserExercise> getPendingDeletionExercises() {
        return pendingDeletionExercises;
    }
    // ===================================================================================
    // === FIN MOD NUEVA FUNCIONALIDAD ===================================================
    // ===================================================================================

    public TrainingDetailAdapter(List<UserExercise> exercises, RecyclerView recyclerView, String dayKey) {
        Log.d(TAG, "TrainingDetailAdapter - Constructor: Inicializando adaptador con lista de ejercicios.");
        this.exercises = exercises;
        this.recyclerView = recyclerView;
        this.dayKey = dayKey;
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "TrainingDetailAdapter - onCreateViewHolder: Inflando vista de item_contenedor.");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contenedor, parent, false);
        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        UserExercise exercise = exercises.get(position);
        Log.d(TAG, "TrainingDetailAdapter - onBindViewHolder: Asignando datos al ejercicio en posición " + position + " → " + exercise.getName());
        holder.bind(exercise);

        holder.itemView.setOnClickListener(v -> {
            hasUnsavedChanges = true;
            Log.d(TAG, "TrainingDetailAdapter - onBindViewHolder: ⚠️ Se detectó un cambio en " + exercise.getName());
        });

        viewHolders.add(holder);
    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "TrainingDetailAdapter - getItemCount: Cantidad total de ejercicios: " + exercises.size());
        return exercises.size();
    }

    public boolean hasUnsavedChanges() {
        Log.d(TAG, "TrainingDetailAdapter - hasUnsavedChanges: Estado actual → " + hasUnsavedChanges);
        return hasUnsavedChanges;
    }

    public void setChangesSaved() {
        hasUnsavedChanges = false;
        Log.d(TAG, "TrainingDetailAdapter - setChangesSaved: Todos los cambios han sido marcados como guardados.");
    }

    public List<ExerciseSeries> getSeriesListForExercise(String exerciseName) {
        Log.d(TAG, "TrainingDetailAdapter - getSeriesListForExercise: Buscando series para el ejercicio " + exerciseName);
        for (ExerciseViewHolder holder : viewHolders) {
            if (holder.exerciseName.getText().toString().equals(exerciseName)) {
                Log.d(TAG, "TrainingDetailAdapter - getSeriesListForExercise: ✅ Encontrado: " + exerciseName);
                return holder.seriesAdapter.getSeriesList();
            }
        }
        Log.w(TAG, "TrainingDetailAdapter - getSeriesListForExercise: ⚠️ No se encontró el ejercicio: " + exerciseName);
        return null;
    }

    public void addExercise(UserExercise exercise) {
        // Verificar si ya existe un ejercicio con el mismo nombre
        for (UserExercise e : exercises) {
            if (e.getName().equalsIgnoreCase(exercise.getName())) {
                Log.w(TAG, "addExercise: ⚠️ El ejercicio '" + exercise.getName() + "' ya está en la lista. No se añadirá duplicado.");
                Toast.makeText(recyclerView.getContext(), "Este ejercicio ya está agregado.", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        this.exercises.add(exercise);
        notifyItemInserted(exercises.size() - 1);
        Log.d("TrainingDetailAdapter", "addExercise: Se añadió el ejercicio: " + exercise.getName());
    }

    // ===================================================================================
    // === INICIO MOD NUEVA FUNCIONALIDAD: Getter opcional para obtener "exercises"   ===
    // ===================================================================================
    /**
     * Retorna la lista completa de ejercicios que el adapter está mostrando
     * (ya sin los que se hayan eliminado localmente).
     */
    public List<UserExercise> getExercises() {
        return exercises;
    }
    // ===================================================================================
    // === FIN MOD NUEVA FUNCIONALIDAD ===================================================
    // ===================================================================================

    public int getVisibleSeriesCountForExercise(String exerciseName) {
        for (int i = 0; i < getItemCount(); i++) {
            UserExercise exercise = exercises.get(i);
            if (exercise.getName().equals(exerciseName)) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                if (viewHolder instanceof ExerciseViewHolder) {
                    ExerciseViewHolder exerciseViewHolder = (ExerciseViewHolder) viewHolder;
                    if (exerciseViewHolder.seriesAdapter != null) {
                        int count = exerciseViewHolder.seriesAdapter.getSeriesList().size();
                        Log.d(TAG, "TrainingDetailAdapter - getVisibleSeriesCountForExercise: Series visibles para '" + exerciseName + "' = " + count);
                        return count;
                    }
                }
            }
        }
        Log.w(TAG, "TrainingDetailAdapter - getVisibleSeriesCountForExercise: No se encontró el ejercicio: " + exerciseName);
        return 0;
    }

    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        RecyclerView recyclerViewSeries;
        Button btnAddSeries;
        SeriesAdapter seriesAdapter;
        List<ExerciseSeries> seriesList;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exerciseName);
            recyclerViewSeries = itemView.findViewById(R.id.recyclerViewSeries);
            btnAddSeries = itemView.findViewById(R.id.btnAddSeries);
            iconAction = itemView.findViewById(R.id.iconAction);
        }

        public void bind(UserExercise exercise) {
            exerciseName.setText(exercise.getName());

            seriesList = new ArrayList<>();
            for (int i = 0; i < exercise.getSeries(); i++) {
                seriesList.add(new ExerciseSeries(i + 1, exercise.getReps(), exercise.getWeight(), exercise.getRpe()));
            }

            seriesAdapter = new SeriesAdapter(seriesList);
            recyclerViewSeries.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerViewSeries.setAdapter(seriesAdapter);

            exerciseSeriesCountMap.put(exercise.getName(), seriesList.size());

            btnAddSeries.setOnClickListener(v -> {
                int newSeriesNumber = seriesList.size() + 1;
                seriesList.add(new ExerciseSeries(newSeriesNumber, 1, 0.0, 0.0));
                seriesAdapter.notifyItemInserted(seriesList.size() - 1);
                exerciseSeriesCountMap.put(exercise.getName(), seriesList.size());
            });

            iconAction.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(itemView.getContext(), iconAction);
                popup.inflate(R.menu.menu_exercise_actions);

                popup.setOnMenuItemClickListener(item -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        if (item.getItemId() == R.id.action_edit) {
                            Toast.makeText(itemView.getContext(), "Editar " + exercise.getName(), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "PopupMenu - Editar: " + exercise.getName());
                            // Aquí podrías reiniciar seriesSaved o implementar un diálogo de edición
                            return true;

                        } else if (item.getItemId() == R.id.action_delete) {
                            Log.d(TAG, "PopupMenu - Eliminar: " + exercise.getName());

                            new AlertDialog.Builder(itemView.getContext())
                                    .setTitle("Eliminar ejercicio")
                                    .setMessage("¿Estás seguro de que deseas eliminar " + exercise.getName() + "?")
                                    .setPositiveButton("Eliminar", (dialog, which) -> {
                                        // ==========================================================================
                                        // == INICIO MOD NUEVA FUNCIONALIDAD: marcar en "pendingDeletionExercises" ==
                                        // ==========================================================================
                                        UserExercise exerciseToDelete = exercises.get(position);
                                        // ==========================================================================
                                        // == FIN MOD NUEVA FUNCIONALIDAD ===========================================
                                        // ==========================================================================

                                        exercises.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, exercises.size());
                                        Log.d(TAG, "PopupMenu - Ejercicio eliminado localmente del RecyclerView: " + exercise.getName());

                                        // ==========================================================================
                                        // == INICIO MOD NUEVA FUNCIONALIDAD: almacenar en lista de pendientes    ==
                                        // ==========================================================================
                                        pendingDeletionExercises.add(exerciseToDelete);
                                        Log.d(TAG, "PopupMenu - Se agregó a la lista de pendientes: " + exerciseToDelete.getName());
                                        // ==========================================================================
                                        // == FIN MOD NUEVA FUNCIONALIDAD ===========================================
                                        // ==========================================================================
                                    })
                                    .setNegativeButton("Cancelar", null)
                                    .show();

                            return true;
                        }

                    }
                    return false;
                });

                popup.show();
            });

            exerciseName.setOnClickListener(v -> {
                Log.d(TAG, "ExerciseViewHolder - exerciseName Click: 📄 Solicitando detalles para " + exercise.getName());

                DatabaseReference dayExercisesRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("userExercises"); // ⚠️ Cambiado a userExercises global

                dayExercisesRef.orderByChild("name").equalTo(exercise.getName())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                                        String firebaseKey = exerciseSnapshot.getKey(); // Esta es la que quieres

                                        Log.d(TAG, "✅ Clave real de Firebase: " + firebaseKey);

                                        ExerciseDetailsDialogFragment dialog =
                                                ExerciseDetailsDialogFragment.newInstance(firebaseKey, dayKey); // ← Aquí

                                        FragmentActivity activity = (FragmentActivity) itemView.getContext();
                                        dialog.show(activity.getSupportFragmentManager(), "ExerciseDetailsDialog");
                                        break;
                                    }
                                } else {
                                    Log.e(TAG, "❌ No se encontró el ejercicio en /userExercises.");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "❌ Error al acceder a /userExercises", error.toException());
                            }
                        });
            });

        }
    }

    public Map<String, List<ExerciseSeries>> getAllCompletedSeriesGroupedByExercise() {
        Map<String, List<ExerciseSeries>> seriesMap = new HashMap<>();

        for (ExerciseViewHolder holder : viewHolders) {
            String name = holder.exerciseName.getText().toString();
            List<ExerciseSeries> completadas = new ArrayList<>();

            for (ExerciseSeries series : holder.seriesAdapter.getSeriesList()) {
                if (series.isCompleted()) {
                    completadas.add(series);
                }
            }

            seriesMap.put(name, completadas);
            Log.d(TAG, "getAllCompletedSeriesGroupedByExercise: " + name + " → " + completadas.size() + " completadas");
        }

        return seriesMap;
    }

}
