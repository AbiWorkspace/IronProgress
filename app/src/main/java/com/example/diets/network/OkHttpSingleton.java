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
            /* ─ timeouts más holgados ─ */
            .connectTimeout(15, TimeUnit.SECONDS)   // abrir socket
            .readTimeout(45,   TimeUnit.SECONDS)    // recibir cada bloque
            .callTimeout(50,   TimeUnit.SECONDS)    // toda la llamada

            /* ─ reintento en fallos transitorios + back-off exponencial ─ */
            .addInterceptor(new RetryInterceptor(3, 1_000)) // 3 intentos, 1 s → 2 s → 4 s

            /* ─ encabezado User-Agent en cada request ─ */
            .addInterceptor(chain -> chain.proceed(
                    chain.request()
                            .newBuilder()
                            .header("User-Agent", "IronProgressApp/1.0 (Android)")
                            .build()))

            /* mantener reintentos de OkHttp por error IO */
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

                long sleep = baseDelayMs * (1L << (tryCount - 1)); // 1s, 2s, 4s…
                try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
            }
        }
        throw lastException != null ? lastException : new IOException("Unknown network error");
    }
}
