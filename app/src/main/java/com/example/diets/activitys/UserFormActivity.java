package com.example.diets.activitys;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.diets.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class UserFormActivity extends AppCompatActivity {

    private Spinner genderSpinner;
    private Spinner activityLevelSpinner;
    private Spinner goalSpinner;
    private Spinner weightSpinner;
    private Spinner heightSpinner;
    private Spinner ageSpinner;
    private FloatingActionButton helpButton;
    private FloatingActionButton goalHelpButton;
    private MaterialButton submitButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_form);

        // Inicializar Firebase Auth, Database y Firestore
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        db = FirebaseFirestore.getInstance();

        // Inicializar vistas
        genderSpinner = findViewById(R.id.genderSpinner);
        activityLevelSpinner = findViewById(R.id.activityLevelSpinner);
        goalSpinner = findViewById(R.id.goalSpinner);
        weightSpinner = findViewById(R.id.weightSpinner);
        heightSpinner = findViewById(R.id.heightSpinner);
        ageSpinner = findViewById(R.id.ageSpinner);
        helpButton = findViewById(R.id.helpButton);
        goalHelpButton = findViewById(R.id.goalHelpButton);
        submitButton = findViewById(R.id.submitButton);

        configureSpinners();

        // Mostrar el propósito del formulario
        showFormPurposeDialog();

        // Configuración de botones de ayuda
        helpButton.setOnClickListener(v -> showActivityLevelInfoDialog());
        goalHelpButton.setOnClickListener(v -> showGoalInfoDialog());

        // Configuración del botón Enviar
        submitButton.setOnClickListener(v -> handleSubmit());
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
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, arrayResId, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    private void showFormPurposeDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_form_purpose);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button closeButton = dialog.findViewById(R.id.closePurposeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showActivityLevelInfoDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_activity_level_info);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button closeButton = dialog.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showGoalInfoDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_goal_info);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Button closeButton = dialog.findViewById(R.id.closeGoalButton);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void handleSubmit() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            showErrorDialog("Usuario no autenticado.");
            return;
        }

        String userId = currentUser.getUid();
        String gender = genderSpinner.getSelectedItem().toString();
        String activityLevel = activityLevelSpinner.getSelectedItem().toString();
        String goal = goalSpinner.getSelectedItem().toString();
        String weight = weightSpinner.getSelectedItem().toString();
        String height = heightSpinner.getSelectedItem().toString();
        String age = ageSpinner.getSelectedItem().toString();

        if (gender.isEmpty() || activityLevel.isEmpty() || goal.isEmpty() || weight.isEmpty() || height.isEmpty() || age.isEmpty()) {
            showErrorDialog("Por favor, completa todos los campos antes de enviar.");
            return;
        }

        Map<String, Object> userData = new HashMap<>();
        userData.put("weight", Double.parseDouble(weight));
        userData.put("height", Double.parseDouble(height));
        userData.put("age", Integer.parseInt(age));
        userData.put("gender", gender);
        userData.put("activityLevel", activityLevel);
        userData.put("goal", goal);
        userData.put("firstTime", false);

        // Actualizar en Firestore
        db.collection("users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> Log.d("UserFormActivity", "Datos enviados a Firestore"))
                .addOnFailureListener(e -> Log.e("UserFormActivity", "Error al enviar datos a Firestore", e));

        // Actualizar en Realtime Database
        mDatabase.child("users").child(userId).setValue(userData)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showSuccessDialog("¡Formulario enviado exitosamente!");
                    } else {
                        showErrorDialog("Error al actualizar la información.");
                    }
                });
    }

    private void showErrorDialog(String message) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_error);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView errorMessage = dialog.findViewById(R.id.errorMessage);
        Button closeButton = dialog.findViewById(R.id.closeErrorButton);
        errorMessage.setText(message);
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void showSuccessDialog(String message) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_success);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        TextView successMessage = dialog.findViewById(R.id.successMessage);
        Button closeButton = dialog.findViewById(R.id.closeSuccessButton);
        successMessage.setText(message);
        closeButton.setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(UserFormActivity.this, ViewPagerActivity.class));
            finish();
        });
        dialog.show();
    }
}
