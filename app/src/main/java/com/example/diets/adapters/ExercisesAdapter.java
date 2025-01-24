package com.example.diets.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.model.Exercise;
import java.util.List;

public class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
    private final List<Exercise> exercises;
    private final OnExerciseClickListener onExerciseClickListener;

    public ExercisesAdapter(List<Exercise> exercises, OnExerciseClickListener onExerciseClickListener) {
        this.exercises = exercises;
        this.onExerciseClickListener = onExerciseClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
        return new ViewHolder(view, onExerciseClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Exercise exercise = exercises.get(position);
        holder.exerciseName.setText(exercise.getName());
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView exerciseName;
        OnExerciseClickListener onExerciseClickListener;

        ViewHolder(View itemView, OnExerciseClickListener onExerciseClickListener) {
            super(itemView);
            exerciseName = itemView.findViewById(R.id.exerciseName);
            this.onExerciseClickListener = onExerciseClickListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onExerciseClickListener.onExerciseClick(getAdapterPosition());
        }
    }

    public interface OnExerciseClickListener {
        void onExerciseClick(int position);
    }
}