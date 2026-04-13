package com.example.diets.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.diets.R;
import com.example.diets.model.UserExercise;

import java.util.List;

public class SimilarExercisesAdapter extends RecyclerView.Adapter<SimilarExercisesAdapter.ViewHolder> {
    private List<UserExercise> similarExercisesList;

    public SimilarExercisesAdapter(List<UserExercise> similarExercisesList) {
        this.similarExercisesList = similarExercisesList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_similar_exercise, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserExercise exercise = similarExercisesList.get(position);

        holder.textExerciseName.setText(exercise.getExerciseName());
        holder.textMusclesWorked.setText("Músculos: " + exercise.getMusclesWorked());

        // Cargar imagen con Glide
        Glide.with(holder.itemView.getContext())
                .load(exercise.getImage())
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_image_error)
                .into(holder.imageExercise);
    }

    @Override
    public int getItemCount() {
        return similarExercisesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageExercise;
        TextView textExerciseName, textMusclesWorked;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageExercise = itemView.findViewById(R.id.imageExercise);
            textExerciseName = itemView.findViewById(R.id.textExerciseName);
            textMusclesWorked = itemView.findViewById(R.id.textMusclesWorked);
        }
    }
}
