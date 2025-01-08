package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SelectExercisesFragment extends Fragment {
    private static final String TAG = "SelectExercisesFragment";
    private List<String> selectedExercises = new ArrayList<>();
    private RecyclerView recyclerView;
    private ExercisesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Simulación de datos de ejercicios
        List<String> exercises = Arrays.asList("Push-up", "Squat", "Lunge", "Plank", "Burpee");
        adapter = new ExercisesAdapter(exercises, selectedExercises);
        recyclerView.setAdapter(adapter);

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Ejercicios seleccionados: " + selectedExercises);
            // Aquí puedes agregar la lógica para guardar los ejercicios seleccionados
        });
    }

    private static class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
        private final List<String> exercises;
        private final List<String> selectedExercises;

        ExercisesAdapter(List<String> exercises, List<String> selectedExercises) {
            this.exercises = exercises;
            this.selectedExercises = selectedExercises;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String exercise = exercises.get(position);
            holder.exerciseName.setText(exercise);
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedExercises.contains(exercise));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedExercises.add(exercise);
                } else {
                    selectedExercises.remove(exercise);
                }
            });
        }

        @Override
        public int getItemCount() {
            return exercises.size();
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
}