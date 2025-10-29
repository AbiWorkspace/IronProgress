package com.example.diets.model;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FoodBlock {

    private static final String TAG = "FoodBlock";

    /* ---------- CAMPOS ---------- */
    private String title;
    private final List<FoodItem> items = new ArrayList<>();

    /* ---------- CONSTRUCTORES ---------- */
    public FoodBlock() {           // requerido por Gson / Firebase
        Log.d(TAG, "Constructor vacío llamado");
    }

    public FoodBlock(String title) {
        this.title = title;
        Log.d(TAG, "Constructor con título: " + title);
    }

    public FoodBlock(String title, List<FoodItem> items) {
        this.title = title;
        setItems(items); // reutiliza el setter para normalizar
        Log.d(TAG, "Constructor con título+items: " + title + " (" + this.items.size() + " items)");
    }

    /* ---------- CRUD ITEMS ---------- */
    public void addItem(FoodItem it) {
        if (it == null) return;
        items.add(it);
        Log.d(TAG, "addItem: " + it.getName() + " añadido");
    }

    public void removeItem(FoodItem it) {
        if (it == null) return;
        items.remove(it);
    }

    /** Setter requerido por DashboardFragment: nunca deja la lista en null. */
    public void setItems(List<FoodItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        Log.d(TAG, "setItems: tamaño ahora = " + items.size());
    }

    /* ---------- GET / SET ---------- */
    public String getTitle() { return title; }

    public void setTitle(String t) {
        this.title = t;
    }

    /** Nunca retorna null. */
    public List<FoodItem> getItems() { return items; }

    /* ===========================================================
       MÉTODO NECESARIO PARA EL ADAPTER --> CLONA PROFUNDAMENTE  */
    public FoodBlock deepCopy(String suffix) {
        FoodBlock clone = new FoodBlock((this.title == null ? "" : this.title) + " " + suffix);

        for (FoodItem it : this.items) {
            if (it == null) continue;
            FoodItem copy = new FoodItem(
                    it.getName(),
                    it.getGrams(),
                    it.getKcalPer100g(),
                    it.getProteinPer100g(),
                    it.getCarbsPer100g(),
                    it.getFatPer100g(),
                    it.getImageUrl()
            );
            copy.setRecipe(it.isRecipe());
            copy.setIngredients(it.getIngredients());
            copy.setInstructions(it.getInstructions());
            copy.setChecked(it.isChecked());
            clone.addItem(copy);
        }
        return clone;
    }

    /* ---------- EQUALS & HASHCODE (para DiffUtil) ---------- */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FoodBlock)) return false;
        FoodBlock fb = (FoodBlock) o;
        return Objects.equals(title, fb.title) &&
                Objects.equals(items, fb.items);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, items);
    }
}
