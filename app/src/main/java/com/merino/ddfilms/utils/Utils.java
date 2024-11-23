package com.merino.ddfilms.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.merino.ddfilms.configuration.AppConfig;

import java.util.concurrent.atomic.AtomicReference;

public class Utils {

    public static String getApiKey() {
        AppConfig appConfig = new AppConfig();
        AtomicReference<String> apikey = new AtomicReference<>("");
        appConfig.getTmdbApiKey((result, error) -> {
            if (error != null) {
                // Manejar el error
                Log.e("Utils", "Error: " + error.getMessage());
            } else {
                apikey.set(result);
            }
        });

        return apikey.get();
    }

    public static void showError(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
