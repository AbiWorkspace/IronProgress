package com.example.diets.activitys;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.diets.R;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.User;
import com.example.diets.model.UserMovementPattern;
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
import java.util.UUID;
//LISTO AUDITORIA??
public class MainActivity extends AppCompatActivity {
//ultima actualizacion del codigo
    private static final String TAG = "MainActivity";
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private Button registerButton;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "onCreate() llamado - iniciando configuración de MainActivity");

        // --- Referencias a la UI ---
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        Log.d(TAG, "Componentes de UI inicializados: emailEditText, passwordEditText, loginButton, registerButton");

        // --- Inicialización de Firebase Auth y Realtime Database ---
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        Log.d(TAG, "FirebaseAuth y DatabaseReference inicializados correctamente");

        // --- Verificación de sesión activa ---
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // El usuario ya tiene sesión activa, se intenta saltar el login
            Log.d(TAG, "Usuario ya autenticado al abrir la app. uid=" + currentUser.getUid());
            checkFirstTimeLogin(currentUser.getUid());
        } else {
            Log.d(TAG, "No hay usuario autenticado. Se muestra pantalla de login.");
        }

        // --- Listener del botón de inicio de sesión ---
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click en loginButton");

                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                Log.d(TAG, "Datos capturados desde UI. email=" + email + " (password no se imprime por seguridad)");

                // Validación local de los datos antes de ir a Firebase Auth
                if (validateInputs(email, password)) {
                    Log.d(TAG, "Validación de inputs exitosa. Intentando iniciar sesión con FirebaseAuth.");
                    loginUser(email, password);
                } else {
                    Log.d(TAG, "Validación de inputs fallida. No se llama a loginUser().");
                }
            }
        });

        // --- Listener del botón de registro ---
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "click en registerButton - navegando a RegisterActivity");
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });

        Log.d(TAG, "onCreate() finalizado - MainActivity lista para interacción del usuario");
    }


    private boolean validateInputs(String email, String password) {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Correo electrónico inválido");
            Log.d(TAG, "Correo electrónico inválido: " + email);
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("La contraseña debe tener al menos 6 caracteres");
            Log.d(TAG, "Contraseña inválida: " + password);
            return false;
        }
        return true;
    }

    private void loginUser(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.d(TAG, "Inicio de sesión exitoso: " + user.getUid());
                            checkFirstTimeLogin(user.getUid());
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Log.d(TAG, "Error en el inicio de sesión: " + errorMessage);
                        handleAuthError(errorMessage);
                    }
                });
    }

    private void handleAuthError(String errorMessage) {
        if (errorMessage.contains("blocked all requests")) {
            Toast.makeText(this, "Demasiados intentos fallidos. Intenta más tarde.", Toast.LENGTH_LONG).show();
        } else if (errorMessage.contains("The supplied auth credential is incorrect")) {
            Toast.makeText(this, "Correo o contraseña incorrectos.", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "Error en el inicio de sesión: " + errorMessage, Toast.LENGTH_LONG).show();
        }
    }

//Metodo que nos ayud a validar si es su primer inicio de sesion

    private void checkFirstTimeLogin(String userId) {
        Log.d(TAG, "Verificando primer inicio de sesión para el usuario: " + userId);

        mDatabase.child("users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Log.e(TAG, "No se encontraron datos del usuario.");
                            Toast.makeText(MainActivity.this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // -------- Campos para formularios (flujo normal) --------
                        String goal = snapshot.child("goal").getValue(String.class);

                        boolean trainingFormCompleted = snapshot.child("trainingFormCompleted").getValue(Boolean.class) != null
                                ? snapshot.child("trainingFormCompleted").getValue(Boolean.class) : false;

                        boolean foodFormCompleted = snapshot.child("firstTime").getValue(Boolean.class) != null
                                ? snapshot.child("firstTime").getValue(Boolean.class) : false;

                        // --------- Campos para lógica de coach ----------
                        String role = snapshot.child("role").getValue(String.class);
                        if (role == null) role = "user"; // por defecto

                        boolean coachCandidate = snapshot.child("coachCandidate").getValue(Boolean.class) != null
                                ? snapshot.child("coachCandidate").getValue(Boolean.class) : false;

                        Log.d(TAG, "role=" + role + "  coachCandidate=" + coachCandidate);
                        Log.d(TAG, "trainingFormCompleted=" + trainingFormCompleted + "  foodFormCompleted=" + foodFormCompleted);

                        // --------- Ruteo según tipo ----------
                        if ("coach".equalsIgnoreCase(role)) {
                            // Coach aprobado: ir a pantalla de coach (lista de usuarios)
                            Intent i = new Intent(MainActivity.this, UsersListActivity.class);
                            startActivity(i);
                            finish();
                            return;
                        }

                        if (coachCandidate) {
                            //  Solicitó ser coach pero aún no aprobado: pantalla de espera
                            Intent i = new Intent(MainActivity.this, CoachPendingActivity.class);
                            startActivity(i);
                            finish();
                            return;
                        }

                        // (opcional) admin si lo manejas en tu objeto User
                        // if (user.isAdmin()) { ... }

                        // 👤 Usuario normal: flujo habitual al ViewPager
                        Intent intent = new Intent(MainActivity.this, ViewPagerActivity.class);
                        intent.putExtra("trainingFormCompleted", trainingFormCompleted);
                        intent.putExtra("foodFormCompleted", foodFormCompleted);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error de base de datos: " + error.getMessage());
                        Toast.makeText(MainActivity.this, "Error de base de datos", Toast.LENGTH_SHORT).show();
                    }
                });
    }



}
