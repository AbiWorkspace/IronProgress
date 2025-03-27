package com.example.diets.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
    private boolean seriesSaved = false; // 🔐 Previene múltiples guardados

    public TrainingDetailAdapter(List<UserExercise> exercises, RecyclerView recyclerView) {
        Log.d(TAG, "TrainingDetailAdapter - Constructor: Inicializando adaptador con lista de ejercicios.");
        this.exercises = exercises;
        this.recyclerView = recyclerView;
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
        this.exercises.add(exercise);
        notifyItemInserted(exercises.size() - 1);
        Log.d("TrainingDetailAdapter", "addExercise: Se añadió el ejercicio: " + exercise.getName());
    }

    // 🔧 NUEVO MÉTODO actualizado como pediste
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

    public int getTotalSeriesForExercise(String exerciseName) {
        Integer count = exerciseSeriesCountMap.get(exerciseName);
        Log.d(TAG, "TrainingDetailAdapter - getTotalSeriesForExercise: " + exerciseName + " => " + (count != null ? count : 0));
        return count != null ? count : 0;
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


    class ExerciseViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        RecyclerView recyclerViewSeries;
        Button btnSaveAllSeries, btnAddSeries;
        SeriesAdapter seriesAdapter;
        List<ExerciseSeries> seriesList;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "ExerciseViewHolder - Constructor: Inicializando vistas del ViewHolder.");
            exerciseName = itemView.findViewById(R.id.exerciseName);
            recyclerViewSeries = itemView.findViewById(R.id.recyclerViewSeries);

            btnAddSeries = itemView.findViewById(R.id.btnAddSeries);
        }

        public void bind(UserExercise exercise) {
            Log.d(TAG, "ExerciseViewHolder - bind: Enlazando datos del ejercicio → " + exercise.getName());
            exerciseName.setText(exercise.getName());

            seriesList = new ArrayList<>();
            for (int i = 0; i < exercise.getSeries(); i++) {
                seriesList.add(new ExerciseSeries(i + 1, exercise.getReps(), exercise.getWeight(), exercise.getRpe()));
            }

            Log.d(TAG, "ExerciseViewHolder - bind: Inicializando SeriesAdapter con " + seriesList.size() + " series.");
            seriesAdapter = new SeriesAdapter(seriesList);
            recyclerViewSeries.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            recyclerViewSeries.setAdapter(seriesAdapter);

            // 🔄 Inicializar contador
            exerciseSeriesCountMap.put(exercise.getName(), seriesList.size());

            btnAddSeries.setOnClickListener(v -> {
                int newSeriesNumber = seriesList.size() + 1;
                seriesList.add(new ExerciseSeries(newSeriesNumber, 1, 0.0, 0.0));
                seriesAdapter.notifyItemInserted(seriesList.size() - 1);
                Log.d(TAG, "ExerciseViewHolder - btnAddSeries: ➕ Añadida nueva serie #" + newSeriesNumber);
                exerciseSeriesCountMap.put(exercise.getName(), seriesList.size());
            });





            exerciseName.setOnClickListener(v -> {
                Log.d(TAG, "ExerciseViewHolder - exerciseName Click: 📄 Solicitando detalles para " + exercise.getName());
                DatabaseReference userExercisesRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                        .child("userExercises");

                userExercisesRef.orderByChild("name").equalTo(exercise.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                                String exerciseKey = exerciseSnapshot.getKey();
                                Log.d(TAG, "ExerciseViewHolder - onDataChange: 🔍 Clave encontrada en Firebase → " + exerciseKey);
                                ExerciseDetailsDialogFragment dialog = ExerciseDetailsDialogFragment.newInstance(exerciseKey);
                                FragmentActivity activity = (FragmentActivity) itemView.getContext();
                                dialog.show(activity.getSupportFragmentManager(), "ExerciseDetailsDialog");
                                break;
                            }
                        } else {
                            Log.e(TAG, "ExerciseViewHolder - onDataChange: ❌ No se encontró el ejercicio en Firebase.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "ExerciseViewHolder - onCancelled: ❌ Error al acceder a Firebase.", error.toException());
                    }
                });
            });
        }
        private void showIncompleteSeriesDialog(UserExercise exercise, List<ExerciseSeries> updatedSeries) {
            AlertDialog.Builder builder = new AlertDialog.Builder(itemView.getContext());
            View dialogView = LayoutInflater.from(itemView.getContext()).inflate(R.layout.dialog_incomplete_series, null);

            builder.setView(dialogView);
            AlertDialog dialog = builder.create();

            Button btnCancelar = dialogView.findViewById(R.id.btnCancelar);
            Button btnGuardar = dialogView.findViewById(R.id.btnGuardarDeTodosModos);

            btnCancelar.setOnClickListener(v -> dialog.dismiss());

            btnGuardar.setOnClickListener(v -> {
                saveSelectedSeriesToDatabase(exercise, updatedSeries);
                seriesSaved = true; // ✅ Se marca como guardado desde aquí también
                dialog.dismiss();
            });

            dialog.show();
        }


        private void saveSelectedSeriesToDatabase(UserExercise exercise, List<ExerciseSeries> updatedSeries) {
            Log.d(TAG, "ExerciseViewHolder - saveSelectedSeriesToDatabase: Guardando series para " + exercise.getName());

            DatabaseReference userExercisesRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                    .child("userExercises");

            userExercisesRef.orderByChild("name").equalTo(exercise.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Log.d(TAG, "ExerciseViewHolder - saveSelectedSeriesToDatabase/onDataChange: Ejercicio existente. Actualizando series...");
                        for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                            String key = exerciseSnapshot.getKey();
                            DatabaseReference seriesRef = userExercisesRef.child(key).child("seriesData");

                            List<Map<String, Object>> allSeriesList = new ArrayList<>();
                            if (exerciseSnapshot.child("seriesData").exists()) {
                                for (DataSnapshot ds : exerciseSnapshot.child("seriesData").getChildren()) {
                                    allSeriesList.add((Map<String, Object>) ds.getValue());
                                }
                            }

                            for (ExerciseSeries series : updatedSeries) {
                                if (series.isCompleted()) {
                                    Map<String, Object> newSeriesData = new HashMap<>();
                                    newSeriesData.put("seriesNumber", allSeriesList.size() + 1);
                                    newSeriesData.put("reps", series.getReps());
                                    newSeriesData.put("weight", series.getWeight());
                                    newSeriesData.put("rpe", series.getRpe());
                                    allSeriesList.add(newSeriesData);
                                }
                            }

                            seriesRef.setValue(allSeriesList)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "✅ Series actualizadas correctamente en Firebase"))
                                    .addOnFailureListener(e -> Log.e(TAG, "❌ Error al guardar series", e));
                        }
                    } else {
                        Log.d(TAG, "ExerciseViewHolder - saveSelectedSeriesToDatabase: Ejercicio nuevo. Guardando en Firebase.");
                        String newKey = userExercisesRef.push().getKey();
                        if (newKey != null) {
                            DatabaseReference newRef = userExercisesRef.child(newKey);
                            Map<String, Object> newExerciseData = new HashMap<>();
                            newExerciseData.put("name", exercise.getName());
                            newExerciseData.put("series", exercise.getSeries());
                            newExerciseData.put("reps", exercise.getReps());
                            newExerciseData.put("weight", exercise.getWeight());
                            newExerciseData.put("rpe", exercise.getRpe());

                            newRef.setValue(newExerciseData).addOnSuccessListener(aVoid -> {
                                Log.d(TAG, "✅ Nuevo ejercicio guardado en Firebase.");
                                DatabaseReference newSeriesRef = newRef.child("seriesData");
                                List<Map<String, Object>> newSeriesList = new ArrayList<>();
                                for (ExerciseSeries series : updatedSeries) {
                                    if (series.isCompleted()) {
                                        Map<String, Object> data = new HashMap<>();
                                        data.put("seriesNumber", newSeriesList.size() + 1);
                                        data.put("reps", series.getReps());
                                        data.put("weight", series.getWeight());
                                        data.put("rpe", series.getRpe());
                                        newSeriesList.add(data);
                                    }
                                }
                                newSeriesRef.setValue(newSeriesList)
                                        .addOnSuccessListener(aVoid1 -> Log.d(TAG, "✅ Series nuevas guardadas correctamente."))
                                        .addOnFailureListener(e -> Log.e(TAG, "❌ Error al guardar nuevas series", e));
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "ExerciseViewHolder - saveSelectedSeriesToDatabase/onCancelled: ❌ Error de Firebase", error.toException());
                }
            });
        }

    }
}
