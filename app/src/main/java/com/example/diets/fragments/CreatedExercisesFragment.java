package com.example.diets.fragments;

import android.content.ClipData;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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
import com.example.diets.model.MovementPattern;
import com.example.diets.model.TrainingDay;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
public class CreatedExercisesFragment extends Fragment implements AddExerciseDialogFragment.AddExerciseListener, ExercisesAdapter.OnExerciseClickListener {
    private static final String TAG = "CreatedExercisesFragment";
    private RecyclerView recyclerView;
    private Button doneButton, deleteExerciseButton;
    private List<Exercise> exercises = new ArrayList<>();
    private List<Exercise> selectedExercises = new ArrayList<>();
    private static final String FILE_NAME = "exercises.json";
    private DatabaseReference mDatabase;
    private ExercisesAdapter adapter;

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

        doneButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated: Botón doneButton presionado");
            MenuPopupFragment parentFragment = (MenuPopupFragment) getParentFragment();
            if (parentFragment != null) {
                for (Exercise exercise : selectedExercises) {
                    Log.d(TAG, "onViewCreated: Pasando ejercicio seleccionado: " + exercise.getName());
                    parentFragment.exerciseSelected(exercise);
                }
                parentFragment.dismiss();
            } else {
                Log.d(TAG, "onViewCreated: parentFragment es nulo");
            }
            getParentFragmentManager().beginTransaction().remove(this).commit();
        });

        addExerciseButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated: Botón add_exercise_button presionado");
            AddExerciseDialogFragment dialogFragment = new AddExerciseDialogFragment();
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(getParentFragmentManager(), "AddExerciseDialogFragment");
        });

        deleteExerciseButton.setOnClickListener(v -> {
            Log.d(TAG, "onViewCreated: Botón delete_exercise_button presionado");
            showDeleteConfirmationDialog();
        });

        Log.d(TAG, "onViewCreated: Método onViewCreated finalizado");
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
        exercises.removeAll(selectedExercises);
        selectedExercises.clear();
        saveExercisesToInternalStorage();
        adapter.notifyDataSetChanged();
        Log.d(TAG, "deleteSelectedExercises: Ejercicios eliminados y almacenamiento interno actualizado");
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
            Log.d(TAG, "loadExercisesFromInternalStorage: Cargando ejercicios desde el almacenamiento interno");
            FileInputStream fis = getContext().openFileInput(FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            List<Exercise> loadedExercises = (List<Exercise>) ois.readObject();
            ois.close();
            fis.close();

            exercises.clear();
            exercises.addAll(loadedExercises);

            for (Exercise exercise : exercises) {
                Log.d(TAG, "Ejercicio cargado: " + exercise.toString());
            }

            if (recyclerView != null) {
                List<String> selectedExerciseNames = selectedExercises.stream()
                        .map(Exercise::getName)
                        .collect(Collectors.toList());
                adapter = new ExercisesAdapter(exercises, selectedExerciseNames, exercise -> {
                    addExerciseToSelection(exercise);
                    Log.d(TAG, "onExerciseClick: Ejercicio seleccionado: " + exercise.getName());
                });
                recyclerView.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                Log.d(TAG, "loadExercisesFromInternalStorage: Adaptador de ejercicios configurado");
            } else {
                Log.e(TAG, "loadExercisesFromInternalStorage: RecyclerView es nulo");
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Error al cargar ejercicios", e);
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

    private void saveExercisesToInternalStorage() {
        try {
            Log.d(TAG, "saveExercisesToInternalStorage: Guardando ejercicios en el almacenamiento interno");
            FileOutputStream fos = getContext().openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(new ArrayList<>(exercises)); // Asegurarse de escribir una nueva lista
            oos.close();
            fos.close();
            Log.d(TAG, "saveExercisesToInternalStorage: Ejercicios guardados en el almacenamiento interno");
        } catch (IOException e) {
            Log.e(TAG, "Error al guardar ejercicios", e);
        }
    }
}