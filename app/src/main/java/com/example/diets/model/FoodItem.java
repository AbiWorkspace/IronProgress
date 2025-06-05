package com.example.diets.model;

import java.util.List;

public class FoodItem {
    private static final long serialVersionUID = 1L;

    /* ---------- datos básicos ---------- */
    private String imageUrl;          // 🔥 Imagen del alimento
    private String name;
    private double grams;
    private boolean isChecked = true;
    private boolean isRecipe;        // ✅ Nuevo campo para distinguir receta/producto
    private List<String> ingredients;
    private String instructions;

    /* ---------- valores por 100 g ---------- */
    private double kcalPer100g;
    private double proteinPer100g;
    private double carbsPer100g;
    private double fatPer100g;

    /* ---------- totales calculados ---------- */
    private double totalKcal;
    private double totalProtein;
    private double totalCarbs;
    private double totalFat;

    /* ------------ constructor vacío requerido por Gson ------------ */
    public FoodItem() {
        // Necesario para que Gson pueda deserializar correctamente
        this.isChecked = true;
    }

    // (1) Constructor sin macros detallados
    public FoodItem(String name,
                    double grams,
                    double kcalPer100g,
                    String imageUrl) {
        this(name, grams, kcalPer100g,
                0, 0, 0, imageUrl, false);
    }

    // (2) Constructor completo sin isRecipe (compatibilidad)
    public FoodItem(String name,
                    double grams,
                    double kcalPer100g,
                    double proteinPer100g,
                    double carbsPer100g,
                    double fatPer100g,
                    String imageUrl) {
        this(name, grams, kcalPer100g,
                proteinPer100g, carbsPer100g, fatPer100g, imageUrl, false);
    }

    // (3) ✅ Constructor completo con isRecipe
    public FoodItem(String name,
                    double grams,
                    double kcalPer100g,
                    double proteinPer100g,
                    double carbsPer100g,
                    double fatPer100g,
                    String imageUrl,
                    boolean isRecipe) {
        this.name = name;
        this.grams = grams;
        this.kcalPer100g = kcalPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbsPer100g = carbsPer100g;
        this.fatPer100g = fatPer100g;
        this.imageUrl = imageUrl;

        this.isRecipe = isRecipe;  // usa el valor recibido
        this.isChecked = true;     // por defecto marcado al crear

        calculateTotals();
    }

    /* ------------ lógica interna ------------ */
    private void calculateTotals() {
        double factor = grams / 100.0;
        totalKcal = kcalPer100g * factor;
        totalProtein = proteinPer100g * factor;
        totalCarbs = carbsPer100g * factor;
        totalFat = fatPer100g * factor;
    }

    /* ---------------- getters ---------------- */
    public String getName()             { return name; }
    public double getGrams()            { return grams; }
    public double getKcalPer100g()      { return kcalPer100g; }
    public double getProteinPer100g()   { return proteinPer100g; }
    public double getCarbsPer100g()     { return carbsPer100g; }
    public double getFatPer100g()       { return fatPer100g; }
    public double getTotalKcal()        { return totalKcal; }
    public double getTotalProtein()     { return totalProtein; }
    public double getTotalCarbs()       { return totalCarbs; }
    public double getTotalFat()         { return totalFat; }
    public String getImageUrl()         { return imageUrl; }
    public boolean isChecked()          { return isChecked; }
    public boolean isRecipe()           { return isRecipe; }
    public List<String> getIngredients(){ return ingredients; }
    public String getInstructions()     { return instructions; }

    /* ---------------- setters ---------------- */
    public void setGrams(double grams) {
        this.grams = grams;
        calculateTotals();
    }
    public void setRecipe(boolean recipe) {
        this.isRecipe = recipe;
    }
    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }
    public void setIngredients(List<String> ingredients) {
        this.ingredients = ingredients;
    }
    public void setIsRecipe(boolean isRecipe) {
        this.isRecipe = isRecipe;
    }
    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
