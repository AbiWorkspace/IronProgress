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
import com.example.diets.model.Muscle;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AddExerciseDialogFragment extends DialogFragment {
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText nameEditText, descriptionEditText;
    private Spinner equipmentTypeSpinner, exerciseTypeSpinner, movementTypeSpinner, musclesWorkedSpinner;
    private Button saveButton, cancelButton, selectImageButton;
    private ImageView imagePreview;
    private AddExerciseListener listener;
    private List<String> musclesList = new ArrayList<>();
    private String imageBase64;

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

        selectImageButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        saveButton.setOnClickListener(v -> {
            Log.d("AddExerciseDialogFragment", "saveButton: Botón de guardar presionado");
            Exercise newExercise = new Exercise();
            newExercise.setName(nameEditText.getText().toString());
            newExercise.setEquipmentType(equipmentTypeSpinner.getSelectedItem().toString());
            newExercise.setExerciseType(exerciseTypeSpinner.getSelectedItem().toString());
            newExercise.setMusclesWorked(musclesWorkedSpinner.getSelectedItem().toString());
            newExercise.setImage(imageBase64);
            newExercise.setDescription(descriptionEditText.getText().toString());
            newExercise.setMovementType(movementTypeSpinner.getSelectedItem().toString());

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
        DatabaseReference musclesRef = FirebaseDatabase.getInstance().getReference().child("muscles");
        musclesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                musclesList.clear();
                for (DataSnapshot muscleSnapshot : snapshot.getChildren()) {
                    Muscle muscle = muscleSnapshot.getValue(Muscle.class);
                    if (muscle != null) {
                        musclesList.add(muscle.getName());
                        Log.d("AddExerciseDialogFragment", "loadMusclesFromDatabase: Músculo agregado: " + muscle.getName());
                    }
                }
                ArrayAdapter<String> musclesAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, musclesList);
                musclesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                musclesWorkedSpinner.setAdapter(musclesAdapter);
                Log.d("AddExerciseDialogFragment", "loadMusclesFromDatabase: Adaptador de músculos configurado");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AddExerciseDialogFragment", "Error al cargar músculos desde la base de datos", error.toException());
            }
        });
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