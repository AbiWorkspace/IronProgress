package com.example.diets.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.diets.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private Button registerButton, takePhotoButton;
    private ImageView profileImageView;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private Bitmap photoBitmap; // Bitmap para almacenar la foto tomada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeUI();
        setupPhotoCapture();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        registerButton.setOnClickListener(v -> {
            String firstName = firstNameEditText.getText().toString().trim();
            String lastName = lastNameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            if (validateInputs(firstName, lastName, email, password)) {
                registerUser(firstName, lastName, email, password);
            }
        });
    }
    private void writeNewUser(String userId, String firstName, String lastName, String email, Bitmap photoBitmap) {
        DatabaseReference userRef = mDatabase.child("users").child(userId);

        String encodedImage = (photoBitmap != null) ? encodeBitmapToBase64(photoBitmap) : "default";
        Log.d(TAG, "Encoded image: " + encodedImage);

        Map<String, Object> userUpdates = new HashMap<>();
        userUpdates.put("firstName", firstName);
        userUpdates.put("lastName", lastName);
        userUpdates.put("email", email);
        userUpdates.put("profilePhoto", encodedImage);
        userUpdates.put("isFirstTime", true);

        userRef.updateChildren(userUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Usuario registrado exitosamente", Toast.LENGTH_SHORT).show();

                        // Redirección a MainActivity
                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        Log.e(TAG, "Error al guardar el usuario: " + task.getException());
                        Toast.makeText(this, "Error al guardar el usuario", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error crítico al guardar los datos: " + e.getMessage());
                    Toast.makeText(this, "Error crítico al guardar los datos", Toast.LENGTH_LONG).show();
                });
    }

    private void initializeUI() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        registerButton = findViewById(R.id.registerButton);
        takePhotoButton = findViewById(R.id.takePhotoButton);
        profileImageView = findViewById(R.id.profileImageView);
    }

    private void setupPhotoCapture() {
        takePhotoButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 2);
            } else {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 101);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            photoBitmap = (Bitmap) data.getExtras().get("data");
            if (photoBitmap != null) {
                profileImageView.setImageBitmap(photoBitmap);
            } else {
                Log.e(TAG, "Bitmap is null after capturing the photo.");
            }
        }
    }

    private boolean validateInputs(String firstName, String lastName, String email, String password) {
        if (firstName.isEmpty() || !firstName.matches("[a-zA-Z]+")) {
            firstNameEditText.setError("Nombre requerido y no puede contener caracteres especiales ni números");
            return false;
        }
        if (lastName.isEmpty() || !lastName.matches("[a-zA-Z]+")) {
            lastNameEditText.setError("Apellido requerido y no puede contener caracteres especiales ni números");
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Correo electrónico inválido");
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("La contraseña debe tener al menos 6 caracteres");
            return false;
        }
        return true;
    }

    private void registerUser(String firstName, String lastName, String email, String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            writeNewUser(userId, firstName, lastName, email, photoBitmap);
                        }
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Error desconocido";
                        Log.e(TAG, "Error en el registro: " + errorMessage);
                        Toast.makeText(RegisterActivity.this, "Error en el registro: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }



    private String encodeBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null, cannot encode.");
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}
