package com.example.diets.model;

import android.util.Log;

public class ExerciseSeries {
    private static final String TAG = "ExerciseSeries"; // 🔥 Nombre de la clase para logs
    private int seriesNumber;
    private int reps;
    private double weight; // ✅ Debe ser double
    private double rpe;    // ✅ Debe ser double (no int)
    private boolean isCompleted; // ✅ Nuevo atributo para indicar si la serie está completada
    private Double previousLoad;
    private long date;          // Epoch millis
    public long  getDate()  { return date; }
    public void  setDate(long d){ this.date = d; }

    // 🔥 Constructor vacío requerido por Firebase
    public ExerciseSeries() {
        this.seriesNumber = 0;
        this.reps = 0;
        this.weight = 0.0;
        this.rpe = 0.0;
        this.isCompleted = false;
    }

    // 🔥 Constructor principal con todos los atributos
    public ExerciseSeries(int seriesNumber, int reps, double weight, double rpe, boolean isCompleted) {
        Log.d(TAG, "ExerciseSeries - Constructor: Creando nueva serie.");
        this.seriesNumber = seriesNumber;
        this.reps = validateReps(reps);
        this.weight = validateWeight(weight);
        this.rpe = validateRpe(rpe);
        this.isCompleted = isCompleted;

        Log.d(TAG, "✅ Serie creada: Número de serie: " + seriesNumber + ", Reps: " + this.reps + ", Peso: " + this.weight + ", RPE: " + this.rpe + ", Completada: " + this.isCompleted);
    }

    // 🔥 Constructor alternativo
    public ExerciseSeries(int seriesNumber, int reps, double weight, double rpe) {
        this(seriesNumber, reps, weight, rpe, false);
    }

    // 🔥 Getters y Setters con logs
    public int getSeriesNumber() {
        return seriesNumber;
    }

    public void setSeriesNumber(int seriesNumber) {
        this.seriesNumber = seriesNumber;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = validateReps(reps);
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = validateWeight(weight);
    }

    public double getRpe() {
        return rpe;
    }

    public void setRpe(double rpe) {
        this.rpe = validateRpe(rpe);
    }

    public boolean isCompleted() {
        Log.d(TAG, "ExerciseSeries - isCompleted: Estado de completado: " + isCompleted);
        return isCompleted;
    }

    public void setCompleted(boolean completed) {

        this.isCompleted = completed;
    }

    public Double getPreviousLoad() {
        return previousLoad;
    }

    public void setPreviousLoad(Double previousLoad) {
        this.previousLoad = previousLoad;
    }

    @Override
    public String toString() {
        String info = "Serie " + seriesNumber + " → Reps: " + reps + ", Peso: " + weight + " kg, RPE: " + rpe + ", Completada: " + isCompleted;
        Log.d(TAG, "ExerciseSeries - toString: " + info);
        return info;
    }

    // 🔥 Métodos de validación
    private int validateReps(int reps) {
        if (reps < 1 || reps > 30) {
            Log.e(TAG, "❌ ERROR: Reps fuera de rango (1-30). Asignando 1 por defecto.");
            return 1;
        }
        return reps;
    }

    private double validateWeight(double weight) {
        if (weight < 0 || weight > 500) {
            Log.e(TAG, "❌ ERROR: Peso fuera de rango (0-500 kg). Asignando 0.0 kg por defecto.");
            return 0.0;
        }
        return weight;
    }

    private double validateRpe(double rpe) {
        if (rpe < 0 || rpe > 10) {
            Log.e(TAG, "❌ ERROR: RPE fuera de rango (0-10). Asignando 0.0 por defecto.");
            return 0.0;
        }
        return rpe;
    }

    // 🔥 Método para asegurar RPE correcto al leer de Firebase
    public static double getRpeFromFirebase(Object rpeValue) {
        if (rpeValue instanceof Long) {
            return ((Long) rpeValue).doubleValue();
        } else if (rpeValue instanceof Double) {
            return (double) rpeValue;
        } else if (rpeValue instanceof String) {
            try {
                return Double.parseDouble((String) rpeValue);
            } catch (NumberFormatException e) {
                Log.e(TAG, "❌ ERROR: RPE en Firebase no es un número válido. Asignando 0.0 por defecto.");
                return 0.0;
            }
        } else {
            Log.e(TAG, "❌ ERROR: Tipo de dato inesperado para RPE en Firebase. Asignando 0.0 por defecto.");
            return 0.0;
        }
    }

    public String toSimple() {
        return "w=" + getWeight()
                + " r=" + getReps()
                + " rpe=" + getRpe();
    }

}
