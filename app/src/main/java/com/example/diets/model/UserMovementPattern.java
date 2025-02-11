package com.example.diets.model;

import java.util.ArrayList;
import java.util.List;

public class UserMovementPattern extends MovementPattern {
    private List<UserExercise> userExercises;

    // Constructor sin argumentos requerido por Firebase
    public UserMovementPattern() {
        this.userExercises = new ArrayList<>();
    }

    public UserMovementPattern(MovementPattern pattern) {
        super(pattern.getId(), pattern.getName(), pattern.getSynergistMuscles(), pattern.getExercises());
        this.userExercises = new ArrayList<>();
    }

    public List<UserExercise> getUserExercises() {
        return userExercises;
    }

    public void setUserExercises(List<UserExercise> userExercises) {
        this.userExercises = userExercises;
    }
}