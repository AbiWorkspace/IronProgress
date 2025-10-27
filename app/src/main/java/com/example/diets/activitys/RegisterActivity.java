package com.example.diets.activitys;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;
import android.view.View; // ⬅️ para setVisibility
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.diets.R;
import com.google.android.material.checkbox.MaterialCheckBox;              // ⬅️ Material
import com.google.android.material.textfield.TextInputLayout;           // ⬅️ Material

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // UI
    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private Button registerButton, takePhotoButton;
    private ImageView profileImageView;

    // Coach UI
    private MaterialCheckBox chkCoachRequest;   // ⬅️ tipo correcto para MaterialCheckBox
    private EditText coachNoteEditText;
    private TextInputLayout coachNoteTil;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private Bitmap photoBitmap; // foto tomada

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeUI();
        setupPhotoCapture();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Mostrar/ocultar nota al marcar "Solicitar cuenta coach"
        if (chkCoachRequest != null && coachNoteTil != null) {
            chkCoachRequest.setOnCheckedChangeListener((btn, checked) ->
                    coachNoteTil.setVisibility(checked ? View.VISIBLE : View.GONE));
        }

        registerButton.setOnClickListener(v -> {
            String firstName = firstNameEditText.getText().toString().trim();
            String lastName  = lastNameEditText.getText().toString().trim();
            String email     = emailEditText.getText().toString().trim();
            String password  = passwordEditText.getText().toString().trim();

            boolean requestCoach = (chkCoachRequest != null) && chkCoachRequest.isChecked();
            String coachNote = (coachNoteEditText != null)
                    ? coachNoteEditText.getText().toString().trim()
                    : null;

            if (validateInputs(firstName, lastName, email, password)) {
                registerUser(firstName, lastName, email, password, requestCoach, coachNote);
            }
        });
    }

    private void initializeUI() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText  = findViewById(R.id.lastNameEditText);
        emailEditText     = findViewById(R.id.emailEditText);
        passwordEditText  = findViewById(R.id.passwordEditText);
        registerButton    = findViewById(R.id.registerButton);
        takePhotoButton   = findViewById(R.id.takePhotoButton);
        profileImageView  = findViewById(R.id.profileImageView);

        // Coach UI (IDs agregados al XML)
        chkCoachRequest   = findViewById(R.id.chkCoachRequest);
        coachNoteEditText = findViewById(R.id.coachNoteEditText);
        coachNoteTil      = findViewById(R.id.coachNoteTil);
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
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

    // ✅ Firma correcta con requestCoach/coachNote
    private void registerUser(String firstName, String lastName, String email, String password,
                              boolean requestCoach, @Nullable String coachNote) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();
                            writeNewUser(userId, firstName, lastName, email, photoBitmap, requestCoach, coachNote);
                        }
                    } else {
                        Exception ex = task.getException();
                        String msg = getAuthErrorEs(ex);
                        Log.e(TAG, "Error en el registro", ex);
                        Toast.makeText(RegisterActivity.this, "No se pudo registrar: " + msg, Toast.LENGTH_LONG).show();

                        // Marcar campo cuando aplique
                        if (ex instanceof FirebaseAuthWeakPasswordException) {
                            passwordEditText.setError("Contraseña débil (mínimo 6 caracteres).");
                        } else if (ex instanceof FirebaseAuthUserCollisionException) {
                            emailEditText.setError("Ya existe una cuenta con este correo.");
                        } else if (ex instanceof FirebaseAuthInvalidCredentialsException) {
                            emailEditText.setError("Correo electrónico inválido.");
                        }
                    }
                });
    }

    // Escribe el usuario con estructura PLANA bajo /users/{uid}
    private void writeNewUser(String userId, String firstName, String lastName, String email,
                              @Nullable Bitmap photoBitmap, boolean requestCoach, @Nullable String coachNote) {

        DatabaseReference root = mDatabase;

        String encodedImage = (photoBitmap != null) ? encodeBitmapToBase64(photoBitmap) : "default";
        if (encodedImage == null || encodedImage.trim().isEmpty()) encodedImage = "default";

        // 🔹 Estructura PLANA (sin /profile)
        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("profilePhoto", encodedImage);
        user.put("isFirstTime", true);
        user.put("role", "user");                // visible, no otorga permisos
        user.put("coachCandidate", requestCoach);

        Map<String, Object> updates = new HashMap<>();
        updates.put("/users/" + userId, user);

        // (Opcional) si quieres limpiar por si alguna vez se creó /profile:
        // updates.put("/users/" + userId + "/profile", null);

        // Solicitud de coach en rama separada
        if (requestCoach) {
            Map<String, Object> req = new HashMap<>();
            req.put("name", firstName + " " + lastName);
            req.put("email", email);
            req.put("createdAt", ServerValue.TIMESTAMP);
            if (coachNote != null && !coachNote.isEmpty()) req.put("note", coachNote);

            updates.put("/coachRequests/" + userId, req);
        }

        root.updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                requestCoach ? "Registro completo. Solicitud de coach enviada."
                                        : "Registro completo.",
                                Toast.LENGTH_LONG).show();

                        Intent intent;
                        if (requestCoach) {
                            // ⏳ Usuario pidió cuenta de coach → pantalla de espera
                            intent = new Intent(RegisterActivity.this, CoachPendingActivity.class);
                        } else {
                            // ✅ Usuario normal → tu flujo normal (ViewPager2 de formularios)
                            intent = new Intent(RegisterActivity.this, MainActivity.class);
                        }
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        Log.e(TAG, "Error al guardar el usuario: ", task.getException());
                        Toast.makeText(this, "Error al guardar el usuario", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error crítico al guardar los datos: " + e.getMessage());
                    Toast.makeText(this, "Error crítico al guardar los datos", Toast.LENGTH_LONG).show();
                });

    }


    /** Errores de autenticación en español. */
    private String getAuthErrorEs(@Nullable Exception e) {
        if (e == null) return "Error desconocido.";

        if (e instanceof FirebaseAuthWeakPasswordException) {
            return "La contraseña es demasiado débil. Usa al menos 6 caracteres.";
        }
        if (e instanceof FirebaseAuthUserCollisionException) {
            return "Ya existe una cuenta registrada con este correo.";
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            FirebaseAuthInvalidCredentialsException ex = (FirebaseAuthInvalidCredentialsException) e;
            String code = ex.getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) {
                return "El correo electrónico no tiene un formato válido.";
            }
            if ("ERROR_CREDENTIAL_ALREADY_IN_USE".equals(code)) {
                return "Estas credenciales ya están en uso por otra cuenta.";
            }
            return "Credenciales inválidas. Verifica tus datos.";
        }

        if (e instanceof FirebaseNetworkException) {
            return "Problema de red. Revisa tu conexión a Internet e inténtalo de nuevo.";
        }
        if (e instanceof FirebaseTooManyRequestsException) {
            return "Demasiados intentos. Intenta nuevamente más tarde.";
        }

        if (e instanceof FirebaseAuthException) {
            FirebaseAuthException ex = (FirebaseAuthException) e;
            String code = ex.getErrorCode();
            switch (code) {
                case "ERROR_OPERATION_NOT_ALLOWED":
                    return "Operación no permitida. Contacta al administrador.";
                case "ERROR_USER_DISABLED":
                    return "La cuenta está deshabilitada.";
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return "Fallo de red. Intenta nuevamente.";
                case "ERROR_TOO_MANY_REQUESTS":
                    return "Demasiados intentos. Intenta nuevamente más tarde.";
                default:
                    return "Error de autenticación (" + code + ").";
            }
        }

        String fallback = e.getLocalizedMessage();
        return (fallback != null && !fallback.isEmpty())
                ? fallback
                : "Error inesperado. Intenta de nuevo.";
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
