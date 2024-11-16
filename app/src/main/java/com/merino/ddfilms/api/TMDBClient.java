package com.merino.ddfilms.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TMDBClient {
    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private static final String AUTH_TOKEN = "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIzZjQ0MjA3YmU5MzVhYmJiOWRiYzRjNzhmMjJjYWJmMCIsIm5iZiI6MTczMTM0ODY2NC45MzIyMzUyLCJzdWIiOiI1ZWRhNThkY2IzZjZmNTAwMjA5ODk1YjQiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.2GVXLVK1vviDpW26p8x8WrJduG7S6oIYJfLKD25NoCw"; // Reemplaza con tu API key
    private static Retrofit retrofit;

    public static Retrofit getClient() {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder().header("Authorization", AUTH_TOKEN).header("accept", "application/json").method(original.method(), original.body());

                return chain.proceed(requestBuilder.build());
            }).build();

            retrofit = new Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
}
