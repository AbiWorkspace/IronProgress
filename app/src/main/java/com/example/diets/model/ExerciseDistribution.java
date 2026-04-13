package com.example.diets.model;

import java.util.ArrayList;
import java.util.List;

public class ExerciseDistribution {
    private String title;
    private int numberOfDays;
    private List<TrainingDay> trainingDays;
    private boolean frecuenciaSimetrica;
    private String frecuencia;
    private boolean recomendado;

    // Constructor vacío necesario para Firebase
    public ExerciseDistribution() {}

    // Constructor completo
    public ExerciseDistribution(String title, int numberOfDays, List<TrainingDay> trainingDays, boolean frecuenciaSimetrica, String frecuencia, boolean recomendado) {
        this.title = title;
        this.numberOfDays = numberOfDays;
        this.trainingDays = trainingDays;
        this.frecuenciaSimetrica = frecuenciaSimetrica;
        this.frecuencia = frecuencia;
        this.recomendado = recomendado;
    }

    // Getters y Setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }

    public void setNumberOfDays(int numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    public List<TrainingDay> getTrainingDays() {
        return trainingDays;
    }

    public void setTrainingDays(List<TrainingDay> trainingDays) {
        this.trainingDays = trainingDays;
    }

    public boolean isFrecuenciaSimetrica() {
        return frecuenciaSimetrica;
    }

    public void setFrecuenciaSimetrica(boolean frecuenciaSimetrica) {
        this.frecuenciaSimetrica = frecuenciaSimetrica;
    }

    public String getFrecuencia() {
        return frecuencia;
    }

    public void setFrecuencia(String frecuencia) {
        this.frecuencia = frecuencia;
    }

    public boolean isRecomendado() {
        return recomendado;
    }

    public void setRecomendado(boolean recomendado) {
        this.recomendado = recomendado;
    }

    public static List<ExerciseDistribution> filterByNumberOfDays(List<ExerciseDistribution> distributions, int selectedDays) {
        List<ExerciseDistribution> filteredDistributions = new ArrayList<>();
        for (ExerciseDistribution distribution : distributions) {
            if (distribution.getNumberOfDays() == selectedDays) {
                filteredDistributions.add(distribution);
            }
        }
        return filteredDistributions;
    }
}
