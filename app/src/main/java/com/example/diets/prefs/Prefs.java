package com.example.diets.prefs;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String FILE = "ironprogress_prefs";
    private static final String KEY_GLOBAL = "dynamic_enabled";
    private Prefs() {}

    public static boolean isDynamicEnabled(Context ctx) {
        try {
            SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
            return sp.getBoolean(KEY_GLOBAL, true);
        } catch (Exception e) { return true; }
    }

    public static boolean isDynamicEnabledForExercise(Context ctx, String exerciseName) {
        try {
            SharedPreferences sp = ctx.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
            String key = "dynamic_enabled_" + safeKey(exerciseName);
            return sp.getBoolean(key, isDynamicEnabled(ctx));
        } catch (Exception e) { return isDynamicEnabled(ctx); }
    }

    private static String safeKey(String s) {
        if (s == null) return "";
        return s.trim().toLowerCase().replaceAll("\\s+", "_");
    }
}
