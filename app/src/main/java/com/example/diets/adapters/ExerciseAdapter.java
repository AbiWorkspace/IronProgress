package com.example.diets.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.fragments.TrainingDayDetailFragment;
import com.example.diets.model.UserExercise;
import com.example.diets.R;

import java.util.List;
public class ExerciseAdapter extends RecyclerView.Adapter<ExerciseAdapter.ViewHolder> {
    private List<UserExercise> exercises;
    private TrainingDayDetailFragment fragment;

    public ExerciseAdapter(List<UserExercise> exercises, TrainingDayDetailFragment fragment) {
        this.exercises = exercises;
        this.fragment = fragment;
        Log.d("ExerciseAdapter", "Constructor: Adapter creado con " + exercises.size() + " ejercicios");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d("ExerciseAdapter", "onCreateViewHolder: Inflando el layout del item");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserExercise exercise = exercises.get(position);
        Log.d("ExerciseAdapter", "onBindViewHolder: Vinculando ejercicio en la posición " + position + ": " + exercise);
        holder.bind(exercise, fragment);
    }

    @Override
    public int getItemCount() {
        Log.d("ExerciseAdapter", "getItemCount: Número de ejercicios: " + exercises.size());
        return exercises.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView exerciseNameTextView;
        private EditText weightEditText;
        private EditText repsEditText;
        private EditText rpeEditText;
        private Button saveButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseNameTextView = itemView.findViewById(R.id.exerciseNameTextView);
            weightEditText = itemView.findViewById(R.id.weightEditText);
            repsEditText = itemView.findViewById(R.id.repsEditText);
            rpeEditText = itemView.findViewById(R.id.rpeEditText);
            saveButton = itemView.findViewById(R.id.saveButton);
        }

        public void bind(UserExercise exercise, TrainingDayDetailFragment fragment) {
            exerciseNameTextView.setText(exercise.getName());
            weightEditText.setText(String.valueOf(exercise.getCurrentLoad()));
            repsEditText.setText(String.valueOf(exercise.getMaxReps()));
            rpeEditText.setText(exercise.getFamiliarity());
            Log.d("ExerciseAdapter", "bind: Ejercicio vinculado: " + exercise);

            saveButton.setOnClickListener(v -> {
                // Actualizar los valores del ejercicio
                exercise.setCurrentLoad(Integer.parseInt(weightEditText.getText().toString()));
                exercise.setMaxReps(Integer.parseInt(repsEditText.getText().toString()));
                exercise.setFamiliarity(rpeEditText.getText().toString());

                // Guardar el progreso del usuario
                fragment.saveUserProgress(exercise);
                Log.d("ExerciseAdapter", "bind: Progreso guardado para el ejercicio: " + exercise);
            });
        }
    }
}