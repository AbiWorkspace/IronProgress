package com.example.diets.adapters;


import java.util.Locale;

public class Util {
    /** Formato 1 decimal sin ceros de sobra (220 → 220 ·  220.5 → 220.5) */
    public static String f1(double v) {
        return (v % 1 == 0) ? String.valueOf((int) v)
                : String.format(Locale.US, "%.1f", v);
    }
}