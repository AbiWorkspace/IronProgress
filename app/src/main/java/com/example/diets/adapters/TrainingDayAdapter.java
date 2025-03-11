package com.example.diets.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;

import java.util.ArrayList;
import java.util.List;
public class TrainingDayAdapter extends RecyclerView.Adapter<TrainingDayAdapter.ViewHolder> {
    private List<TrainingDay> trainingDays;
    private OnItemClickListener listener;
    private static final String TAG = "TrainingDayAdapter";

    public interface OnItemClickListener {
        void onItemClick(TrainingDay trainingDay);
    }

    public TrainingDayAdapter(List<TrainingDay> trainingDays, OnItemClickListener listener) {
        this.trainingDays = trainingDays;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "🆕 Creando ViewHolder para día de entrenamiento.");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training_day2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrainingDay trainingDay = trainingDays.get(position);
        holder.bind(trainingDay, listener);
    }
    @Override
    public int getItemCount() {
        Log.d(TAG, "📏 Total de días en Adapter: " + trainingDays.size());  // 👀 Agrega esto
        return trainingDays.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView dayIndexTextView, dayTitleTextView;
        private RecyclerView exercisesRecyclerView;
        private Button startWorkoutButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayIndexTextView = itemView.findViewById(R.id.dayIndexTextView);
            dayTitleTextView = itemView.findViewById(R.id.dayTitleTextView);
            exercisesRecyclerView = itemView.findViewById(R.id.exercisesRecyclerView);
            startWorkoutButton = itemView.findViewById(R.id.startWorkoutButton);
        }

        public void bind(final TrainingDay trainingDay, final OnItemClickListener listener) {
            Log.d(TAG, "📅 Vinculando datos de: " + trainingDay.getDayTitle());

            // 🔹 Asegurar que la lista de ejercicios no sea NULL
            if (trainingDay.getExercises() == null) {
                Log.w(TAG, "⚠️ La lista de ejercicios es NULL. Se inicializa vacía.");
                trainingDay.setExercises(new ArrayList<>());
            }

            Log.d(TAG, "✅ Total ejercicios: " + trainingDay.getExercises().size());
            for (UserExercise exercise : trainingDay.getExercises()) {
                Log.d(TAG, "🏋️‍♂️ Ejercicio: " + exercise.getName());
            }

            // 🔥 Mostrar el título correcto del día
            String originalTitle = trainingDay.getDayTitle();
            if (originalTitle == null || originalTitle.isEmpty()) {
                originalTitle = "Día " + trainingDay.getDayIndex();
            }

            dayIndexTextView.setText(originalTitle);
            dayTitleTextView.setText(trainingDay.getDayTitle());

            // 🔥 Configurar el RecyclerView interno con diseño HORIZONTAL para ejercicios
            exercisesRecyclerView.setLayoutManager(new LinearLayoutManager(itemView.getContext(), LinearLayoutManager.HORIZONTAL, false));

            // Asegurar que los ejercicios tengan un adaptador y actualizarlo
            ExerciseListAdapter exerciseAdapter = new ExerciseListAdapter(trainingDay.getExercises());
            exercisesRecyclerView.setAdapter(exerciseAdapter);
            exerciseAdapter.notifyDataSetChanged();

            // Acción al hacer clic en el botón
            startWorkoutButton.setOnClickListener(v -> listener.onItemClick(trainingDay));
        }
    }
}