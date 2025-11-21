package com.example.diets.fragments;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.diets.R;
import com.example.diets.activitys.ViewPagerActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;
public class UserFormFragment extends Fragment {

    private Spinner genderSpinner, activityLevelSpinner, goalSpinner, weightSpinner, heightSpinner, ageSpinner;
    private FloatingActionButton helpButton, goalHelpButton;
    private MaterialButton submitButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String TAG = "UserFormFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_user_form, container, false);

        // Inicializar Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Inicializar vistas
        genderSpinner = view.findViewById(R.id.genderSpinner);
        activityLevelSpinner = view.findViewById(R.id.activityLevelSpinner);
        goalSpinner = view.findViewById(R.id.goalSpinner);
        weightSpinner = view.findViewById(R.id.weightSpinner);
        heightSpinner = view.findViewById(R.id.heightSpinner);
        ageSpinner = view.findViewById(R.id.ageSpinner);
        helpButton = view.findViewById(R.id.helpButton);
        goalHelpButton = view.findViewById(R.id.goalHelpButton);
        submitButton = view.findViewById(R.id.submitButton);

        configureSpinners();

        // Mostrar el propósito del formulario
        showFormPurposeDialog();

        // Configuración de botones de ayuda
        helpButton.setOnClickListener(v -> showActivityLevelInfoDialog());
        goalHelpButton.setOnClickListener(v -> showGoalInfoDialog());

        // Configuración del botón Enviar
        submitButton.setOnClickListener(v -> handleSubmit());

        return view;
    }

    private void configureSpinners() {
        setSpinnerAdapter(genderSpinner, R.array.gender_options);
        setSpinnerAdapter(activityLevelSpinner, R.array.activity_level_options);
        setSpinnerAdapter(goalSpinner, R.array.goal_options);
        setSpinnerAdapter(weightSpinner, R.array.weight_options);
        setSpinnerAdapter(heightSpinner, R.array.height_options);
        setSpinnerAdapter(ageSpinner, R.array.age_options);
    }

    private void setSpinnerAdapter(Spinner spinner, int arrayResId) {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(), arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void showFormPurposeDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_form_purpose);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button closeButton = dialog.findViewById(R.id.closePurposeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showActivityLevelInfoDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_activity_level_info);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showGoalInfoDialog() {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_goal_info);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button closeButton = dialog.findViewById(R.id.closeGoalButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleSubmit() {
        Log.d(TAG, " handleSubmit() iniciado.");

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e(TAG, " Usuario no autenticado.");
            showErrorDialog("Usuario no autenticado.");
            return;
        }

        String userId = currentUser.getUid();
        Log.d(TAG, " ID del usuario: " + userId);

        String gender = genderSpinner.getSelectedItem().toString();
        String activityLevel = activityLevelSpinner.getSelectedItem().toString();
        String goal = goalSpinner.getSelectedItem().toString();
        String weight = weightSpinner.getSelectedItem().toString();
        String height = heightSpinner.getSelectedItem().toString();
        String age = ageSpinner.getSelectedItem().toString();

        Log.d(TAG, " Datos capturados:");
        Log.d(TAG, "    Género: " + gender);
        Log.d(TAG, "    Nivel de actividad: " + activityLevel);
        Log.d(TAG, "    Meta: " + goal);
        Log.d(TAG, "    Peso: " + weight);
        Log.d(TAG, "   Altura: " + height);
        Log.d(TAG, "    Edad: " + age);

        if (gender.isEmpty() || activityLevel.isEmpty() || goal.isEmpty()
                || weight.isEmpty() || height.isEmpty() || age.isEmpty()) {
            Log.e(TAG, " Campos vacíos detectados.");
            showErrorDialog("Por favor, completa todos los campos antes de enviar.");
            return;
        }

        // 🔎 AUDITORÍA: persistencia de perfil biométrico (sin cálculos de macros aquí)
        Log.i(TAG, "AUDIT_TRAIL: Guardando perfil biométrico. Peso: " + weight
                + " | Meta: " + goal + ". (Cálculo de macros delegado al Dashboard).");

        Map<String, Object> userData = new HashMap<>();
        userData.put("weight", Double.parseDouble(weight));
        userData.put("height", Double.parseDouble(height));
        userData.put("age", Integer.parseInt(age));
        userData.put("gender", gender);
        userData.put("activityLevel", activityLevel);
        userData.put("goal", goal);
        userData.put("isFirstTime", false);  //  Esto se actualiza junto con los demás datos

        Log.d(TAG, " Datos preparados para guardar en Firebase.");

        //  Guardar en Realtime Database
        mDatabase.child("users").child(userId).updateChildren(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, " Datos guardados correctamente (incluido firstTime = false).");
                        showSuccessDialog("¡Formulario enviado exitosamente!");
                    } else {
                        Log.e(TAG, " Error al guardar en Realtime Database", task.getException());
                        showErrorDialog("Error al actualizar la información.");
                    }
                });

        Log.d(TAG, " handleSubmit() finalizado.");
    }



    private void showErrorDialog(String message) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_error);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView errorMessage = dialog.findViewById(R.id.errorMessage);
        Button closeButton = dialog.findViewById(R.id.closeErrorButton);
        errorMessage.setText(message);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSuccessDialog(String message) {
        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_success);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView successMessage = dialog.findViewById(R.id.successMessage);
        Button closeButton = dialog.findViewById(R.id.closeSuccessButton);
        successMessage.setText(message);
        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            requireActivity().startActivity(new Intent(requireActivity(), ViewPagerActivity.class));
            requireActivity().finish();
        });
        dialog.show();
    }
}
