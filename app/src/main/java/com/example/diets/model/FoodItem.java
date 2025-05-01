package com.example.diets.model;

public class FoodItem {

    /* ---------- datos básicos ---------- */
    private final String imageUrl;        // 🔥 NUEVO
    private String  name;
    private double  grams;
    private boolean isChecked = true;
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

    /* ------------ constructores ------------ */
    // (1) sin macros detallados
    public FoodItem(String name,
                    double grams,
                    double kcalPer100g,
                    String imageUrl) {          // 🔥
        this(name, grams, kcalPer100g,
                0, 0, 0, imageUrl);               // delega al completo
    }

    // (2) completo, con macros y url
    public FoodItem(String name,
                    double grams,
                    double kcalPer100g,
                    double proteinPer100g,
                    double carbsPer100g,
                    double fatPer100g,
                    String imageUrl) {          // 🔥
        this.name  = name;
        this.grams = grams;
        this.kcalPer100g    = kcalPer100g;
        this.proteinPer100g = proteinPer100g;
        this.carbsPer100g   = carbsPer100g;
        this.fatPer100g     = fatPer100g;
        this.imageUrl       = imageUrl;        // 🔥
        calculateTotals();
    }

    /* ------------ lógica interna ------------ */
    private void calculateTotals() {
        double factor = grams / 100.0;
        totalKcal    = kcalPer100g    * factor;
        totalProtein = proteinPer100g * factor;
        totalCarbs   = carbsPer100g   * factor;
        totalFat     = fatPer100g     * factor;
    }

    /* ---------------- getters ---------------- */
    public String  getName()          { return name; }
    public double  getGrams()         { return grams; }
    public double  getKcalPer100g()   { return kcalPer100g; }
    public double  getProteinPer100g(){ return proteinPer100g; }
    public double  getCarbsPer100g()  { return carbsPer100g; }
    public double  getFatPer100g()    { return fatPer100g; }
    public double  getTotalKcal()     { return totalKcal; }
    public double  getTotalProtein()  { return totalProtein; }
    public double  getTotalCarbs()    { return totalCarbs; }
    public double  getTotalFat()      { return totalFat; }
    public String  getImageUrl()      { return imageUrl; }   // 🔥

    /* ---------------- setters ---------------- */
    public void setGrams(double grams) {
        this.grams = grams;
        calculateTotals();
    }
    public boolean isChecked() {
        return isChecked;
    }

    public void setChecked(boolean checked) {
        this.isChecked = checked;
    }
}
