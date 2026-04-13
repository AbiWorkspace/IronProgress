package com.example.diets.model;
public class WeightRecord {
    private String id;
    private double weight;
    private double calories;
    private String date;

    public WeightRecord() {} // Obligatorio para Firebase

    public WeightRecord(String id, double weight, double calories, String date) {
        this.id = id;
        this.weight = weight;
        this.calories = calories;
        this.date = date;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
}
