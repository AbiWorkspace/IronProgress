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
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.example.diets.model.UserMovementPattern;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SelectExercisesFragment extends Fragment implements ExercisesAdapter.OnExerciseSelectionChangedListener {
    private static final String TAG = "SelectExercisesFragment";
    private List<Exercise> selectedExercises = new ArrayList<>();
    private LinearLayout containerLayout;
    private DatabaseReference mDatabase;
    private String userId;
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
            userId = data.getString("userId"); // Obtener el userId
            createExerciseContainers(trainingDays);
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Ejercicios seleccionados: " + selectedExercises);
            saveSelectedExercises();
        });
    }
    private void saveSelectedExercises() {
        boolean allContainersHaveExercises = true;
        for (TrainingDay trainingDay : trainingDays) {
            if (trainingDay.getExercises().isEmpty()) {
                allContainersHaveExercises = false;
                Log.d(TAG, "saveSelectedExercises: El contenedor del día " + trainingDay.getDayIndex() + " está vacío.");
                break;
            }
        }

        if (!allContainersHaveExercises) {
            Toast.makeText(getContext(), "Cada contenedor debe tener al menos un ejercicio", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference userRef = mDatabase.child("users").child(userId); // Asegúrate de tener el userId disponible
        DatabaseReference userExercisesRef = userRef.child("userExercises");
        DatabaseReference userMovementPatternsRef = userRef.child("userMovementPatterns");

        List<UserExercise> userExercises = new ArrayList<>();
        List<UserMovementPattern> userMovementPatterns = new ArrayList<>();

        for (TrainingDay trainingDay : trainingDays) {
            for (MovementPattern pattern : trainingDay.getMovementPatterns()) {
                UserMovementPattern userMovementPattern = new UserMovementPattern(pattern);
                for (Exercise exercise : trainingDay.getExercises()) {
                    UserExercise userExercise = new UserExercise();
                    userExercise.setName(exercise.getName());
                    userExercise.setEquipmentType(exercise.getEquipmentType());
                    userExercise.setExerciseType(exercise.getExerciseType());
                    userExercise.setMusclesWorked(exercise.getMusclesWorked());
                    userExercise.setLoadIntensity(exercise.getLoadIntensity());
                    userExercise.setImage(exercise.getImage());
                    userExercise.setDescription(exercise.getDescription());
                    userExercise.setMovementType(exercise.getMovementType());
                    userExercise.setSynergistMuscles(exercise.getSynergistMuscles());
                    userExercise.setGoals(exercise.getGoals());
                    userExercise.setMovementPatterns(exercise.getMovementPatterns());
                    userExercises.add(userExercise);
                    userMovementPattern.getUserExercises().add(userExercise);
                }
                userMovementPatterns.add(userMovementPattern);
            }
        }

        userExercisesRef.setValue(userExercises)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Ejercicios seleccionados guardados exitosamente");
                    Toast.makeText(getContext(), "Ejercicios seleccionados guardados exitosamente", Toast.LENGTH_SHORT).show();

                    Bundle data = new Bundle();
                    data.putSerializable("trainingDays", (Serializable) trainingDays);
                    data.putString("userGoal", userGoal);

                    ViewPagerActivity activity = (ViewPagerActivity) getActivity();
                    if (activity != null) {
                        activity.switchToAssignLoadFragment(data);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar los ejercicios seleccionados", e);
                    Toast.makeText(getContext(), "Error al guardar los ejercicios seleccionados", Toast.LENGTH_SHORT).show();
                });

        userMovementPatternsRef.setValue(userMovementPatterns)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Patrones de movimiento guardados exitosamente");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error al guardar los patrones de movimiento", e);
                });
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
                loadExercises(pattern, trainingDay); // Pasar también el TrainingDay
                dayContainer.addView(recyclerView);
            }

            Button addButton = new Button(getContext());
            addButton.setText("Agregar Ejercicios");
            addButton.setTextColor(Color.WHITE);
            addButton.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.rounded_button));
            addButton.setTag(trainingDay); // Asociar el botón con el TrainingDay correspondiente
            addButton.setOnClickListener(v -> {
                TrainingDay day = (TrainingDay) v.getTag();
                showExerciseSelectionDialog(day, userGoal);
            });
            dayContainer.addView(addButton);

            containerLayout.addView(dayContainer);
            dayContainers.put(trainingDay, dayContainer);
            Log.d(TAG, "createExerciseContainers: dayContainer añadido para trainingDay: " + trainingDay.getDayIndex() + " con ID: " + containerId);
        }


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
    private void loadExercises(MovementPattern pattern, TrainingDay trainingDay) {
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
                        addExerciseToSelection(exercise, trainingDay); // Pasar también el TrainingDay
                        Log.d(TAG, "onExerciseClick: Ejercicio seleccionado: " + exercise.getName());
                    }, SelectExercisesFragment.this); // Pasar 'this' como OnExerciseSelectionChangedListener
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

    @Override
    public void onExerciseSelected(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
            Log.d(TAG, "onExerciseSelected: Ejercicio añadido: " + exercise.getName());
        }
    }

    @Override
    public void onExerciseDeselected(Exercise exercise) {
        selectedExercises.remove(exercise);
        Log.d(TAG, "onExerciseDeselected: Ejercicio eliminado: " + exercise.getName());
    }

    public void addSelectedExercise(Exercise exercise, TrainingDay trainingDay) {
        // Obtener el contenedor específico del día
        LinearLayout dayContainer = dayContainers.get(trainingDay);
        boolean exerciseAlreadyInContainer = false;

        if (dayContainer != null) {
            for (int i = 0; i < dayContainer.getChildCount(); i++) {
                View child = dayContainer.getChildAt(i);
                TextView exerciseNameView = child.findViewById(R.id.exerciseName);
                if (exerciseNameView != null && exerciseNameView.getText().toString().equals(exercise.getName())) {
                    exerciseAlreadyInContainer = true;
                    break;
                }
            }
        }

        if (!exerciseAlreadyInContainer) {
            Log.d(TAG, "addSelectedExercise: Añadiendo ejercicio seleccionado: " + exercise.getName());
            selectedExercises.add(exercise);
            // Añadir el ejercicio seleccionado al dayContainer correspondiente
            addExerciseToDayContainer(exercise, trainingDay);
        } else {
            Log.d(TAG, "addSelectedExercise: Ejercicio ya estaba en el contenedor del día: " + trainingDay.getDayIndex());
        }
    }

    private void addExerciseToDayContainer(Exercise exercise, TrainingDay trainingDay) {
        Log.d(TAG, "addExerciseToDayContainer: Iniciando proceso para añadir ejercicio al contenedor del día");

        // Obtener el contenedor específico del día
        LinearLayout dayContainer = dayContainers.get(trainingDay);
        if (dayContainer != null) {
            Log.d(TAG, "addExerciseToDayContainer: dayContainer encontrado para trainingDay: " + trainingDay.getDayIndex());

            // Verificar si el ejercicio ya está en el contenedor específico
            boolean exerciseAlreadyInContainer = false;
            for (int i = 0; i < dayContainer.getChildCount(); i++) {
                View child = dayContainer.getChildAt(i);
                TextView exerciseNameView = child.findViewById(R.id.exerciseName);
                if (exerciseNameView != null && exerciseNameView.getText().toString().equals(exercise.getName())) {
                    exerciseAlreadyInContainer = true;
                    break;
                }
            }

            if (!exerciseAlreadyInContainer) {
                // Inflar el contenedor personalizado
                LayoutInflater inflater = LayoutInflater.from(getContext());
                View exerciseContainer = inflater.inflate(R.layout.exercise_item_container, dayContainer, false);

                // Configurar el nombre del ejercicio
                TextView exerciseNameView = exerciseContainer.findViewById(R.id.exerciseName);
                exerciseNameView.setText(exercise.getName());

                // Añadir el contenedor del ejercicio justo encima del botón "Agregar Ejercicios"
                int addButtonIndex = dayContainer.getChildCount() - 1;
                dayContainer.addView(exerciseContainer, addButtonIndex);

                Log.d(TAG, "addExerciseToDayContainer: Ejercicio añadido al contenedor del día: " + trainingDay.getDayIndex());

                // Añadir el ejercicio a la lista de ejercicios del TrainingDay
                trainingDay.getExercises().add(exercise);

                // Configurar el menú de tres puntos
                ImageView menuButton = exerciseContainer.findViewById(R.id.menuButton);
                menuButton.setOnClickListener(v -> {
                    PopupMenu popupMenu = new PopupMenu(getContext(), menuButton);
                    popupMenu.inflate(R.menu.exercise_item_menu);
                    popupMenu.setOnMenuItemClickListener(item -> {
                        int itemId = item.getItemId();
                        if (itemId == R.id.action_edit) {
                            // Lógica para editar el ejercicio
                            Log.d(TAG, "Editar ejercicio: " + exercise.getName());
                            return true;
                        } else if (itemId == R.id.action_delete) {
                            // Lógica para eliminar el ejercicio
                            Log.d(TAG, "Eliminar ejercicio: " + exercise.getName());
                            dayContainer.removeView(exerciseContainer);
                            selectedExercises.remove(exercise);
                            trainingDay.getExercises().remove(exercise); // Eliminar el ejercicio de la lista del TrainingDay
                            return true;
                        } else {
                            return false;
                        }
                    });
                    popupMenu.show();
                });
            } else {
                Log.d(TAG, "addExerciseToDayContainer: Ejercicio ya está en el contenedor del día: " + trainingDay.getDayIndex());
            }
        } else {
            Log.d(TAG, "addExerciseToDayContainer: dayContainer es nulo para trainingDay: " + trainingDay.getDayIndex());
        }
    }

    public void addExerciseToSelection(Exercise exercise, TrainingDay trainingDay) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
            Log.d(TAG, "addExerciseToSelection: Ejercicio añadido: " + exercise.getName());
            addExerciseToDayContainer(exercise, trainingDay);
        } else {
            Log.d(TAG, "addExerciseToSelection: Ejercicio ya estaba en la lista: " + exercise.getName());
        }
    }

}