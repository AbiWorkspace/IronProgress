package com.example.diets.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Exercise implements Serializable {
    private static final long serialVersionUID = 1L; // Asegurar compatibilidad con Serializable

    private int id;
    private String name;
    private String equipmentType;
    private String exerciseType;
    private String musclesWorked;
    private String loadIntensity;
    private String image;
    private String description;
    private String movementType;
    private List<String> synergistMuscles = new ArrayList<>(); // Ahora se almacena como List<String>
    private List<String> goals = new ArrayList<>();
    private List<MovementPattern> movementPatterns = new ArrayList<>();
    private int series = 3; // Valor por defecto
    private List<String> images = new ArrayList<>();
    private List<String> imageGallery = new ArrayList<>();
    // Constructor vacío requerido por Firebase
    public Exercise() {
    }

    // Constructor con argumentos
    public Exercise(int id, String name, String equipmentType, String exerciseType, String musclesWorked, String loadIntensity, String image, String description, String movementType, List<String> synergistMuscles, List<String> goals, List<MovementPattern> movementPatterns) {
        this.id = id;
        this.name = name;
        this.equipmentType = equipmentType;
        this.exerciseType = exerciseType;
        this.musclesWorked = musclesWorked;
        this.loadIntensity = loadIntensity;
        this.image = image;
        this.description = description;
        this.movementType = movementType;
        this.synergistMuscles = (synergistMuscles != null) ? new ArrayList<>(synergistMuscles) : new ArrayList<>();
        this.goals = (goals != null) ? new ArrayList<>(goals) : new ArrayList<>();
        this.movementPatterns = (movementPatterns != null) ? new ArrayList<>(movementPatterns) : new ArrayList<>();
    }

    // 🔹 Método para obtener los IDs de los patrones de movimiento
    public List<String> getMovementPatternIds() {
        List<String> movementPatternIds = new ArrayList<>();
        for (MovementPattern pattern : movementPatterns) {
            if (pattern != null && pattern.getId() != null) {
                movementPatternIds.add(pattern.getId());
            }
        }
        return movementPatternIds;
    }

    // ✅ Métodos para los músculos sinergistas (ahora se almacenan como String)
    public List<String> getSynergistMuscles() {
        return synergistMuscles;
    }

    public void setSynergistMuscles(List<String> synergistMuscles) {
        this.synergistMuscles = (synergistMuscles != null) ? new ArrayList<>(synergistMuscles) : new ArrayList<>();
    }
    public int getSeries() {
        return series;
    }
    public List<String> getImages() {              // NUEVO
        return images;
    }
    public void setImages(List<String> images) {   // NUEVO
        this.images = (images != null) ? new ArrayList<>(images) : new ArrayList<>();
    }

    /* Mantén este para compatibilidad si lo usabas en otros sitios */
    @Deprecated
    public String getImage() {                     // devuelve la 1.ª si existe
        return images.isEmpty() ? null : images.get(0);
    }
    @Deprecated
    public void setImage(String image) {           // solo pone/unifica la 1.ª
        images.clear();
        if (image != null) images.add(image);
    }
    public void setSeries(int series) {
        this.series = series;
    }

    // 🔹 Método auxiliar para obtener la lista de músculos como objetos Muscle
    public List<Muscle> getSynergistMuscleObjects() {
        List<Muscle> muscleObjects = new ArrayList<>();
        for (String muscleName : synergistMuscles) {
            muscleObjects.add(new Muscle(muscleName));
        }
        return muscleObjects;
    }

    // 🔹 Método auxiliar para establecer los músculos sinergistas desde objetos Muscle
    public void setSynergistMuscleObjects(List<Muscle> muscleObjects) {
        this.synergistMuscles.clear();
        for (Muscle muscle : muscleObjects) {
            this.synergistMuscles.add(muscle.getName());
        }
    }

    // Getters y Setters normales
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
        if (movementType.equals("multiarticular") || movementType.equals("aislado")) {
            this.movementType = movementType;
        } else {
            this.movementType = "desconocido";
        }
    }
    public List<String> getImageGallery() {
        return imageGallery;
    }

    public void setImageGallery(List<String> imageGallery) {
        this.imageGallery = imageGallery;
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
                ", loadIntensity='" + loadIntensity + '\'' +
                ", image='" + image + '\'' +
                ", description='" + description + '\'' +
                ", movementType='" + movementType + '\'' +
                ", synergistMuscles=" + synergistMuscles +
                ", goals=" + goals +
                ", movementPatterns=" + movementPatterns +
                '}';
    }
}
