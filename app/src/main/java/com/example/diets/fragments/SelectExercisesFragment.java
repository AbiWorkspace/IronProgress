package com.example.diets.fragments;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseDistribution;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.example.diets.model.TrainingDay;

import java.util.ArrayList;
import java.util.List;
public class SelectExercisesFragment extends Fragment {
    private static final String TAG = "SelectExercisesFragment";
    private List<String> selectedExercises = new ArrayList<>();
    private LinearLayout containerLayout;
    private DatabaseReference mDatabase;
    private List<TrainingDay> trainingDays;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        containerLayout = view.findViewById(R.id.containerLayout);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            String selectedDistribution = data.getString("selectedDistribution");
            trainingDays = (List<TrainingDay>) data.getSerializable("trainingDays");
            createExerciseContainers(trainingDays);
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Ejercicios seleccionados: " + selectedExercises);
            // Aquí puedes agregar la lógica para guardar los ejercicios seleccionados
        });
    }

    private void createExerciseContainers(List<TrainingDay> trainingDays) {
        for (TrainingDay trainingDay : trainingDays) {
            LinearLayout dayContainer = new LinearLayout(getContext());
            dayContainer.setOrientation(LinearLayout.VERTICAL);
            dayContainer.setPadding(16, 16, 16, 16);
            dayContainer.setBackgroundColor(Color.parseColor("#2F2F2F"));

            // Agregar margen inferior y borde para separar los contenedores
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            dayContainer.setLayoutParams(params);
            dayContainer.setBackgroundResource(R.drawable.container_background);

            TextView dayTitle = new TextView(getContext());
            dayTitle.setText(trainingDay.getDayName());
            dayTitle.setTextColor(Color.WHITE);
            dayTitle.setTextSize(20);
            dayTitle.setTypeface(null, Typeface.BOLD);
            dayContainer.addView(dayTitle);

            RecyclerView recyclerView = new RecyclerView(getContext());
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            loadExercises(recyclerView, trainingDay.getMovementPatterns());
            dayContainer.addView(recyclerView);

            Button addButton = new Button(getContext());
            addButton.setText("Agregar Ejercicios");
            addButton.setTextColor(Color.WHITE);
            addButton.setBackgroundColor(Color.parseColor("#E63946"));
            addButton.setOnClickListener(v -> {
                // Lógica para agregar ejercicios
                showExerciseSelectionDialog(trainingDay);
            });
            dayContainer.addView(addButton);

            containerLayout.addView(dayContainer);
        }
    }

    private void showExerciseSelectionDialog(TrainingDay trainingDay) {
        ExerciseSelectionDialogFragment dialogFragment = new ExerciseSelectionDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable("trainingDay", trainingDay);
        dialogFragment.setArguments(args);
        dialogFragment.show(getFragmentManager(), "ExerciseSelectionDialog");
    }

    private void loadExercises(RecyclerView recyclerView, List<String> movementPatterns) {
        DatabaseReference exercisesRef = mDatabase.child("exercises");
        exercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Exercise> exercises = new ArrayList<>();
                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    Exercise exercise = exerciseSnapshot.getValue(Exercise.class);
                    if (exercise != null && movementPatterns.contains(exercise.getMovementType())) {
                        for (String pattern : movementPatterns) {
                            MovementPattern movementPattern = getMovementPatternByName(pattern);
                            if (movementPattern != null && exercise.getSynergistMuscles().containsAll(movementPattern.getSynergistMuscles())) {
                                exercises.add(exercise);
                                break;
                            }
                        }
                    }
                }
                ExercisesAdapter adapter = new ExercisesAdapter(exercises, selectedExercises);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "Error de base de datos: " + error.getMessage());
            }
        });
    }

    private MovementPattern getMovementPatternByName(String name) {
        // Aquí debes implementar la lógica para obtener el patrón de movimiento por su nombre.
        // Esto puede ser desde una base de datos, una lista en memoria, etc.
        return null; // Implementa esta lógica según tu necesidad
    }

    private static class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
        private final List<Exercise> exercises;
        private final List<String> selectedExercises;

        ExercisesAdapter(List<Exercise> exercises, List<String> selectedExercises) {
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
            Exercise exercise = exercises.get(position);
            holder.exerciseName.setText(exercise.getName());
            holder.checkBox.setOnCheckedChangeListener(null);
            holder.checkBox.setChecked(selectedExercises.contains(exercise.getName()));
            holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    selectedExercises.add(exercise.getName());
                } else {
                    selectedExercises.remove(exercise.getName());
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