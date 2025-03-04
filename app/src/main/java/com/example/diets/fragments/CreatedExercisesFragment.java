package com.example.diets.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.diets.R;
import com.example.diets.activitys.AddExerciseDialogFragment;
import com.example.diets.adapters.ExercisesAdapter;
import com.example.diets.model.Exercise;
import com.google.common.reflect.TypeToken;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.io.BufferedReader;
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
import java.util.List;
import java.util.stream.Collectors;

public class CreatedExercisesFragment extends Fragment implements AddExerciseDialogFragment.AddExerciseListener, ExercisesAdapter.OnExerciseClickListener, ExercisesAdapter.OnExerciseSelectionChangedListener {
    private static final String TAG = "CreatedExercisesFragment";
    private RecyclerView recyclerView;
    private Button doneButton, deleteExerciseButton;
    private List<Exercise> exercises = new ArrayList<>();
    private List<Exercise> selectedExercises = new ArrayList<>();
    private static final String FILE_NAME = "exercises.json";
    private DatabaseReference mDatabase;
    private ExercisesAdapter adapter;

    private MenuPopupFragment.OnExerciseSelectedListener listener;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: Inflando el layout del fragmento");
        return inflater.inflate(R.layout.fragment_created_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recyclerView);
        doneButton = view.findViewById(R.id.doneButton);
        Button addExerciseButton = view.findViewById(R.id.add_exercise_button);
        deleteExerciseButton = view.findViewById(R.id.delete_exercise_button);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            Log.d(TAG, "onViewCreated: RecyclerView inicializado");
        } else {
            Log.e(TAG, "onViewCreated: RecyclerView es nulo");
        }

        // Cargar ejercicios desde el almacenamiento interno
        loadExercisesFromInternalStorage();

        // ✅ 3. Modificado: Ahora usa el listener para pasar ejercicios a `MenuPopupFragment`
        doneButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated: Botón doneButton presionado");

            if (listener != null) {
                for (Exercise exercise : selectedExercises) {
                    listener.onExerciseSelected(exercise); // ✅ Envía ejercicios al `MenuPopupFragment`
                }
            }

            // Cierra el fragmento después de enviar los ejercicios
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        addExerciseButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated: Botón add_exercise_button presionado");
            AddExerciseDialogFragment dialogFragment = new AddExerciseDialogFragment();
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), "AddExerciseDialogFragment");
        });

        deleteExerciseButton.setOnClickListener(v -> showDeleteConfirmationDialog());
    }

    public void setOnExerciseSelectedListener(MenuPopupFragment.OnExerciseSelectedListener listener) {
        this.listener = listener;
    }
    public interface OnExerciseSelectedListener {
        void onExerciseSelected(Exercise exercise);
    }
    private void showDeleteConfirmationDialog() {
        new AlertDialog.Builder(getContext())
                .setTitle("Eliminar Ejercicio")
                .setMessage("¿Estás seguro de que deseas eliminar los ejercicios seleccionados?")
                .setPositiveButton("Sí", (dialog, which) -> {
                    deleteSelectedExercises();
                })
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteSelectedExercises() {
        Log.d(TAG, "deleteSelectedExercises: Ejercicios antes de la eliminación: " + exercises.toString());
        Log.d(TAG, "deleteSelectedExercises: Ejercicios seleccionados para eliminar: " + selectedExercises.toString());

        exercises.removeAll(selectedExercises);
        selectedExercises.clear();

        saveExercisesToInternalStorage();

        adapter.notifyDataSetChanged();

        Log.d(TAG, "deleteSelectedExercises: Ejercicios después de la eliminación: " + exercises.toString());
        Log.d(TAG, "deleteSelectedExercises: Ejercicios eliminados y almacenamiento interno actualizado");
    }

    private void saveExercisesToInternalStorage() {
        try {
            Log.d(TAG, "saveExercisesToInternalStorage: Guardando ejercicios en el almacenamiento interno");
            File file = new File(getContext().getFilesDir(), FILE_NAME);
            FileWriter writer = new FileWriter(file);
            Gson gson = new Gson();

            // Guardamos en formato JSON correcto
            String json = gson.toJson(exercises);

            writer.write(json);
            writer.close();

            Log.d(TAG, "✅ Ejercicios guardados correctamente en JSON: " + json);
        } catch (IOException e) {
            Log.e(TAG, "❌ Error al guardar ejercicios", e);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        Log.d(TAG, "onDestroyView: View destroyed");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.d(TAG, "onDetach: Fragment detached");
    }

    private void loadExercisesFromInternalStorage() {
        try {
            File file = new File(getContext().getFilesDir(), FILE_NAME);
            if (!file.exists()) {
                Log.e(TAG, "❌ Archivo de ejercicios no encontrado.");
                return;
            }

            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder jsonBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
            reader.close();

            String jsonData = jsonBuilder.toString();
            Log.d(TAG, "📥 JSON leído desde almacenamiento interno: " + jsonData);

            // Validar si el JSON es un array
            if (!jsonData.trim().startsWith("[")) {
                Log.e(TAG, "⚠ JSON inválido: Se esperaba un array pero se encontró otra estructura.");
                return;
            }

            Gson gson = new Gson();
            Type listType = new TypeToken<List<Exercise>>() {}.getType();
            List<Exercise> loadedExercises = gson.fromJson(jsonData, listType);

            if (loadedExercises != null) {
                exercises.clear();
                exercises.addAll(loadedExercises);
                Log.d(TAG, "✅ Ejercicios cargados correctamente: " + exercises.size());
            } else {
                Log.e(TAG, "❌ Error al convertir JSON a lista de ejercicios.");
            }

            setupRecyclerView();

        } catch (Exception e) {
            Log.e(TAG, "❌ Error al cargar ejercicios desde almacenamiento interno", e);
        }
    }
    private void setupRecyclerView() {
        if (recyclerView != null) {
            List<String> selectedExerciseNames = selectedExercises.stream()
                    .map(Exercise::getName)
                    .collect(Collectors.toList());

            adapter = new ExercisesAdapter(exercises, selectedExerciseNames, exercise -> {
                addExerciseToSelection(exercise);
                Log.d(TAG, "onExerciseClick: Ejercicio seleccionado: " + exercise.getName());
                showExerciseDetails(exercise); // Mostrar detalles del ejercicio
            }, this); // Pasar 'this' como OnExerciseSelectionChangedListener

            recyclerView.setAdapter(adapter);
            adapter.notifyDataSetChanged();
            Log.d(TAG, "✅ Adaptador de ejercicios configurado correctamente.");
        } else {
            Log.e(TAG, "❌ setupRecyclerView: RecyclerView es nulo.");
        }
    }

    public void addExerciseToSelection(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
            Log.d(TAG, "addExerciseToSelection: Ejercicio añadido: " + exercise.getName());
        } else {
            Log.d(TAG, "addExerciseToSelection: Ejercicio ya estaba en la lista: " + exercise.getName());
        }
    }

    private void showExerciseDetails(Exercise exercise) {
        ExerciseDetailsFragment detailsFragment = ExerciseDetailsFragment.newInstance(exercise);
        getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, detailsFragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onExerciseAdded(Exercise exercise) {
        Log.d(TAG, "onExerciseAdded: Ejercicio agregado: " + exercise.getName());
        exercises.add(exercise);
        adapter.notifyItemInserted(exercises.size() - 1);
        saveExercisesToInternalStorage();
    }

    @Override
    public void onExerciseClick(Exercise exercise) {
        addExerciseToSelection(exercise);
    }

    @Override
    public void onExerciseSelected(Exercise exercise) {
        if (!selectedExercises.contains(exercise)) {
            selectedExercises.add(exercise);
            Log.d(TAG, "onExerciseSelected: Ejercicio añadido: " + exercise.getName());
        }
    }

    @Override
    public void onExerciseDeselected(Exercise exercise) {
        selectedExercises.remove(exercise);
        Log.d(TAG, "onExerciseDeselected: Ejercicio eliminado: " + exercise.getName());
    }
}
