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

import androidx.annotation.NonNull;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Activity de registro de usuarios.
 *
 * <p>Incluye trazas de auditoría (AUDIT_TRAIL) para seguir el flujo completo
 * de registro y, en particular, la regla de unicidad de Coach tanto en UI
 * (visibilidad del checkbox) como en backend .</p>
 */
public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";

    // UI
    private EditText firstNameEditText, lastNameEditText, emailEditText, passwordEditText;
    private Button registerButton, takePhotoButton;
    private ImageView profileImageView;
    private MaterialCheckBox chkCoachRequest;

    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    private Bitmap photoBitmap;

    /**
     * Callback simple para reutilizar la consulta de "¿existe coach?".
     * Usa un único booleano para devolver el resultado de forma asíncrona.
     */
    private interface BoolCallback { void onResult(boolean value); }

    /**
     * Inicializa la pantalla de registro, configura captura de foto, instancia
     * Firebase y realiza una primera validación de unicidad de Coach contra la nube.
     *
     * @param savedInstanceState estado previamente guardado de la Activity.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        initializeUI();
        setupPhotoCapture();

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Mostrar u ocultar el check según servidor
        checkIfCoachAlreadyExists();

        registerButton.setOnClickListener(v -> {
            String firstName = firstNameEditText.getText().toString().trim();
            String lastName  = lastNameEditText.getText().toString().trim();
            String email     = emailEditText.getText().toString().trim();
            String password  = passwordEditText.getText().toString().trim();
            boolean requestCoach = chkCoachRequest != null
                    && chkCoachRequest.getVisibility() == View.VISIBLE
                    && chkCoachRequest.isChecked();

            // Auditoría: intento de registro y estado de la solicitud de coach en UI
            Log.d(TAG, "AUDIT_TRAIL: Click en registrar. requestCoachUI=" + requestCoach + " | email=" + email);

            if (validateInputs(firstName, lastName, email, password)) {
                registerButton.setEnabled(false);
                registerUser(firstName, lastName, email, password, requestCoach);
            }
        });
    }

    /**
     * Al volver a primer plano, vuelve a consultar al backend para decidir
     * si el checkbox de solicitud de coach debe mostrarse u ocultarse.
     */
    @Override
    protected void onResume() {
        super.onResume();
        checkIfCoachAlreadyExists();
    }

    /**
     * Asocia las vistas del layout con los campos de la Activity.
     */
    private void initializeUI() {
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText  = findViewById(R.id.lastNameEditText);
        emailEditText     = findViewById(R.id.emailEditText);
        passwordEditText  = findViewById(R.id.passwordEditText);
        registerButton    = findViewById(R.id.registerButton);
        takePhotoButton   = findViewById(R.id.takePhotoButton);
        profileImageView  = findViewById(R.id.profileImageView);
        chkCoachRequest   = findViewById(R.id.chkCoachRequest);
    }

    /**
     * Configura el botón de captura de foto para solicitar permisos de cámara
     * y lanzar el intent de captura cuando estén concedidos.
     */
    private void setupPhotoCapture() {
        takePhotoButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 2);
            } else {
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 101);
            }
        });
    }

    /**
     * Recibe el resultado de la captura de foto y actualiza la vista de perfil.
     *
     * @param requestCode código de la petición.
     * @param resultCode  resultado de la operación.
     * @param data        intent con los datos devueltos por la cámara.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == RESULT_OK && data != null) {
            photoBitmap = (Bitmap) data.getExtras().get("data");
            if (photoBitmap != null) profileImageView.setImageBitmap(photoBitmap);
            else Log.e(TAG, "Bitmap is null after capturing the photo.");
        }
    }

    /**
     * Valida los campos del formulario de registro.
     *
     * @param firstName nombre capturado.
     * @param lastName  apellido capturado.
     * @param email     correo electrónico capturado.
     * @param password  contraseña capturada.
     * @return {@code true} si todos los campos son válidos, {@code false} en caso contrario.
     */
    private boolean validateInputs(String firstName, String lastName, String email, String password) {
        if (firstName.isEmpty() || !firstName.matches("[a-zA-Z]+")) {
            firstNameEditText.setError("Nombre requerido y sin números/esp.");
            return false;
        }
        if (lastName.isEmpty() || !lastName.matches("[a-zA-Z]+")) {
            lastNameEditText.setError("Apellido requerido y sin números/esp.");
            return false;
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Correo electrónico inválido");
            return false;
        }
        if (password.isEmpty() || password.length() < 6) {
            passwordEditText.setError("Mínimo 6 caracteres");
            return false;
        }
        // Auditoría ligera de validación exitosa
        Log.d(TAG, "AUDIT_TRAIL: Validación de campos exitosa para email=" + email);
        return true;
    }

    /**
     * Orquesta el registro del usuario en Firebase Authentication.
     * Si la autenticación es exitosa, delega la escritura del perfil en RTDB.
     *
     * @param firstName    nombre del usuario.
     * @param lastName     apellido del usuario.
     * @param email        correo electrónico del usuario.
     * @param password     contraseña en texto plano (no se loguea).
     * @param requestCoach indica si el usuario marcó la solicitud de coach en UI.
     */
    private void registerUser(String firstName, String lastName, String email, String password,
                              boolean requestCoach) {
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    registerButton.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            Log.i(TAG, "AUDIT_TRAIL: Registro FirebaseAuth exitoso -> UID=" + user.getUid() + " | email=" + email + " | requestCoachUI=" + requestCoach);
                            writeNewUser(user.getUid(), firstName, lastName, email, photoBitmap, requestCoach);
                        }
                    } else {
                        Exception ex = task.getException();
                        Log.e(TAG, "Error en el registro", ex);
                        Log.w(TAG, "AUDIT_TRAIL: Falló registro FirebaseAuth -> email=" + email + " | exception=" + (ex != null ? ex.getClass().getSimpleName() : "desconocida"));

                        // --- MODIFICACIÓN ---
                        // Mover el Toast para ser más específico
                        if (ex instanceof FirebaseAuthWeakPasswordException) {
                            String msg = getAuthErrorEs(ex); // "La contraseña es demasiado débil..."
                            passwordEditText.setError(msg);
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show(); // Toast específico

                        } else if (ex instanceof FirebaseAuthUserCollisionException) {
                            String msg = getAuthErrorEs(ex); // "Ya existe una cuenta..."
                            emailEditText.setError(msg);
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show(); // ¡¡El Toast que el usuario quiere!!

                        } else if (ex instanceof FirebaseAuthInvalidCredentialsException) {
                            String msg = getAuthErrorEs(ex); // "Correo inválido."
                            emailEditText.setError(msg);
                            Toast.makeText(RegisterActivity.this, msg, Toast.LENGTH_LONG).show(); // Toast específico

                        } else {
                            // Para todos los otros errores (red, etc.)
                            String msg = getAuthErrorEs(ex);
                            Toast.makeText(RegisterActivity.this, "Error: " + msg, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    /**
     * Construye el mapa de usuario y lo persiste en RTDB.
     * Aquí se aplica la regla de negocio de unicidad de Coach:

     * en la UI y el backend confirma que aún NO existe un coach.
     *
     * @param userId         UID del usuario autenticado.
     * @param firstName      nombre del usuario.
     * @param lastName       apellido del usuario.
     * @param email          correo electrónico del usuario.
     * @param photoBitmap    foto de perfil en bitmap (puede ser null).
     * @param requestCoachUI estado del checkbox de solicitud de coach en UI.
     */
    private void writeNewUser(String userId, String firstName, String lastName, String email,
                              @Nullable Bitmap photoBitmap, boolean requestCoachUI) {

        DatabaseReference root = mDatabase;

        String encodedImage = (photoBitmap != null) ? encodeBitmapToBase64(photoBitmap) : "default";
        if (encodedImage == null || encodedImage.trim().isEmpty()) encodedImage = "default";

        Map<String, Object> user = new HashMap<>();
        user.put("firstName", firstName);
        user.put("lastName", lastName);
        user.put("email", email);
        user.put("profilePhoto", encodedImage);
        user.put("isFirstTime", true);
        user.put("role", "user");


        // El backend decide con certeza:
        existsCoach(exists -> {
            boolean enviarSolicitud = requestCoachUI && !exists;

            // Auditoría de regla de negocio (unicidad de Coach)
            Log.i(TAG, "AUDIT_TRAIL: Evaluando regla de unicidad de Coach -> requestCoachUI=" + requestCoachUI + " | existeCoach=" + enviarSolicitud);



            Map<String, Object> updates = new HashMap<>();
            updates.put("/users/" + userId, user);

            // Auditoría requerida: escritura del perfil en RTDB
            Log.i(TAG, "AUDIT_TRAIL: Escribiendo perfil en RTDB. UID: " + userId + " | Role: " + user.get("role"));

            root.updateChildren(updates).addOnCompleteListener(t -> {
                postRegisterNavigate(enviarSolicitud);
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Error crítico al guardar los datos: " + e.getMessage());
                Toast.makeText(RegisterActivity.this, "Error crítico al guardar los datos", Toast.LENGTH_LONG).show();
            });
        });
    }

    // ---------- VISIBILIDAD DEL CHECK (front) CONTROLADA POR BACKEND ----------

    /**
     * Controla la visibilidad del checkbox de solicitud de coach según lo que
     * responda el backend. Solo se muestra si NO existe ningún coach registrado.
     *
     * La regla de negocio de unicidad de Coach se inicia aquí desde la perspectiva de UI.
     */
    private void checkIfCoachAlreadyExists() {
        if (chkCoachRequest == null) return;
        chkCoachRequest.setVisibility(View.GONE);
        chkCoachRequest.setChecked(false);

        // AUDIT: inicio de validación de unicidad de Coach (llamada a la nube)
        Log.d(TAG, "AUDIT_TRAIL: Iniciando validación de unicidad de Coach en la nube...");

        existsCoach(exists -> {
            // Si existe coach -> oculto; si no, muestro desmarcado
            chkCoachRequest.setVisibility(exists ? View.GONE : View.VISIBLE);
            chkCoachRequest.setChecked(false);
        });
    }

    /**
     * Consulta en RTDB si existe al menos un usuario con {@code role == "coach"}.
     * El resultado se devuelve a través de un callback booleano.
     *
     * <p>En caso de error (onCancelled), se aplica un fail-safe conservador:
     * se asume que SÍ existe un coach para evitar violar la unicidad.</p>
     *
     * @param cb callback que recibe {@code true} si existe coach o si hay error (fail-safe),
     *           {@code false} si no existe coach.
     */
    private void existsCoach(@NonNull final BoolCallback cb) {
        // Esta es la consulta que tú esperas:
        // Busca en /users, ordena por "role" y mira si existe alguno igual a "coach"
        mDatabase.child("users").orderByChild("role").equalTo("coach").limitToFirst(1)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        // dataSnapshot.exists() será 'true' si encuentra al menos 1 coach
                        boolean has = dataSnapshot.exists();
                        Log.d(TAG, "Consulta de 'role' == 'coach' existe = " + has);
                        Log.i(TAG, "AUDIT_TRAIL: ¿Existe Coach registrado?: " + has + ". Ajustando visibilidad del checkbox.");
                        cb.onResult(has);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Log.e(TAG, "Error al consultar 'role' de coach: ", databaseError.toException());
                        Log.i(TAG, "AUDIT_TRAIL: Error al consultar unicidad de Coach. Se aplica fail-safe asumiendo que ya existe un Coach y se oculta el checkbox.");
                        // Fail-safe: Si hay un error de reglas, OCULTAMOS el check
                        // Es más seguro asumir que SÍ existe un coach si no podemos verificar
                        cb.onResult(true);
                    }
                });
    }

    /**
     * Navega a la pantalla correspondiente después del registro según se haya
     * enviado o no una solicitud de coach.
     *
     * @param envioSolicitudCoach {@code true} si el usuario quedó como candidato a coach,
     *                            {@code false} si es un usuario estándar.
     */
    private void postRegisterNavigate(boolean envioSolicitudCoach) {
        if (envioSolicitudCoach) {
            Log.i(TAG, "AUDIT_TRAIL: Navegación post-registro -> CoachPendingActivity (solicitud de coach enviada).");
            Toast.makeText(this, "Registro completo. Solicitud de coach enviada.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, CoachPendingActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        } else {
            Log.i(TAG, "AUDIT_TRAIL: Navegación post-registro -> MainActivity (sin solicitud de coach).");
            Toast.makeText(this, "Registro completo.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        }
        finish();
    }

    /**
     * Traductor de excepciones de Firebase Auth a mensajes entendibles en español.
     *
     * @param e excepción original de Firebase.
     * @return mensaje de error localizado.
     */
    private String getAuthErrorEs(@Nullable Exception e) {
        if (e == null) return "Error desconocido.";
        if (e instanceof FirebaseAuthWeakPasswordException) return "La contraseña es demasiado débil. Usa al menos 6 caracteres.";
        if (e instanceof FirebaseAuthUserCollisionException) return "Ya existe una cuenta registrada con este correo.";
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            String code = ((FirebaseAuthInvalidCredentialsException) e).getErrorCode();
            if ("ERROR_INVALID_EMAIL".equals(code)) return "El correo electrónico no tiene un formato válido.";
            if ("ERROR_CREDENTIAL_ALREADY_IN_USE".equals(code)) return "Estas credenciales ya están en uso por otra cuenta.";
            return "Credenciales inválidas. Verifica tus datos.";
        }
        if (e instanceof FirebaseNetworkException) return "Problema de red. Revisa tu conexión.";
        if (e instanceof FirebaseTooManyRequestsException) return "Demasiados intentos. Intenta más tarde.";
        if (e instanceof FirebaseAuthException) return "Error de autenticación.";
        String fb = e.getLocalizedMessage();
        return (fb != null && !fb.isEmpty()) ? fb : "Error inesperado. Intenta de nuevo.";
    }

    /**
     * Codifica un {@link Bitmap} en Base64 para persistirlo como texto en RTDB.
     *
     * @param bitmap imagen a codificar (puede ser {@code null}).
     * @return cadena Base64 o {@code null} si no se pudo codificar.
     */
    private String encodeBitmapToBase64(Bitmap bitmap) {
        if (bitmap == null) return null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] imageBytes = baos.toByteArray();
        return Base64.encodeToString(imageBytes, Base64.DEFAULT);
    }
}
