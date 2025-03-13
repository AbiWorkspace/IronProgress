package com.example.diets.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.adapters.TrainingDetailAdapter;
import com.example.diets.model.UserExercise;

import java.util.ArrayList;
import java.util.List;

public class TrainingDetailFragment extends Fragment {
    private static final String TAG = "TrainingDetailFragment"; // 🔥 Nombre de la clase para logs

    private RecyclerView recyclerViewExercises;
    private TrainingDetailAdapter adapter;
    private List<UserExercise> exercises;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "🟢 onCreateView() → Inflando el layout del fragmento.");

        View view = inflater.inflate(R.layout.fragment_training_detail, container, false);
        Log.d(TAG, "✅ Layout inflado correctamente.");

        // Inicializar RecyclerView
        recyclerViewExercises = view.findViewById(R.id.recyclerViewExercises);
        if (recyclerViewExercises != null) {
            Log.d(TAG, "✅ RecyclerView encontrado en el layout.");
            recyclerViewExercises.setLayoutManager(new LinearLayoutManager(getContext()));
        } else {
            Log.e(TAG, "❌ ERROR: recyclerViewExercises es NULL. Verifica el ID en fragment_training_detail.xml.");
        }

        // Obtener los ejercicios del bundle
        if (getArguments() != null) {
            Log.d(TAG, "📦 Recuperando datos del Bundle...");
            exercises = (List<UserExercise>) getArguments().getSerializable("exercises");

            if (exercises != null) {
                Log.d(TAG, "✅ Se cargaron " + exercises.size() + " ejercicios desde el Bundle.");
            } else {
                Log.w(TAG, "⚠️ El Bundle no contiene ejercicios. Inicializando lista vacía.");
                exercises = new ArrayList<>();
            }
        } else {
            Log.w(TAG, "⚠️ No hay argumentos en el fragmento. Inicializando lista vacía.");
            exercises = new ArrayList<>();
        }

        // 🔥 Configurar adaptador de ejercicios
        Log.d(TAG, "📌 Configurando TrainingDetailAdapter...");
        adapter = new TrainingDetailAdapter(exercises);

        if (recyclerViewExercises != null) {
            recyclerViewExercises.setAdapter(adapter);
            Log.d(TAG, "✅ Adapter configurado correctamente con " + exercises.size() + " ejercicios.");
        } else {
            Log.e(TAG, "❌ ERROR: No se pudo configurar el adaptador porque recyclerViewExercises es NULL.");
        }

        Log.d(TAG, "🟢 onCreateView() finalizado correctamente.");
        return view;
    }
}