package com.example.diets.network;

import android.util.Log;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Métricas y logging sin depender de Context.
 * - NET_URL: URL completa de Spoonacular (apiKey oculta)
 * - NET_METRICS: línea legible (API, método, path, código, t_ms, intento)
 * - SPN_METRICS: JSON compacto para pegar en reportes
 */
public final class MetricsInterceptor implements Interceptor {

    private static final boolean METRICS_ENABLED = true;
    private static final String METRICS_TAG = "NET_METRICS";

    @Override
    public Response intercept(Chain chain) throws IOException {
        long t0 = System.nanoTime();
        Request req = chain.request();

        // Detectar API por host
        String host = req.url().host();
        String api = host.contains("openfoodfacts") ? "OFF"
                : host.contains("spoonacular")      ? "SPN"
                : "OTHER";

        // Mostrar URL (Spoonacular) ocultando apiKey
        if ("SPN".equals(api)) {
            String safeUrl = req.url().toString()
                    .replaceAll("(?i)(apiKey=)[^&]+", "$1***hidden***");
            Log.d("NET_URL", "SPN " + safeUrl);
        }

        // Intento actual (inyectado por RetryInterceptor)
        String attemptHdr = req.header("X-Retry-Count");
        int attempt = 1;
        try { if (attemptHdr != null) attempt = Integer.parseInt(attemptHdr); }
        catch (NumberFormatException ignored) {}

        Response rsp = null;
        IOException thrown = null;

        try {
            rsp = chain.proceed(req);
            return rsp;
        } catch (IOException e) {
            thrown = e;
            throw e;
        } finally {
            long ms = (System.nanoTime() - t0) / 1_000_000L;
            int code = (rsp != null) ? rsp.code() : -1;
            long payload = -1L;
            try {
                if (rsp != null && rsp.body() != null) payload = rsp.body().contentLength();
            } catch (Exception ignore) {}

            if (METRICS_ENABLED) {
                if (thrown == null) {
                    Log.d(METRICS_TAG,
                            api + " " + req.method() + " " + req.url().encodedPath() +
                                    " code=" + code +
                                    " t_ms=" + ms +
                                    " try=" + attempt);
                } else {
                    Log.w(METRICS_TAG,
                            api + " " + req.method() + " " + req.url().encodedPath() +
                                    " error=" + thrown.getClass().getSimpleName() +
                                    " t_ms=" + ms +
                                    " try=" + attempt);
                }
            }

            // JSON compacto (sin Context: network/tier como "unknown")
            String json = String.format(Locale.US,
                    "{\"ts\":%d,\"api\":\"%s\",\"latency_ms\":%d,\"http_code\":%d,\"retries\":%d," +
                            "\"network\":\"unknown\",\"device_tier\":\"unknown\",\"payload_bytes\":%d}",
                    System.currentTimeMillis(), api, ms, code, attempt, payload);
            Log.i("SPN_METRICS", json);
        }
    }
}
