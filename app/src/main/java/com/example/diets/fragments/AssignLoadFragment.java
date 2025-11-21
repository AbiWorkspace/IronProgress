package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.diets.R;
import com.example.diets.activitys.SharedViewModel;
import com.example.diets.activitys.ViewPagerActivity;
import com.example.diets.model.Exercise;
import com.example.diets.model.ExerciseSeries;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.Muscle;
import com.example.diets.model.TrainingDay;
import com.example.diets.model.UserExercise;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AssignLoadFragment extends Fragment {
    private static final String TAG = "AssignLoadFragment";
    private List<TrainingDay> trainingDays;
    private String userGoal;
    private LinearLayout containerLayout;
    private List<UserExercise> userExercises = new ArrayList<>();
    private DatabaseReference mDatabase;
    private String userId;
    private SharedViewModel sharedViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "AssignLoadFragment - onCreateView: Inflando el diseño para AssignLoadFragment");
        return inflater.inflate(R.layout.fragment_assign_load, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "AssignLoadFragment - onViewCreated: Vista creada para AssignLoadFragment");

        containerLayout = view.findViewById(R.id.containerLayout);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Inicializar el ViewModel compartido
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        sharedViewModel.getSelectedExercise().observe(getViewLifecycleOwner(), updatedExercise -> {
            if (updatedExercise != null) {
                for (UserExercise ue : userExercises) {
                    if (ue.getName().equals(updatedExercise.getName())) {
                        ue.setCurrentLoad(updatedExercise.getCurrentLoad());
                        ue.setMaxReps(updatedExercise.getMaxReps());
                        ue.setFamiliarity(updatedExercise.getFamiliarity());
                        ue.setCompleted(updatedExercise.isCompleted());
                        ue.setExerciseSeriesList(updatedExercise.getExerciseSeriesList());
                        // NO break; se actualizan todos
                    }
                }
                // Ahora se actualizan todos los duplicados
                updateStatusButtonIconForExercise(updatedExercise);
            }
        });

        // Obtener el userId del usuario autenticado
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            Log.d(TAG, "AssignLoadFragment - onViewCreated: Usuario autenticado con userId " + userId);
        } else {
            Log.e(TAG, "AssignLoadFragment - onViewCreated: Usuario no autenticado");
            return;
        }

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            trainingDays = (List<TrainingDay>) data.getSerializable("trainingDays");
            userGoal = data.getString("userGoal");
            Log.d(TAG, "AssignLoadFragment - onViewCreated: Datos recibidos - trainingDays: " + trainingDays + ", userGoal: " + userGoal);
            loadUserExercises();
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "AssignLoadFragment - onViewCreated: Botón de guardar clicado");

            //  AUDITORÍA: intento global de cierre del formulario de cargas
            Log.i(TAG, "AUDIT_TRAIL: Intento de finalizar asignación de cargas. Validando " + userExercises.size() + " ejercicios.");

            saveAssignedLoads();
            // Mostrar mensaje al usuario

            if (!areAllExercisesValid()) {
                Log.e(TAG, "AssignLoadFragment - saveAssignedLoads:  No se pueden guardar los datos. Hay ejercicios con información incompleta.");
                Toast.makeText(getContext(), "Completa todos los datos de los ejercicios antes de guardar.", Toast.LENGTH_LONG).show();
                return;
            } else {
                Toast.makeText(getContext(), "Formulario de entrenamiento completado", Toast.LENGTH_SHORT).show();
                // Navegar al nuevo fragmento
                if (getActivity() instanceof ViewPagerActivity) {
                    Bundle bundle = new Bundle();
                    bundle.putString("userId", userId);

                    ((ViewPagerActivity) getActivity()).switchToDashboardWorkoutFragment(bundle);
                }
            }
        });
    }


    private void loadUserExercises() {
        if (userId == null) {
            Log.e(TAG, "AssignLoadFragment - loadUserExercises: userId es nulo");
            return;
        }
        DatabaseReference userExercisesRef = mDatabase.child("users").child(userId).child("userExercises");
        userExercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        // Convertir el DataSnapshot a un HashMap
                        Map<String, Object> map = (Map<String, Object>) childSnapshot.getValue();
                        if (map != null) {
                            // Convertir el HashMap a un objeto UserExercise
                            UserExercise userExercise = convertMapToUserExercise(map);
                            if (userExercise != null) {
                                userExercises.add(userExercise);
                                Log.d(TAG, "AssignLoadFragment - loadUserExercises: Ejercicio cargado - " + userExercise.getName());
                            }
                        } else {
                            Log.e(TAG, "AssignLoadFragment - loadUserExercises: Se esperaba un mapa pero se encontró otro tipo de dato");
                        }
                    }
                } else {
                    Log.e(TAG, "AssignLoadFragment - loadUserExercises: Los datos no existen");
                }
                listExercises();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "AssignLoadFragment - loadUserExercises: Error al cargar los ejercicios del usuario: " + error.getMessage());
            }
        });
    }
    private UserExercise convertMapToUserExercise(Map<String, Object> map) {
        UserExercise userExercise = new UserExercise();

        userExercise.setName((String) map.get("name"));
        userExercise.setEquipmentType((String) map.get("equipmentType"));
        userExercise.setExerciseType((String) map.get("exerciseType"));
        userExercise.setMusclesWorked((String) map.get("musclesWorked"));
        userExercise.setLoadIntensity((String) map.get("loadIntensity"));
        userExercise.setImage((String) map.get("image"));
        userExercise.setDescription((String) map.get("description"));
        userExercise.setMovementType((String) map.get("movementType"));

        // 🔹 Convertir lista de Strings a lista de Muscle
        List<Muscle> synergistMuscles = new ArrayList<>();
        Object rawSynergistMuscles = map.get("synergistMuscles");
        if (rawSynergistMuscles instanceof List<?>) {
            for (Object obj : (List<?>) rawSynergistMuscles) {
                if (obj instanceof String) {
                    synergistMuscles.add(new Muscle((String) obj));
                } else if (obj instanceof Muscle) {
                    synergistMuscles.add((Muscle) obj);
                }
            }
        }
        List<String> synergistMuscleNames = new ArrayList<>();
        for (Muscle muscle : synergistMuscles) {
            synergistMuscleNames.add(muscle.getName());
        }
        userExercise.setSynergistMuscles(synergistMuscleNames);

        userExercise.setGoals(convertList(map.get("goals"), String.class));
        userExercise.setMovementPatterns(convertList(map.get("movementPatterns"), MovementPattern.class));
        userExercise.setCompleted(map.get("isCompleted") != null ? (Boolean) map.get("isCompleted") : false);
        userExercise.setMaxReps(map.get("maxReps") != null ? ((Long) map.get("maxReps")).intValue() : 0);

        //  SOLO currentLoad - sin fallback
        if (map.get("currentLoad") instanceof Long) {
            int value = ((Long) map.get("currentLoad")).intValue();
            Log.d(TAG, "convertMapToUserExercise:  currentLoad válido para " + userExercise.getName() + ": " + value);
            userExercise.setCurrentLoad(value);
        } else {
            Log.e(TAG, "convertMapToUserExercise:  currentLoad inválido o nulo para " + userExercise.getName());
            userExercise.setCurrentLoad(0);
        }

        // Familiaridad
        userExercise.setFamiliarity(map.get("familiarity") != null ? (String) map.get("familiarity") : "Desconocido");

        return userExercise;
    }




    private <T> List<T> convertList(Object obj, Class<T> clazz) {
        List<T> list = new ArrayList<>();

        if (obj instanceof List<?>) {
            for (Object item : (List<?>) obj) {
                if (clazz == Muscle.class && item instanceof String) {
                    // Convertir String a objeto Muscle
                    list.add(clazz.cast(new Muscle((String) item)));
                } else if (clazz.isInstance(item)) {
                    list.add(clazz.cast(item));
                }
            }
        }
        return list;
    }


    private void updateAllStatusButtonIcons() {
        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View exerciseView = containerLayout.getChildAt(i);
            ImageButton statusButton = exerciseView.findViewById(R.id.statusButton);
            TextView exerciseNameView = exerciseView.findViewById(R.id.exerciseName);
            String exerciseName = exerciseNameView.getText().toString();
            for (UserExercise userExercise : userExercises) {
                if (userExercise.getName().equals(exerciseName)) {
                    updateStatusButtonIcon(statusButton, userExercise);
                    break;
                }
            }
        }
    }

    private void updateStatusButtonIconForExercise(UserExercise userExercise) {
        for (int i = 0; i < containerLayout.getChildCount(); i++) {
            View exerciseView = containerLayout.getChildAt(i);
            TextView exerciseNameView = exerciseView.findViewById(R.id.exerciseName);
            String exerciseName = exerciseNameView.getText().toString();
            if (userExercise.getName().equals(exerciseName)) {
                ImageButton statusButton = exerciseView.findViewById(R.id.statusButton);
                updateStatusButtonIcon(statusButton, userExercise);
                break;
            }
        }
    }

    private void listExercises() {
        if (containerLayout == null) {
            Log.e(TAG, "AssignLoadFragment - listExercises: containerLayout es nulo");
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
            UserExercise userExercise = null;
            for (UserExercise ue : userExercises) {
                if (ue.getName().equals(exercise.getName())) {
                    userExercise = ue;
                    break;
                }
            }
            if (userExercise == null) {
                userExercise = new UserExercise();
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
            }

            // Configurar el icono inicial del botón según el estado del ejercicio
            updateStatusButtonIcon(statusButton, userExercise);

            final UserExercise finalUserExercise = userExercise;
            final ImageButton finalStatusButton = statusButton;

            statusButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "AssignLoadFragment - listExercises: Botón de estado clicado para " + finalUserExercise.getName());
                    // Cambiar el estado del ejercicio y actualizar el icono
                    toggleExerciseStatus(finalUserExercise);
                    updateStatusButtonIcon(finalStatusButton, finalUserExercise);
                }
            });

            exerciseView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "AssignLoadFragment - listExercises: Ejercicio clicado - " + finalUserExercise.getName());
                    // Abrir ExerciseDetailsLoadFragment al hacer clic en el ejercicio
                    openExerciseDetailsLoadFragment(finalUserExercise, finalStatusButton);
                }
            });

            containerLayout.addView(exerciseView);
        }
    }

    private void updateStatusButtonIcon(ImageButton button, UserExercise userExercise) {
        // Ejemplo: Basado en el valor de currentLoad
        if (userExercise.getCurrentLoad() > 0) {
            button.setImageResource(R.drawable.ic_completed_status);
            Log.d(TAG, "COMPLETED AssignLoadFragment - updateStatusButtonIcon: Icono actualizado para " + userExercise.getName());
            // Mostrar mensaje en la pantalla
            Toast.makeText(button.getContext(), "Información del ejercicio " + userExercise.getName() + " cargada con éxito", Toast.LENGTH_SHORT).show();
        } else {
            button.setImageResource(R.drawable.ic_default_status);
            Log.d(TAG, "DEFAULT AssignLoadFragment - updateStatusButtonIcon: Icono actualizado para " + userExercise.getName());
        }
        button.invalidate(); // Forzar la actualización de la vista


    }

    private void toggleExerciseStatus(UserExercise userExercise) {
        // Cambiar el estado del ejercicio
        userExercise.setCompleted(!userExercise.isCompleted());
        Log.d(TAG, "AssignLoadFragment - toggleExerciseStatus: Estado cambiado para " + userExercise.getName() + " a " + (userExercise.isCompleted() ? "completado" : "no completado"));
    }

    private void openExerciseDetailsLoadFragment(UserExercise userExercise, ImageButton statusButton) {
        Bundle data = new Bundle();
        data.putSerializable("userExercise", userExercise);

        ExerciseDetailsLoadFragment fragment = new ExerciseDetailsLoadFragment();
        fragment.setArguments(data);

        // Pasar la referencia del botón de estado al fragmento
        fragment.setStatusButton(statusButton);

        // Mostrar el fragmento como un diálogo
        fragment.show(getParentFragmentManager(), "ExerciseDetailsLoadFragment");
    }

    private void saveAssignedLoads() {
        Log.d(TAG, "AssignLoadFragment - saveAssignedLoads: Iniciando validación de datos de todos los ejercicios...");

        if (!areAllExercisesValid()) {
            Log.e(TAG, "AssignLoadFragment - saveAssignedLoads:  No se pueden guardar los datos. Hay ejercicios con información incompleta.");
            Toast.makeText(getContext(), "Completa todos los datos de los ejercicios antes de guardar.", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "AssignLoadFragment - saveAssignedLoads:  Todos los ejercicios están completos. Procediendo a guardar...");

        DatabaseReference userExercisesRef = mDatabase.child("users").child(userId).child("userExercises");

        for (UserExercise exercise : userExercises) {
            // 🔹 Buscar el ID del ejercicio en Firebase
            userExercisesRef.orderByChild("name").equalTo(exercise.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                            String exerciseKey = childSnapshot.getKey(); // 🔥 Obtiene el ID único en Firebase

                            if (exerciseKey != null) {
                                Map<String, Object> exerciseUpdates = new HashMap<>();

                                //  Agregar solo los nuevos datos sin eliminar los existentes
                                exerciseUpdates.put("currentLoad", exercise.getCurrentLoad());
                                exerciseUpdates.put("maxReps", exercise.getMaxReps());
                                exerciseUpdates.put("familiarity", exercise.getFamiliarity());
                                exerciseUpdates.put("completed", exercise.isCompleted());

                                //  Agregar o actualizar series de ejercicios sin eliminar el ejercicio completo
                                List<Map<String, Object>> seriesList = new ArrayList<>();
                                for (ExerciseSeries series : exercise.getExerciseSeriesList()) {
                                    Map<String, Object> seriesData = new HashMap<>();
                                    seriesData.put("seriesNumber", series.getSeriesNumber());
                                    seriesData.put("reps", series.getReps());
                                    seriesData.put("weight", series.getWeight());
                                    seriesData.put("rpe", series.getRpe());
                                    seriesList.add(seriesData);
                                }
                                exerciseUpdates.put("exerciseSeriesList", seriesList);

                                //  Usamos updateChildren para NO sobrescribir el ejercicio existente
                                userExercisesRef.child(exerciseKey).updateChildren(exerciseUpdates)
                                        .addOnSuccessListener(aVoid -> Log.d(TAG, " Datos actualizados para el ejercicio: " + exercise.getName()))
                                        .addOnFailureListener(e -> Log.e(TAG, " Error al actualizar el ejercicio: " + exercise.getName(), e));
                            }
                        }
                    } else {
                        Log.e(TAG, " No se encontró el ejercicio en Firebase: " + exercise.getName());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, " Error al buscar el ejercicio en Firebase: " + error.getMessage());
                }
            });
        }

        //  Actualizar el estado del formulario de entrenamiento
        DatabaseReference userRef = mDatabase.child("users").child(userId);
        userRef.child("trainingFormCompleted").setValue(true)
                .addOnSuccessListener(aVoid -> Log.d(TAG, " Estado del formulario actualizado"))
                .addOnFailureListener(e -> Log.e(TAG, " Error al actualizar el estado del formulario.", e));

        Log.d(TAG, "AssignLoadFragment - saveAssignedLoads:  Todos los ejercicios han sido guardados correctamente.");
        Toast.makeText(getContext(), "Cargas asignadas guardadas exitosamente", Toast.LENGTH_SHORT).show();

        //  Actualizar los iconos de estado
        updateAllStatusButtonIcons();
    }



    /**
     * ✅ Valida que todos los ejercicios tengan los datos completos.
     */
    private boolean areAllExercisesValid() {
        Log.d(TAG, "areAllExercisesValid: Tamaño de userExercises = " + userExercises.size());
        for (int i = 0; i < userExercises.size(); i++) {
            UserExercise ue = userExercises.get(i);
            Log.d(TAG, "➤ Index " + i + " = " + ue.getName() + " | currentLoad=" + ue.getCurrentLoad() +
                    " | maxReps=" + ue.getMaxReps() + " | familiarity=" + ue.getFamiliarity());
        }

        for (UserExercise exercise : userExercises) {
            if (!isExerciseValid(exercise)) {
                Log.e(TAG, "AssignLoadFragment - areAllExercisesValid: ❌ Ejercicio con datos incompletos: " + exercise.getName());
                return false;
            }
        }
        return true;
    }

    /**
     * ✅ Verifica si un solo ejercicio tiene todos los datos requeridos.
     */
    private boolean isExerciseValid(UserExercise exercise) {
        if (exercise == null) {
            Log.e(TAG, "AssignLoadFragment - isExerciseValid: ❌ Ejercicio es nulo.");
            return false;
        }

        Log.d(TAG, "🟡 Validando ejercicio: " + exercise.getName());
        Log.d(TAG, "    ➤ Peso actual (currentLoad): " + exercise.getCurrentLoad());
        Log.d(TAG, "    ➤ Repeticiones máximas (maxReps): " + exercise.getMaxReps());
        Log.d(TAG, "    ➤ Familiaridad: " + exercise.getFamiliarity());

        if (exercise.getName() == null || exercise.getName().trim().isEmpty()) {
            Log.e(TAG, "AssignLoadFragment - isExerciseValid: ❌ Falta el nombre del ejercicio.");
            return false;
        }

        if (exercise.getCurrentLoad() <= 0) {
            Log.e(TAG, "AssignLoadFragment - isExerciseValid: ❌ Falta asignar peso en " + exercise.getName() + " (Valor: " + exercise.getCurrentLoad() + ")");
            return false;
        }

        if (exercise.getMaxReps() <= 0) {
            Log.e(TAG, "AssignLoadFragment - isExerciseValid: ❌ Falta asignar repeticiones en " + exercise.getName() + " (Valor: " + exercise.getMaxReps() + ")");
            return false;
        }

        if (exercise.getFamiliarity() == null || exercise.getFamiliarity().trim().isEmpty()) {
            Log.e(TAG, "AssignLoadFragment - isExerciseValid: ❌ Falta asignar familiaridad en " + exercise.getName() + " (Valor: " + exercise.getFamiliarity() + ")");
            return false;
        }

        Log.d(TAG, "✅ Datos válidos para " + exercise.getName());
        return true;
    }


}