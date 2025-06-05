package com.example.diets.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class FoodBlock {

    private static final String TAG = "FoodBlock";

    private String title;
    private List<FoodItem> items;

    // 🔧 Constructor vacío requerido por Gson
    public FoodBlock() {
        Log.d(TAG, "Constructor vacío llamado");
        this.items = new ArrayList<>();
    }

    public FoodBlock(String title) {
        this.title = title;
        this.items = new ArrayList<>();
        Log.d(TAG, "Constructor con título: " + title);
    }

    public void removeItem(FoodItem item) {
        if (item == null) {
            Log.w(TAG, "removeItem: intento de eliminar un item null");
            return;
        }
        boolean removed = items.remove(item);
        Log.d(TAG, "removeItem: " + item.getName() + " → " + (removed ? "eliminado" : "no encontrado"));
    }

    public void addItem(FoodItem item) {
        if (item == null) {
            Log.w(TAG, "addItem: intento de agregar un item null");
            return;
        }
        if (items == null) {
            Log.w(TAG, "addItem: lista estaba null, inicializando");
            items = new ArrayList<>();
        }
        items.add(item);
        Log.d(TAG, "addItem: " + item.getName() + " añadido");
    }

    public String getTitle() {
        Log.d(TAG, "getTitle: " + title);
        return title;
    }

    public void setTitle(String title) {
        Log.d(TAG, "setTitle: " + title);
        this.title = title;
    }

    public List<FoodItem> getItems() {
        Log.d(TAG, "getItems: cantidad=" + (items != null ? items.size() : "null"));
        return items;
    }

    public void setItems(List<FoodItem> items) {
        Log.d(TAG, "setItems: asignando lista con " + (items != null ? items.size() : 0) + " elementos");
        this.items = items;
    }
}