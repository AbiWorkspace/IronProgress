package com.example.diets.model;

import static android.content.ContentValues.TAG;
import android.util.Log;
import com.google.firebase.database.IgnoreExtraProperties;
import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class User {
    // Properties
    private String id;
    private String firstName;
    private String lastName;
    private String email;

    // ✅ Nuevo campo Rol
    private String role;

    private int age;
    private double weight;
    private double height;
    private String gender;
    private String activityLevel;
    private String goal;

    // Eliminado type_of_user para evitar confusiones, usamos 'role'

    private boolean firstTime;
    private double caloriesToday;
    private double proteinToday;
    private double fatToday;
    private double carbToday;

    private double latitude;
    private double longitude;
    private String profilePhotoUrl;
    private String profilePhoto;
    private boolean trainingFormCompleted;

    // Propiedades para ejercicios
    private Map<String, UserExercise> userExercises;
    private Map<String, UserMovementPattern> userMovementPatterns;

    // --- CONSTRUCTOR VACÍO (Requerido por Firebase) ---
    public User() {
        this.firstTime = true;
        this.caloriesToday = 0.0;
        this.proteinToday = 0.0;
        this.fatToday = 0.0;
        this.carbToday = 0.0;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.profilePhotoUrl = "default";
        this.userExercises = new HashMap<>();
        this.userMovementPatterns = new HashMap<>();
        this.trainingFormCompleted = false;
        this.role = "user"; // Valor por defecto
    }

    // --- CONSTRUCTOR CON ARGUMENTOS (Sin Password) ---
    public User(String firstName, String lastName, String email, int age, double weight, double height, String gender, String activityLevel, String goal) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.gender = gender;
        this.activityLevel = activityLevel;
        this.goal = goal;
        this.firstTime = true;
        this.caloriesToday = 0.0;
        this.proteinToday = 0.0;
        this.fatToday = 0.0;
        this.carbToday = 0.0;
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.profilePhotoUrl = "default";
        this.userExercises = new HashMap<>();
        this.userMovementPatterns = new HashMap<>();
        this.trainingFormCompleted = false;
        this.role = "user"; // Valor por defecto
    }

    // --- GETTERS Y SETTERS (Aquí estaban faltando los del Rol) ---

    // ✅ ESTOS SON LOS QUE FALTABAN Y CAUSABAN EL ERROR
    public String getRole() {
        return role != null ? role : "user";
    }

    public void setRole(String role) {
        this.role = role;
    }
    // ---------------------------------------------------------

    public Map<String, UserExercise> getUserExercises() {
        return userExercises;
    }

    public void setUserExercises(Map<String, UserExercise> userExercises) {
        this.userExercises = userExercises;
    }

    public Map<String, UserMovementPattern> getUserMovementPatterns() {
        return userMovementPatterns;
    }

    public void setUserMovementPatterns(Map<String, UserMovementPattern> userMovementPatterns) {
        this.userMovementPatterns = userMovementPatterns;
    }

    public boolean isTrainingFormCompleted() {
        return trainingFormCompleted;
    }

    public void setTrainingFormCompleted(boolean trainingFormCompleted) {
        this.trainingFormCompleted = trainingFormCompleted;
    }

    public String getId() {
        return id != null && !id.isEmpty() ? id : "Sin ID";
    }

    public void setId(String id) {
        if (id == null || id.isEmpty()) {
            // Puedes descomentar esto si quieres validación estricta
            // throw new IllegalArgumentException("El ID del usuario no puede ser nulo o vacío");
        }
        this.id = id;
    }

    public String getFirstName() { return firstName != null ? firstName : "Sin nombre"; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName != null ? lastName : "Sin apellido"; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getEmail() { return email != null ? email : "Sin correo"; }
    public void setEmail(String email) { this.email = email; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public double getHeight() { return height; }
    public void setHeight(double height) { this.height = height; }

    public String getGender() { return gender != null ? gender : "Sin género"; }
    public void setGender(String gender) { this.gender = gender; }

    public String getActivityLevel() { return activityLevel != null ? activityLevel : "Sin actividad"; }
    public void setActivityLevel(String activityLevel) { this.activityLevel = activityLevel; }

    public String getGoal() { return goal != null ? goal : "Sin meta"; }
    public void setGoal(String goal) { this.goal = goal; }

    public boolean isFirstTime() { return firstTime; }
    public void setFirstTime(boolean firstTime) { this.firstTime = firstTime; }

    public double getCaloriesToday() { return caloriesToday; }
    public void setCaloriesToday(double caloriesToday) { this.caloriesToday = caloriesToday; }

    public double getProteinToday() { return proteinToday; }
    public double getFatToday() { return fatToday; }
    public double getCarbToday() { return carbToday; }

    // Métodos de suma (Lógica de negocio)
    public void addCaloriesToday(double calories) {
        if (calories < 0) {
            Log.w(TAG, "Intento de añadir calorías negativas ignorado.");
            return;
        }
        this.caloriesToday += calories;
    }

    public void addProteinToday(double protein) {
        if (protein < 0) return;
        this.proteinToday += protein;
    }

    public void addFatToday(double fat) {
        if (fat < 0) return;
        this.fatToday += fat;
    }

    public void addCarbToday(double carbs) {
        if (carbs < 0) return;
        this.carbToday += carbs;
    }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getProfilePhotoUrl() { return profilePhotoUrl != null ? profilePhotoUrl : "default"; }
    public void setProfilePhotoUrl(String profilePhotoUrl) { this.profilePhotoUrl = profilePhotoUrl; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    // Métodos para restar valores
    public void subtractCaloriesToday(double calories) {
        if (calories < 0) return;
        this.caloriesToday = Math.max(0, this.caloriesToday - calories);
    }

    public void subtractProteinToday(double protein) {
        if (protein < 0) return;
        this.proteinToday = Math.max(0, this.proteinToday - protein);
    }

    public void subtractFatToday(double fat) {
        if (fat < 0) return;
        this.fatToday = Math.max(0, this.fatToday - fat);
    }

    public void subtractCarbToday(double carbs) {
        if (carbs < 0) return;
        this.carbToday = Math.max(0, this.carbToday - carbs);
    }

    public void resetDailyValues() {
        this.caloriesToday = 0.0;
        this.proteinToday = 0.0;
        this.fatToday = 0.0;
        this.carbToday = 0.0;
    }

    // Métodos de cálculo (Lógica de negocio)
    public int massIndex(double weight, double height) {
        height = feetToMeters(height);
        return (int) (weight / (height * height));
    }

    private double feetToMeters(double feet) {
        return feet * 0.3048;
    }

    public double calculateCalories(double weight, double height, int age, String gender, String activityLevel, String goal) {
        double bmr;
        if ("male".equalsIgnoreCase(gender)) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        }

        double adjustedBMR = (bmr + (weight * 23)) / 2;

        double activityFactor = switch (activityLevel != null ? activityLevel.toLowerCase() : "sedentary") {
            case "sedentary" -> 1.2;
            case "light" -> 1.375;
            case "moderate" -> 1.55;
            case "high" -> 1.725;
            case "very high" -> 1.9;
            default -> 1.2;
        };

        double get = adjustedBMR * activityFactor;

        double finalCalories = switch (goal != null ? goal.toLowerCase() : "maintain weight") {
            case "lose weight" -> get * 0.75;
            case "gain muscle" -> get * 1.15;
            case "gain weight" -> get * 1.2;
            default -> get;
        };

        Log.d("CalculoCalorias", "Objetivo: " + goal + ", Calorías Finales: " + finalCalories);
        return finalCalories;
    }

    public double calculateProtein() { return weight * 1.6; }
    public double calculateFat() { return weight * 0.8; }
    public double calculateCarbs() { return weight * 4; }

    public boolean isCutting() { return goal != null && goal.toLowerCase().contains("perder"); }
    public boolean isBulking() { return goal != null && goal.toLowerCase().contains("ganar"); }

    public double[] calculateMacros(double weight, String goal, double calories) {
        double proteinGrams = 1.8 * weight;
        double fatGrams = calories * 0.25 / 9;
        double carbGrams = (calories - (proteinGrams * 4) - (fatGrams * 9)) / 4;
        return new double[]{proteinGrams, fatGrams, carbGrams};
    }
}