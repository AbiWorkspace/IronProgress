package com.example.diets.adapters;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;

import java.util.List;
public class TrainingDayAdapter extends RecyclerView.Adapter<TrainingDayAdapter.ViewHolder> {
    private List<TrainingDay> trainingDays;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(TrainingDay trainingDay);
    }

    public TrainingDayAdapter(List<TrainingDay> trainingDays, OnItemClickListener listener) {
        this.trainingDays = trainingDays;
        this.listener = listener;
        Log.d("TrainingDayAdapter", "Constructor: Adapter creado con " + trainingDays.size() + " días de entrenamiento");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("TrainingDayAdapter", "onCreateViewHolder: Inflando el layout del item");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_training_day2, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrainingDay trainingDay = trainingDays.get(position);
        Log.d("TrainingDayAdapter", "onBindViewHolder: Vinculando día de entrenamiento en la posición " + position + ": " + trainingDay);
        holder.bind(trainingDay, listener);
    }

    @Override
    public int getItemCount() {
        Log.d("TrainingDayAdapter", "getItemCount: Número de días de entrenamiento: " + trainingDays.size());
        return trainingDays.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView dayIndexTextView;
        private LinearLayout exercisesLayout;
        private Button startWorkoutButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            dayIndexTextView = itemView.findViewById(R.id.dayIndexTextView);
            exercisesLayout = itemView.findViewById(R.id.exercisesLayout);
            startWorkoutButton = itemView.findViewById(R.id.startWorkoutButton);
            Log.d("TrainingDayAdapter", "ViewHolder: ViewHolder creado");
        }

        public void bind(final TrainingDay trainingDay, final OnItemClickListener listener) {
            dayIndexTextView.setText("Día " + trainingDay.getDayIndex());
            Log.d("TrainingDayAdapter", "bind: Día de entrenamiento vinculado: " + trainingDay);

            // Limpiar el layout de ejercicios antes de añadir nuevos
            exercisesLayout.removeAllViews();

            // Añadir los ejercicios al layout
            for (MovementPattern pattern : trainingDay.getMovementPatterns()) {
                for (Exercise exercise : pattern.getExercises()) {
                    TextView exerciseTextView = new TextView(itemView.getContext());
                    exerciseTextView.setText(exercise.getName());
                    exerciseTextView.setTextColor(Color.WHITE);
                    exerciseTextView.setTextSize(16);
                    exercisesLayout.addView(exerciseTextView);
                }
            }

            itemView.setOnClickListener(v -> {
                Log.d("TrainingDayAdapter", "bind: Día de entrenamiento clicado: " + trainingDay);
                listener.onItemClick(trainingDay);
            });

            startWorkoutButton.setOnClickListener(v -> {
                Log.d("TrainingDayAdapter", "bind: Botón 'Empezar' clicado: " + trainingDay);
                listener.onItemClick(trainingDay);
            });
        }
    }
}