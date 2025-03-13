package com.example.diets.model;

import android.util.Log;

public class ExerciseSeries {
    private static final String TAG = "ExerciseSeries"; // 🔥 Nombre de la clase para logs
    private int seriesNumber;
    private int reps;
    private double weight; // ✅ Debe ser double
    private double rpe;    // ✅ Debe ser double (no int)

    public ExerciseSeries(int seriesNumber, int reps, double weight, double rpe) {
        Log.d(TAG, "ExerciseSeries - Constructor: Creando nueva serie.");
        this.seriesNumber = seriesNumber;
        this.reps = reps;
        this.weight = weight;
        this.rpe = rpe;

        Log.d(TAG, "✅ Serie creada: Número de serie: " + seriesNumber + ", Reps: " + reps + ", Peso: " + weight + ", RPE: " + rpe);
    }

    // 🔥 Getters y Setters con logs
    public int getSeriesNumber() {
        Log.d(TAG, "ExerciseSeries - getSeriesNumber: Obteniendo número de serie: " + seriesNumber);
        return seriesNumber;
    }

    public void setSeriesNumber(int seriesNumber) {
        Log.d(TAG, "ExerciseSeries - setSeriesNumber: Modificando número de serie a: " + seriesNumber);
        this.seriesNumber = seriesNumber;
    }

    public int getReps() {
        Log.d(TAG, "ExerciseSeries - getReps: Obteniendo reps: " + reps);
        return reps;
    }

    public void setReps(int reps) {
        if (reps < 0) {
            Log.e(TAG, "❌ ERROR: Intento de asignar reps negativas. Valor inválido: " + reps);
            return;
        }
        Log.d(TAG, "ExerciseSeries - setReps: Modificando reps a: " + reps);
        this.reps = reps;
    }

    public double getWeight() {
        Log.d(TAG, "ExerciseSeries - getWeight: Obteniendo peso: " + weight);
        return weight;
    }

    public void setWeight(double weight) {
        if (weight < 0) {
            Log.e(TAG, "❌ ERROR: Intento de asignar peso negativo. Valor inválido: " + weight);
            return;
        }
        Log.d(TAG, "ExerciseSeries - setWeight: Modificando peso a: " + weight);
        this.weight = weight;
    }

    public double getRpe() {
        Log.d(TAG, "ExerciseSeries - getRpe: Obteniendo RPE: " + rpe);
        return rpe;
    }

    public void setRpe(double rpe) {
        if (rpe < 0 || rpe > 10) {
            Log.e(TAG, "❌ ERROR: Intento de asignar RPE fuera de rango (0-10). Valor inválido: " + rpe);
            return;
        }
        Log.d(TAG, "ExerciseSeries - setRpe: Modificando RPE a: " + rpe);
        this.rpe = rpe;
    }

    @Override
    public String toString() {
        String info = "Serie " + seriesNumber + " → Reps: " + reps + ", Peso: " + weight + " kg, RPE: " + rpe;
        Log.d(TAG, "ExerciseSeries - toString: " + info);
        return info;
    }
}