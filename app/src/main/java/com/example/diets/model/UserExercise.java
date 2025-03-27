package com.example.diets.model;

import android.util.Log;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UserExercise extends Exercise implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean isCompleted;
    private int maxReps;
    private int currentLoad;
    private String familiarity;
    private int series;
    private double weight;
    private int reps;
    private double rpe;
    private String dayName;
    private List<ExerciseSeries> exerciseSeriesList;


    @PropertyName("name")  // 🔥 Se mapea correctamente a Firebase
    private String exerciseName;



    // ✅ Constructor para inicializar desde un ejercicio base
    public UserExercise(Exercise exercise) {
        super(
                exercise.getId(),
                exercise.getName(),
                exercise.getEquipmentType(),
                exercise.getExerciseType(),
                exercise.getMusclesWorked(),
                exercise.getLoadIntensity(),
                exercise.getImage(),
                exercise.getDescription(),
                exercise.getMovementType(),
                safeConvertToList(exercise.getSynergistMuscles()),
                safeConvertToList(exercise.getGoals()),
                safeConvertToMovementPatternList(exercise.getMovementPatterns())
        );

        this.isCompleted = false;
        this.maxReps = 0;
        this.currentLoad = 0;
        this.familiarity = "";
        this.series = 3;
        this.dayName = "";
        this.exerciseSeriesList = new ArrayList<>();

        Log.d("UserExercise", "Constructor con parámetros - Inicializando desde un ejercicio base.");
    }

    // ✅ Métodos Getter y Setter con LOGS DETALLADOS
    public boolean isCompleted() {
        Log.d("UserExercise", "isCompleted() llamado: " + isCompleted);
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        Log.d("UserExercise", "setCompleted() llamado: " + completed);
        isCompleted = completed;
    }


    // ✅ Excluir `getName()` para evitar conflictos con Firebase
    @Exclude
    @Override
    public String getName() {
        return super.getName();
    }

    public String getFamiliarity() {
        Log.d("UserExercise", "getFamiliarity() llamado: " + familiarity);
        return familiarity;
    }

    public void setFamiliarity(String familiarity) {
        Log.d("UserExercise", "setFamiliarity() llamado: " + familiarity);
        this.familiarity = familiarity;
    }

    public double getWeight() {
        Log.d("UserExercise", "getWeight() llamado: " + weight);
        return weight;
    }

    public void setWeight(double weight) {
        Log.d("UserExercise", "setWeight() llamado: " + weight);
        this.weight = weight;
    }

    public int getReps() {
        Log.d("UserExercise", "getReps() llamado: " + reps);
        return reps;
    }

    public void setReps(int reps) {
        Log.d("UserExercise", "setReps() llamado: " + reps);
        this.reps = reps;
    }

    public double getRpe() {
        Log.d("UserExercise", "getRpe() llamado: " + rpe);
        return rpe;
    }

    public void setRpe(double rpe) {
        Log.d("UserExercise", "setRpe() llamado: " + rpe);
        this.rpe = rpe;
    }

    public int getSeries() {
        Log.d("UserExercise", "getSeries() llamado: " + series);
        return series;
    }

    public void setSeries(int series) {
        Log.d("UserExercise", "setSeries() llamado: " + series);
        this.series = series;
    }

    @PropertyName("dayAssigned")
    public String getDayName() {
        Log.d("UserExercise", "getDayName() llamado: " + dayName);
        return dayName;
    }

    public void setDayName(String dayName) {
        Log.d("UserExercise", "setDayName() llamado: " + dayName);
        this.dayName = dayName;
    }

    @PropertyName("exerciseSeriesList")
    public List<ExerciseSeries> getExerciseSeriesList() {
        Log.d("UserExercise", "getExerciseSeriesList() llamado. Tamaño: " + exerciseSeriesList.size());
        return exerciseSeriesList;
    }

    public void setExerciseSeriesList(List<ExerciseSeries> exerciseSeriesList) {
        Log.d("UserExercise", "setExerciseSeriesList() llamado. Tamaño: " + exerciseSeriesList.size());
        this.exerciseSeriesList = exerciseSeriesList;
    }

    public void addExerciseSeries(ExerciseSeries series) {
        Log.d("UserExercise", "addExerciseSeries() llamado con serie: " + series);
        this.exerciseSeriesList.add(series);
    }

    public void saveToFirebase(DatabaseReference databaseReference) {
        String exerciseId = String.valueOf(getId());
        databaseReference.child("userExercises").child(exerciseId).setValue(this)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "✅ Ejercicio guardado con éxito en Firebase"))
                .addOnFailureListener(e -> Log.e("Firebase", "❌ Error al guardar el ejercicio en Firebase", e));
    }
    // ✅ Método para convertir de manera segura un objeto a una lista de Strings
    private static List<String> safeConvertToList(Object obj) {
        if (obj instanceof List) {
            List<String> stringList = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof String) {
                    stringList.add((String) item);
                }
            }
            Log.d("UserExercise", "safeConvertToList() - Lista convertida con tamaño: " + stringList.size());
            return stringList;
        } else if (obj instanceof String) {
            List<String> singleItemList = Arrays.asList(((String) obj).split(",\\s*"));
            Log.d("UserExercise", "safeConvertToList() - Convertido desde String con tamaño: " + singleItemList.size());
            return singleItemList;
        }
        Log.e("UserExercise", "safeConvertToList() - Objeto no reconocido, devolviendo lista vacía.");
        return new ArrayList<>();
    }

    // ✅ Método para convertir una lista de Map a una lista de MovementPattern
    private static List<MovementPattern> safeConvertToMovementPatternList(Object obj) {
        if (obj instanceof List) {
            List<MovementPattern> movementPatterns = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof Map) {
                    movementPatterns.add(mapToMovementPattern((Map<String, Object>) item));
                }
            }
            Log.d("UserExercise", "safeConvertToMovementPatternList() - Lista convertida con tamaño: " + movementPatterns.size());
            return movementPatterns;
        }
        Log.e("UserExercise", "safeConvertToMovementPatternList() - Objeto no reconocido, devolviendo lista vacía.");
        return new ArrayList<>();
    }

    // ✅ Método auxiliar para mapear un objeto Map a MovementPattern
    private static MovementPattern mapToMovementPattern(Map<String, Object> dataMap) {
        MovementPattern pattern = new MovementPattern();
        pattern.setName((String) dataMap.getOrDefault("name", ""));
        Log.d("UserExercise", "mapToMovementPattern() - Mapeo completado: " + pattern.getName());
        return pattern;
    }

    public UserExercise() {
        super();
        this.isCompleted = false;
        this.maxReps = 0;
        this.currentLoad = 0;
        this.familiarity = "";
        this.exerciseName = "";
        this.dayName = "";
        this.exerciseSeriesList = new ArrayList<>();
    }

    @PropertyName("name")
    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    @PropertyName("maxReps")
    public int getMaxReps() {
        return maxReps;
    }

    public void setMaxReps(int maxReps) {
        this.maxReps = maxReps;
    }

    @PropertyName("currentLoad")
    public int getCurrentLoad() {
        return currentLoad;
    }

    public void setCurrentLoad(int currentLoad) {
        this.currentLoad = currentLoad;
    }
    @Override
    public String toString() {
        return "UserExercise{" +
                "id='" + getId() + '\'' +
                ", name='" + exerciseName + '\'' +
                ", dayAssigned='" + dayName + '\'' +
                ", isCompleted=" + isCompleted +
                ", maxReps=" + maxReps +
                ", currentLoad=" + currentLoad +
                ", familiarity='" + familiarity + '\'' +
                ", series=" + series +
                ", weight=" + weight +
                ", reps=" + reps +
                ", rpe=" + rpe +
                ", exerciseSeriesList=" + exerciseSeriesList +
                '}';
    }
}
