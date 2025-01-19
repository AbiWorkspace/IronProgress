package com.example.diets.fragments;

import static androidx.constraintlayout.helper.widget.MotionEffect.TAG;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseDistribution;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.Muscle;
import com.example.diets.model.TrainingDay;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
public class ExerciseSelectionDialogFragment extends DialogFragment {
    private static final String TAG = "ExerciseSelectionDialogFragment";
    private RecyclerView recyclerView;
    private Button doneButton;
    private BottomNavigationView bottomNavigationView;
    private List<Exercise> exercises = new ArrayList<>();
    private List<String> selectedExercises = new ArrayList<>();
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflating layout");
        return inflater.inflate(R.layout.fragment_exercise_selection_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        doneButton = view.findViewById(R.id.doneButton);
        bottomNavigationView = view.findViewById(R.id.bottom_navigation);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            Log.d(TAG, "onViewCreated: RecyclerView inicializado");
        } else {
            Log.e(TAG, "onViewCreated: RecyclerView es nulo");
        }

        Bundle args = getArguments();
        if (args != null) {
            TrainingDay trainingDay = (TrainingDay) args.getSerializable("trainingDay");
            String userGoal = args.getString("userGoal");
            if (trainingDay != null) {
                Log.d(TAG, "onViewCreated: TrainingDay y userGoal recibidos: " + trainingDay + ", " + userGoal);
                loadExercises(trainingDay.getMovementPatterns());
            } else {
                Log.d(TAG, "onViewCreated: TrainingDay o userGoal es nulo");
            }
        } else {
            Log.d(TAG, "onViewCreated: No se recibieron argumentos");
        }

        doneButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated: Botón doneButton presionado");
            // Lógica para manejar los ejercicios seleccionados
            dismiss();
        });

        Log.d(TAG, "onViewCreated: Método onViewCreated finalizado");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: View destroyed");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach: Fragment detached");
    }

    private void loadExercises(List<MovementPattern> movementPatterns) {
        DatabaseReference exercisesRef = mDatabase.child("exercises");
        exercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                exercises.clear();
                Log.d(TAG, "loadExercises: Número de ejercicios recuperados: " + snapshot.getChildrenCount());
                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    Exercise exercise = exerciseSnapshot.getValue(Exercise.class);
                    if (exercise != null) {
                        Log.d(TAG, "loadExercises: Ejercicio recuperado: " + exercise.getName());
                        Log.d(TAG, "loadExercises: Musculos: " + exercise.getMusclesWorked());

                        // Dividir los músculos trabajados en una lista y normalizar los nombres
                        List<String> musclesWorkedList = Arrays.stream(exercise.getMusclesWorked().split(",\\s*"))
                                .map(String::toLowerCase)
                                .collect(Collectors.toList());

                        for (MovementPattern pattern : movementPatterns) {
                            Log.d(TAG, "loadExercises: Revisando patrón de movimiento: " + pattern.getName());
                            if (pattern.getSynergistMuscles() != null && !musclesWorkedList.isEmpty()) {
                                // Convertir los músculos sinergistas del patrón en una lista de nombres y normalizar los nombres
                                List<String> synergistMusclesList = pattern.getSynergistMuscles().stream()
                                        .map(muscle -> muscle.getName().toLowerCase())
                                        .collect(Collectors.toList());

                                // Mostrar las listas de músculos para comparación
                                Log.d(TAG, "loadExercises: Muscles Worked List: " + musclesWorkedList);
                                Log.d(TAG, "loadExercises: Synergist Muscles List: " + synergistMusclesList);

                                // Verificar si hay al menos un músculo en común entre las dos listas
                                boolean anyMuscleMatch = musclesWorkedList.stream()
                                        .anyMatch(synergistMusclesList::contains);

                                // Mostrar el resultado de la comparación
                                Log.d(TAG, "loadExercises: anyMuscleMatch: " + anyMuscleMatch);

                                if (anyMuscleMatch) {
                                    Log.d(TAG, "loadExercises: El ejercicio coincide parcialmente con el patrón de movimiento: " + pattern.getName());
                                    exercises.add(exercise);
                                    break;
                                } else {
                                    Log.d(TAG, "loadExercises: El ejercicio no coincide con el patrón de movimiento: " + pattern.getName());
                                }
                            } else {
                                if (pattern.getSynergistMuscles() == null) {
                                    Log.d(TAG, "loadExercises: El patrón de movimiento no tiene músculos sinergistas.");
                                }
                                if (musclesWorkedList.isEmpty()) {
                                    Log.d(TAG, "loadExercises: El ejercicio no tiene músculos trabajados.");
                                }
                            }
                        }
                    } else {
                        Log.d(TAG, "loadExercises: Ejercicio es nulo");
                    }
                }
                if (exercises.isEmpty()) {
                    Log.d(TAG, "loadExercises: No se encontraron ejercicios que coincidan con los criterios.");
                }
                if (recyclerView != null) {
                    ExercisesAdapter adapter = new ExercisesAdapter(exercises, selectedExercises);
                    recyclerView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    Log.d(TAG, "loadExercises: Adaptador de ejercicios configurado");
                } else {
                    Log.e(TAG, "loadExercises: RecyclerView es nulo");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d(TAG, "loadExercises: Error de base de datos: " + error.getMessage());
            }
        });
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
            Log.d(TAG, "onCreateViewHolder: Creating view holder");
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
                Log.d(TAG, "onBindViewHolder: Ejercicio seleccionado actualizado. Total seleccionados: " + selectedExercises.size());
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
                Log.d(TAG, "ViewHolder: ViewHolder inicializado");
            }
        }
    }
}