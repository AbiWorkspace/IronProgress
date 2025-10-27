package com.example.diets.testing.fakes;

public class OffResponse {
    public int status;           // 1 = OK, !=1 = fallo
    public double kcalPer100;
    public double proteinPer100;
    public double carbPer100;
    public double fatPer100;
    public String name;

    public static OffResponse ok(String name, double kcal, double p, double c, double f){
        OffResponse r = new OffResponse();
        r.status = 1; r.name = name;
        r.kcalPer100 = kcal; r.proteinPer100 = p; r.carbPer100 = c; r.fatPer100 = f;
        return r;
    }

    public static OffResponse fail(){
        OffResponse r = new OffResponse();
        r.status = 0;
        return r;
    }
}
