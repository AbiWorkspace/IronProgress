package com.example.diets.fragments;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.Exercise;

import java.util.ArrayList;
import java.util.List;

public class CreatedExercisesFragment extends Fragment {
    private RecyclerView recyclerView;
    private ExercisesAdapter adapter;
    private List<Exercise> exercises = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_created_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view);
        Button addExerciseButton = view.findViewById(R.id.add_exercise_button);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ExercisesAdapter(exercises);
            recyclerView.setAdapter(adapter);
        } else {
            Log.e("CreatedExercisesFragment", "RecyclerView is null");
        }

        addExerciseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addNewExercise();
            }
        });
    }

    private void addNewExercise() {
        // Aquí puedes agregar la lógica para agregar un nuevo ejercicio
        // Por ejemplo, mostrar un diálogo para ingresar los detalles del ejercicio
        Exercise newExercise = new Exercise();
        newExercise.setName("Nuevo Ejercicio");
        exercises.add(newExercise);
        adapter.notifyItemInserted(exercises.size() - 1);
    }

    private static class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
        private final List<Exercise> exercises;

        ExercisesAdapter(List<Exercise> exercises) {
            this.exercises = exercises;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d(TAG, "onCreateViewHolder: Creating view holder");
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Exercise exercise = exercises.get(position);
            holder.exerciseName.setText(exercise.getName());
            Log.d(TAG, "onBindViewHolder: Binding view holder for exercise: " + exercise.getName());
        }

        @Override
        public int getItemCount() {
            return exercises.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView exerciseName;

            ViewHolder(View itemView) {
                super(itemView);
                exerciseName = itemView.findViewById(R.id.exerciseName);
                Log.d(TAG, "ViewHolder: ViewHolder initialized");
            }
        }
    }
}