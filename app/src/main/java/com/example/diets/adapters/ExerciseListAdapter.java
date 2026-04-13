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

import java.util.ArrayList;
import java.util.List;

public class ExerciseListAdapter extends RecyclerView.Adapter<ExerciseListAdapter.ViewHolder> {
    private List<UserExercise> exercises;
    private static final String TAG = "ExerciseListAdapter";

    public ExerciseListAdapter(List<UserExercise> exercises) {
        this.exercises = new ArrayList<>(exercises); // 🔥 Asegurar que se crea una copia de la lista
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "🆕 Creando ViewHolder para ejercicio.");
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise_name, parent, false);
        return new ViewHolder(view);
    }

    public void updateExercises(List<UserExercise> newExercises) {
        Log.d(TAG, "🔄 Actualizando lista de ejercicios en adaptador.");
        this.exercises.clear();
        this.exercises.addAll(newExercises);

        Log.d(TAG, "📏 Total ejercicios en adaptador ahora: " + exercises.size());

        notifyDataSetChanged(); // 🔥 Notificar que los datos cambiaron
    }


    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserExercise exercise = exercises.get(position);
        Log.d(TAG, "🏋️‍♂️ Mostrando ejercicio: " + exercise.getName() + " en posición: " + position);
        holder.bind(exercise);
    }

    @Override
    public int getItemCount() {
        return exercises.size(); // 🔥 Asegurar que siempre devuelve la cantidad real de ejercicios
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
