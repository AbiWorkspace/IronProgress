package com.example.diets.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

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
    ImageView exerciseImage;

    // ===================================================================================
    // === INICIO: LISTA AUXILIAR PARA "pendientes de eliminar" ===
    // ===================================================================================
    private final List<UserExercise> pendingDeletionExercises = new ArrayList<>();

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

        // Marcamos "cambios sin guardar" si hacen click
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

    public List<UserExercise> getExercises() {
        return exercises;
    }

    public int getVisibleSeriesCountForExercise(String exerciseName) {
        for (int i = 0; i < getItemCount(); i++) {
            UserExercise exercise = exercises.get(i);
            if (exercise.getName().equals(exerciseName)) {
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(i);
                if (viewHolder instanceof ExerciseViewHolder) {
                    ExerciseViewHolder exerciseViewHolder = (ExerciseViewHolder) viewHolder;
                    if (exerciseViewHolder.seriesAdapter != null) {
                        int count = exerciseViewHolder.seriesAdapter.getSeriesList().size();
                        Log.d(TAG, "TrainingDetailAdapter - getVisibleSeriesCountForExercise: Series visibles para '"
                                + exerciseName + "' = " + count);
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
            exerciseImage = itemView.findViewById(R.id.exerciseImage);
        }

        public void bind(UserExercise exercise) {
            Log.d(TAG, "bind: 🚀 Iniciando bind() para el ejercicio: " + exercise.getName());
            exerciseName.setText(exercise.getName());

            // ======================================
            //  1) CREAR Y CONFIGURAR LAS SERIES
            // ======================================
            seriesList = new ArrayList<>();
            Log.d(TAG, "bind: 🔍 Cantidad de series solicitadas → " + exercise.getSeries());

            for (int i = 0; i < exercise.getSeries(); i++) {
                int seriesNumber = i + 1;
                int reps = exercise.getReps() > 0 ? exercise.getReps() : 8;
                double rpe = exercise.getRpe() > 0 ? exercise.getRpe() : 7.0;
                double weight = exercise.getLastLoad(); // 🔥 "currentLoad" o última carga usada

                Log.d(TAG, "bind: 📐 Parámetros para serie " + seriesNumber
                        + " → Reps=" + reps + ", RPE=" + rpe + ", Peso=" + weight);

                ExerciseSeries serie = new ExerciseSeries(seriesNumber, reps, weight, rpe);

                // 🔥 Asignamos la carga previa
                serie.setPreviousLoad(weight);
                Log.d(TAG, "bind: 💾 Carga previa asignada a serie " + seriesNumber + " → " + weight);

                seriesList.add(serie);
            }
            Log.d(TAG, "bind: ✅ Final → se han creado " + seriesList.size() + " series para " + exercise.getName());

            // ======================================
            //  2) CONFIGURAR LA IMAGEN
            // ======================================
            String base64Image = exercise.getImage();
            if (base64Image != null && !base64Image.trim().isEmpty()) {
                try {
                    base64Image = base64Image.replaceAll("\\s", "");
                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    if (bitmap != null) {
                        exerciseImage.setImageBitmap(bitmap);
                    } else {
                        Log.e(TAG, "bind: ❌ Error: Bitmap decodificado es null.");
                        exerciseImage.setImageResource(R.drawable.ic_placeholder);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "bind: ❌ Error al decodificar base64: ", e);
                    exerciseImage.setImageResource(R.drawable.ic_placeholder);
                }
            } else {
                Log.w(TAG, "bind: ⚠️ Imagen base64 ausente para " + exercise.getName());
                exerciseImage.setImageResource(R.drawable.ic_placeholder);
            }

            // ======================================
            //  3) SETEAR ADAPTER DE SERIES
            // ======================================
            seriesAdapter = new SeriesAdapter(seriesList);
            recyclerViewSeries.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerViewSeries.setAdapter(seriesAdapter);

            exerciseSeriesCountMap.put(exercise.getName(), seriesList.size());

            // ======================================
            //  4) BOTÓN AGREGAR NUEVA SERIE
            // ======================================
            btnAddSeries.setOnClickListener(v -> {
                int newSeriesNumber = seriesList.size() + 1;
                seriesList.add(new ExerciseSeries(newSeriesNumber, 1, 0.0, 0.0));
                seriesAdapter.notifyItemInserted(seriesList.size() - 1);
                exerciseSeriesCountMap.put(exercise.getName(), seriesList.size());
                Log.d(TAG, "bind: ➕ Nueva serie agregada a " + exercise.getName() + " (Total: " + seriesList.size() + ")");
            });

            // ======================================
            //  5) ICONO DE ACCIONES (Editar / Eliminar)
            // ======================================
            iconAction.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(itemView.getContext(), iconAction);
                popup.inflate(R.menu.menu_exercise_actions);

                popup.setOnMenuItemClickListener(item -> {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        if (item.getItemId() == R.id.action_edit) {
                            Toast.makeText(itemView.getContext(), "Editar " + exercise.getName(), Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "PopupMenu - Editar: " + exercise.getName());
                            return true;

                        } else if (item.getItemId() == R.id.action_delete) {
                            new AlertDialog.Builder(itemView.getContext())
                                    .setTitle("Eliminar ejercicio")
                                    .setMessage("¿Estás seguro de que deseas eliminar " + exercise.getName() + "?")
                                    .setPositiveButton("Eliminar", (dialog, which) -> {
                                        UserExercise exerciseToDelete = exercises.get(position);
                                        exercises.remove(position);
                                        notifyItemRemoved(position);
                                        notifyItemRangeChanged(position, exercises.size());
                                        Log.d(TAG, "PopupMenu - Ejercicio eliminado del RecyclerView: " + exercise.getName());

                                        pendingDeletionExercises.add(exerciseToDelete);
                                        Log.d(TAG, "PopupMenu - Agregado a la lista de pendientes de eliminar: " + exerciseToDelete.getName());
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

            // ======================================
            //  6) CLICK EN EL NOMBRE DEL EJERCICIO
            //     -> MUESTRA DETALLES (DIALOG)
            // ======================================
            exerciseName.setOnClickListener(v -> {
                Log.d(TAG, "ExerciseViewHolder - exerciseName Click: 📄 Solicitando detalles para " + exercise.getName());

                DatabaseReference dayExercisesRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("userExercises");

                dayExercisesRef.orderByChild("name").equalTo(exercise.getName())
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                                        String firebaseKey = exerciseSnapshot.getKey();
                                        Log.d(TAG, "✅ Clave de Firebase encontrada: " + firebaseKey);

                                        ExerciseDetailsDialogFragment dialog =
                                                ExerciseDetailsDialogFragment.newInstance(firebaseKey, dayKey);

                                        FragmentActivity activity = (FragmentActivity) itemView.getContext();
                                        dialog.show(activity.getSupportFragmentManager(), "ExerciseDetailsDialog");
                                        break;
                                    }
                                } else {
                                    Log.e(TAG, "❌ No se encontró el ejercicio en Firebase (/userExercises)");
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e(TAG, "❌ Error en Firebase al consultar /userExercises", error.toException());
                            }
                        });
            });

            Log.d(TAG, "bind: ✅ Finalización de bind() para " + exercise.getName());
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
