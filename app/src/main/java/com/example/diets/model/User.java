package com.example.diets.model;

import static android.content.ContentValues.TAG;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class User {
    // Properties
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private int age;
    private double weight;
    private double height;
    private String gender;
    private String activityLevel;
    private String goal;
    private final int type_of_user = 1; // 1 for user, 2 for admin
    private boolean firstTime; // Nuevo campo para indicar si es la primera vez
    private double caloriesToday; // Calorías de hoy
    private double proteinToday; // Proteínas de hoy
    private double fatToday; // Grasas de hoy
    private double carbToday; // Carbohidratos de hoy
    private boolean isAdmin;
    private double latitude; // Coordenada de latitud
    private double longitude; // Coordenada de longitud
    private String profilePhotoUrl; // URL de la foto de perfil
    private String profilePhoto;
    private boolean trainingFormCompleted; // Nueva propiedad


    // Nuevas propiedades para ejercicios y patrones de movimiento del usuario
    private List<UserExercise> userExercises;
    private List<UserMovementPattern> userMovementPatterns;

    // Constructor sin argumentos requerido por Firebase
    public User() {
        this.firstTime = true;
        this.caloriesToday = 0.0;
        this.proteinToday = 0.0;
        this.fatToday = 0.0;
        this.carbToday = 0.0;
        this.latitude = 0.0; // Inicialización predeterminada
        this.longitude = 0.0; // Inicialización predeterminada
        this.profilePhotoUrl = "default"; // Inicialización predeterminada
        this.userExercises = new ArrayList<>(); // Inicialización de la lista
        this.userMovementPatterns = new ArrayList<>(); // Inicialización de la lista
        this.trainingFormCompleted = false; // Inicialización predeterminada

    }

    // Constructor con argumentos
    public User(String firstName, String lastName, String email, String password, int age, double weight, double height, String gender, String activityLevel, String goal) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.age = age;
        this.weight = weight;
        this.height = height;
        this.gender = gender;
        this.activityLevel = activityLevel;
        this.goal = goal;
        this.firstTime = true; // Por defecto, es la primera vez
        this.caloriesToday = 0.0;
        this.proteinToday = 0.0;
        this.fatToday = 0.0;
        this.carbToday = 0.0;
        this.latitude = 0.0; // Inicialización predeterminada
        this.longitude = 0.0; // Inicialización predeterminada
        this.profilePhotoUrl = "default"; // Inicialización predeterminada
        this.userExercises = new ArrayList<>(); // Inicialización de la lista
        this.userMovementPatterns = new ArrayList<>(); // Inicialización de la lista
        this.trainingFormCompleted = false; // Inicialización predeterminada

    }

    // Getters y Setters para las nuevas propiedades
    public List<UserExercise> getUserExercises() {
        return userExercises;
    }

    public void setUserExercises(List<UserExercise> userExercises) {
        this.userExercises = userExercises;
    }

    public List<UserMovementPattern> getUserMovementPatterns() {
        return userMovementPatterns;
    }
    public boolean isTrainingFormCompleted() {
        return trainingFormCompleted;
    }

    public void setTrainingFormCompleted(boolean trainingFormCompleted) {
        this.trainingFormCompleted = trainingFormCompleted;
    }
    public void setUserMovementPatterns(List<UserMovementPattern> userMovementPatterns) {
        this.userMovementPatterns = userMovementPatterns;
    }

    // Resto de los getters y setters...
    public String getId() {
        return id != null && !id.isEmpty() ? id : "Sin ID";
    }

    public void setId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("El ID del usuario no puede ser nulo o vacío");
        }
        this.id = id;
    }

    public String getFirstName() {
        return firstName != null ? firstName : "Sin nombre";
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName != null ? lastName : "Sin apellido";
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email != null ? email : "Sin correo";
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public String getGender() {
        return gender != null ? gender : "Sin género";
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getActivityLevel() {
        return activityLevel != null ? activityLevel : "Sin actividad";
    }

    public void setActivityLevel(String activityLevel) {
        this.activityLevel = activityLevel;
    }

    public String getGoal() {
        return goal != null ? goal : "Sin meta";
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public boolean isFirstTime() {
        return firstTime;
    }

    public void setFirstTime(boolean firstTime) {
        this.firstTime = firstTime;
    }

    public double getCaloriesToday() {
        return caloriesToday;
    }

    public void setCaloriesToday(double caloriesToday) {
        this.caloriesToday = caloriesToday;
    }

    public double getProteinToday() {
        return proteinToday;
    }

    public double getFatToday() {
        return fatToday;
    }

    public double getCarbToday() {
        return carbToday;
    }

    public void addCaloriesToday(double calories) {
        if (calories < 0) {
            Log.w(TAG, "Intento de añadir calorías negativas ignorado.");
            return;
        }
        Log.d(TAG, "addCaloriesToday | Valor previo: " + this.caloriesToday + ", Añadiendo: " + calories);
        this.caloriesToday += calories;
        Log.d(TAG, "addCaloriesToday | Valor actualizado: " + this.caloriesToday);
    }

    public void addProteinToday(double protein) {
        if (protein < 0) {
            Log.w(TAG, "Intento de añadir proteínas negativas ignorado.");
            return;
        }
        Log.d(TAG, "addProteinToday | Valor previo: " + this.proteinToday + ", Añadiendo: " + protein);
        this.proteinToday += protein;
        Log.d(TAG, "addProteinToday | Valor actualizado: " + this.proteinToday);
    }

    public void addFatToday(double fat) {
        if (fat < 0) {
            Log.w(TAG, "Intento de añadir grasas negativas ignorado.");
            return;
        }
        Log.d(TAG, "addFatToday | Valor previo: " + this.fatToday + ", Añadiendo: " + fat);
        this.fatToday += fat;
        Log.d(TAG, "addFatToday | Valor actualizado: " + this.fatToday);
    }

    public void addCarbToday(double carbs) {
        if (carbs < 0) {
            Log.w(TAG, "Intento de añadir carbohidratos negativos ignorado.");
            return;
        }
        Log.d(TAG, "addCarbToday | Valor previo: " + this.carbToday + ", Añadiendo: " + carbs);
        this.carbToday += carbs;
        Log.d(TAG, "addCarbToday | Valor actualizado: " + this.carbToday);
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl != null ? profilePhotoUrl : "default";
    }

    public String getProfilePhoto() {
        return profilePhoto;
    }

    public void setProfilePhoto(String profilePhoto) {
        this.profilePhoto = profilePhoto;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }
    // Métodos para restar valores
    public void subtractCaloriesToday(double calories) {
        if (calories < 0) {
            Log.w(TAG, "Intento de restar calorías negativas ignorado.");
            return;
        }
        Log.d(TAG, "subtractCaloriesToday | Valor previo: " + this.caloriesToday + ", Restando: " + calories);
        this.caloriesToday = Math.max(0, this.caloriesToday - calories);
        Log.d(TAG, "subtractCaloriesToday | Valor actualizado: " + this.caloriesToday);
    }

    public void subtractProteinToday(double protein) {
        if (protein < 0) {
            Log.w(TAG, "Intento de restar proteínas negativas ignorado.");
            return;
        }
        Log.d(TAG, "subtractProteinToday | Valor previo: " + this.proteinToday + ", Restando: " + protein);
        this.proteinToday = Math.max(0, this.proteinToday - protein);
        Log.d(TAG, "subtractProteinToday | Valor actualizado: " + this.proteinToday);
    }

    public void subtractFatToday(double fat) {
        if (fat < 0) {
            Log.w(TAG, "Intento de restar grasas negativas ignorado.");
            return;
        }
        Log.d(TAG, "subtractFatToday | Valor previo: " + this.fatToday + ", Restando: " + fat);
        this.fatToday = Math.max(0, this.fatToday - fat);
        Log.d(TAG, "subtractFatToday | Valor actualizado: " + this.fatToday);
    }

    public void subtractCarbToday(double carbs) {
        if (carbs < 0) {
            Log.w(TAG, "Intento de restar carbohidratos negativos ignorado.");
            return;
        }
        Log.d(TAG, "subtractCarbToday | Valor previo: " + this.carbToday + ", Restando: " + carbs);
        this.carbToday = Math.max(0, this.carbToday - carbs);
        Log.d(TAG, "subtractCarbToday | Valor actualizado: " + this.carbToday);
    }

    public void resetDailyValues() {
        Log.d(TAG, "resetDailyValues | Reiniciando valores diarios a cero.");
        this.caloriesToday = 0.0;
        this.proteinToday = 0.0;
        this.fatToday = 0.0;
        this.carbToday = 0.0;
    }

    // Métodos de cálculo
    public int massIndex(double weight, double height) {
        height = feetToMeters(height);
        return (int) (weight / (height * height));
    }

    private double feetToMeters(double feet) {
        return feet * 0.3048;
    }

    public double calculateCalories(double weight, double height, int age, String gender, String activityLevel, String goal) {
        double bmr;

        // 🔥 Cálculo mejorado del BMR
        if ("male".equalsIgnoreCase(gender)) {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) + 5;
        } else {
            bmr = (10 * weight) + (6.25 * height) - (5 * age) - 161;
        }

        // 📌 Ajuste con peso x 23 y suavizado del impacto
        double adjustedBMR = (bmr + (weight * 23)) / 2;

        // 🔥 Factor de actividad basado en nivel de actividad
        double activityFactor = switch (activityLevel != null ? activityLevel.toLowerCase() : "sedentary") {
            case "sedentary" -> 1.2;       // Poco o nada de ejercicio
            case "light" -> 1.375;         // Ejercicio ligero (1-3 días/semana)
            case "moderate" -> 1.55;       // Ejercicio moderado (3-5 días/semana)
            case "high" -> 1.725;          // Ejercicio intenso (6-7 días/semana)
            case "very high" -> 1.9;       // Atletas o entrenamientos extremos
            default -> 1.2;                // Sedentario por defecto
        };

        // 🔥 Cálculo del Gasto Energético Total (GET)
        double get = adjustedBMR * activityFactor;

        // 🔥 Ajuste según objetivo del usuario
        double finalCalories = switch (goal != null ? goal.toLowerCase() : "maintain weight") {
            case "lose weight" -> get * 0.75;       // 🔹 Deficit calórico (-25%)
            case "gain muscle" -> get * 1.15;       // 🔹 Superávit calórico (+15%)
            case "gain weight" -> get * 1.2;        // 🔹 Subida de peso más agresiva (+20%)
            default -> get;                         // 🔹 Mantenimiento
        };

        // 📌 Logs para depuración en Logcat
        Log.d("CalculoCalorias", "📌 Peso: " + weight + " kg, Altura: " + height + " cm, Edad: " + age + " años, Género: " + gender);
        Log.d("CalculoCalorias", "📌 Nivel de Actividad: " + activityLevel + " (Factor: " + activityFactor + ")");
        Log.d("CalculoCalorias", "📌 BMR Original: " + bmr + ", BMR Ajustado: " + adjustedBMR);
        Log.d("CalculoCalorias", "📌 Gasto Energético Total (GET): " + get);
        Log.d("CalculoCalorias", "📌 Objetivo: " + goal + ", Calorías Finales: " + finalCalories);

        return finalCalories;
    }


    public double calculateProtein() {
        // Ejemplo: 1.6 gramos de proteína por kilogramo de peso corporal
        return weight * 1.6;
    }

    public double calculateFat() {
        // Ejemplo: 0.8 gramos de grasa por kilogramo de peso corporal
        return weight * 0.8;
    }

    public double calculateCarbs() {
        // Ejemplo: 4 gramos de carbohidratos por kilogramo de peso corporal
        return weight * 4;
    }

    public double[] calculateMacros(double weight, String goal, double calories) {
        double proteinGrams = 1.8 * weight;
        double fatGrams = calories * 0.25 / 9;
        double carbGrams = (calories - (proteinGrams * 4) - (fatGrams * 9)) / 4;
        return new double[]{proteinGrams, fatGrams, carbGrams};
    }
}
