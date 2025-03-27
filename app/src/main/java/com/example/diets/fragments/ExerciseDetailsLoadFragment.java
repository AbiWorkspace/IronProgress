package com.example.diets.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;



import com.example.diets.R;
import com.example.diets.activitys.SharedViewModel;
import com.example.diets.model.UserExercise;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

public class ExerciseDetailsLoadFragment extends DialogFragment {
    private static final String TAG = "ExerciseDetailsLoadFragment";
    private UserExercise userExercise;
    private DatabaseReference mDatabase;
    private String userId;
    private ImageButton statusButton; // Añadir campo para el botón de estado
    private SharedViewModel sharedViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "ExerciseDetailsLoadFragment - onCreateView: Inflando el diseño para ExerciseDetailsLoadFragment");
        return inflater.inflate(R.layout.fragment_exercise_details_load, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d(TAG, "ExerciseDetailsLoadFragment - onViewCreated: Vista creada para ExerciseDetailsLoadFragment");

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        Bundle data = getArguments();
        if (data != null) {
            userExercise = (UserExercise) data.getSerializable("userExercise");
            Log.d(TAG, "Datos de UserExercise recibidos: " + userExercise.toString());
            loadExerciseDetails(view);
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "Guardando detalles del ejercicio...");
            saveExerciseDetails();
            dismiss();
        });

        Button backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "Regresando...");
            dismiss();
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference();
        } else {
            Log.e(TAG, "Usuario no autenticado");
        }
    }

    private void loadExerciseDetails(View view) {
        Log.d(TAG, "Cargando detalles del ejercicio...");

        TextView exerciseNameView = view.findViewById(R.id.exerciseName);
        TextInputEditText loadEditText = view.findViewById(R.id.loadEditText);
        Spinner repsSpinner = view.findViewById(R.id.repsSpinner);
        Spinner familiaritySpinner = view.findViewById(R.id.familiaritySpinner);
//        Spinner seriesSpinner = view.findViewById(R.id.seriesSpinner); // ✅ NUEVO SPINNER

        exerciseNameView.setText(userExercise.getName());
        loadEditText.setText(String.valueOf(userExercise.getCurrentLoad()));

        // Configurar Spinner de repeticiones
        ArrayAdapter<Integer> repsAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, getRepsOptions());
        repsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repsSpinner.setAdapter(repsAdapter);
        repsSpinner.setSelection(userExercise.getMaxReps());

        // Configurar Spinner de familiaridad
        ArrayAdapter<CharSequence> familiarityAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.familiarity_options, android.R.layout.simple_spinner_item);
        familiarityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        familiaritySpinner.setAdapter(familiarityAdapter);
        familiaritySpinner.setSelection(userExercise.isCompleted() ? 3 : 0);

        // ✅ Configurar Spinner de Series
        ArrayAdapter<Integer> seriesAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, getSeriesOptions());
        seriesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        seriesSpinner.setAdapter(seriesAdapter);
//        seriesSpinner.setSelection(userExercise.getSeries() - 1); // Ajustar selección

        Log.d(TAG, "Detalles del ejercicio cargados correctamente.");
    }

    private List<Integer> getRepsOptions() {
        List<Integer> options = new ArrayList<>();
        for (int i = 0; i <= 15; i++) {
            options.add(i);
        }
        return options;
    }

    // ✅ Método para obtener opciones de series (1 a 6)
    private List<Integer> getSeriesOptions() {
        List<Integer> options = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            options.add(i);
        }
        return options;
    }

    private void saveExerciseDetails() {
        Log.d(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Iniciando validación de datos del ejercicio...");

        // Obtener referencias a los campos
        TextInputEditText loadEditText = getView().findViewById(R.id.loadEditText);
        Spinner repsSpinner = getView().findViewById(R.id.repsSpinner);
        Spinner familiaritySpinner = getView().findViewById(R.id.familiaritySpinner);

        if (!isExerciseDataValid(loadEditText, repsSpinner, familiaritySpinner)) {
            Log.e(TAG, "❌ Datos incompletos. No se guardará la información.");
            showToast("⚠️ Completa todos los campos antes de guardar.");
            return;
        }

        int currentLoad = Integer.parseInt(loadEditText.getText().toString());
        int maxReps = repsSpinner.getSelectedItemPosition() + 1;
        boolean isCompleted = familiaritySpinner.getSelectedItemPosition() == 3;
        String familiarity = familiaritySpinner.getSelectedItem().toString();

        userExercise.setCurrentLoad(currentLoad);
        userExercise.setMaxReps(maxReps);
        userExercise.setCompleted(isCompleted);
        userExercise.setFamiliarity(familiarity);

        Log.d(TAG, "✅ Datos validados y asignados al objeto local.");

        if (userId == null || mDatabase == null) {
            Log.e(TAG, "❌ Error: Usuario no autenticado o referencia a Firebase nula.");
            return;
        }

        DatabaseReference userExercisesRef = mDatabase.child("users").child(userId).child("userExercises");

        userExercisesRef.orderByChild("name").equalTo(userExercise.getName()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                        String exerciseKey = childSnapshot.getKey();

                        if (exerciseKey != null) {
                            Map<String, Object> exerciseUpdates = new HashMap<>();
                            exerciseUpdates.put("currentLoad", currentLoad);
                            exerciseUpdates.put("maxReps", maxReps);
                            exerciseUpdates.put("isCompleted", isCompleted);
                            exerciseUpdates.put("familiarity", familiarity);

                            userExercisesRef.child(exerciseKey).updateChildren(exerciseUpdates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "✅ Datos actualizados correctamente para: " + userExercise.getName());
                                        sharedViewModel.selectExercise(userExercise);
                                        showToast("✅ Datos guardados correctamente");
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "❌ Error al actualizar el ejercicio en Firebase: " + userExercise.getName(), e);
                                        showToast("❌ Error al guardar los datos.");
                                    });
                        }
                    }
                } else {
                    Log.e(TAG, "❌ No se encontró el ejercicio en Firebase: " + userExercise.getName());
                    showToast("❌ No se encontró el ejercicio en la base de datos.");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al buscar el ejercicio en Firebase: " + error.getMessage());
            }
        });
    }


    /**
     * Verifica si todos los datos del ejercicio están completos antes de guardarlos.
     */
    private boolean isExerciseDataValid(TextInputEditText loadEditText, Spinner repsSpinner,  Spinner familiaritySpinner) {
        if (loadEditText == null || repsSpinner == null  || familiaritySpinner == null) {
            Log.e(TAG, "ExerciseDetailsLoadFragment - isExerciseDataValid: ❌ Un spinner o campo de entrada es nulo.");
            return false;
        }

        if (TextUtils.isEmpty(loadEditText.getText())) {
            Log.e(TAG, "ExerciseDetailsLoadFragment - isExerciseDataValid: ❌ El campo de peso está vacío.");
            return false;
        }

        try {
            int loadValue = Integer.parseInt(loadEditText.getText().toString());
            if (loadValue <= 0) {
                Log.e(TAG, "ExerciseDetailsLoadFragment - isExerciseDataValid: ❌ El peso debe ser mayor a 0.");
                return false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "ExerciseDetailsLoadFragment - isExerciseDataValid: ❌ Error al convertir el peso a número.", e);
            return false;
        }

        if (repsSpinner.getSelectedItemPosition() == 0) {
            Log.e(TAG, "ExerciseDetailsLoadFragment - isExerciseDataValid: ❌ No se ha seleccionado el número de repeticiones.");
            return false;
        }

//        if (seriesSpinner.getSelectedItemPosition() == 0) {
//            Log.e(TAG, "ExerciseDetailsLoadFragment - isExerciseDataValid: ❌ No se ha seleccionado el número de series.");
//            return false;
//        }



        Log.d(TAG, "ExerciseDetailsLoadFragment - isExerciseDataValid: ✅ Todos los datos están completos.");
        return true;
    }
    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "❌ No se puede mostrar el Toast: El fragmento no está adjunto.");
        }
    }

    // Método para establecer el botón de estado desde AssignLoadFragment
    public void setStatusButton(ImageButton statusButton) {
        this.statusButton = statusButton;
    }
    // Método para actualizar el icono del botón de estado
    private void updateStatusButtonIcon(ImageButton button, UserExercise userExercise) {
        if (userExercise.isCompleted()) {
            button.setImageResource(R.drawable.ic_completed_status);
            Log.d(TAG, "COMPLETED ExerciseDetailsLoadFragment - updateStatusButtonIcon: Icono actualizado para " + userExercise.getName());
        } else {
            button.setImageResource(R.drawable.ic_default_status);
            Log.d(TAG, "DEFAULT ExerciseDetailsLoadFragment - updateStatusButtonIcon: Icono actualizado para " + userExercise.getName());
        }
        button.invalidate(); // Forzar la actualización de la vista
    }

    private void setIconCompleted(ImageButton button, UserExercise userExercise) {

            button.setImageResource(R.drawable.ic_completed_status);
            Log.d(TAG, "COMPLETED ExerciseDetailsLoadFragment - updateStatusButtonIcon: Icono actualizado para " + userExercise.getName());


        button.invalidate(); // Forzar la actualización de la vista
    }
}