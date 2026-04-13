package com.example.diets.model;

import java.io.Serializable;

public class Muscle {
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

    // ✅ Nuevo constructor que acepta solo el nombre (Solución al error)
    public Muscle(String name) {
        this.id = 0; // ID por defecto
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
