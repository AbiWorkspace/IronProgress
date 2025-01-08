package com.example.diets.model;

import java.util.List;

public class MovementPattern {
    private int id;
    private String name;
    private List<Muscle> synergistMuscles;

    // Constructor
    public MovementPattern(int id, String name, List<Muscle> synergistMuscles) {
        this.id = id;
        this.name = name;
        this.synergistMuscles = synergistMuscles;
    }

    // Getters and Setters
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