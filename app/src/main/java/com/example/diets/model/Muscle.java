package com.example.diets.model;

import java.io.Serializable;

public class Muscle implements Serializable {
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;

    // Constructor sin argumentos requerido por Firebase
    public Muscle() {
    }

    // Constructor con argumentos
    public Muscle(int id, String name) {
        this.id = id;
        this.name = name;
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

    @Override
    public String toString() {
        return "Muscle{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}