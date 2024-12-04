package com.merino.ddfilms.api;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TMDBClient {
    private static final String BASE_URL = "https://api.themoviedb.org/3/";

    private static Retrofit retrofit;

    public static Retrofit getClient(String apiKey) {
        if (retrofit == null) {
            OkHttpClient client = new OkHttpClient.Builder().addInterceptor(chain -> {
                Request original = chain.request();
                Request.Builder requestBuilder = original.newBuilder().header("Authorization", "Bearer " + apiKey).header("Content-Type", "application/json").header("accept", "application/json").method(original.method(), original.body());

                return chain.proceed(requestBuilder.build());
            }).build();

            retrofit = new Retrofit.Builder().baseUrl(BASE_URL).client(client).addConverterFactory(GsonConverterFactory.create()).build();
        }
        return retrofit;
    }
}
