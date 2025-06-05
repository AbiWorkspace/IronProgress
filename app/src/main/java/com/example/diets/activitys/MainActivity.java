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

public class MainActivity extends AppCompatActivity {

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

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        loginButton = findViewById(R.id.loginButton);
        registerButton = findViewById(R.id.registerButton);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();


        // Verifica si el usuario ya está autenticado
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Usuario ya autenticado: " + currentUser.getUid());
            checkFirstTimeLogin(currentUser.getUid());
        }

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (validateInputs(email, password)) {
                    Log.d(TAG, "Intentando iniciar sesión con: " + email);
                    loginUser(email, password);
                }
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, RegisterActivity.class));
            }
        });
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


    private void checkFirstTimeLogin(String userId) {
        Log.d(TAG, "Verificando primer inicio de sesión para el usuario: " + userId);
        mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Log.d(TAG, "onDataChange llamado");

                if (snapshot.exists()) {
                    Log.d(TAG, "Snapshot existe para el usuario: " + userId);

                    // 🔍 Obtener datos básicos
                    String goal = snapshot.child("goal").getValue(String.class);

                    boolean trainingFormCompleted = snapshot.child("trainingFormCompleted").getValue(Boolean.class) != null
                            ? snapshot.child("trainingFormCompleted").getValue(Boolean.class) : false;

                    boolean foodFormCompleted = snapshot.child("firstTime").getValue(Boolean.class) != null
                            ? snapshot.child("firstTime").getValue(Boolean.class) : false;

                    Log.d(TAG, "📌 trainingFormCompleted: " + trainingFormCompleted);
                    Log.d(TAG, "📌 foodFormCompleted: " + foodFormCompleted);

                    // 🔥 Deserializar `userMovementPatterns` manualmente
                    Map<String, MovementPattern> movementPatterns = new HashMap<>();
                    DataSnapshot movementPatternsSnapshot = snapshot.child("userMovementPatterns");

                    if (movementPatternsSnapshot.exists()) {
                        for (DataSnapshot patternSnapshot : movementPatternsSnapshot.getChildren()) {
                            MovementPattern pattern = patternSnapshot.getValue(MovementPattern.class);
                            if (pattern != null) {
                                movementPatterns.put(patternSnapshot.getKey(), pattern);
                            }
                        }
                    }

                    // ✅ Convertir `MovementPattern` a `UserMovementPattern`
                    List<UserMovementPattern> userMovementPatternList = new ArrayList<>();
                    for (MovementPattern pattern : movementPatterns.values()) {
                        UserMovementPattern userPattern = new UserMovementPattern();
                        userPattern.setName(pattern.getName());
                        userPattern.setUserExercises(new ArrayList<>()); // Inicializamos la lista de ejercicios
                        userMovementPatternList.add(userPattern);
                    }

                    // ✅ Crear objeto `User` manualmente
                    User user = new User();
                    user.setGoal(goal);
                    user.setTrainingFormCompleted(trainingFormCompleted);
                    Map<String, UserMovementPattern> userMovementPatternMap = new HashMap<>();
                    for (UserMovementPattern pattern : userMovementPatternList) {
                        String id = pattern.getId() != null && !pattern.getId().isEmpty()
                                ? pattern.getId()
                                : UUID.randomUUID().toString(); // Generar ID si no hay

                        userMovementPatternMap.put(id, pattern);
                    }
                    user.setUserMovementPatterns(userMovementPatternMap);



                    Log.d(TAG, "✅ Datos del usuario obtenidos correctamente.");

                    // 🔥 Redirección según tipo de usuario
                    if (user.isAdmin()) {
                        Log.d(TAG, "Usuario es administrador, redirigiendo a AdminActivity");
                        startActivity(new Intent(MainActivity.this, AdminActivity.class));
                    } else {
                        Log.d(TAG, "Redirigiendo al dashboard");

                        // 🔥 Enviar flags de formularios a ViewPagerActivity
                        Intent intent = new Intent(MainActivity.this, ViewPagerActivity.class);
                        intent.putExtra("trainingFormCompleted", trainingFormCompleted);
                        intent.putExtra("foodFormCompleted", foodFormCompleted);
                        startActivity(intent);
                    }
                    finish();
                } else {
                    Log.e(TAG, "❌ No se encontraron datos del usuario.");
                    Toast.makeText(MainActivity.this, "Error al obtener datos del usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error de base de datos: " + error.getMessage());
                Toast.makeText(MainActivity.this, "Error de base de datos", Toast.LENGTH_SHORT).show();
            }
        });
    }


}
