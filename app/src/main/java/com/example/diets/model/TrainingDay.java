package com.example.diets.model;

import java.io.Serializable;
import java.util.List;

public class TrainingDay implements Serializable {
    private String dayName;
    private List<String> movementPatterns;

    public TrainingDay() {
        // Constructor vacío necesario para Firebase
    }

    public TrainingDay(String dayName, List<String> movementPatterns) {
        this.dayName = dayName;
        this.movementPatterns = movementPatterns;
    }

    public String getDayName() {
        return dayName;
    }

    public void setDayName(String dayName) {
        this.dayName = dayName;
    }

    public List<String> getMovementPatterns() {
        return movementPatterns;
    }

    public void setMovementPatterns(List<String> movementPatterns) {
        this.movementPatterns = movementPatterns;
    }
}