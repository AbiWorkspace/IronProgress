package com.example.diets.activitys;

import android.app.Activity;
import android.content.Intent;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
public class AddExerciseDialogFragment extends DialogFragment {
    private static final int PICK_IMAGE_REQUEST = 1;

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
                return;
            }

            Exercise newExercise = new Exercise();
            newExercise.setName(exerciseName);
            newExercise.setEquipmentType(equipmentTypeSpinner.getSelectedItem().toString());
            newExercise.setExerciseType(exerciseTypeSpinner.getSelectedItem().toString());
            newExercise.setMusclesWorked(musclesWorkedSpinner.getSelectedItem().toString());
            newExercise.setDescription(descriptionEditText.getText().toString());
            newExercise.setMovementType(movementTypeSpinner.getSelectedItem().toString());

            // Añadir el patrón de movimiento seleccionado
            String selectedPatternName = movementPatternSpinner.getSelectedItem().toString();
            MovementPattern selectedPattern = new MovementPattern();
            selectedPattern.setName(selectedPatternName);
            newExercise.getMovementPatterns().add(selectedPattern);

            if (listener != null) {
                Log.d("AddExerciseDialogFragment", "saveButton: Llamando al listener para agregar el ejercicio");
                listener.onExerciseAdded(newExercise);
            }
            dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            Log.d("AddExerciseDialogFragment", "cancelButton: Botón de cancelar presionado");
            dismiss();
        });
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
                        Exercise exercise = exerciseSnapshot.getValue(Exercise.class);
                        if (exercise != null && exercise.getMusclesWorked() != null) {
                            String[] musclesArray = exercise.getMusclesWorked().split(",");
                            for (String muscle : musclesArray) {
                                musclesSet.add(capitalizeFirstLetter(muscle.trim().toLowerCase()));
                            }
                        }
                    }
                }
                // Configurar el adaptador del spinner de músculos
                ArrayAdapter<String> musclesAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>(musclesSet));
                musclesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                musclesWorkedSpinner.setAdapter(musclesAdapter);
                Log.d("AddExerciseDialogFragment", "loadMusclesFromDatabase: Adaptador de músculos configurado con " + musclesSet.size() + " elementos");

                loadMovementPatternsFromDatabase(); // Llamamos al método para cargar los patrones de movimiento después de cargar los músculos
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AddExerciseDialogFragment", "Error al cargar músculos desde la base de datos", error.toException());
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
                                String patternName = patternSnapshot.child("name").getValue(String.class);
                                if (patternName != null && !musclesSet.contains(capitalizeFirstLetter(patternName.trim().toLowerCase()))) {
                                    movementPatternsSet.add(capitalizeFirstLetter(patternName.trim().toLowerCase()));
                                }
                            }
                        }
                    }
                }
                ArrayAdapter<String> patternsAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, new ArrayList<>(movementPatternsSet));
                patternsAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                movementPatternSpinner.setAdapter(patternsAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AddExerciseDialogFragment", "Error al cargar patrones de movimiento desde la base de datos", error.toException());
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