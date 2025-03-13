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
import java.util.List;
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
//        Spinner seriesSpinner = getView().findViewById(R.id.seriesSpinner);

        // Validar que los campos no estén vacíos
        if (!isExerciseDataValid(loadEditText, repsSpinner,familiaritySpinner)) {
            Log.e(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: ❌ Datos incompletos. No se guardará la información.");
            Toast.makeText(getContext(), "⚠️ Completa todos los campos antes de guardar.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Convertir valores ingresados
        int currentLoad = Integer.parseInt(loadEditText.getText().toString());
        int maxReps = repsSpinner.getSelectedItemPosition() + 1;
//        int series = seriesSpinner.getSelectedItemPosition() + 1;
        boolean isCompleted = familiaritySpinner.getSelectedItemPosition() == 3;
        String familiarity = familiaritySpinner.getSelectedItem().toString();

        // Asignar valores a `userExercise`
        userExercise.setCurrentLoad(currentLoad);
        userExercise.setMaxReps(maxReps);
//        userExercise.setSeries(series);
        userExercise.setCompleted(isCompleted);
        userExercise.setFamiliarity(familiarity);

        Log.d(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: ✅ Datos del ejercicio validados y asignados.");

        // Guardar en Firebase
        if (userId != null && mDatabase != null) {
            DatabaseReference exerciseRef = mDatabase.child("users").child(userId).child("userExercises").child(String.valueOf(userExercise.getId()));

            exerciseRef.child("currentLoad").setValue(currentLoad);
            exerciseRef.child("maxReps").setValue(maxReps);
//            exerciseRef.child("series").setValue(series);
            exerciseRef.child("isCompleted").setValue(isCompleted);
            exerciseRef.child("familiarity").setValue(familiarity)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: ✅ Datos guardados correctamente en Firebase.");
                        sharedViewModel.selectExercise(userExercise);
                    })
                    .addOnFailureListener(e -> Log.e(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: ❌ Error al guardar en Firebase", e));
        } else {
            Log.e(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: ❌ Error: Usuario no autenticado o referencia a Firebase nula.");
        }
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