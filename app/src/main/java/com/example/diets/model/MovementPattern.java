package com.example.diets.model;

import android.util.Log;
import androidx.annotation.NonNull;
import java.util.ArrayList;
import java.util.List;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.firebase.database.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class MovementPattern implements Serializable {
    private static final long serialVersionUID = 1L; // 🔹 Importante para evitar problemas de serialización

    private String id;
    private String name;
    private List<String> synergistMuscles = new ArrayList<>();
    private List<String> exerciseIds = new ArrayList<>();
    private List<String> musclesWorked = new ArrayList<>();
    // Constructor sin argumentos requerido por Firebase
    public MovementPattern() {}

    public MovementPattern(String id, String name, List<String> synergistMuscles, List<String> exerciseIds) {
        this.id = id;
        this.name = name;
        this.synergistMuscles = (synergistMuscles != null) ? new ArrayList<>(synergistMuscles) : new ArrayList<>();
        this.exerciseIds = (exerciseIds != null) ? new ArrayList<>(exerciseIds) : new ArrayList<>();
    }

    // Getters y Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getSynergistMuscles() {
        return synergistMuscles;
    }

    public void setSynergistMuscles(List<String> synergistMuscles) {
        this.synergistMuscles = (synergistMuscles != null) ? new ArrayList<>(synergistMuscles) : new ArrayList<>();
    }

    public List<String> getExerciseIds() {
        return exerciseIds;
    }

    public void setExerciseIds(List<String> exerciseIds) {
        this.exerciseIds = (exerciseIds != null) ? new ArrayList<>(exerciseIds) : new ArrayList<>();
    }

    // ✅ Nueva interfaz para la carga asíncrona de ejercicios
    public interface OnExercisesLoadedListener {
        void onExercisesLoaded(List<Exercise> exercises);
    }

    // ✅ Método corregido para obtener ejercicios con un callback
    public void getExercises(DatabaseReference exercisesRef, final OnExercisesLoadedListener listener) {
        List<Exercise> exercises = new ArrayList<>();

        if (exerciseIds.isEmpty()) {
            listener.onExercisesLoaded(exercises);
            return;
        }

        for (String exerciseId : exerciseIds) {
            exercisesRef.child(exerciseId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Exercise exercise = snapshot.getValue(Exercise.class);
                    if (exercise != null) {
                        exercises.add(exercise);
                    }
                    if (exercises.size() == exerciseIds.size()) {
                        listener.onExercisesLoaded(exercises);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Error al obtener ejercicio: " + error.getMessage());
                }
            });
        }
    }

    public List<String> getMusclesWorked() {
        return musclesWorked;
    }

    public void setMusclesWorked(List<String> musclesWorked) {
        this.musclesWorked = (musclesWorked != null) ? new ArrayList<>(musclesWorked) : new ArrayList<>();
    }
    @Override
    public String toString() {
        return "MovementPattern{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", synergistMuscles=" + synergistMuscles +
                ", exerciseIds=" + exerciseIds +
                '}';
    }
}
