package com.example.diets.testutil;

import java.util.*;
import androidx.annotation.Nullable;

public class TestDoubles {

    public static Set<String> normalize(List<String> xs){
        Set<String> out = new HashSet<>();
        if (xs==null) return out;
        for (String x: xs){
            if (x==null) continue;
            String s = x.toLowerCase(Locale.ROOT).trim()
                    .replace("á","a").replace("é","e").replace("í","i")
                    .replace("ó","o").replace("ú","u");
            out.add(s);
        }
        return out;
    }

    public static boolean hasMatchingMuscles(List<String> patternWorked, List<String> patternSyn,
                                             List<String> exWorked, List<String> exSyn){
        Set<String> p = normalize(merge(patternWorked, patternSyn));
        Set<String> e = normalize(merge(exWorked, exSyn));
        p.remove(""); e.remove("");
        p.retainAll(e);
        return !p.isEmpty();
    }

    private static List<String> merge(List<String> a, List<String> b){
        List<String> z = new ArrayList<>();
        if (a!=null) z.addAll(a);
        if (b!=null) z.addAll(b);
        return z;
    }

    public static Number calcularPreviousLoad(List<Number> weightsHistory){
        if (weightsHistory==null || weightsHistory.isEmpty()) return null;
        return weightsHistory.get(weightsHistory.size()-1);
    }

    public static Map<String,Double> calcularMacrosPorPorcion(double gramos, OffResponse base100){
        Map<String,Double> m = new HashMap<>();
        double f = gramos/100.0;
        m.put("kcal", round(base100.kcalPer100 * f));
        m.put("protein", round(base100.proteinPer100 * f));
        m.put("carb", round(base100.carbPer100 * f));
        m.put("fat", round(base100.fatPer100 * f));
        return m;
    }

    private static double round(double v){ return Math.round(v*100.0)/100.0; }

    public static byte[] decodeBase64ToBytes(String b64){
        try { return java.util.Base64.getDecoder().decode(b64); }
        catch (IllegalArgumentException ex){ return null; }
    }

    public static class OffResponse {
        public int status; public double kcalPer100, proteinPer100, carbPer100, fatPer100;
        public static OffResponse ok(double kcal, double p, double c, double f){
            OffResponse r = new OffResponse();
            r.status = 1; r.kcalPer100=kcal; r.proteinPer100=p; r.carbPer100=c; r.fatPer100=f; return r;
        }
        public static OffResponse fail(){ OffResponse r=new OffResponse(); r.status=0; return r; }
    }

    public static class Exercise {
        public String equipmentType = "desconocido";
        public String exerciseType  = "desconocido";
    }
    public static Exercise mapToExercise(Map<String, Object> dataMap){
        Exercise e = new Exercise();
        Object eq = dataMap.get("equipmentType");
        Object ex = dataMap.get("exerciseType");
        if (eq instanceof String && !((String)eq).isEmpty()) e.equipmentType = (String) eq;
        if (ex instanceof String && !((String)ex).isEmpty()) e.exerciseType  = (String) ex;
        return e;
    }

    public static boolean isExerciseValid(@Nullable Integer currentLoad, Integer maxReps, Integer familiarity){
        if (currentLoad == null || currentLoad <= 0) return false;
        if (maxReps == null || maxReps <= 0) return false;
        if (familiarity == null || familiarity < 0) return false;
        return true;
    }

    public static class RpeField {
        public String text = "";
        public boolean hasError = false;
        public void afterTextChanged(String input){
            try {
                int v = Integer.parseInt(input.trim());
                hasError = (v < 1 || v > 10);
                if (!hasError) text = input; // NO sobrescribir por "0"
            } catch (Exception e){ hasError = true; }
        }
    }

    public static class SeriesAdapterFake {
        private final List<Map<String,Object>> buffer = new ArrayList<>();
        public void editRow(int idx, String k, Object v){
            while (buffer.size() <= idx) buffer.add(new HashMap<>());
            buffer.get(idx).put(k, v);
        }
        public List<Map<String,Object>> commitAllEdits(){
            List<Map<String,Object>> out = new ArrayList<>();
            for (Map<String,Object> row : buffer){
                out.add(Collections.unmodifiableMap(new HashMap<>(row)));
            }
            return Collections.unmodifiableList(out);
        }
    }

    public static List<String> filtrarEjerciciosLocal(List<String> allExercisesNames,
                                                      List<String> patternWorked, List<String> patternSyn){
        List<String> r = new ArrayList<>();
        boolean quierePecho = hasMatchingMuscles(patternWorked, patternSyn,
                Collections.singletonList("Pecho"), Collections.emptyList());
        for (String name : allExercisesNames){
            if (quierePecho) {
                if (name.toLowerCase(Locale.ROOT).contains("press")) r.add(name);
            } else {
                r.add(name);
            }
        }
        return r;
    }
}
