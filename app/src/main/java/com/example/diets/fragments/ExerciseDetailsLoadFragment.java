package com.example.diets.fragments;

import android.os.Bundle;
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

        // Inicializar el ViewModel compartido
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);

        // Obtener los datos del Bundle
        Bundle data = getArguments();
        if (data != null) {
            userExercise = (UserExercise) data.getSerializable("userExercise");
            Log.d(TAG, "ExerciseDetailsLoadFragment - onViewCreated: Datos de UserExercise recibidos - " + userExercise.toString());
            loadExerciseDetails(view);
        }

        // Configurar el botón de guardar
        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "ExerciseDetailsLoadFragment - onViewCreated: Botón de guardar clicado");
            saveExerciseDetails();
            dismiss(); // Cerrar el diálogo después de guardar
        });

        // Configurar el botón de regresar
        Button backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "ExerciseDetailsLoadFragment - onViewCreated: Botón de regresar clicado");
            dismiss(); // Cerrar el diálogo
        });

        // Inicializar Firebase
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userId = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference();
        } else {
            Log.e(TAG, "ExerciseDetailsLoadFragment - onViewCreated: Usuario no autenticado");
        }
    }

    private void loadExerciseDetails(View view) {
        Log.d(TAG, "ExerciseDetailsLoadFragment - loadExerciseDetails: Cargando detalles del ejercicio");
        TextView exerciseNameView = view.findViewById(R.id.exerciseName);
        TextInputEditText loadEditText = view.findViewById(R.id.loadEditText);
        Spinner repsSpinner = view.findViewById(R.id.repsSpinner);
        Spinner familiaritySpinner = view.findViewById(R.id.familiaritySpinner);

        exerciseNameView.setText(userExercise.getName());
        loadEditText.setText(String.valueOf(userExercise.getCurrentLoad()));

        // Configurar el Spinner de repeticiones
        ArrayAdapter<Integer> repsAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, getRepsOptions());
        repsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repsSpinner.setAdapter(repsAdapter);
        repsSpinner.setSelection(userExercise.getMaxReps() - 1); // Ajustar según el valor de repeticiones

        // Configurar el Spinner de familiaridad
        ArrayAdapter<CharSequence> familiarityAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.familiarity_options, android.R.layout.simple_spinner_item);
        familiarityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        familiaritySpinner.setAdapter(familiarityAdapter);

        // Establecer la selección inicial basada en el estado del ejercicio
        int familiarityPosition = userExercise.isCompleted() ? 3 : 0; // Ajusta según tus opciones
        familiaritySpinner.setSelection(familiarityPosition);

        Log.d(TAG, "ExerciseDetailsLoadFragment - loadExerciseDetails: Detalles del ejercicio cargados");
    }

    private List<Integer> getRepsOptions() {
        Log.d(TAG, "ExerciseDetailsLoadFragment - getRepsOptions: Generando opciones de repeticiones del 0 al 15");
        List<Integer> options = new ArrayList<>();
        for (int i = 0; i <= 15; i++) {
            options.add(i);
        }
        return options;
    }

    private void saveExerciseDetails() {
        Log.d(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Guardando detalles del ejercicio");

        // Obtener los valores de los Spinners y EditText
        TextInputEditText loadEditText = getView().findViewById(R.id.loadEditText);
        Spinner repsSpinner = getView().findViewById(R.id.repsSpinner);
        Spinner familiaritySpinner = getView().findViewById(R.id.familiaritySpinner);

        int currentLoad = Integer.parseInt(loadEditText.getText().toString());
        int maxReps = repsSpinner.getSelectedItemPosition() + 1;
        boolean isCompleted = familiaritySpinner.getSelectedItemPosition() == 3;
        String familiarity = familiaritySpinner.getSelectedItem().toString(); // Obtener el valor de familiaridad

        // Actualizar el objeto UserExercise
        userExercise.setCurrentLoad(currentLoad);
        userExercise.setMaxReps(maxReps);
        userExercise.setCompleted(isCompleted);
        userExercise.setFamiliarity(familiarity); // Establecer familiaridad

        // Guardar en Firebase
        if (userId != null && mDatabase != null) {
            DatabaseReference exerciseRef = mDatabase.child("users").child(userId).child("userExercises").child(String.valueOf(userExercise.getId()));

            // Verificar si el ejercicio ya existe
            exerciseRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        // El ejercicio ya existe, actualizar los campos
                        exerciseRef.child("currentLoad").setValue(currentLoad);
                        exerciseRef.child("maxReps").setValue(maxReps);
                        exerciseRef.child("isCompleted").setValue(isCompleted);
                        exerciseRef.child("familiarity").setValue(familiarity)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Detalles del ejercicio actualizados exitosamente");
                                    // Actualizar el icono del botón de estado
                                    if (userExercise.isCompleted()== false) {
                                        Log.d(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Estatus actualizado a: " + userExercise.isCompleted());
                                        setIconCompleted(statusButton, userExercise);
                                    }
                                    // Actualizar el ViewModel compartido
                                    sharedViewModel.selectExercise(userExercise);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Error al actualizar los detalles del ejercicio", e));
                    } else {
                        // El ejercicio no existe, crear uno nuevo
                        exerciseRef.setValue(userExercise)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Detalles del ejercicio guardados exitosamente");
                                    // Actualizar el icono del botón de estado
                                    if (statusButton != null) {
                                        updateStatusButtonIcon(statusButton, userExercise);
                                    }
                                    // Actualizar el ViewModel compartido
                                    sharedViewModel.selectExercise(userExercise);
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Error al guardar los detalles del ejercicio", e));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Error al verificar la existencia del ejercicio", error.toException());
                }
            });
        } else {
            Log.e(TAG, "ExerciseDetailsLoadFragment - saveExerciseDetails: Usuario no autenticado o referencia a la base de datos nula");
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