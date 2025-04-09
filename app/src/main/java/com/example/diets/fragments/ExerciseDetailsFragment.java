package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.diets.R;
import com.example.diets.model.Exercise;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.widget.ImageView;

public class ExerciseDetailsFragment extends Fragment {

    private static final String ARG_EXERCISE = "exercise";

    public static ExerciseDetailsFragment newInstance(Exercise exercise) {
        ExerciseDetailsFragment fragment = new ExerciseDetailsFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EXERCISE, exercise);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercise_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText nameEditText = view.findViewById(R.id.nameEditText);
        EditText equipmentTypeEditText = view.findViewById(R.id.equipmentTypeEditText);
        EditText exerciseTypeEditText = view.findViewById(R.id.exerciseTypeEditText);
        EditText musclesWorkedEditText = view.findViewById(R.id.musclesWorkedEditText);
        EditText descriptionEditText = view.findViewById(R.id.descriptionEditText);
        ImageView imagePreview = view.findViewById(R.id.imagePreview);
        Button backButton = view.findViewById(R.id.backButton);


        if (getArguments() != null) {
            Exercise exercise = (Exercise) getArguments().getSerializable(ARG_EXERCISE);
            Log.d("ExerciseDetailsFragment", "Base64 recibido: " + exercise.getImage());
            String base64Image = exercise.getImage();

            if (base64Image != null && !base64Image.trim().isEmpty()) {
                try {
                    base64Image = base64Image.replaceAll("\\s", ""); // elimina espacios y saltos de línea

                    byte[] decodedBytes = Base64.decode(base64Image, Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    if (decodedBitmap != null) {
                        imagePreview.setImageBitmap(decodedBitmap);
                        Log.d("ExerciseDetailsFragment", "✅ Imagen cargada correctamente");
                    } else {
                        Log.e("ExerciseDetailsFragment", "❌ Error: Bitmap nulo luego de decodificar");
                    }
                } catch (Exception e) {
                    Log.e("ExerciseDetailsFragment", "❌ Excepción al decodificar base64: " + e.getMessage(), e);
                }
            } else {
                Log.w("ExerciseDetailsFragment", "⚠ No hay imagen base64 para mostrar");
            }


            if (exercise != null) {
                nameEditText.setText(exercise.getName());
                equipmentTypeEditText.setText(exercise.getEquipmentType());
                exerciseTypeEditText.setText(exercise.getExerciseType());
                musclesWorkedEditText.setText(exercise.getMusclesWorked());
                descriptionEditText.setText(exercise.getDescription());
                // Aquí puedes cargar la imagen en imagePreview si tienes una URL o un recurso
            }
        }

        backButton.setOnClickListener(v -> {
            getParentFragmentManager().popBackStack();
        });
    }
    private void showExerciseDetails(Exercise exercise) {
        ExerciseDetailsFragment detailsFragment = ExerciseDetailsFragment.newInstance(exercise);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }
}