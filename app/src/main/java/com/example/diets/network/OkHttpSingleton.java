package com.example.diets.network;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class OkHttpSingleton {

    // Instancia diferida para evitar fallos en <clinit>
    private static volatile OkHttpClient INSTANCE;

    private OkHttpSingleton() {}

    /** Obtén el cliente único y reutilizable */
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
                            .addInterceptor(new RetryInterceptor(3, 1000)) // 1s, 2s, 4s
                            .addInterceptor(chain -> {
                                Request request = chain.request().newBuilder()
                                        .header("User-Agent", "IronProgressApp/1.0 (Android)")
                                        .build();
                                return chain.proceed(request);
                            })
                            .retryOnConnectionFailure(true)
                            .build();
                }
            }
        }
        return local;
    }

    /** Interceptor de reintento con back-off exponencial */
    static final class RetryInterceptor implements Interceptor {
        private final int maxRetries;
        private final long baseDelayMs;

        RetryInterceptor(int maxRetries, long baseDelayMs) {
            this.maxRetries = maxRetries;
            this.baseDelayMs = baseDelayMs;
        }

        @Override
        public Response intercept(Chain chain) throws IOException {
            int tryCount = 0;
            IOException last = null;

            while (tryCount < maxRetries) {
                try {
                    return chain.proceed(chain.request());
                } catch (IOException e) {
                    last = e;
                    tryCount++;
                    if (tryCount >= maxRetries) break;
                    long sleep = baseDelayMs * (1L << (tryCount - 1)); // 1s, 2s, 4s
                    try { Thread.sleep(sleep); } catch (InterruptedException ignored) {}
                }
            }
            throw last != null ? last : new IOException("Error de red tras múltiples intentos.");
        }
    }
}
