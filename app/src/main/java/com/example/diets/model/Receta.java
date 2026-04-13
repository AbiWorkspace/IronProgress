package com.example.diets.model;

import java.util.ArrayList;
import java.util.List;

public class Receta {
    private String nombre;
    private List<Producto> productos; // Lista de productos en la receta

    // Constructor vacío requerido para Firebase
    public Receta() {
        this.productos = new ArrayList<>();
    }

    // Constructor con nombre
    public Receta(String nombre) {
        this.nombre = nombre;
        this.productos = new ArrayList<>();
    }

    public void agregarProducto(Producto producto) {
        this.productos.add(producto);
    }

    // Métodos para calcular los totales de macronutrientes y micronutrientes
    public double calcularCaloriasTotales() {
        return productos.stream().mapToDouble(Producto::getCalorias).sum();
    }

    public double calcularProteinasTotales() {
        return productos.stream().mapToDouble(Producto::getProteinas).sum();
    }

    public double calcularCarbohidratosTotales() {
        return productos.stream().mapToDouble(Producto::getCarbohidratos).sum();
    }

    public double calcularGrasasTotales() {
        return productos.stream().mapToDouble(Producto::getGrasas).sum();
    }

    public double calcularFibraTotal() {
        return productos.stream().mapToDouble(Producto::getFibra).sum();
    }

    public double calcularSodioTotal() {
        return productos.stream().mapToDouble(Producto::getSodio).sum();
    }

    public double calcularCalcioTotal() {
        return productos.stream().mapToDouble(Producto::getCalcio).sum();
    }

    public double calcularHierroTotal() {
        return productos.stream().mapToDouble(Producto::getHierro).sum();
    }

    public double calcularVitaminaCTotal() {
        return productos.stream().mapToDouble(Producto::getVitaminaC).sum();
    }

    // Getters y setters
    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public List<Producto> getProductos() {
        return productos;
    }

    public void setProductos(List<Producto> productos) {
        this.productos = productos;
    }

    @Override
    public String toString() {
        return "Receta{" +
                "nombre='" + nombre + '\'' +
                ", productos=" + productos +
                ", caloriasTotales=" + calcularCaloriasTotales() +
                ", proteinasTotales=" + calcularProteinasTotales() +
                ", carbohidratosTotales=" + calcularCarbohidratosTotales() +
                ", grasasTotales=" + calcularGrasasTotales() +
                ", fibraTotal=" + calcularFibraTotal() +
                ", sodioTotal=" + calcularSodioTotal() +
                ", calcioTotal=" + calcularCalcioTotal() +
                ", hierroTotal=" + calcularHierroTotal() +
                ", vitaminaCTotal=" + calcularVitaminaCTotal() +
                '}';
    }
}
