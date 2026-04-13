package com.example.diets.testing.fakes;

public class Product {
    public final String name;
    public final double kcalPer100, proteinPer100, carbPer100, fatPer100;

    public Product(String name, double kcalPer100, double proteinPer100, double carbPer100, double fatPer100) {
        this.name = name;
        this.kcalPer100 = kcalPer100;
        this.proteinPer100 = proteinPer100;
        this.carbPer100 = carbPer100;
        this.fatPer100 = fatPer100;
    }
}
