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
    private ImageButton statusButton;
    private SharedViewModel sharedViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflando el diseño para ExerciseDetailsLoadFragment");
        return inflater.inflate(R.layout.fragment_exercise_details_load, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onViewCreated: Iniciando configuración de vista y lógica del fragmento");
        super.onViewCreated(view, savedInstanceState);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        Log.d(TAG, "onViewCreated: ViewModel compartido obtenido");

        Bundle data = getArguments();
        if (data != null) {
            Log.d(TAG, "onViewCreated: Bundle recibido con datos");
            userExercise = (UserExercise) data.getSerializable("userExercise");
            Log.d(TAG, "onViewCreated: userExercise obtenido: " + userExercise);
            loadExerciseDetails(view);
        } else {
            Log.w(TAG, "onViewCreated: No se recibió bundle con datos");
        }

        Button saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated - saveButton: Clic en botón Guardar");
            saveExerciseDetails();
            dismiss();
        });

        Button backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated - backButton: Clic en botón Volver");
            dismiss();
        });

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "onViewCreated: Usuario autenticado con UID: " + currentUser.getUid());
            userId = currentUser.getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference();
        } else {
            Log.e(TAG, "onViewCreated: Usuario no autenticado");
        }
    }

    private void loadExerciseDetails(View view) {
        Log.d(TAG, "loadExerciseDetails: Cargando y mostrando los detalles del ejercicio");

        TextView exerciseNameView = view.findViewById(R.id.exerciseName);
        TextInputEditText loadEditText = view.findViewById(R.id.loadEditText);
        Spinner repsSpinner = view.findViewById(R.id.repsSpinner);
        Spinner familiaritySpinner = view.findViewById(R.id.familiaritySpinner);

        exerciseNameView.setText(userExercise.getName());
        loadEditText.setText(String.valueOf(userExercise.getCurrentLoad()));
        Log.d(TAG, "loadExerciseDetails: Nombre y carga actual asignados");

        ArrayAdapter<Integer> repsAdapter = new ArrayAdapter<>(getContext(),
                android.R.layout.simple_spinner_item, getRepsOptions());
        repsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        repsSpinner.setAdapter(repsAdapter);
        repsSpinner.setSelection(userExercise.getMaxReps());
        Log.d(TAG, "loadExerciseDetails: Spinner de repeticiones configurado");

        ArrayAdapter<CharSequence> familiarityAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.familiarity_options, android.R.layout.simple_spinner_item);
        familiarityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        familiaritySpinner.setAdapter(familiarityAdapter);
        familiaritySpinner.setSelection(userExercise.isCompleted() ? 3 : 0);
        Log.d(TAG, "loadExerciseDetails: Spinner de familiaridad configurado");

        Log.d(TAG, "loadExerciseDetails: Detalles del ejercicio cargados correctamente");
    }

    private List<Integer> getRepsOptions() {
        Log.d(TAG, "getRepsOptions: Generando lista de repeticiones");
        List<Integer> options = new ArrayList<>();
        for (int i = 0; i <= 15; i++) {
            options.add(i);
        }
        return options;
    }

    private List<Integer> getSeriesOptions() {
        Log.d(TAG, "getSeriesOptions: Generando lista de series");
        List<Integer> options = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            options.add(i);
        }
        return options;
    }

    private void saveExerciseDetails() {
        Log.d(TAG, "saveExerciseDetails: 🚀 Iniciando guardado de datos del ejercicio");

        TextInputEditText loadEditText = getView().findViewById(R.id.loadEditText);
        Spinner repsSpinner = getView().findViewById(R.id.repsSpinner);
        Spinner familiaritySpinner = getView().findViewById(R.id.familiaritySpinner);

        if (!isExerciseDataValid(loadEditText, repsSpinner, familiaritySpinner)) {
            Log.w(TAG, "saveExerciseDetails: ❌ Validación fallida - campos incompletos");
            showToast("⚠️ Completa todos los campos antes de guardar.");
            return;
        }

        int currentLoad = Integer.parseInt(loadEditText.getText().toString());
        int maxReps = repsSpinner.getSelectedItemPosition() + 1;
        boolean isCompleted = familiaritySpinner.getSelectedItemPosition() == 3;
        String familiarity = familiaritySpinner.getSelectedItem().toString();

        Log.d(TAG, "saveExerciseDetails: 📥 Valores del formulario:");
        Log.d(TAG, "    ➤ currentLoad: " + currentLoad);
        Log.d(TAG, "    ➤ maxReps: " + maxReps);
        Log.d(TAG, "    ➤ isCompleted: " + isCompleted);
        Log.d(TAG, "    ➤ familiarity: " + familiarity);

        userExercise.setCurrentLoad(currentLoad);
        userExercise.setMaxReps(maxReps);
        userExercise.setCompleted(isCompleted);
        userExercise.setFamiliarity(familiarity);

        Log.d(TAG, "saveExerciseDetails: ✅ Datos asignados a userExercise: " + userExercise.getName());

        // 🔎 AUDITORÍA: antes de enviar a Firebase, dejamos huella de la carga inicial
        Log.i(TAG, "AUDIT_TRAIL: Guardando carga inicial para ejercicio: " + userExercise.getName()
                + " | Carga: " + currentLoad + " | Reps: " + maxReps);

        if (userId == null || mDatabase == null) {
            Log.e(TAG, "saveExerciseDetails: ❌ UID o referencia Firebase no disponible");
            return;
        }

        DatabaseReference userExercisesRef = mDatabase.child("users").child(userId).child("userExercises");
        Log.d(TAG, "saveExerciseDetails: 🔍 Consultando ejercicio por nombre en Firebase (name=" + userExercise.getName() + ")");

        userExercisesRef.orderByChild("name").equalTo(userExercise.getName())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Log.d(TAG, "saveExerciseDetails - onDataChange: 📦 Datos recibidos desde Firebase");

                        if (snapshot.exists()) {
                            Log.d(TAG, "saveExerciseDetails - onDataChange: ✅ Ejercicio encontrado en Firebase");
                            for (DataSnapshot childSnapshot : snapshot.getChildren()) {
                                String exerciseKey = childSnapshot.getKey();
                                Log.d(TAG, "saveExerciseDetails - onDataChange: ➤ Firebase Key del ejercicio: " + exerciseKey);

                                if (exerciseKey != null) {
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("currentLoad", currentLoad);
                                    updates.put("maxReps", maxReps);
                                    updates.put("isCompleted", isCompleted);
                                    updates.put("familiarity", familiarity);

                                    Log.d(TAG, "saveExerciseDetails - onDataChange: 🔧 Datos a guardar: " + updates.toString());

                                    userExercisesRef.child(exerciseKey).updateChildren(updates)
                                            .addOnSuccessListener(aVoid -> {
                                                Log.d(TAG, "saveExerciseDetails - onSuccess: ✅ Datos guardados exitosamente para " + userExercise.getName());
                                                sharedViewModel.selectExercise(userExercise);
                                                showToast("✅ Datos guardados correctamente");
                                            })
                                            .addOnFailureListener(e -> {
                                                Log.e(TAG, "saveExerciseDetails - onFailure: ❌ Error al guardar en Firebase", e);
                                                showToast("❌ Error al guardar los datos.");
                                            });
                                } else {
                                    Log.e(TAG, "saveExerciseDetails - onDataChange: ❌ Firebase Key nula para el ejercicio");
                                }
                            }
                        } else {
                            Log.w(TAG, "saveExerciseDetails - onDataChange: ⚠️ No se encontró el ejercicio con nombre: " + userExercise.getName());
                            showToast("❌ No se encontró el ejercicio en la base de datos.");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "saveExerciseDetails - onCancelled: ❌ Error de Firebase - " + error.getMessage());
                    }
                });
    }


    private boolean isExerciseDataValid(TextInputEditText loadEditText, Spinner repsSpinner, Spinner familiaritySpinner) {
        Log.d(TAG, "isExerciseDataValid: Validando campos del formulario");

        if (loadEditText == null || repsSpinner == null || familiaritySpinner == null) {
            Log.e(TAG, "isExerciseDataValid: ❌ Un campo es nulo");
            return false;
        }

        if (TextUtils.isEmpty(loadEditText.getText())) {
            Log.e(TAG, "isExerciseDataValid: ❌ El campo de carga está vacío");
            return false;
        }

        try {
            int load = Integer.parseInt(loadEditText.getText().toString());
            if (load <= 0) {
                Log.e(TAG, "isExerciseDataValid: ❌ El peso debe ser mayor que 0");
                return false;
            }
        } catch (NumberFormatException e) {
            Log.e(TAG, "isExerciseDataValid: ❌ Error al convertir la carga", e);
            return false;
        }

        if (repsSpinner.getSelectedItemPosition() == 0) {
            Log.e(TAG, "isExerciseDataValid: ❌ Repeticiones no seleccionadas");
            return false;
        }

        Log.d(TAG, "isExerciseDataValid: ✅ Validación completada correctamente");
        return true;
    }

    private void showToast(String message) {
        if (getActivity() != null) {
            Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
        } else {
            Log.e(TAG, "showToast: ❌ No se puede mostrar el toast, fragmento no adjunto");
        }
    }

    public void setStatusButton(ImageButton statusButton) {
        Log.d(TAG, "setStatusButton: Botón de estado asignado desde fragmento externo");
        this.statusButton = statusButton;
    }
}
