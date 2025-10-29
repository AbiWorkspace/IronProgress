package com.example.diets.Utils;


import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;

public final class NetworkInfoUtil {
    public static String networkType(Context c) {
        try {
            ConnectivityManager cm = (ConnectivityManager)c.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (nc == null) return "UNKNOWN";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "WIFI";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "CELL";
            return "OTHER";
        } catch (Exception e) { return "UNKNOWN"; }
    }
    public static String deviceTier(Context c) {
        ActivityManager am = (ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE);
        int mc = am.getMemoryClass(); // ~RAM app MB
        if (mc <= 2048) return "low";
        if (mc <= 4096) return "med";
        return "high";
    }
}
