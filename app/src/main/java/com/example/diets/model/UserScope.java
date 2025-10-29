package com.example.diets.model;


import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class UserScope {
    private static String activeUid;
    private static boolean coachMode;

    public static void start(String targetUid, boolean isCoachMode) {
        coachMode = isCoachMode;
        if (isCoachMode && targetUid != null && !targetUid.isEmpty()) {
            activeUid = targetUid; // editar cliente
        } else {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            activeUid = (u != null ? u.getUid() : null); // editarme a mí
        }
    }

    public static String getActiveUid() {
        if (activeUid == null) {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            activeUid = (u != null ? u.getUid() : null);
        }
        return activeUid;
    }

    public static boolean isCoachMode() { return coachMode; }

    public static void clear() {
        coachMode = false;
        activeUid = null;
    }

    private UserScope() {}
}