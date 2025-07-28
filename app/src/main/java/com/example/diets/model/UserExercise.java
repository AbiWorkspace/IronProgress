package com.example.diets.model;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
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
    private double lastLoad;
    @PropertyName("name")
    private String exerciseName;
    private boolean isFromInternal;
    // Constructor que copia todos los campos de Exercise al nuevo UserExercise
    private boolean sugerenciasAplicadas = false;
    public boolean isSugerenciasAplicadas()      { return sugerenciasAplicadas; }
    public void     setSugerenciasAplicadas(boolean v){ sugerenciasAplicadas = v; }


    public UserExercise(Exercise exercise) {
        // 1) Llamada a super(...) como PRIMERA instrucción
        super(
                exercise.getId(),
                exercise.getName(),
                exercise.getEquipmentType(),
                exercise.getExerciseType(),
                exercise.getMusclesWorked(),
                exercise.getLoadIntensity(),
                exercise.getImage(),
                exercise.getDescription(),
                exercise.getMovementType(),   // ← AQUÍ
                safeConvertToList(exercise.getSynergistMuscles()),
                safeConvertToList(exercise.getGoals()),
                safeConvertToMovementPatternList(exercise.getMovementPatterns())
        );


        // 2) Luego podemos hacer logs acerca de 'exercise'
        Log.d("UserExercise", ">>> [CONSTRUCTOR] Recibimos un Exercise con:");
        Log.d("UserExercise", "        name=" + exercise.getName());
        Log.d("UserExercise", "        equipmentType=" + exercise.getEquipmentType());
        Log.d("UserExercise", "        exerciseType=" + exercise.getExerciseType());
        Log.d("UserExercise", "        musclesWorked=" + exercise.getMusclesWorked());
        Log.d("UserExercise", "        loadIntensity=" + exercise.getLoadIntensity());

        Log.d("UserExercise", "        description=" + exercise.getDescription());
        Log.d("UserExercise", "        movementType=" + exercise.getMovementType());

        // 3) Revisamos lo que quedó asignado en la superclase
        Log.d("UserExercise", ">>> [CONSTRUCTOR] Después de super(...):");
        Log.d("UserExercise", "        getEquipmentType()=" + this.getEquipmentType());
        Log.d("UserExercise", "        getExerciseType()=" + this.getExerciseType());

        // 4) Asignamos el "name" al campo exerciseName (que es el que se sube como "name" en Firebase)
        this.setExerciseName(exercise.getName());
        this.setImages(exercise.getImages());

        // 5) Inicializamos campos propios de UserExercise
        this.isCompleted = false;
        this.maxReps = 0;
        this.currentLoad = 0;
        this.familiarity = "";
        this.series = 3;  // Valor por defecto
        this.dayName = "";
        this.exerciseSeriesList = new ArrayList<>();
        this.setFromInternal(exercise.isFromInternal());
        Log.d("UserExercise", ">>> [CONSTRUCTOR] isFromInternal() = " + this.isFromInternal());


        // 6) Logs finales con los valores de este UserExercise
        Log.d("UserExercise", ">>> [CONSTRUCTOR] Campos finales del UserExercise:");
        Log.d("UserExercise", "        exerciseName=" + this.exerciseName);
        Log.d("UserExercise", "        equipmentType=" + this.getEquipmentType());
        Log.d("UserExercise", "        exerciseType=" + this.getExerciseType());
        Log.d("UserExercise", "        series=" + this.series);
        Log.d("UserExercise", ">>> [CONSTRUCTOR] Fin de constructor");
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

    // =============== GETTERS/SETTERS CON LOGS ===============

    public boolean isCompleted() {
        Log.d("UserExercise", "isCompleted() llamado: " + isCompleted);
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        Log.d("UserExercise", "setCompleted() llamado: " + completed);
        isCompleted = completed;
    }

    // Excluimos getName() para evitar conflictos con Firebase
    @Exclude
    @Override
    public String getName() {
        return super.getName();
    }

    public boolean isFromInternal() {
        return isFromInternal;
    }

    public void setFromInternal(boolean fromInternal) {
        isFromInternal = fromInternal;
    }
    public String getFamiliarity() {
        Log.d("UserExercise", "getFamiliarity() llamado: " + familiarity);
        return familiarity;
    }

    public void setFamiliarity(String familiarity) {
        Log.d("UserExercise", "setFamiliarity() llamado: " + familiarity);
        this.familiarity = familiarity;
    }
    public void hydrateFromSnapshot(@NonNull DataSnapshot snap) {

        // Campos simples -------------------------------------------------------
        setDescription      (snap.child("description")   .getValue(String.class));
        setEquipmentType    (snap.child("equipmentType") .getValue(String.class));
        setExerciseType     (snap.child("exerciseType")  .getValue(String.class));
        setLoadIntensity    (snap.child("loadIntensity") .getValue(String.class));
        setMovementType     (snap.child("movementType")  .getValue(String.class));
        setMusclesWorked    (snap.child("musclesWorked") .getValue(String.class));
        setImage            (snap.child("image")         .getValue(String.class));

        // Lista de músculos sinérgicos ----------------------------------------
        List<String> sinergias = new ArrayList<>();
        for (DataSnapshot m : snap.child("synergistMuscles").getChildren()) {
            String s = m.getValue(String.class);
            if (s != null) sinergias.add(s);
        }
        setSynergistMuscles(sinergias);

        // IMPORTANTE: el nombre se serializa con la key "name"
        // - Asegúrate de que el campo exerciseName queda lleno.
        setExerciseName(snap.child("name").getValue(String.class));
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

    // =========================================
    //       MÉTODOS DE COPIA SEGURA
    // =========================================

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
    private String firebaseKey;

    public String getFirebaseKey() { return firebaseKey; }
    public void setFirebaseKey(String firebaseKey) { this.firebaseKey = firebaseKey; }

    private static List<MovementPattern> safeConvertToMovementPatternList(Object obj) {

        /* 1⃣  Caso habitual: lista de mapas -------------------------------------------------- */
        if (obj instanceof List) {
            List<MovementPattern> movementPatterns = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof Map) {
                    movementPatterns.add(mapToMovementPattern((Map<String, Object>) item));
                }
            }
            Log.d("UserExercise",
                    "safeConvertToMovementPatternList() - Lista convertida con tamaño: "
                            + movementPatterns.size());
            return movementPatterns;
        }

        /* 2⃣  NUEVO: el campo vino como String (“Dominante de rodilla”, etc.) --------------- */
        if (obj instanceof String) {
            String name = ((String) obj).trim();
            if (!name.isEmpty()) {
                MovementPattern mp = new MovementPattern();
                mp.setName(name);

                List<MovementPattern> list = new ArrayList<>();
                list.add(mp);

                Log.d("UserExercise",
                        "safeConvertToMovementPatternList() - Convertido desde String: "
                                + name);
                return list;
            }
        }

        /* 3⃣  Fallback ---------------------------------------------------------------------- */
        Log.e("UserExercise",
                "safeConvertToMovementPatternList() - Objeto no reconocido, devolviendo lista vacía.");
        return new ArrayList<>();
    }

    private static MovementPattern mapToMovementPattern(Map<String, Object> dataMap) {
        MovementPattern pattern = new MovementPattern();
        pattern.setName((String) dataMap.getOrDefault("name", ""));
        Log.d("UserExercise", "mapToMovementPattern() - Mapeo completado: " + pattern.getName());
        return pattern;
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
    public double getLastLoad() {
        return lastLoad;
    }
    public void setLastLoad(double lastLoad) {
        this.lastLoad = lastLoad;
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
                ", equipmentType='" + getEquipmentType() + '\'' +
                ", exerciseType='" + getExerciseType() + '\'' +
                '}';
    }
}
