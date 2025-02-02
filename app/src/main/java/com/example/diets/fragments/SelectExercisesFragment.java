package com.example.diets.fragments;

import android.content.ClipData;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SelectExercisesFragment extends Fragment {
    private static final String TAG = "SelectExercisesFragment";
    private List<Exercise> selectedExercises = new ArrayList<>();
    private LinearLayout containerLayout;
    private DatabaseReference mDatabase;
    private List<TrainingDay> trainingDays;
    private String userGoal; // Meta del usuario
    private RecyclerView recyclerView; // Añadir esta línea
    private List<Exercise> exercises = new ArrayList<>(); // Añadir esta línea

    private Map<TrainingDay, LinearLayout> dayContainers = new HashMap<>(); // Mapa para almacenar los contenedores de días

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_select_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        containerLayout = view.findViewById(R.id.containerLayout);
        Log.d(TAG, "onViewCreated: containerLayout inicializado");
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            String selectedDistribution = data.getString("selectedDistribution");
            trainingDays = (List<TrainingDay>) data.getSerializable("trainingDays");
            userGoal = data.getString("userGoal"); // Obtener la meta del usuario
            createExerciseContainers(trainingDays);
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Ejercicios seleccionados: " + selectedExercises);
            // Aquí puedes agregar la lógica para guardar los ejercicios seleccionados
            saveSelectedExercises();
        });
    }

    private void saveSelectedExercises() {
        // Obtener la referencia a la base de datos
        DatabaseReference selectedExercisesRef = mDatabase.child("selectedExercises");

        // Crear un mapa para almacenar los ejercicios seleccionados
        Map<String, Object> exercisesMap = new HashMap<>();
        for (Exercise exercise : selectedExercises) {
            exercisesMap.put(exercise.getName(), true);
        }

        // Guardar los ejercicios seleccionados en la base de datos
        selectedExercisesRef.setValue(exercisesMap)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Ejercicios seleccionados guardados exitosamente"))
                .addOnFailureListener(e -> Log.e(TAG, "Error al guardar los ejercicios seleccionados", e));
    }

    private void createExerciseContainers(List<TrainingDay> trainingDays) {
        Log.d(TAG, "createExerciseContainers: Iniciando creación de contenedores");
        for (int i = 0; i < trainingDays.size(); i++) {
            TrainingDay trainingDay = trainingDays.get(i);
            Log.d(TAG, "createExerciseContainers: Creando contenedor para trainingDay: " + trainingDay.getDayIndex());

            LinearLayout dayContainer = new LinearLayout(getContext());
            dayContainer.setOrientation(LinearLayout.VERTICAL);
            dayContainer.setPadding(16, 16, 16, 16);
            dayContainer.setBackgroundColor(Color.parseColor("#2F2F2F"));
            int containerId = View.generateViewId();
            dayContainer.setId(containerId);
            trainingDay.setContainerId(containerId);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            dayContainer.setLayoutParams(params);
            dayContainer.setBackgroundResource(R.drawable.container_background);

            RelativeLayout titleLayout = new RelativeLayout(getContext());
            titleLayout.setPadding(0, 0, 0, 16);

            LinearLayout textLayout = new LinearLayout(getContext());
            textLayout.setOrientation(LinearLayout.HORIZONTAL);

            TextView dayTitle = new TextView(getContext());
            dayTitle.setText("Día " + (i + 1));
            dayTitle.setTextColor(Color.WHITE);
            dayTitle.setTextSize(20);
            dayTitle.setTypeface(null, Typeface.BOLD);
            textLayout.addView(dayTitle);

            for (MovementPattern pattern : trainingDay.getMovementPatterns()) {
                TextView patternTitle = new TextView(getContext());
                patternTitle.setText(" - " + pattern.getName());
                patternTitle.setTextColor(Color.WHITE);
                patternTitle.setTextSize(18);
                patternTitle.setTypeface(null, Typeface.BOLD);
                textLayout.addView(patternTitle);
            }

            RelativeLayout.LayoutParams textLayoutParams = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT
            );
            textLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_START);
            textLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            textLayout.setLayoutParams(textLayoutParams);
            titleLayout.addView(textLayout);

            ImageView dragHandle = new ImageView(getContext());
            dragHandle.setImageResource(R.drawable.ic_drag_handle);
            dragHandle.setColorFilter(Color.WHITE);
            RelativeLayout.LayoutParams dragHandleParams = new RelativeLayout.LayoutParams(
                    100,
                    100
            );
            dragHandleParams.addRule(RelativeLayout.ALIGN_PARENT_END);
            dragHandleParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            dragHandle.setLayoutParams(dragHandleParams);
            dragHandle.setOnLongClickListener(v -> {
                Log.d(TAG, "Drag handle long click - initiating drag");
                ClipData data = ClipData.newPlainText("", "");
                View.DragShadowBuilder shadowBuilder = new View.DragShadowBuilder(dayContainer);
                dayContainer.startDragAndDrop(data, shadowBuilder, dayContainer, 0);
                return true;
            });
            titleLayout.addView(dragHandle);

            dayContainer.addView(titleLayout);

            for (MovementPattern pattern : trainingDay.getMovementPatterns()) {
                RecyclerView recyclerView = new RecyclerView(getContext());
                recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                loadExercises(pattern);
                dayContainer.addView(recyclerView);
            }

            Button addButton = new Button(getContext());
            addButton.setText("Agregar Ejercicios");
            addButton.setTextColor(Color.WHITE);
            addButton.setBackgroundColor(Color.parseColor("#E63946"));
            addButton.setOnClickListener(v -> showExerciseSelectionDialog(trainingDay, userGoal));
            dayContainer.addView(addButton);

            containerLayout.addView(dayContainer);
            dayContainers.put(trainingDay, dayContainer);
            Log.d(TAG, "createExerciseContainers: dayContainer añadido para trainingDay: " + trainingDay.getDayIndex() + " con ID: " + containerId);
        }

        containerLayout.setOnDragListener((v, event) -> {
            switch (event.getAction()) {
                case DragEvent.ACTION_DRAG_STARTED:
                    Log.d(TAG, "Drag started");
                    return true;
                case DragEvent.ACTION_DRAG_ENTERED:
                    Log.d(TAG, "Drag entered");
                    return true;
                case DragEvent.ACTION_DRAG_LOCATION:
                    Log.d(TAG, "Drag location: " + event.getY());
                    return true;
                case DragEvent.ACTION_DRAG_EXITED:
                    Log.d(TAG, "Drag exited");
                    return true;
                case DragEvent.ACTION_DROP:
                    Log.d(TAG, "Drop event");
                    View draggedView = (View) event.getLocalState();
                    draggedView.setVisibility(View.VISIBLE);
                    containerLayout.removeView(draggedView);
                    int dropPosition = (int) event.getY();
                    int insertIndex = 0;
                    for (int i = 0; i < containerLayout.getChildCount(); i++) {
                        View child = containerLayout.getChildAt(i);
                        if (dropPosition < child.getBottom()) {
                            insertIndex = i;
                            break;
                        }
                    }
                    Log.d(TAG, "Inserting at index: " + insertIndex);
                    containerLayout.addView(draggedView, insertIndex);
                    return true;
                case DragEvent.ACTION_DRAG_ENDED:
                    Log.d(TAG, "Drag ended");
                    View view = (View) event.getLocalState();
                    view.setVisibility(View.VISIBLE);
                    return true;
                default:
                    return false;
            }
        });
    }

    private void showExerciseSelectionDialog(TrainingDay trainingDay, String userGoal) {
        MenuPopupFragment dialogFragment = new MenuPopupFragment();
        Bundle args = new Bundle();
        args.putSerializable("trainingDay", trainingDay);
        args.putString("userGoal", userGoal); // Pasar la meta del usuario
        dialogFragment.setArguments(args);
        dialogFragment.show(getParentFragmentManager(), "ExerciseSelectionDialog");
        Log.d(TAG, "showExerciseSelectionDialog: Mostrando diálogo de selección de ejercicios");
    }
    private void loadExercises(MovementPattern pattern) {
        DatabaseReference exercisesRef = mDatabase.child("exercises");
        exercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                exercises.clear();
                selectedExercises.clear(); // Clear the selected exercises list
                Log.d(TAG, "loadExercises: Número de ejercicios recuperados: " + snapshot.getChildrenCount());
                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    Exercise exercise = exerciseSnapshot.getValue(Exercise.class);
                    if (exercise != null) {
                        Log.d(TAG, "loadExercises: Ejercicio recuperado: " + exercise.getName());
                        Log.d(TAG, "loadExercises: Musculos: " + exercise.getMusclesWorked());

                        if (exercise.getMusclesWorked() != null) {
                            List<String> musclesWorkedList = Arrays.stream(exercise.getMusclesWorked().split(",\\s*"))
                                    .map(String::toLowerCase)
                                    .collect(Collectors.toList());

                            if (pattern.getSynergistMuscles() != null && !musclesWorkedList.isEmpty()) {
                                List<String> synergistMusclesList = pattern.getSynergistMuscles().stream()
                                        .map(muscle -> muscle.getName().toLowerCase())
                                        .collect(Collectors.toList());

                                Log.d(TAG, "loadExercises: Muscles Worked List: " + musclesWorkedList);
                                Log.d(TAG, "loadExercises: Synergist Muscles List: " + synergistMusclesList);

                                boolean anyMuscleMatch = musclesWorkedList.stream()
                                        .anyMatch(synergistMusclesList::contains);

                                Log.d(TAG, "loadExercises: anyMuscleMatch: " + anyMuscleMatch);

                                if (anyMuscleMatch) {
                                    Log.d(TAG, "loadExercises: El ejercicio coincide parcialmente con el patrón de movimiento: " + pattern.getName());
                                    exercises.add(exercise);
                                    pattern.getExercises().add(exercise);
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
                        } else {
                            Log.d(TAG, "loadExercises: MusclesWorked es nulo para el ejercicio: " + exercise.getName());
                        }
                    } else {
                        Log.d(TAG, "loadExercises: Ejercicio es nulo");
                    }
                }
                if (exercises.isEmpty()) {
                    Log.d(TAG, "loadExercises: No se encontraron ejercicios que coincidan con los criterios.");
                }
                if (recyclerView != null) {
                    List<String> selectedExerciseNames = selectedExercises.stream()
                            .map(Exercise::getName)
                            .collect(Collectors.toList());
                    com.example.diets.adapters.ExercisesAdapter adapter = new com.example.diets.adapters.ExercisesAdapter(exercises, selectedExerciseNames, exercise -> {
                        addExerciseToSelection(exercise);
                        Log.d(TAG, "onExerciseClick: Ejercicio seleccionado: " + exercise.getName());
                    });
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
        private final OnExerciseClickListener listener;

        ExercisesAdapter(List<Exercise> exercises, List<String> selectedExercises, OnExerciseClickListener listener) {
            this.exercises = exercises;
            this.selectedExercises = selectedExercises;
            this.listener = listener;
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
                    Log.d(TAG, "onBindViewHolder: Ejercicio seleccionado: " + exercise.getName());
                } else {
                    selectedExercises.remove(exercise.getName());
                    Log.d(TAG, "onBindViewHolder: Ejercicio deseleccionado: " + exercise.getName());
                }
                listener.onExerciseClick(exercise);
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

        interface OnExerciseClickListener {
            void onExerciseClick(Exercise exercise);
        }
    }
    public void addSelectedExercise(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            Log.d(TAG, "addSelectedExercise: Añadiendo ejercicio seleccionado: " + exercise.getName());
            selectedExercises.add(exercise);
            // Añadir el ejercicio seleccionado al dayContainer correspondiente
            addExerciseToDayContainer(exercise);
        } else {
            Log.d(TAG, "addSelectedExercise: Ejercicio ya estaba en la lista: " + exercise.getName());
        }
    }

    private void addExerciseToDayContainer(Exercise exercise) {
        Log.d(TAG, "addExerciseToDayContainer: Iniciando proceso para añadir ejercicio al contenedor del día");
        for (TrainingDay trainingDay : trainingDays) {
            Log.d(TAG, "addExerciseToDayContainer: Revisando trainingDay: " + trainingDay.getDayIndex());
            if (trainingDay.getMovementPatterns().stream().anyMatch(pattern -> pattern.getExercises().contains(exercise))) {
                Log.d(TAG, "addExerciseToDayContainer: Ejercicio coincide con un patrón de movimiento en trainingDay: " + trainingDay.getDayIndex());
                LinearLayout dayContainer = dayContainers.get(trainingDay);
                if (dayContainer != null) {
                    Log.d(TAG, "addExerciseToDayContainer: dayContainer encontrado para trainingDay: " + trainingDay.getDayIndex());
                    TextView exerciseView = new TextView(getContext());
                    exerciseView.setText(exercise.getName());
                    exerciseView.setTextColor(Color.WHITE);
                    exerciseView.setTextSize(18);

                    // Añadir el ejercicio justo encima del botón "Agregar Ejercicios"
                    int addButtonIndex = dayContainer.getChildCount() - 1;
                    dayContainer.addView(exerciseView, addButtonIndex);

                    Log.d(TAG, "addExerciseToDayContainer: Ejercicio añadido al contenedor del día: " + trainingDay.getDayIndex());
                } else {
                    Log.d(TAG, "addExerciseToDayContainer: dayContainer es nulo para trainingDay: " + trainingDay.getDayIndex());
                }
                break;
            } else {
                Log.d(TAG, "addExerciseToDayContainer: Ejercicio no coincide con ningún patrón de movimiento en trainingDay: " + trainingDay.getDayIndex());
            }
        }
    }

    public void addExerciseToSelection(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
            Log.d(TAG, "addExerciseToSelection: Ejercicio añadido: " + exercise.getName());
        } else {
            Log.d(TAG, "addExerciseToSelection: Ejercicio ya estaba en la lista: " + exercise.getName());
        }
    }


}