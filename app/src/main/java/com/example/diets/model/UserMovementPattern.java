package com.example.diets.model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class UserMovementPattern extends MovementPattern {
    private List<UserExercise> userExercises;

    // Constructor sin argumentos requerido por Firebase
    public UserMovementPattern() {
        super(); // Llama al constructor vacío de MovementPattern
        this.userExercises = new ArrayList<>();
    }

    // Constructor que solo recibe un MovementPattern
    public UserMovementPattern(MovementPattern pattern) {
        super(pattern.getId(), pattern.getName(), pattern.getSynergistMuscles(), pattern.getExerciseIds()); // ✅ Agregado getSynergistMuscles()
        this.userExercises = new ArrayList<>();
    }

    // Constructor que recibe un MovementPattern y recupera ejercicios desde Firebase
    public UserMovementPattern(MovementPattern pattern, DatabaseReference exercisesRef) {
        super(pattern.getId(), pattern.getName(), pattern.getSynergistMuscles(), pattern.getExerciseIds()); // ✅ Agregado getSynergistMuscles()
        this.userExercises = new ArrayList<>();
        loadExercisesFromFirebase(exercisesRef, pattern.getExerciseIds());
    }

    // Método para cargar ejercicios desde Firebase
    private void loadExercisesFromFirebase(DatabaseReference exercisesRef, List<String> exerciseIds) {
        for (String exerciseId : exerciseIds) {
            exercisesRef.child(exerciseId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    Exercise exercise = snapshot.getValue(Exercise.class);
                    if (exercise != null) {
                        userExercises.add(new UserExercise(exercise));
                        Log.d("Firebase", "Ejercicio añadido a UserMovementPattern: " + exercise.getName());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Log.e("Firebase", "Error al obtener ejercicio: " + error.getMessage());
                }
            });
        }
    }

    public List<UserExercise> getUserExercises() {
        return userExercises;
    }

    public void setUserExercises(List<UserExercise> userExercises) {
        this.userExercises = userExercises;
    }

    // Método para obtener los patrones de movimiento
    public List<MovementPattern> getMovementPatterns() {
        List<MovementPattern> movementPatterns = new ArrayList<>();
        movementPatterns.add(this);
        return movementPatterns;
    }

    // Método para obtener el título del patrón de movimiento
    public String getTitle() {
        return super.getName();
    }

    @Override
    public String toString() {
        return "UserMovementPattern{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", synergistMuscles=" + getSynergistMuscles() +
                ", exerciseIds=" + getExerciseIds() +
                ", userExercises=" + userExercises +
                '}';
    }
}
