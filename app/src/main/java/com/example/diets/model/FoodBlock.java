package com.example.diets.model;

import java.util.ArrayList;
import java.util.List;

public class FoodBlock {
    private String title;
    private List<FoodItem> items;

    public FoodBlock(String title) {
        this.title = title;
        this.items = new ArrayList<>();
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<FoodItem> getItems() { return items; }
    public void addItem(FoodItem item) { items.add(item); }
}
