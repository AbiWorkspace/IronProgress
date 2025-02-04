package com.example.diets.adapters;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.fragments.ExerciseDetailsFragment;
import com.example.diets.fragments.SelectExercisesFragment;
import com.example.diets.model.Exercise;
import java.util.List;

public class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
    private final List<Exercise> exercises;
    private final List<String> selectedExercises;
    private final OnExerciseClickListener listener;

    public ExercisesAdapter(List<Exercise> exercises, List<String> selectedExercises, OnExerciseClickListener listener) {
        this.exercises = exercises;
        this.selectedExercises = selectedExercises;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.exerciseName.setText(exercise.getName());
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(selectedExercises.contains(exercise.getName()));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedExercises.contains(exercise.getName())) {
                    selectedExercises.add(exercise.getName());
                    Log.d("ExercisesAdapter", "Ejercicio seleccionado: " + exercise.getName());
                }
            } else {
                selectedExercises.remove(exercise.getName());
                Log.d("ExercisesAdapter", "Ejercicio deseleccionado: " + exercise.getName());
            }
            listener.onExerciseClick(exercise);
        });

        // Añadir OnClickListener para ver los detalles del ejercicio
        holder.itemView.setOnClickListener(v -> {
            listener.onExerciseClick(exercise);
        });
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView exerciseName;
        CheckBox checkBox;

        ViewHolder(View itemView) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exerciseName);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}