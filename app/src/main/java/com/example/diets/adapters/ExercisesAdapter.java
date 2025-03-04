package com.example.diets.adapters;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.model.Exercise;
import java.util.List;

public class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
    private final List<Exercise> exercises;
    private final List<String> selectedExercises;
    private final OnExerciseClickListener listener;
    private final OnExerciseSelectionChangedListener selectionChangedListener;

    public ExercisesAdapter(List<Exercise> exercises, List<String> selectedExercises, OnExerciseClickListener listener, OnExerciseSelectionChangedListener selectionChangedListener) {
        this.exercises = exercises;
        this.selectedExercises = selectedExercises;
        this.listener = listener;
        this.selectionChangedListener = selectionChangedListener;
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
            Log.d("ExercisesAdapter", "Checkbox clickeado: " + isChecked);
            if (isChecked) {
                if (!selectedExercises.contains(exercise.getName())) {
                    selectedExercises.add(exercise.getName());
                    Log.d("ExercisesAdapter", "Ejercicio seleccionado: " + exercise.getName());
                    selectionChangedListener.onExerciseSelected(exercise);
                }
            } else {
                selectedExercises.remove(exercise.getName());
                Log.d("ExercisesAdapter", "Ejercicio deseleccionado: " + exercise.getName());
                selectionChangedListener.onExerciseDeselected(exercise);
            }
            Log.d("ExercisesAdapter", "Lista de ejercicios seleccionados: " + selectedExercises);
        });

        holder.itemView.setOnClickListener(v -> {
            Log.d("ExercisesAdapter", "Contenedor clickeado");
            listener.onExerciseClick(exercise);
            Log.d("ExercisesAdapter", "Detalles del ejercicio mostrados: " + exercise.getName());
        });

        holder.checkBox.setOnTouchListener((v, event) -> {
            v.getParent().requestDisallowInterceptTouchEvent(true);
            return false;
        });

        holder.itemView.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                holder.checkBox.setPressed(false);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return exercises.size();
    }

    public interface OnExerciseClickListener {
        void onExerciseClick(Exercise exercise);
    }

    public interface OnExerciseSelectionChangedListener {
        void onExerciseSelected(Exercise exercise);
        void onExerciseDeselected(Exercise exercise);
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