package com.example.diets.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.example.diets.model.UserExercise;

public class ExerciseDetailsLoadFragment extends Fragment {
    private static final String TAG = "ExerciseDetailsLoadFragment";
    private UserExercise userExercise;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_details_load, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            userExercise = (UserExercise) data.getSerializable("userExercise");
            loadExerciseDetails(view);
        }
    }

    private void loadExerciseDetails(View view) {
        TextView exerciseNameView = view.findViewById(R.id.exerciseName);
        EditText loadEditText = view.findViewById(R.id.loadEditText);
        EditText repsEditText = view.findViewById(R.id.repsEditText);
        EditText familiarityEditText = view.findViewById(R.id.familiarityEditText);

        exerciseNameView.setText(userExercise.getName());
        loadEditText.setText(String.valueOf(userExercise.getCurrentLoad()));
        repsEditText.setText(String.valueOf(userExercise.getMaxReps()));
        familiarityEditText.setText(userExercise.isCompleted() ? "Familiarizado" : "No Familiarizado");

        // Aquí puedes agregar más lógica para editar y guardar los detalles del ejercicio
    }
}