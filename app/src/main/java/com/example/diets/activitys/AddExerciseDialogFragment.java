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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.diets.R;
import com.example.diets.model.Exercise;
import com.example.diets.model.MovementPattern;
import com.example.diets.model.Muscle;
import com.google.common.reflect.TypeToken;
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
    private static final String FILE_NAME = "exercises.json"; // Añadir esta línea

    private EditText nameEditText, descriptionEditText;
    private Spinner equipmentTypeSpinner, exerciseTypeSpinner, movementTypeSpinner, musclesWorkedSpinner, movementPatternSpinner;
    private Button saveButton, cancelButton, selectImageButton;
    private ImageView imagePreview;
    private AddExerciseListener listener;
    private Set<String> musclesSet = new HashSet<>();
    private String imageBase64;
    private Set<String> movementPatternsSet = new HashSet<>();

    public interface AddExerciseListener {
        void onExerciseAdded(Exercise exercise);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("AddExerciseDialogFragment", "onCreateView: Inflando el layout del fragmento");
        return inflater.inflate(R.layout.fragment_add_exercise_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("AddExerciseDialogFragment", "onViewCreated: Vista creada");

        nameEditText = view.findViewById(R.id.nameEditText);
        equipmentTypeSpinner = view.findViewById(R.id.equipmentTypeSpinner);
        exerciseTypeSpinner = view.findViewById(R.id.exerciseTypeSpinner);
        musclesWorkedSpinner = view.findViewById(R.id.musclesWorkedSpinner);
        movementPatternSpinner = view.findViewById(R.id.movementPatternSpinner);
        descriptionEditText = view.findViewById(R.id.descriptionEditText);
        movementTypeSpinner = view.findViewById(R.id.movementTypeSpinner);
        saveButton = view.findViewById(R.id.saveButton);
        cancelButton = view.findViewById(R.id.cancelButton);
        selectImageButton = view.findViewById(R.id.selectImageButton);
        imagePreview = view.findViewById(R.id.imagePreview);

        // Configurar los Spinners
        ArrayAdapter<CharSequence> equipmentAdapter = ArrayAdapter.createFromResource(getContext(), R.array.equipment_types, android.R.layout.simple_spinner_item);
        equipmentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        equipmentTypeSpinner.setAdapter(equipmentAdapter);
        Log.d("AddExerciseDialogFragment", "onViewCreated: Spinner de tipo de equipo configurado");

        ArrayAdapter<CharSequence> exerciseAdapter = ArrayAdapter.createFromResource(getContext(), R.array.exercise_types, android.R.layout.simple_spinner_item);
        exerciseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        exerciseTypeSpinner.setAdapter(exerciseAdapter);
        Log.d("AddExerciseDialogFragment", "onViewCreated: Spinner de tipo de ejercicio configurado");

        ArrayAdapter<CharSequence> movementAdapter = ArrayAdapter.createFromResource(getContext(), R.array.movement_types, android.R.layout.simple_spinner_item);
        movementAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        movementTypeSpinner.setAdapter(movementAdapter);
        Log.d("AddExerciseDialogFragment", "onViewCreated: Spinner de tipo de movimiento configurado");

        // Cargar músculos desde la base de datos
        loadMusclesFromDatabase();

        // Cargar patrones de movimiento desde la base de datos
        loadMovementPatternsFromDatabase();

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        saveButton.setOnClickListener(v -> {
            Log.d("AddExerciseDialogFragment", "saveButton: Botón de guardar presionado");
            String exerciseName = nameEditText.getText().toString().trim();
            if (exerciseName.isEmpty()) {
                nameEditText.setError("El nombre del ejercicio no puede ser nulo");
                Log.d("AddExerciseDialogFragment", "saveButton: El nombre del ejercicio está vacío");
                return;
            }

            Exercise newExercise = new Exercise();
            newExercise.setName(exerciseName);
            Log.d("AddExerciseDialogFragment", "saveButton: Nombre del ejercicio establecido: " + exerciseName);

            String equipmentType = equipmentTypeSpinner.getSelectedItem().toString();
            newExercise.setEquipmentType(equipmentType);
            Log.d("AddExerciseDialogFragment", "saveButton: Tipo de equipo establecido: " + equipmentType);

            String exerciseType = exerciseTypeSpinner.getSelectedItem().toString();
            newExercise.setExerciseType(exerciseType);
            Log.d("AddExerciseDialogFragment", "saveButton: Tipo de ejercicio establecido: " + exerciseType);

            String musclesWorked = musclesWorkedSpinner.getSelectedItem().toString();
            newExercise.setMusclesWorked(musclesWorked);
            Log.d("AddExerciseDialogFragment", "saveButton: Músculos trabajados establecidos: " + musclesWorked);

            String description = descriptionEditText.getText().toString();
            newExercise.setDescription(description);
            Log.d("AddExerciseDialogFragment", "saveButton: Descripción establecida: " + description);

            String movementType = movementTypeSpinner.getSelectedItem().toString();
            newExercise.setMovementType(movementType);
            Log.d("AddExerciseDialogFragment", "saveButton: Tipo de movimiento establecido: " + movementType);

            // Añadir el patrón de movimiento seleccionado
            String selectedPatternName = movementPatternSpinner.getSelectedItem().toString();
            MovementPattern selectedPattern = new MovementPattern();
            selectedPattern.setName(selectedPatternName);
            newExercise.getMovementPatterns().add(selectedPattern);
            Log.d("AddExerciseDialogFragment", "saveButton: Patrón de movimiento agregado: " + selectedPatternName);

            // Guardar el ejercicio en el almacenamiento interno
            saveExerciseToInternalStorage(newExercise);

            if (listener != null) {
                Log.d("AddExerciseDialogFragment", "saveButton: Llamando al listener para agregar el ejercicio");
                listener.onExerciseAdded(newExercise);
            } else {
                Log.d("AddExerciseDialogFragment", "saveButton: Listener es null");
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            Log.d("AddExerciseDialogFragment", "cancelButton: Botón de cancelar presionado");
            dismiss();
        });
    }

    private void saveExerciseToInternalStorage(Exercise exercise) {
        Log.d("AddExerciseDialogFragment", "saveExerciseToInternalStorage: Guardando ejercicio en almacenamiento interno");

        try {
            // Cargar ejercicios existentes desde JSON
            List<Exercise> exercises = loadExercisesFromInternalStorage();

            // Añadir el nuevo ejercicio a la lista
            exercises.add(exercise);

            // Guardar lista en formato JSON
            File file = new File(getContext().getFilesDir(), FILE_NAME);
            FileWriter writer = new FileWriter(file);
            Gson gson = new Gson();
            String json = gson.toJson(exercises);

            writer.write(json);
            writer.close();

            Log.d("AddExerciseDialogFragment", "saveExerciseToInternalStorage: ✅ Ejercicio guardado correctamente.");
        } catch (IOException e) {
            Log.e("AddExerciseDialogFragment", "❌ Error al guardar ejercicio", e);
        }
    }


    private void loadMusclesFromDatabase() {
        Log.d("AddExerciseDialogFragment", "loadMusclesFromDatabase: Cargando músculos desde la base de datos");
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), imageUri);
                imagePreview.setImageBitmap(bitmap);
                imageBase64 = encodeImageToBase64(bitmap);
            } catch (IOException e) {
                Log.e("AddExerciseDialogFragment", "Error al seleccionar la imagen", e);
            }
        }
    }
    private List<Exercise> loadExercisesFromInternalStorage() {
        List<Exercise> exercises = new ArrayList<>();

        try {
            File file = new File(getContext().getFilesDir(), FILE_NAME);
            if (!file.exists()) {
                Log.e("AddExerciseDialogFragment", "❌ Archivo de ejercicios no encontrado.");
                return exercises;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            Gson gson = new Gson();
            Type listType = new TypeToken<List<Exercise>>() {}.getType();

            exercises = gson.fromJson(reader, listType);
            reader.close();

            if (exercises == null) {
                exercises = new ArrayList<>();
            }

            Log.d("AddExerciseDialogFragment", "📥 Ejercicios cargados correctamente: " + exercises.size());
        } catch (Exception e) {
            Log.e("AddExerciseDialogFragment", "❌ Error al cargar ejercicios desde almacenamiento interno", e);
        }

        return exercises;
    }

    private String encodeImageToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    public void setAddExerciseListener(AddExerciseListener listener) {
        this.listener = listener;
        Log.d("AddExerciseDialogFragment", "setAddExerciseListener: Listener configurado");
    }
}