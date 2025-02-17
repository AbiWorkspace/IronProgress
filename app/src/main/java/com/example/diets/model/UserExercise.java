package com.example.diets.model;

public class UserExercise extends Exercise {
    private boolean isCompleted;
    private int maxReps;
    private int currentLoad;
    private String familiarity; // Nuevo campo para familiaridad

    // Constructor sin argumentos requerido por Firebase
    public UserExercise() {
        super();
        this.isCompleted = false;
        this.maxReps = 0;
        this.currentLoad = 0;
        this.familiarity = ""; // Inicializar familiaridad
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public int getMaxReps() {
        return maxReps;
    }

    public void setMaxReps(int maxReps) {
        this.maxReps = maxReps;
    }

    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }

    public String getFamiliarity() {
        return familiarity;
    }

    public void setFamiliarity(String familiarity) {
        this.familiarity = familiarity;
    }
}