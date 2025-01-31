package com.example.diets.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
public class TrainingDay implements Serializable {
    private int dayIndex;
    private List<MovementPattern> movementPatterns;
    private List<Exercise> exercises;
    private int containerId; // Añadir campo containerId

    public TrainingDay() {
        // Constructor vacío necesario para Firebase
        this.movementPatterns = new ArrayList<>();
        this.exercises = new ArrayList<>();
    }

    public TrainingDay(int dayIndex, List<MovementPattern> movementPatterns, List<Exercise> exercises) {
        this.dayIndex = dayIndex;
        this.movementPatterns = movementPatterns != null ? movementPatterns : new ArrayList<>();
        this.exercises = exercises != null ? exercises : new ArrayList<>();
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

    public int getContainerId() {
        return containerId;
    }

    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }
}