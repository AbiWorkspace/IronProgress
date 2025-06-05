package com.example.diets.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class OkHttpSingleton {

    /** Cliente único y reutilizable para toda la app */
    private static final OkHttpClient INSTANCE = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)   // ⏱ abrir conexión
            .readTimeout(45, TimeUnit.SECONDS)      // ⏱ esperar respuesta
            .callTimeout(50, TimeUnit.SECONDS)      // ⏱ tiempo total permitido

            .addInterceptor(new RetryInterceptor(3, 1000)) // 🔁 Reintentos con back-off (1s, 2s, 4s)

            .addInterceptor(chain -> {
                Request request = chain.request()
                        .newBuilder()
                        .header("User-Agent", "IronProgressApp/1.0 (Android)")
                        .build();
                return chain.proceed(request);
            })

            .retryOnConnectionFailure(true)
            .build();

    public static OkHttpClient get() { return INSTANCE; }

    private OkHttpSingleton() {}
}

/** Interceptor de reintento con back-off exponencial */
class RetryInterceptor implements Interceptor {
    private final int maxRetries;
    private final long baseDelayMs;

    RetryInterceptor(int maxRetries, long baseDelayMs) {
        this.maxRetries = maxRetries;
        this.baseDelayMs = baseDelayMs;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        int tryCount = 0;
        IOException lastException = null;

        while (tryCount < maxRetries) {
            try {
                return chain.proceed(chain.request());
            } catch (IOException e) {
                lastException = e;
                tryCount++;

                if (tryCount >= maxRetries) break;

                long sleep = baseDelayMs * (1L << (tryCount - 1)); // 1s, 2s, 4s
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException ignored) {}
            }
        }

        throw lastException != null ? lastException : new IOException("❌ Error de red desconocido tras múltiples intentos.");
    }
}
