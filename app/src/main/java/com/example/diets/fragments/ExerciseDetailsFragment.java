package com.example.diets.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.diets.R;
import com.example.diets.adapters.ImagePagerAdapter;
import com.example.diets.model.Exercise;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class ExerciseDetailsFragment extends Fragment {

    private static final String TAG = "ExerciseDetailsFragment";
    private static final String ARG_EXERCISE = "exercise";

    private ViewPager2 imagePager;
    private final List<Bitmap> carouselBitmaps = new ArrayList<>();

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
        Button backButton = view.findViewById(R.id.backButton);
        imagePager = view.findViewById(R.id.imagePager);

        if (getArguments() != null) {
            Exercise exercise = (Exercise) getArguments().getSerializable(ARG_EXERCISE);
            Log.d(TAG, "📦 Objeto recibido: " + (exercise != null ? exercise.getName() : "null"));

            if (exercise != null) {
                nameEditText.setText(exercise.getName());
                equipmentTypeEditText.setText(exercise.getEquipmentType());
                exerciseTypeEditText.setText(exercise.getExerciseType());
                musclesWorkedEditText.setText(exercise.getMusclesWorked());
                descriptionEditText.setText(exercise.getDescription());

                List<String> base64Images = exercise.getImages();



                if (base64Images != null && !base64Images.isEmpty()) {
                    Log.d(TAG, "🖼️ Imágenes encontradas localmente. Intentando decodificar...");

                    boolean algunaDecodificada = cargarImagenesDesdeListaYVerificar(base64Images);

                    if (!algunaDecodificada) {
                        Log.w(TAG, "⚠ Ninguna imagen local válida. Intentando buscar en Firebase...");
                        buscarImagenesPorNombre(exercise.getName());
                    }
                } else {
                    Log.d(TAG, "🌐 No hay imágenes locales. Buscando en Firebase...");
                    buscarImagenesPorNombre(exercise.getName());
                }

            }
        }

        backButton.setOnClickListener(v -> {
            Fragment parent = getParentFragment();
            if (parent != null) {
                // 🔙 Volver en el MISMO FragmentManager donde se hizo el replace
                parent.getChildFragmentManager().popBackStack();
            } else {
                // Fallback por si algún día lo usas como fragmento "normal"
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

    }

    private boolean cargarImagenesDesdeListaYVerificar(List<String> base64Images) {
        carouselBitmaps.clear();
        boolean algunaExitosa = false;

        for (int i = 0; i < base64Images.size(); i++) {
            String base64 = base64Images.get(i);

            if (base64 != null && !base64.trim().isEmpty()) {
                try {
                    base64 = base64.replaceAll("\\s", "");
                    byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

                    if (bitmap != null) {
                        carouselBitmaps.add(bitmap);
                        algunaExitosa = true;
                        Log.d(TAG, "✅ Imagen local [" + i + "] decodificada correctamente.");
                    } else {
                        Log.w(TAG, "⚠ Imagen local [" + i + "] retornó bitmap nulo.");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error al decodificar imagen local [" + i + "]: " + e.getMessage());
                }
            } else {
                Log.w(TAG, "⚠ Imagen local [" + i + "] está vacía o es nula.");
            }
        }

        if (algunaExitosa) {
            mostrarImagenesEnCarrusel();
        }

        return algunaExitosa;
    }

    private void buscarImagenesPorNombre(String nombreEjercicio) {
        Log.d(TAG, "🔍 Buscando imágenes en Firebase para: " + nombreEjercicio);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("exerciseImages");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                boolean matchFound = false;

                for (DataSnapshot ejercicioSnapshot : snapshot.getChildren()) {
                    String nombreFirebase = ejercicioSnapshot.child("name").getValue(String.class);

                    if (nombreFirebase != null && nombreFirebase.equalsIgnoreCase(nombreEjercicio)) {
                        Log.d(TAG, "✅ Coincidencia encontrada en Firebase: " + nombreFirebase);
                        DataSnapshot imagesSnapshot = ejercicioSnapshot.child("images");
                        cargarImagenesDesdeSnapshot(imagesSnapshot);
                        matchFound = true;
                        break;
                    }
                }

                if (!matchFound) {
                    Log.w(TAG, "⚠ No se encontraron imágenes en Firebase para: " + nombreEjercicio);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "❌ Error al consultar exerciseImages", error.toException());
            }
        });
    }

    private void cargarImagenesDesdeSnapshot(DataSnapshot imagesSnapshot) {
        carouselBitmaps.clear();

        for (DataSnapshot imageEntry : imagesSnapshot.getChildren()) {
            String base64 = imageEntry.getValue(String.class);
            if (base64 != null && !base64.trim().isEmpty()) {
                try {
                    base64 = base64.replaceAll("\\s", "");
                    byte[] decodedBytes = Base64.decode(base64, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    if (bitmap != null) {
                        carouselBitmaps.add(bitmap);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Error al decodificar una imagen base64 de Firebase", e);
                }
            }
        }

        if (!carouselBitmaps.isEmpty()) {
            mostrarImagenesEnCarrusel();
            Log.d(TAG, "✅ Imágenes Firebase cargadas: " + carouselBitmaps.size());
        } else {
            Log.w(TAG, "⚠ No se cargaron imágenes válidas desde Firebase.");
        }
    }

    private void mostrarImagenesEnCarrusel() {
        ImagePagerAdapter adapter = new ImagePagerAdapter();
        adapter.submit(carouselBitmaps);
        imagePager.setAdapter(adapter);
    }
}
