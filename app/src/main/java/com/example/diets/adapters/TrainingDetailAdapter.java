package com.example.diets.adapters;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.UserExercise;

import java.util.ArrayList;
import java.util.List;

public class TrainingDetailAdapter extends RecyclerView.Adapter<TrainingDetailAdapter.ExerciseViewHolder> {
    private static final String TAG = "TrainingDetailAdapter"; // 🔥 Nombre de la clase para logs
    private List<UserExercise> exercises;

    public TrainingDetailAdapter(List<UserExercise> exercises) {
        Log.d(TAG, "🟢 Constructor → Recibiendo lista de ejercicios.");
        this.exercises = exercises;
        Log.d(TAG, "✅ Se recibieron " + exercises.size() + " ejercicios.");
    }

    @NonNull
    @Override
    public ExerciseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "🟢 onCreateViewHolder() → Inflando layout para cada ejercicio.");

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contenedor, parent, false);

        if (view == null) {
            Log.e(TAG, "❌ ERROR: No se pudo inflar item_contenedor.xml. Verifica el archivo XML.");
        } else {
            Log.d(TAG, "✅ Layout inflado correctamente.");
        }

        return new ExerciseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExerciseViewHolder holder, int position) {
        Log.d(TAG, "🟢 onBindViewHolder() → Vinculando vista en posición " + position);

        UserExercise exercise = exercises.get(position);
        Log.d(TAG, "📌 Cargando ejercicio: " + exercise.getName());

        holder.bind(exercise);
        Log.d(TAG, "✅ Ejercicio vinculado correctamente.");
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ExerciseViewHolder extends RecyclerView.ViewHolder {
        private static final String TAG = "ExerciseViewHolder"; // 🔥 Nombre de la clase
        TextView exerciseName;
        RecyclerView recyclerViewSeries;
        SeriesAdapter seriesAdapter;

        public ExerciseViewHolder(@NonNull View itemView) {
            super(itemView);
            Log.d(TAG, "🟢 Constructor → Inicializando vistas.");

            exerciseName = itemView.findViewById(R.id.exerciseName);
            recyclerViewSeries = itemView.findViewById(R.id.recyclerViewSeries);

            if (exerciseName == null || recyclerViewSeries == null) {
                Log.e(TAG, "❌ ERROR: Alguna vista es NULL. Verifica los IDs en item_contenedor.xml.");
            } else {
                recyclerViewSeries.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                Log.d(TAG, "✅ recyclerViewSeries inicializado con LinearLayoutManager.");
            }
        }

        public void bind(UserExercise exercise) {
            Log.d(TAG, "🟢 bind() → Vinculando ejercicio: " + exercise.getName());
            exerciseName.setText(exercise.getName());

            // 🔥 Crear lista de series para este ejercicio
            List<ExerciseSeries> seriesList = new ArrayList<>();
            for (int i = 0; i < exercise.getSeries(); i++) {
                seriesList.add(new ExerciseSeries(
                        i + 1,                         // Número de serie
                        exercise.getReps(),            // Repeticiones
                        exercise.getWeight(),          // Peso (double)
                        exercise.getRpe()              // RPE (double)
                ));
                Log.d(TAG, "📌 Serie " + (i + 1) + " → Reps: " + exercise.getReps() + ", Peso: " + exercise.getWeight() + ", RPE: " + exercise.getRpe());
            }

            // 🔥 Configurar adaptador de series
            if (recyclerViewSeries != null) {
                seriesAdapter = new SeriesAdapter(seriesList);
                recyclerViewSeries.setAdapter(seriesAdapter);
                Log.d(TAG, "✅ SeriesAdapter configurado con " + seriesList.size() + " series.");
            } else {
                Log.e(TAG, "❌ No se pudo configurar el adaptador porque recyclerViewSeries es NULL.");
            }
        }
    }
}
