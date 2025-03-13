package com.example.diets.fragments;

import android.content.ClipData;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
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
import java.util.concurrent.atomic.AtomicInteger;
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

        Log.d(TAG, "[onViewCreated] 🔄 INICIALIZANDO SelectExercisesFragment");

        // 📌 Inicializar el contenedor principal
        containerLayout = view.findViewById(R.id.containerLayout);
        Log.d(TAG, "[onViewCreated] ✅ containerLayout inicializado");



        // 🔥 Inicializar Firebase Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // 📌 Obtener datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            String selectedDistribution = data.getString("selectedDistribution");
            trainingDays = (List<TrainingDay>) data.getSerializable("trainingDays");
            userGoal = data.getString("userGoal");
            userId = data.getString("userId");

            Log.d(TAG, "[onViewCreated] 📌 Datos obtenidos del Bundle: userGoal=" + userGoal + ", userId=" + userId);

            // 📌 Crear los contenedores de ejercicios para cada día
            createExerciseContainers(trainingDays);
        } else {
            Log.e(TAG, "[onViewCreated] ❌ ERROR: No se recibieron datos en el Bundle.");
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        if (saveButton != null) {
            saveButton.setOnClickListener(v -> {
                Log.d(TAG, "[onViewCreated] 📌 Botón 'Guardar' presionado. Iniciando validación de ejercicios.");

                // 🔍 Validar que cada día tenga al menos un ejercicio antes de guardar
                boolean allContainersHaveExercises = true;

                for (TrainingDay trainingDay : trainingDays) {
                    if (trainingDay.getExercises().isEmpty()) { // ❌ Día sin ejercicios
                        allContainersHaveExercises = false;
                        Log.e(TAG, "❌ ERROR: El contenedor del día `" + trainingDay.getDayTitle() + "` está vacío.");
                    }
                }

                if (!allContainersHaveExercises) {
                    Toast.makeText(getContext(), "Cada día debe tener al menos un ejercicio.", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "[onViewCreated] ❌ ERROR: No se puede guardar, hay días sin ejercicios.");
                    return; // ⛔ No continuar si hay días vacíos
                }

                Log.d(TAG, "[onViewCreated] ✅ Validación exitosa. Procediendo a guardar.");

                saveSelectedExercises(); // 🔥 Guardar datos en Firebase

                // ✅ Llamar a `switchToTrainingDetailFragment` en `ViewPagerActivity`
                ViewPagerActivity activity = (ViewPagerActivity) getActivity();
                if (activity != null) {
                    Bundle traka = new Bundle();
                    traka.putSerializable("trainingDays", (Serializable) trainingDays);
                    traka.putString("userGoal", userGoal);
                    traka.putString("userId", userId);
                    activity.switchToAssignLoadFragment(traka);
                    Log.d(TAG, "[onViewCreated] ✅ Cambio a TrainingDetailFragment exitoso.");
                } else {
                    Log.e(TAG, "[onViewCreated] ❌ ERROR: No se pudo obtener ViewPagerActivity.");
                }
            });
        } else {
            Log.e(TAG, "[onViewCreated] ❌ ERROR: No se encontró el botón de guardar.");
        }

        Log.d(TAG, "[onViewCreated] ✅ MÉTODO FINALIZADO");

    }
    private void saveSelectedExercises() {
        Log.d(TAG, "💾 Iniciando guardado en Firebase.");

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference()
                .child("users").child(userId);

        DatabaseReference userExercisesRef = userRef.child("userExercises");
        DatabaseReference userMovementPatternsRef = userRef.child("userMovementPatterns");

        // 🔥 LIMPIAR datos previos
        userExercisesRef.removeValue();
        userMovementPatternsRef.removeValue();

        for (int i = 0; i < trainingDays.size(); i++) {
            TrainingDay trainingDay = trainingDays.get(i);
            String movementPatternName = "Día " + (i + 1);
            if (trainingDay.getMovementPatterns() != null && !trainingDay.getMovementPatterns().isEmpty()) {
                movementPatternName = trainingDay.getMovementPatterns().get(0).getName();
            }

            Log.d(TAG, "📌 Guardando entrenamiento: " + movementPatternName);

            DatabaseReference dayRef = userMovementPatternsRef.child("Día " + (i + 1));
            dayRef.child("name").setValue(movementPatternName);

            List<Map<String, Object>> exerciseList = new ArrayList<>();
            for (UserExercise exercise : trainingDay.getExercises()) {
                Map<String, Object> exerciseMap = convertExerciseToMap(exercise);
                exerciseMap.put("series", exercise.getSeries()); // 🔥 Guardar series
                exerciseList.add(exerciseMap);

                userExercisesRef.push().setValue(exerciseMap);
            }

            dayRef.child("userExercises").setValue(exerciseList);
        }

        Log.d(TAG, "✅ Datos guardados correctamente en Firebase.");
    }


    // ✅ Método para convertir `Exercise` a `Map<String, Object>`
    private Map<String, Object> convertExerciseToMap(Exercise exercise) {
        Map<String, Object> exerciseMap = new HashMap<>();
        exerciseMap.put("name", exercise.getName());
        exerciseMap.put("equipmentType", exercise.getEquipmentType());
        exerciseMap.put("exerciseType", exercise.getExerciseType());
        exerciseMap.put("musclesWorked", exercise.getMusclesWorked());
        exerciseMap.put("loadIntensity", exercise.getLoadIntensity());
        exerciseMap.put("image", exercise.getImage());
        exerciseMap.put("description", exercise.getDescription());
        exerciseMap.put("movementType", exercise.getMovementType());
        exerciseMap.put("synergistMuscles", exercise.getSynergistMuscles());
        exerciseMap.put("goals", exercise.getGoals());
        exerciseMap.put("movementPatterns", exercise.getMovementPatterns());
        return exerciseMap;
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
        MenuPopupFragment menuPopupFragment = new MenuPopupFragment();

        if (trainingDay == null) {
            Log.e(TAG, "❌ ERROR: TrainingDay es NULL antes de abrir MenuPopupFragment");
        }

        Bundle args = new Bundle();
        args.putSerializable("trainingDay", trainingDay); // 📌 Asegurar que se pase correctamente
        args.putString("userGoal", userGoal);
        menuPopupFragment.setArguments(args);


        menuPopupFragment.show(getParentFragmentManager(), "MenuPopupFragment");
        Log.d(TAG, "showExerciseSelectionDialog: ✅ Abriendo MenuPopupFragment para seleccionar ejercicios.");
    }


    private void loadExercises(MovementPattern pattern, TrainingDay trainingDay) {
        Log.d(TAG, "[loadExercises] 🔄 MÉTODO INICIADO");

        DatabaseReference exercisesRef = mDatabase.child("exercises");
        exercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                exercises.clear();
                Log.d(TAG, "[loadExercises] 📌 Número de ejercicios recuperados: " + snapshot.getChildrenCount());

                for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                    Object data = exerciseSnapshot.getValue();

                    if (data instanceof Map) {
                        Map<String, Object> dataMap = (Map<String, Object>) data;
                        Exercise exercise = mapToExercise(dataMap);

                        if (exercise != null) {
                            Log.d(TAG, "[loadExercises] ✅ Ejercicio recuperado: " + exercise.getName());
                            exercises.add(exercise);
                        }
                    } else {
                        Log.e(TAG, "[loadExercises] ❌ ERROR: Tipo de datos inválido: " + (data != null ? data.getClass().getSimpleName() : "null"));
                    }
                }


            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "[loadExercises] ❌ Error en Firebase: " + error.getMessage());
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
        Log.d(TAG, "[addSelectedExercise] 🔄 MÉTODO INICIADO para el ejercicio: " + exercise.getName());

        LinearLayout dayContainer = dayContainers.get(trainingDay);
        if (dayContainer == null) {
            Log.e(TAG, "[addSelectedExercise] ❌ ERROR: `dayContainer` es NULL para trainingDay: " + trainingDay.getDayIndex());
            return;
        }

        // Verificar si el ejercicio ya está en el contenedor
        for (int i = 0; i < dayContainer.getChildCount(); i++) {
            View child = dayContainer.getChildAt(i);
            TextView exerciseNameView = child.findViewById(R.id.exerciseName);
            if (exerciseNameView != null && exerciseNameView.getText().toString().equals(exercise.getName())) {
                Log.w(TAG, "[addSelectedExercise] ⚠ ADVERTENCIA: El ejercicio `" + exercise.getName() + "` ya está en el contenedor del día " + trainingDay.getDayIndex());
                return;
            }
        }

        // ✅ Agregar el ejercicio al contenedor
        addExerciseToDayContainer(exercise, trainingDay);

        Log.d(TAG, "[addSelectedExercise] ✅ MÉTODO FINALIZADO");
    }
    private void addExerciseToDayContainer(Exercise exercise, TrainingDay trainingDay) {
        Log.d(TAG, "[addExerciseToDayContainer] 🔄 MÉTODO INICIADO para: " + exercise.getName());

        LinearLayout dayContainer = dayContainers.get(trainingDay);
        if (dayContainer == null) {
            Log.e(TAG, "[addExerciseToDayContainer] ❌ ERROR: No se encontró `dayContainer` para el día " + trainingDay.getDayIndex());
            return;
        }

        // Inflar el layout del ejercicio
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View exerciseContainer = inflater.inflate(R.layout.exercise_item_container, dayContainer, false);

        if (exerciseContainer == null) {
            Log.e(TAG, "[addExerciseToDayContainer] ❌ ERROR: `exerciseContainer` es NULL después de inflar.");
            return;
        }

        // Configurar el nombre del ejercicio
        TextView exerciseNameView = exerciseContainer.findViewById(R.id.exerciseName);
        EditText seriesInput = exerciseContainer.findViewById(R.id.seriesInput); // 🔥 Nuevo campo

        if (exerciseNameView == null || seriesInput == null) {
            Log.e(TAG, "[addExerciseToDayContainer] ❌ ERROR: `exerciseNameView` o `seriesInput` son NULL.");
            return;
        }

        exerciseNameView.setText(exercise.getName());

        // ✅ Verificar si el ejercicio ya existe en el día
        UserExercise userExercise = null;
        for (UserExercise ue : trainingDay.getExercises()) {
            if (ue.getName().equals(exercise.getName())) {
                userExercise = ue;
                break;
            }
        }

        if (userExercise == null) {
            userExercise = new UserExercise(exercise);
            userExercise.setSeries(3); // 🔥 Valor por defecto
            trainingDay.getExercises().add(userExercise);
        }

        // **🔥 Solución: Usar AtomicInteger para modificar en el TextWatcher**
        AtomicInteger seriesValue = new AtomicInteger(userExercise.getSeries());
        seriesInput.setText(String.valueOf(seriesValue.get()));

        // 🔥 Guardar cambios cuando se editen las series
        UserExercise finalUserExercise = userExercise; // **✅ Hacer una copia para usar dentro del TextWatcher**
        seriesInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    int series = Integer.parseInt(s.toString());
                    seriesValue.set(series);
                    finalUserExercise.setSeries(series); // **✅ Modificar la copia final**
                    Log.d(TAG, "[addExerciseToDayContainer] 📌 Número de series actualizado: " + series);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "[addExerciseToDayContainer] ❌ ERROR: Valor no válido en seriesInput");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 🔥 Agregar el contenedor visualmente en la posición correcta
        int addButtonIndex = dayContainer.getChildCount() - 1;
        dayContainer.addView(exerciseContainer, addButtonIndex);

        // 🚀 Actualizar la UI
        dayContainer.post(() -> {
            dayContainer.invalidate();
            dayContainer.requestLayout();
        });

        // ✅ Configurar el menú de tres puntos (PopupMenu)
        configurePopupMenu(exerciseContainer, exercise, trainingDay, dayContainer);

        Log.d(TAG, "[addExerciseToDayContainer] ✅ Ejercicio agregado visualmente al día " + trainingDay.getDayIndex());
    }



    private void configurePopupMenu(View exerciseContainer, Exercise exercise, TrainingDay trainingDay, LinearLayout dayContainer) {
        ImageView menuButton = exerciseContainer.findViewById(R.id.menuButton);
        if (menuButton == null) {
            Log.e(TAG, "[configurePopupMenu] ❌ ERROR: `menuButton` es NULL. Verifica el ID en el XML.");
            return;
        }

        Log.d(TAG, "[configurePopupMenu] ✅ `menuButton` encontrado y configurado.");

        menuButton.setClickable(true);
        menuButton.setFocusable(true);

        menuButton.setOnClickListener(v -> {
            Log.d(TAG, "[configurePopupMenu] 📌 `menuButton` clickeado para `" + exercise.getName() + "`. Abriendo PopupMenu...");

            PopupMenu popupMenu = new PopupMenu(requireContext(), menuButton);
            popupMenu.inflate(R.menu.exercise_item_menu);

            popupMenu.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();

                if (itemId == R.id.action_edit) {
                    Log.d(TAG, "[configurePopupMenu] ✏ Editar ejercicio seleccionado: " + exercise.getName());
                    return true;
                } else if (itemId == R.id.action_delete) {
                    Log.d(TAG, "[configurePopupMenu] 🗑 Eliminando ejercicio: " + exercise.getName());


                    // 🔥 Remover visualmente el ejercicio del contenedor
                    dayContainer.removeView(exerciseContainer);
                    trainingDay.getExercises().removeIf(ex -> ex.getName().equals(exercise.getName()));
                    Log.d(TAG, "[configurePopupMenu] ✅ Ejercicio eliminado correctamente.");

                    // 🔥 Refrescar la UI
                    dayContainer.post(() -> {
                        dayContainer.invalidate();
                        dayContainer.requestLayout();
                    });

                    Toast.makeText(getContext(), "✅ Ejercicio eliminado", Toast.LENGTH_SHORT).show();
                    return true;
                }
                return false;
            });

            popupMenu.show();
        });
    }

    private Exercise mapToExercise(Map<String, Object> dataMap) {
        Exercise exercise = new Exercise();
        exercise.setName((String) dataMap.getOrDefault("name", ""));
        exercise.setEquipmentType((String) dataMap.getOrDefault("equipmentType", ""));
        exercise.setExerciseType((String) dataMap.getOrDefault("exerciseType", ""));
        exercise.setMusclesWorked((String) dataMap.getOrDefault("musclesWorked", ""));
        exercise.setLoadIntensity((String) dataMap.getOrDefault("loadIntensity", ""));
        exercise.setImage((String) dataMap.getOrDefault("image", ""));
        exercise.setDescription((String) dataMap.getOrDefault("description", ""));
        exercise.setMovementType((String) dataMap.getOrDefault("movementType", ""));

        // ✅ Conversión segura de listas
        exercise.setSynergistMuscles(convertToStringList(dataMap.get("synergistMuscles")));
        exercise.setGoals(convertToStringList(dataMap.get("goals")));
        exercise.setMovementPatterns(convertToMovementPatternList(dataMap.get("movementPatterns")));

        return exercise;
    }

    // ✅ Convierte un objeto en List<String>, asegurando que no sea null
    private List<String> convertToStringList(Object obj) {
        if (obj instanceof List) {
            return (List<String>) obj;
        }
        return new ArrayList<>(); // Si es null o incorrecto, devolver lista vacía
    }

    // ✅ Convierte un objeto en List<MovementPattern>
    private List<MovementPattern> convertToMovementPatternList(Object obj) {
        if (obj instanceof List) {
            List<MovementPattern> movementPatterns = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof Map) {
                    // Convertir cada elemento del Map a un objeto MovementPattern
                    movementPatterns.add(mapToMovementPattern((Map<String, Object>) item));
                }
            }
            return movementPatterns;
        }
        return new ArrayList<>();
    }

    // ✅ Método para convertir Map a MovementPattern
    private MovementPattern mapToMovementPattern(Map<String, Object> dataMap) {
        MovementPattern pattern = new MovementPattern();
        pattern.setName((String) dataMap.getOrDefault("name", ""));
        return pattern;
    }
    public interface OnExercisesSelectedListener {
        void onExercisesSelected(List<Exercise> selectedExercises, TrainingDay trainingDay);
    }


}
