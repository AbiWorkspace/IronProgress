package com.example.diets.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
public class Exercise implements Serializable {
    private static final long serialVersionUID = 1L; // Añadido serialVersionUID

    private int id;
    private String name;
    private String equipmentType;
    private String exerciseType;
    private String musclesWorked;
    private String loadIntensity;
    private String image;
    private String description;
    private String movementType;
    private List<Muscle> synergistMuscles = new ArrayList<>(); // Inicialización de la lista
    private List<String> goals = new ArrayList<>();
    private List<MovementPattern> movementPatterns = new ArrayList<>();

    // Constructor sin argumentos requerido por Firebase
    public Exercise() {
    }

    // Constructor con argumentos
    public Exercise(int id, String name, String equipmentType, String exerciseType, String musclesWorked, String loadIntensity, String image, String description, String movementType, List<Muscle> synergistMuscles, List<String> goals, List<MovementPattern> movementPatterns) {
        this.id = id;
        this.name = name;
        this.equipmentType = equipmentType;
        this.exerciseType = exerciseType;
        this.musclesWorked = musclesWorked;
        this.loadIntensity = loadIntensity;
        this.image = image;
        this.description = description;
        this.movementType = movementType;
        this.synergistMuscles = synergistMuscles != null ? synergistMuscles : new ArrayList<>();
        this.goals = goals != null ? goals : new ArrayList<>();
        this.movementPatterns = movementPatterns != null ? movementPatterns : new ArrayList<>();
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        if (equipmentType.equals("mancuernas") || equipmentType.equals("barra") || equipmentType.equals("cable")) {
            this.equipmentType = equipmentType;
        } else {
            this.equipmentType = "desconocido";
        }
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        if (exerciseType.equals("fuerza") || exerciseType.equals("hipertrofia")) {
            this.exerciseType = exerciseType;
        } else {
            this.exerciseType = "desconocido";
        }
    }

    public String getMusclesWorked() {
        return musclesWorked;
    }

    public void setMusclesWorked(String musclesWorked) {
        this.musclesWorked = musclesWorked;
    }

    public String getLoadIntensity() {
        return loadIntensity;
    }

    public void setLoadIntensity(String loadIntensity) {
        this.loadIntensity = loadIntensity;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMovementType(String movementType) {
        if (movementType.equals("compound") || movementType.equals("isolation")) {
            this.movementType = movementType;
        } else {
            this.movementType = "desconocido";
        }
    }

    public List<Muscle> getSynergistMuscles() {
        return synergistMuscles;
    }

    public void setSynergistMuscles(List<Muscle> synergistMuscles) {
        this.synergistMuscles = synergistMuscles;
    }

    public List<String> getGoals() {
        return goals;
    }

    public void setGoals(List<String> goals) {
        this.goals = goals;
    }

    public List<MovementPattern> getMovementPatterns() {
        return movementPatterns;
    }

    public void setMovementPatterns(List<MovementPattern> movementPatterns) {
        this.movementPatterns = movementPatterns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exercise exercise = (Exercise) o;
        return id == exercise.id && Objects.equals(name, exercise.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }

    @Override
    public String toString() {
        return "Exercise{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", equipmentType='" + equipmentType + '\'' +
                ", exerciseType='" + exerciseType + '\'' +
                ", musclesWorked='" + musclesWorked + '\'' +
                ", loadIntensity='" + loadIntensity + '\'' +  // Corregido aquí
                ", image='" + image + '\'' +
                ", description='" + description + '\'' +
                ", movementType='" + movementType + '\'' +
                ", synergistMuscles=" + synergistMuscles +
                ", goals=" + goals +
                ", movementPatterns=" + movementPatterns +
                '}';
    }
}