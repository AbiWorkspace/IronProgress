package com.example.diets.model;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.PropertyName;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class UserExercise extends Exercise implements Serializable {
    private static final long serialVersionUID = 1L; // Asegura compatibilidad de serialización

    private boolean isCompleted;
    private int maxReps;
    private int currentLoad;
    private String familiarity; // Nuevo campo para familiaridad
    private int series;
    // Campos específicos del usuario
    private double weight;
    private int reps;
    private double rpe;
    @PropertyName("name")  // 🔥 Asegura que Firebase lo mapea correctamente
    private String exerciseName;

    // Constructor sin argumentos requerido por Firebase
    public UserExercise() {
        super();
        this.isCompleted = false;
        this.maxReps = 0;
        this.currentLoad = 0;
        this.familiarity = ""; // Inicializar familiaridad
        this.exerciseName = "";
    }

    // ✅ Constructor corregido para manejar conversiones de listas
    public UserExercise(Exercise exercise) {
        super(exercise.getId(), exercise.getName(), exercise.getEquipmentType(),
                exercise.getExerciseType(), exercise.getMusclesWorked(), exercise.getLoadIntensity(),
                exercise.getImage(), exercise.getDescription(), exercise.getMovementType(),
                safeConvertToList(exercise.getSynergistMuscles()), // ✅ Conversión segura
                safeConvertToList(exercise.getGoals()), // ✅ Conversión segura
                safeConvertToMovementPatternList(exercise.getMovementPatterns())); // ✅ Conversión segura

        this.isCompleted = false;
        this.maxReps = 0;
        this.currentLoad = 0;
        this.familiarity = "";
        this.series = 3;
    }

    // Métodos getter y setter
    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }
    @Exclude  // 🔥 Evita duplicación con "name" en Firebase
    public String getExerciseName() {
        return super.getName();
    }

    public void setExerciseName(String exerciseName) {
        super.setName(exerciseName);
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

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public int getReps() {
        return reps;
    }

    public void setReps(int reps) {
        this.reps = reps;
    }

    public double getRpe() {
        return rpe;
    }

    public void setRpe(double rpe) {
        this.rpe = rpe;
    }


    // ✅ Método corregido para convertir `List<Muscle>` a `List<String>`
    public List<String> getSynergistMusclesAsString() {
        return safeConvertToList(super.getSynergistMuscles());
    }

    @Override
    public void setSynergistMuscles(List<String> synergistMuscles) {
        if (synergistMuscles != null) {
            List<String> convertedList = new ArrayList<>();

            for (Object obj : synergistMuscles) {
                if (obj instanceof String) {
                    convertedList.add((String) obj);
                } else if (obj instanceof Map) {
                    // Si Firebase lo guarda como HashMap, extraemos el valor
                    for (Object value : ((Map<?, ?>) obj).values()) {
                        if (value instanceof String) {
                            convertedList.add((String) value);
                        }
                    }
                }
            }

            super.setSynergistMuscles(convertedList);
        } else {
            super.setSynergistMuscles(new ArrayList<>());
        }
    }


    // ✅ Método seguro para convertir `Object` en `List<String>`
    private static List<String> safeConvertToList(Object obj) {
        if (obj instanceof List) {
            List<String> stringList = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof String) {
                    stringList.add((String) item);
                }
            }
            return stringList;
        } else if (obj instanceof String) {
            return Arrays.asList(((String) obj).split(",\\s*")); // Convierte una string en lista separada por comas
        }
        return new ArrayList<>();
    }

    // ✅ Método seguro para convertir `Object` en `List<MovementPattern>`
    private static List<MovementPattern> safeConvertToMovementPatternList(Object obj) {
        if (obj instanceof List) {
            List<MovementPattern> movementPatterns = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof Map) {
                    movementPatterns.add(mapToMovementPattern((Map<String, Object>) item));
                }
            }
            return movementPatterns;
        }
        return new ArrayList<>();
    }

    // ✅ Método para convertir `Map<String, Object>` a `MovementPattern`
    private static MovementPattern mapToMovementPattern(Map<String, Object> dataMap) {
        MovementPattern pattern = new MovementPattern();
        pattern.setName((String) dataMap.getOrDefault("name", ""));
        return pattern;
    }


    public int getSeries() {
        return series;
    }

    public void setSeries(int series) {
        this.series = series;
    }
    @Override
    public String toString() {
        return "UserExercise{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", equipmentType='" + getEquipmentType() + '\'' +
                ", exerciseType='" + getExerciseType() + '\'' +
                ", musclesWorked='" + getMusclesWorked() + '\'' +
                ", loadIntensity='" + getLoadIntensity() + '\'' +
                ", image='" + getImage() + '\'' +
                ", description='" + getDescription() + '\'' +
                ", movementType='" + getMovementType() + '\'' +
                ", isCompleted=" + isCompleted +
                ", maxReps=" + maxReps +
                ", currentLoad=" + currentLoad +
                ", familiarity='" + familiarity + '\'' +
                ", series=" + series +
                ", weight=" + weight +
                ", reps=" + reps +
                ", rpe=" + rpe +
                ", synergistMuscles=" + getSynergistMusclesAsString() +
                ", goals=" + getGoals() +
                ", movementPatterns=" + getMovementPatterns() +
                '}';
    }

}
