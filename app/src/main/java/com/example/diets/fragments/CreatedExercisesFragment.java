package com.example.diets.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.diets.R;
import com.example.diets.activitys.AddExerciseDialogFragment;
import com.example.diets.model.Exercise;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class CreatedExercisesFragment extends Fragment implements AddExerciseDialogFragment.AddExerciseListener {
    private RecyclerView recyclerView;
    private ExercisesAdapter adapter;
    private List<Exercise> exercises = new ArrayList<>();
    private static final String FILE_NAME = "exercises.json";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d("CreatedExercisesFragment", "onCreateView: Inflando el layout del fragmento");
        return inflater.inflate(R.layout.fragment_created_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.d("CreatedExercisesFragment", "onViewCreated: Vista creada");

        recyclerView = view.findViewById(R.id.recyclerView);
        Button addExerciseButton = view.findViewById(R.id.add_exercise_button);

        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new ExercisesAdapter(exercises);
            recyclerView.setAdapter(adapter);
            Log.d("CreatedExercisesFragment", "onViewCreated: RecyclerView configurado");
        } else {
            Log.e("CreatedExercisesFragment", "RecyclerView es nulo");
        }

        addExerciseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("CreatedExercisesFragment", "addExerciseButton: Botón de agregar ejercicio presionado");
                showAddExerciseDialog();
            }
        });

        // Cargar ejercicios desde el almacenamiento interno
        loadExercisesFromInternalStorage();
    }

    private void showAddExerciseDialog() {
        Log.d("CreatedExercisesFragment", "showAddExerciseDialog: Mostrando diálogo para agregar ejercicio");
        AddExerciseDialogFragment dialog = new AddExerciseDialogFragment();
        dialog.setAddExerciseListener(this);
        dialog.show(getChildFragmentManager(), "AddExerciseDialogFragment");
    }

    @Override
    public void onExerciseAdded(Exercise exercise) {
        Log.d("CreatedExercisesFragment", "onExerciseAdded: Ejercicio agregado: " + exercise.getName());
        exercises.add(exercise);
        adapter.notifyItemInserted(exercises.size() - 1);
        saveExercisesToInternalStorage();
    }

    private void saveExercisesToInternalStorage() {
        try {
            Log.d("CreatedExercisesFragment", "saveExercisesToInternalStorage: Guardando ejercicios en el almacenamiento interno");
            FileOutputStream fos = getContext().openFileOutput(FILE_NAME, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(new ArrayList<>(exercises)); // Asegurarse de escribir una nueva lista
            oos.close();
            fos.close();
            Log.d("CreatedExercisesFragment", "saveExercisesToInternalStorage: Ejercicios guardados en el almacenamiento interno");
        } catch (IOException e) {
            Log.e("CreatedExercisesFragment", "Error al guardar ejercicios", e);
        }
    }

    private void loadExercisesFromInternalStorage() {
        try {
            Log.d("CreatedExercisesFragment", "loadExercisesFromInternalStorage: Cargando ejercicios desde el almacenamiento interno");
            FileInputStream fis = getContext().openFileInput(FILE_NAME);
            ObjectInputStream ois = new ObjectInputStream(fis);
            List<Exercise> loadedExercises = (List<Exercise>) ois.readObject();
            ois.close();
            fis.close();

            exercises.clear();
            exercises.addAll(loadedExercises);

            // Imprimir detalles de cada ejercicio cargado
            for (Exercise exercise : exercises) {
                Log.d("CreatedExercisesFragment", "Ejercicio cargado: " + exercise.toString());
            }

            adapter.notifyDataSetChanged();
            Log.d("CreatedExercisesFragment", "loadExercisesFromInternalStorage: Ejercicios cargados desde el almacenamiento interno y adaptador notificado");
        } catch (IOException | ClassNotFoundException e) {
            Log.e("CreatedExercisesFragment", "Error al cargar ejercicios", e);
        }
    }

    private static class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.ViewHolder> {
        private final List<Exercise> exercises;

        ExercisesAdapter(List<Exercise> exercises) {
            this.exercises = exercises;
            Log.d("ExercisesAdapter", "ExercisesAdapter: Adaptador inicializado con " + exercises.size() + " ejercicios");
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            Log.d("ExercisesAdapter", "onCreateViewHolder: Creando ViewHolder");
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_exercise, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Exercise exercise = exercises.get(position);
            holder.exerciseName.setText(exercise.getName());
            Log.d("ExercisesAdapter", "onBindViewHolder: Enlazando ViewHolder para el ejercicio: " + exercise.getName());
        }

        @Override
        public int getItemCount() {
            int count = exercises.size();
            Log.d("ExercisesAdapter", "getItemCount: Número de ejercicios en el adaptador: " + count);
            return count;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView exerciseName;

            ViewHolder(View itemView) {
                super(itemView);
                exerciseName = itemView.findViewById(R.id.exerciseName);
                Log.d("ExercisesAdapter", "ViewHolder: ViewHolder inicializado");
            }
        }
    }
}