package com.example.diets.adapters;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.model.UserExercise;
import java.util.List;

public class ExerciseListAdapter extends RecyclerView.Adapter<ExerciseListAdapter.ViewHolder> {
    private List<UserExercise> exercises;
    private static final String TAG = "ExerciseListAdapter";

    public ExerciseListAdapter(List<UserExercise> exercises) {
        this.exercises = exercises;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "🆕 Creando ViewHolder para ejercicio.");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_name, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserExercise exercise = exercises.get(position);
        holder.bind(exercise);
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView exerciseNameTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            exerciseNameTextView = itemView.findViewById(R.id.exerciseNameTextView);
        }

        public void bind(UserExercise exercise) {
            Log.d(TAG, "🏋️‍♂️ Mostrando ejercicio: " + exercise.getName());
            exerciseNameTextView.setText(exercise.getName());
        }
    }
}
