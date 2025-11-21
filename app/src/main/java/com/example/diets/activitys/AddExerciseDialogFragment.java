package com.example.diets.activitys;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.diets.R;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.Muscle;
import com.google.common.reflect.TypeToken;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AddExerciseDialogFragment extends DialogFragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String FILE_NAME       = "exercises.json";
    private static final String TAG             = "AddExerciseDialogFragment";

    /* ------------ UI ------------ */
    private EditText nameEditText, descriptionEditText;
    private Spinner equipmentTypeSpinner, exerciseTypeSpinner,
            movementTypeSpinner, musclesWorkedSpinner, movementPatternSpinner;
    private Button  saveButton, cancelButton, selectImageButton;
    private ImageView imagePreview;

    /* ------------ Datos ------------ */
    private AddExerciseListener listener;
    private final Set<String> musclesSet          = new HashSet<>();
    private final Set<String> movementPatternsSet = new HashSet<>();

    /* >>> MOD: ahora guardamos TODAS las imágenes seleccionadas <<< */
    private final List<String> imagesBase64 = new ArrayList<>();   // NUEVO
    private String imageBase64;                                     // se mantiene por compatibilidad

    /* ------------------------------------------------------------------ */
    public interface AddExerciseListener { void onExerciseAdded(Exercise exercise); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_exercise_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        /* ==== referencias ==== */
        nameEditText           = view.findViewById(R.id.nameEditText);
        equipmentTypeSpinner   = view.findViewById(R.id.equipmentTypeSpinner);
        exerciseTypeSpinner    = view.findViewById(R.id.exerciseTypeSpinner);
        musclesWorkedSpinner   = view.findViewById(R.id.musclesWorkedSpinner);
        movementPatternSpinner = view.findViewById(R.id.movementPatternSpinner);
        descriptionEditText    = view.findViewById(R.id.descriptionEditText);
        movementTypeSpinner    = view.findViewById(R.id.movementTypeSpinner);
        saveButton             = view.findViewById(R.id.saveButton);
        cancelButton           = view.findViewById(R.id.cancelButton);
        selectImageButton      = view.findViewById(R.id.selectImageButton);
        imagePreview           = view.findViewById(R.id.imagePreview);

        /* ==== spinners fijos (igual que antes) ==== */
        ArrayAdapter<CharSequence> equipmentAdapter =
                ArrayAdapter.createFromResource(getContext(), R.array.equipment_types,
                        android.R.layout.simple_spinner_item);
        equipmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        equipmentTypeSpinner.setAdapter(equipmentAdapter);

        ArrayAdapter<CharSequence> exerciseAdapter =
                ArrayAdapter.createFromResource(getContext(), R.array.exercise_types,
                        android.R.layout.simple_spinner_item);
        exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exerciseTypeSpinner.setAdapter(exerciseAdapter);

        ArrayAdapter<CharSequence> movementAdapter =
                ArrayAdapter.createFromResource(getContext(), R.array.movement_types,
                        android.R.layout.simple_spinner_item);
        movementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        movementTypeSpinner.setAdapter(movementAdapter);

        /* ==== datos dinámicos ==== */
        loadMusclesFromDatabase();
        loadMovementPatternsFromDatabase();

        /* ==== listeners ==== */
        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);        //  permite varias
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Selecciona imagen(es)"),
                    PICK_IMAGE_REQUEST);
        });

        cancelButton.setOnClickListener(v -> dismiss());

        saveButton.setOnClickListener(v -> saveExercise());
    }


    /* ------------------------------------------------------------------ */
    private void saveExercise() {
        String exerciseName = nameEditText.getText().toString().trim();
        if (exerciseName.isEmpty()) {
            nameEditText.setError("El nombre del ejercicio no puede ser nulo");
            return;
        }

        // AUDITORÍA: intento de creación con conteo de imágenes
        Log.i(TAG, "AUDIT_TRAIL: Intentando crear ejercicio: " + exerciseName + " | Imágenes: " + imagesBase64.size());

        Exercise newExercise = new Exercise();

        // 🔍 Establecer como ejercicio de almacenamiento interno


        newExercise.setName(exerciseName);
        newExercise.setFromInternal(true);
        Log.d("AddExerciseDialogFragment", "Creando ejercicio - isFromInternal(): " + newExercise.isFromInternal());
        newExercise.setEquipmentType(equipmentTypeSpinner.getSelectedItem().toString());
        newExercise.setExerciseType(exerciseTypeSpinner.getSelectedItem().toString());

        Object selectedMuscle = musclesWorkedSpinner.getSelectedItem();
        if (selectedMuscle == null) {
            Toast.makeText(getContext(), "Selecciona al menos un músculo trabajado", Toast.LENGTH_SHORT).show();
            return;
        }
        newExercise.setMusclesWorked(selectedMuscle.toString());

        newExercise.setDescription(descriptionEditText.getText().toString());
        newExercise.setMovementType(movementTypeSpinner.getSelectedItem().toString());

        // 🔄 Agregar patrón de movimiento
        MovementPattern selectedPattern = new MovementPattern();
        selectedPattern.setName(movementPatternSpinner.getSelectedItem().toString());
        newExercise.getMovementPatterns().add(selectedPattern);

        // 🖼️ Agregar imágenes
        newExercise.setImages(imagesBase64);

        // 🧪 LOG resumen del ejercicio antes de guardar
        Log.d("AddExerciseDialogFragment", "🧾 Resumen ejercicio nuevo:");
        Log.d("AddExerciseDialogFragment", "🔢 Nombre: " + newExercise.getName());
        Log.d("AddExerciseDialogFragment", "🏋️ Equipo: " + newExercise.getEquipmentType());
        Log.d("AddExerciseDialogFragment", "⚙️ Tipo: " + newExercise.getExerciseType());
        Log.d("AddExerciseDialogFragment", "💪 Músculo: " + newExercise.getMusclesWorked());
        Log.d("AddExerciseDialogFragment", "📝 Descripción: " + newExercise.getDescription());
        Log.d("AddExerciseDialogFragment", "🔁 Movimiento: " + newExercise.getMovementType());
        Log.d("AddExerciseDialogFragment", "📊 Patrón: " + selectedPattern.getName());

        Log.d("AddExerciseDialogFragment", "📌 ¿Desde almacenamiento interno? " + newExercise.isFromInternal());

        saveExerciseToInternalStorage(newExercise);

        if (listener != null) listener.onExerciseAdded(newExercise);
        dismiss();
    }

    /* ------------------------------------------------------------------ */
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PICK_IMAGE_REQUEST || resultCode != Activity.RESULT_OK || data == null)
            return;

        /* —— varias imágenes —— */
        if (data.getClipData() != null) {
            int n = data.getClipData().getItemCount();
            for (int i = 0; i < n; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                addImageFromUri(uri, i == 0);
            }
        } else if (data.getData() != null) {        // —— una sola ——
            addImageFromUri(data.getData(), true);
        }
    }

    private void addImageFromUri(Uri uri, boolean showPreview) {
        if (uri == null) return;
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(), uri);

            String b64 = encodeImageToBase64(bitmap);
            imagesBase64.add(b64);          //  se añade a la lista
            imageBase64 = b64;              // compatibilidad con antiguo campo

            if (showPreview) imagePreview.setImageBitmap(bitmap);
        } catch (IOException e) {
            Log.e("AddExerciseDialogFragment", "Error al seleccionar la imagen", e);
        }
    }
    // Helpers de seguridad / archivo por usuario
    private @Nullable String uid() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return (u != null) ? u.getUid() : null;
    }

    private boolean ready() {
        return isAdded() && getContext() != null;
    }

    /** Archivo JSON local privado por usuario: <UID>_exercises.json */
    private File file() {
        return new File(requireContext().getFilesDir(), uid() + "_exercises.json");
    }

    private void saveExerciseToInternalStorage(Exercise exercise) {
        Log.d(TAG, "saveExerciseToInternalStorage: Guardando ejercicio en almacenamiento interno");

        if (!ready()) {
            Log.w(TAG, "saveExerciseToInternalStorage: Fragment no está listo.");
            return;
        }

        String u = uid();
        if (u == null) {
            Log.w(TAG, "saveExerciseToInternalStorage: uid null (usuario no autenticado). No se guarda.");
            return;
        }

        try {
            // Cargar ejercicios existentes desde JSON (<UID>_exercises.json)
            List<Exercise> exercises = loadExercisesFromInternalStorage();

            // Añadir el nuevo ejercicio a la lista
            exercises.add(exercise);

            // Guardar lista en formato JSON
            File f = file();

            Log.d(TAG, "AUDIT_TRAIL: Persistiendo " + exercises.size()
                    + " ejercicios en archivo aislado por UID: " + f.getName());

            try (FileWriter writer = new FileWriter(f)) {
                Gson gson = new Gson();
                String json = gson.toJson(exercises);
                writer.write(json);
            }

            Log.d(TAG, "saveExerciseToInternalStorage: Ejercicio guardado correctamente en " + f.getName());
        } catch (IOException e) {
            Log.e(TAG, "saveExerciseToInternalStorage: Error al guardar ejercicio", e);
        }
    }



    private void loadMusclesFromDatabase() {
        Log.d("AddExerciseDialogFragment", "loadMusclesFromDatabase: Cargando músculos desde la base de datos");
        // AUDITORÍA: sincronización de catálogos para validar formularios
        Log.d(TAG, "AUDIT_TRAIL: Sincronizando catálogos desde Firebase para validación de formularios.");

        DatabaseReference exercisesRef = FirebaseDatabase.getInstance().getReference().child("exercises");
        exercisesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                musclesSet.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot exerciseSnapshot : snapshot.getChildren()) {
                        Object musclesData = exerciseSnapshot.child("musclesWorked").getValue();

                        if (musclesData instanceof String) {
                            String[] musclesArray = ((String) musclesData).split(",");
                            for (String muscle : musclesArray) {
                                musclesSet.add(capitalizeFirstLetter(muscle.trim().toLowerCase()));
                            }
                        } else if (musclesData instanceof Map) {
                            for (Object value : ((Map<?, ?>) musclesData).values()) {
                                if (value instanceof String) {
                                    musclesSet.add(capitalizeFirstLetter(((String) value).trim().toLowerCase()));
                                }
                            }
                        }
                    }
                }

                ArrayAdapter<String> musclesAdapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_item, new ArrayList<>(musclesSet));
                musclesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                musclesWorkedSpinner.setAdapter(musclesAdapter);

                Log.d("AddExerciseDialogFragment", "loadMusclesFromDatabase: Músculos cargados: " + musclesSet.size());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AddExerciseDialogFragment", "Error al cargar músculos desde Firebase", error.toException());
            }
        });
    }


    private void loadMovementPatternsFromDatabase() {
        Log.d("AddExerciseDialogFragment", "loadMovementPatternsFromDatabase: Cargando patrones de movimiento desde la base de datos");
        DatabaseReference distributionsRef = FirebaseDatabase.getInstance().getReference().child("exerciseDistributions");
        distributionsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                movementPatternsSet.clear();
                if (snapshot.exists()) {
                    for (DataSnapshot distributionSnapshot : snapshot.getChildren()) {
                        for (DataSnapshot trainingDaySnapshot : distributionSnapshot.child("trainingDays").getChildren()) {
                            for (DataSnapshot patternSnapshot : trainingDaySnapshot.child("movementPatterns").getChildren()) {
                                Object patternData = patternSnapshot.child("name").getValue();

                                if (patternData instanceof String) {
                                    movementPatternsSet.add(capitalizeFirstLetter(((String) patternData).trim().toLowerCase()));
                                } else if (patternData instanceof Map) {
                                    for (Object value : ((Map<?, ?>) patternData).values()) {
                                        if (value instanceof String) {
                                            movementPatternsSet.add(capitalizeFirstLetter(((String) value).trim().toLowerCase()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                ArrayAdapter<String> patternsAdapter = new ArrayAdapter<>(getContext(),
                        android.R.layout.simple_spinner_item, new ArrayList<>(movementPatternsSet));
                patternsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                movementPatternSpinner.setAdapter(patternsAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AddExerciseDialogFragment", "Error al cargar patrones de movimiento desde Firebase", error.toException());
            }
        });
    }


    private String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }


    private List<Exercise> loadExercisesFromInternalStorage() {
        List<Exercise> exercises = new ArrayList<>();

        if (!ready()) {
            Log.w(TAG, "loadExercisesFromInternalStorage: Fragment no está listo.");
            return exercises;
        }

        String u = uid();
        if (u == null) {
            Log.w(TAG, "loadExercisesFromInternalStorage: uid null (usuario no autenticado).");
            return exercises;
        }

        File f = file();
        if (!f.exists()) {
            Log.i(TAG, "loadExercisesFromInternalStorage: Archivo no encontrado para uid="
                    + u + " -> " + f.getName() + ". Lista vacía.");
            return exercises;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Exercise>>() {}.getType();

            exercises = gson.fromJson(reader, listType);
            if (exercises == null) exercises = new ArrayList<>();

            Log.d(TAG, "loadExercisesFromInternalStorage: Ejercicios cargados ("
                    + exercises.size() + ") desde " + f.getName());
        } catch (Exception e) {
            Log.e(TAG, "loadExercisesFromInternalStorage: Error al cargar ejercicios", e);
        }

        return exercises;
    }


    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 30, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public void setAddExerciseListener(AddExerciseListener listener) {
        this.listener = listener;
        Log.d("AddExerciseDialogFragment", "setAddExerciseListener: Listener configurado");
    }
}
