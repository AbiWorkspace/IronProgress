package com.example.diets.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.example.diets.model.Muscle;

public class MovementPattern implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private List<Muscle> synergistMuscles = new ArrayList<>(); // Inicialización de la lista

    // Constructor sin argumentos requerido por Firebase
    public MovementPattern() {
    }

    // Constructor con argumentos
    public MovementPattern(int id, String name, List<Muscle> synergistMuscles) {
        this.id = id;
        this.name = name;
        this.synergistMuscles = synergistMuscles != null ? synergistMuscles : new ArrayList<>();
    }

    // Getters y Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Muscle> getSynergistMuscles() {
        return synergistMuscles;
    }

    public void setSynergistMuscles(List<Muscle> synergistMuscles) {
        this.synergistMuscles = synergistMuscles;
    }

    @Override
    public String toString() {
        return "MovementPattern{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", synergistMuscles=" + synergistMuscles +
                '}';
    }
}