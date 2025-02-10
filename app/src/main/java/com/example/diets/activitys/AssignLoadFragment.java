package com.example.diets.activitys;

import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.example.diets.model.Exercise;
import com.example.diets.model.TrainingDay;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class AssignLoadFragment extends Fragment {
    private static final String TAG = "AssignLoadFragment";
    private List<TrainingDay> trainingDays;
    private String userGoal;
    private LinearLayout containerLayout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assign_load, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        containerLayout = view.findViewById(R.id.containerLayout);

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            trainingDays = (List<TrainingDay>) data.getSerializable("trainingDays");
            userGoal = data.getString("userGoal");
            listExercises();
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            // Lógica para guardar las cargas asignadas
            saveAssignedLoads();
        });
    }

    private void listExercises() {
        if (containerLayout == null) {
            Log.e(TAG, "containerLayout is null");
            return;
        }

        Set<Exercise> uniqueExercises = new HashSet<>();
        for (TrainingDay trainingDay : trainingDays) {
            uniqueExercises.addAll(trainingDay.getExercises());
        }

        for (Exercise exercise : uniqueExercises) {
            View exerciseView = LayoutInflater.from(getContext()).inflate(R.layout.exercise_item_load, containerLayout, false);

            TextView exerciseNameView = exerciseView.findViewById(R.id.exerciseName);
            exerciseNameView.setText(exercise.getName());

            ImageButton statusButton = exerciseView.findViewById(R.id.statusButton);
            // Configurar el icono inicial del botón según el estado del ejercicio
            updateStatusButtonIcon(statusButton, exercise);

            statusButton.setOnClickListener(v -> {
                // Cambiar el estado del ejercicio y actualizar el icono
                toggleExerciseStatus(exercise);
                updateStatusButtonIcon(statusButton, exercise);
            });

            containerLayout.addView(exerciseView);
        }
    }

    private void updateStatusButtonIcon(ImageButton button, Exercise exercise) {
        // Cambiar el icono del botón según el estado del ejercicio
        if (exercise.isCompleted()) {
            button.setImageResource(R.drawable.ic_completed_status);
        } else {
            button.setImageResource(R.drawable.ic_default_status);
        }
    }

    private void toggleExerciseStatus(Exercise exercise) {
        // Cambiar el estado del ejercicio
        exercise.setCompleted(!exercise.isCompleted());
    }

    private void saveAssignedLoads() {
        // Lógica para guardar las cargas asignadas
        Toast.makeText(getContext(), "Cargas asignadas guardadas exitosamente", Toast.LENGTH_SHORT).show();
    }
}