package com.example.diets.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class TrainingDay implements Serializable {
    private int dayIndex;
    private String dayTitle;
    private List<MovementPattern> movementPatterns;
    private List<UserExercise> exercises;
    private int containerId; // Añadir campo containerId

    public TrainingDay() {
        // Constructor vacío necesario para Firebase
        this.movementPatterns = new ArrayList<>();
        this.exercises = new ArrayList<>();
    }

    public TrainingDay(int dayIndex, String dayTitle, List<MovementPattern> movementPatterns, List<UserExercise> exercises, int containerId) {
        this.dayIndex = dayIndex;
        this.dayTitle = dayTitle;
        this.movementPatterns = movementPatterns != null ? movementPatterns : new ArrayList<>();
        this.exercises = exercises != null ? exercises : new ArrayList<>();
        this.containerId = containerId;
    }

    public int getDayIndex() {
        return dayIndex;
    }

    public void setDayIndex(int dayIndex) {
        this.dayIndex = dayIndex;
    }

    public String getDayTitle() {
        return dayTitle;
    }

    public void setDayTitle(String dayTitle) {
        this.dayTitle = dayTitle;
    }

    public List<MovementPattern> getMovementPatterns() {
        return movementPatterns;
    }

    public void setMovementPatterns(List<MovementPattern> movementPatterns) {
        this.movementPatterns = movementPatterns;
    }

    public List<UserExercise> getExercises() { // ✅ Método corregido
        return exercises;
    }

    public void setExercises(List<UserExercise> exercises) { // ✅ Método corregido
        this.exercises = exercises;
    }

    public int getContainerId() {
        return containerId;
    }

    public void setContainerId(int containerId) {
        this.containerId = containerId;
    }

    // ✅ Método para depuración
    @Override
    public String toString() {
        return "TrainingDay{" +
                "dayIndex=" + dayIndex +
                ", dayTitle='" + dayTitle + '\'' +
                ", movementPatterns=" + movementPatterns +
                ", exercises=" + exercises +
                ", containerId=" + containerId +
                '}';
    }
}
