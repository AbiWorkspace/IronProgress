package com.example.diets.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Modelo de ejercicio. Compatible con Firebase (constructor vacío) y Serializable para bundles.
 * Los tipos/clasificaciones se almacenan con la primera letra en mayúscula.
 */
public class Exercise implements Serializable {

    private static final long serialVersionUID = 1L;

    /* ======================= ATRIBUTOS ======================= */
    private int id;
    private String name;
    private String equipmentType;   // Mancuernas / Barra / Cable / Desconocido
    private String exerciseType;    // Fuerza / Hipertrofia / Desconocido
    private String musclesWorked;
    private String loadIntensity;
    private String image;           // compatibilidad legacy (1.ª imagen)
    private String description;
    private String movementType;    // Multiarticular / Aislado / Desconocido

    private List<String> synergistMuscles  = new ArrayList<>();
    private List<String> goals             = new ArrayList<>();
    private List<MovementPattern> movementPatterns = new ArrayList<>();
    private List<String> images            = new ArrayList<>();
    private List<String> imageGallery      = new ArrayList<>();

    private int     series       = 3;
    private boolean fromInternal = false;


    /* ======================= CONSTRUCTORES ======================= */

    /** Constructor vacío requerido por Firebase. */
    public Exercise() { }

    /** Constructor completo. Todos los campos pasan por sus setters para validar y normalizar. */
    public Exercise(int id,
                    String name,
                    String equipmentType,
                    String exerciseType,
                    String musclesWorked,
                    String loadIntensity,
                    String image,
                    String description,
                    String movementType,
                    List<String> synergistMuscles,
                    List<String> goals,
                    List<MovementPattern> movementPatterns) {

        this.id            = id;
        this.name          = name;
        setEquipmentType(equipmentType);
        setExerciseType(exerciseType);
        this.musclesWorked = musclesWorked;
        this.loadIntensity = loadIntensity;
        setImage(image);                   // legacy
        this.description   = description;
        setMovementType(movementType);
        this.synergistMuscles =
                (synergistMuscles != null) ? new ArrayList<>(synergistMuscles) : new ArrayList<>();
        this.goals            =
                (goals != null) ? new ArrayList<>(goals) : new ArrayList<>();
        this.movementPatterns =
                (movementPatterns != null) ? new ArrayList<>(movementPatterns) : new ArrayList<>();
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) +
                s.substring(1).toLowerCase(Locale.ROOT);
    }

    public List<String> getMovementPatternIds() {
        List<String> ids = new ArrayList<>();
        for (MovementPattern mp : movementPatterns) {
            if (mp != null && mp.getId() != null) ids.add(mp.getId());
        }
        return ids;
    }

    public List<Muscle> getSynergistMuscleObjects() {
        List<Muscle> list = new ArrayList<>();
        for (String m : synergistMuscles) list.add(new Muscle(m));
        return list;
    }

    public void setSynergistMuscleObjects(List<Muscle> objs) {
        synergistMuscles.clear();
        if (objs != null) for (Muscle m : objs) synergistMuscles.add(m.getName());
    }


    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEquipmentType() { return equipmentType; }
    public void setEquipmentType(String equipmentType) {
        if (equipmentType == null) { this.equipmentType = "Desconocido"; return; }

        String et = equipmentType.trim().toLowerCase(Locale.ROOT);
        switch (et) {
            case "mancuernas":
                this.equipmentType = "Mancuernas"; break;
            case "barra":
                this.equipmentType = "Barra"; break;
            case "cable":
                this.equipmentType = "Cable"; break;
            default:
                this.equipmentType = "Desconocido";
        }
    }

    public String getExerciseType() { return exerciseType; }
    public void setExerciseType(String exerciseType) {
        if (exerciseType == null) { this.exerciseType = "Desconocido"; return; }

        String xt = exerciseType.trim().toLowerCase(Locale.ROOT);
        switch (xt) {
            case "fuerza":
                this.exerciseType = "Fuerza"; break;
            case "hipertrofia":
                this.exerciseType = "Hipertrofia"; break;
            default:
                this.exerciseType = "Desconocido";
        }
    }

    public String getMusclesWorked() { return musclesWorked; }
    public void setMusclesWorked(String musclesWorked) { this.musclesWorked = musclesWorked; }

    public String getLoadIntensity() { return loadIntensity; }
    public void setLoadIntensity(String loadIntensity) { this.loadIntensity = loadIntensity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getMovementType() { return movementType; }
    public void setMovementType(String movementType) {
        if (movementType == null) { this.movementType = "Desconocido"; return; }

        String mt = movementType.trim().toLowerCase(Locale.ROOT);
        switch (mt) {
            case "multiarticular":
                this.movementType = "Multiarticular"; break;
            case "aislado":
                this.movementType = "Aislado"; break;
            default:
                this.movementType = "Desconocido";
        }
    }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) {
        this.images = (images != null) ? new ArrayList<>(images) : new ArrayList<>();
    }

    @Deprecated
    public String getImage() { return images.isEmpty() ? null : images.get(0); }

    @Deprecated
    public void setImage(String image) {
        images.clear();
        if (image != null) images.add(image);
    }

    public List<String> getImageGallery() { return imageGallery; }
    public void setImageGallery(List<String> imageGallery) { this.imageGallery = imageGallery; }

    public List<String> getSynergistMuscles() { return synergistMuscles; }
    public void setSynergistMuscles(List<String> synergistMuscles) {
        this.synergistMuscles =
                (synergistMuscles != null) ? new ArrayList<>(synergistMuscles) : new ArrayList<>();
    }

    public List<String> getGoals() { return goals; }
    public void setGoals(List<String> goals) { this.goals = goals; }

    public List<MovementPattern> getMovementPatterns() { return movementPatterns; }
    public void setMovementPatterns(List<MovementPattern> movementPatterns) {
        this.movementPatterns = movementPatterns;
    }

    public int getSeries() { return series; }
    public void setSeries(int series) { this.series = series; }

    public boolean isFromInternal() { return fromInternal; }
    public void setFromInternal(boolean fromInternal) { this.fromInternal = fromInternal; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Exercise)) return false;
        Exercise ex = (Exercise) o;
        return id == ex.id && Objects.equals(name, ex.name);
    }

    @Override
    public int hashCode() { return Objects.hash(id, name); }

    @Override
    public String toString() {
        return "Exercise{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", equipmentType='" + equipmentType + '\'' +
                ", exerciseType='" + exerciseType + '\'' +
                ", musclesWorked='" + musclesWorked + '\'' +
                ", loadIntensity='" + loadIntensity + '\'' +
                ", images=" + images +
                ", description='" + description + '\'' +
                ", movementType='" + movementType + '\'' +
                ", synergistMuscles=" + synergistMuscles +
                ", goals=" + goals +
                ", movementPatterns=" + movementPatterns +
                ", series=" + series +
                ", fromInternal=" + fromInternal +
                '}';
    }
}
