package com.example.diets.model;

public class UserExercise extends Exercise {
    private boolean isCompleted;
    private int maxReps;
    private int currentLoad;
    // Otros campos específicos del usuario

    public UserExercise(Exercise exercise) {
        super(exercise.getId(), exercise.getName(), exercise.getEquipmentType(), exercise.getExerciseType(), exercise.getMusclesWorked(), exercise.getLoadIntensity(), exercise.getImage(), exercise.getDescription(), exercise.getMovementType(), exercise.getSynergistMuscles(), exercise.getGoals(), exercise.getMovementPatterns());
        this.isCompleted = false;
        this.maxReps = 0;
        this.currentLoad = 0;
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

    // Otros getters y setters
}