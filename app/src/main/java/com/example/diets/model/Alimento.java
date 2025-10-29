package com.example.diets.model;

public class Alimento {
    private String codigo;
    private String nombre;
    private String marcas;
    private String categorias;
    private double calorias;
    private double proteinas;
    private double carbohidratos;
    private double azucares;
    private double grasas;
    private double grasasSaturadas;
    private double fibra;
    private double sodio;
    private double calcio;
    private double hierro;
    private double vitaminaC;

    public Alimento() {}

    public Alimento(String codigo, String nombre, String marcas, String categorias,
                    double calorias, double proteinas, double carbohidratos, double azucares,
                    double grasas, double grasasSaturadas, double fibra, double sodio,
                    double calcio, double hierro, double vitaminaC) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.marcas = marcas;
        this.categorias = categorias;
        this.calorias = calorias;
        this.proteinas = proteinas;
        this.carbohidratos = carbohidratos;
        this.azucares = azucares;
        this.grasas = grasas;
        this.grasasSaturadas = grasasSaturadas;
        this.fibra = fibra;
        this.sodio = sodio;
        this.calcio = calcio;
        this.hierro = hierro;
        this.vitaminaC = vitaminaC;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getMarcas() {
        return marcas;
    }

    public void setMarcas(String marcas) {
        this.marcas = marcas;
    }

    public String getCategorias() {
        return categorias;
    }

    public void setCategorias(String categorias) {
        this.categorias = categorias;
    }

    public double getCalorias() {
        return calorias;
    }

    public void setCalorias(double calorias) {
        this.calorias = calorias;
    }

    public double getProteinas() {
        return proteinas;
    }

    public void setProteinas(double proteinas) {
        this.proteinas = proteinas;
    }

    public double getCarbohidratos() {
        return carbohidratos;
    }

    public void setCarbohidratos(double carbohidratos) {
        this.carbohidratos = carbohidratos;
    }

    public double getAzucares() {
        return azucares;
    }

    public void setAzucares(double azucares) {
        this.azucares = azucares;
    }

    public double getGrasas() {
        return grasas;
    }

    public void setGrasas(double grasas) {
        this.grasas = grasas;
    }

    public double getGrasasSaturadas() {
        return grasasSaturadas;
    }

    public void setGrasasSaturadas(double grasasSaturadas) {
        this.grasasSaturadas = grasasSaturadas;
    }

    public double getFibra() {
        return fibra;
    }

    public void setFibra(double fibra) {
        this.fibra = fibra;
    }

    public double getSodio() {
        return sodio;
    }

    public void setSodio(double sodio) {
        this.sodio = sodio;
    }

    public double getCalcio() {
        return calcio;
    }

    public void setCalcio(double calcio) {
        this.calcio = calcio;
    }

    public double getHierro() {
        return hierro;
    }

    public void setHierro(double hierro) {
        this.hierro = hierro;
    }

    public double getVitaminaC() {
        return vitaminaC;
    }

    public void setVitaminaC(double vitaminaC) {
        this.vitaminaC = vitaminaC;
    }

    @Override
    public String toString() {
        return "Alimento{" +
                "codigo='" + codigo + '\'' +
                ", nombre='" + nombre + '\'' +
                ", marcas='" + marcas + '\'' +
                ", categorias='" + categorias + '\'' +
                ", calorias=" + calorias +
                ", proteinas=" + proteinas +
                ", carbohidratos=" + carbohidratos +
                ", azucares=" + azucares +
                ", grasas=" + grasas +
                ", grasasSaturadas=" + grasasSaturadas +
                ", fibra=" + fibra +
                ", sodio=" + sodio +
                ", calcio=" + calcio +
                ", hierro=" + hierro +
                ", vitaminaC=" + vitaminaC +
                '}';
    }
}
