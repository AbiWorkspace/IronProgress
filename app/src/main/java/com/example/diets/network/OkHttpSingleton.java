package com.example.diets.network;

import android.util.Log;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Cliente único de OkHttp (sin necesidad de Context):
 * - Timeouts: 15s / 45s / 50s
 * - User-Agent global
 * - Reintentos con backoff exponencial (1s, 2s, 4s) + log de intento
 * - Métricas: MetricsInterceptor (NET_URL, NET_METRICS, SPN_METRICS)
 */
public final class OkHttpSingleton {

    private static volatile OkHttpClient INSTANCE;
    private static final String UA = "IronProgressApp/1.0 (Android)";

    private OkHttpSingleton() {}

    /** Obtén el cliente único y reutilizable (no requiere Context). */
    public static OkHttpClient get() {
        OkHttpClient local = INSTANCE;
        if (local == null) {
            synchronized (OkHttpSingleton.class) {
                local = INSTANCE;
                if (local == null) {
                    INSTANCE = local = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(45, TimeUnit.SECONDS)
                            .callTimeout(50, TimeUnit.SECONDS)
                            // 1) Reintentos con backoff exponencial (1s, 2s, 4s)
                            .addInterceptor(new RetryInterceptor(3, 1000))
                            // 2) User-Agent global
                            .addInterceptor(chain -> {
                                Request request = chain.request().newBuilder()
                                        .header("User-Agent", UA)
                                        .build();
                                return chain.proceed(request);
                            })
                            // 3) Métricas y URL segura
                            .addInterceptor(new MetricsInterceptor())
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return local;
    }

    /** Interceptor de reintento con back-off exponencial y log de cada intento. */
    static final class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        private final long baseDelayMs;

        RetryInterceptor(int maxRetries, long baseDelayMs) {
            this.maxRetries = maxRetries;
            this.baseDelayMs = baseDelayMs;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            int attempt = 1;
            IOException last = null;

            while (attempt <= maxRetries) {
                // Marcar intento para que lo lea MetricsInterceptor
                Request attemptReq = original.newBuilder()
                        .header("X-Retry-Count", String.valueOf(attempt))
                        .build();

                try {
                    return chain.proceed(attemptReq);
                } catch (IOException e) {
                    last = e;
                    if (attempt >= maxRetries) break;

                    long sleep = baseDelayMs * (1L << (attempt - 1)); // 1s, 2s, 4s
                    Log.w("RetryInterceptor",
                            "try=" + attempt + " sleep=" + sleep + "ms url=" + original.url());

                    try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
                    attempt++;
                }
            }
            throw (last != null) ? last : new IOException("Error de red tras múltiples intentos.");
        }
    }
}
