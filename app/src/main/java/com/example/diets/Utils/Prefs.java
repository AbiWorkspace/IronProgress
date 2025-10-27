package com.example.diets.Utils;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "settings";
    private static final String KEY_PROGRE_DYNAMIC = "pref_prog_dinamica";

    public static boolean isDynamicEnabled(Context ctx) {
        return ctx.getSharedPreferences(FILE, MODE_PRIVATE)
                .getBoolean(KEY_PROGRE_DYNAMIC, true);   // ⬅️  por defecto: ON
    }

    public static void setDynamicEnabled(Context ctx, boolean value) {
        ctx.getSharedPreferences(FILE, MODE_PRIVATE)
                .edit().putBoolean(KEY_PROGRE_DYNAMIC, value).apply();
    }

    public static boolean isDynamicEnabled(Context ctx, String exercise){
        return ctx.getSharedPreferences("prefs", MODE_PRIVATE)
                .getBoolean("dyn_"+exercise, false);   // false = OFF por defecto
    }

    public static void setDynamicEnabledForExercise(Context ctx, String exerciseName, boolean enabled) {
        SharedPreferences prefs = ctx.getSharedPreferences("dynamic_prefs", Context.MODE_PRIVATE);
        prefs.edit().putBoolean("dyn_" + exerciseName, enabled).apply();
    }

    public static boolean isDynamicEnabledForExercise(Context ctx, String exerciseName) {
        SharedPreferences prefs = ctx.getSharedPreferences("dynamic_prefs", Context.MODE_PRIVATE);
        return prefs.getBoolean("dyn_" + exerciseName, false);
    }


}
