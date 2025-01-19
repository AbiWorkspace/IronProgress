package com.example.diets.model;

import java.io.Serializable;
import java.util.List;

public class TrainingDay implements Serializable {
    private int dayIndex;
    private List<MovementPattern> movementPatterns; // Cambiado a List<MovementPattern>
    private List<Exercise> exercises;

    public TrainingDay() {
        // Constructor vacío necesario para Firebase
    }

    public TrainingDay(int dayIndex, List<MovementPattern> movementPatterns, List<Exercise> exercises) {
        this.dayIndex = dayIndex;
        this.movementPatterns = movementPatterns;
        this.exercises = exercises;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public List<MovementPattern> getMovementPatterns() {
        return movementPatterns;
    }

    public void setMovementPatterns(List<MovementPattern> movementPatterns) {
        this.movementPatterns = movementPatterns;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }
}