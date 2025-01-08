package com.example.diets.model;
public class Exercise {
    private int id;
    private String name;
    private String equipmentType;
    private String exerciseType;
    private String musclesWorked;
    private String loadIntensity;
    private String image;
    private String description;

    // Constructor
    public Exercise(int id, String name, String equipmentType, String exerciseType, String musclesWorked, String loadIntensity, String image, String description) {
        this.id = id;
        this.name = name;
        this.equipmentType = equipmentType;
        this.exerciseType = exerciseType;
        this.musclesWorked = musclesWorked;
        this.loadIntensity = loadIntensity;
        this.image = image;
        this.description = description;
    }

    // Getters and Setters
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
        this.equipmentType = equipmentType;
    }

    public String getExerciseType() {
        return exerciseType;
    }

    public void setExerciseType(String exerciseType) {
        this.exerciseType = exerciseType;
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
//agregar el tipo empuje o traccion
    @Override
    public String toString() {
        return "Exercise{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", equipmentType='" + equipmentType + '\'' +
                ", exerciseType='" + exerciseType + '\'' +
                ", musclesWorked='" + musclesWorked + '\'' +
                ", loadIntensity='" + loadIntensity + '\'' +
                ", image='" + image + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}