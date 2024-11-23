package com.merino.ddfilms.configuration;

import com.merino.ddfilms.api.FirebaseManager;

import com.merino.ddfilms.utils.TaskCompletionCallback;

public class AppConfig {

    private static String tmdbApiKey;
    private final FirebaseManager firebaseManager = new FirebaseManager();

    public void getTmdbApiKey(TaskCompletionCallback<String> callback) {
        if (tmdbApiKey != null) {
            callback.onComplete(tmdbApiKey, null); // Devolvemos la clave si ya está disponible
        } else {
            fetchTmdbApiKey(callback); // Si no está disponible, la obtenemos
        }
    }

    private void fetchTmdbApiKey(TaskCompletionCallback<String> callback) {
        firebaseManager.getTmdbApiKey((apikey, error) -> {
            if (error != null) {
                callback.onComplete(null, error); // Si hay error, lo devolvemos
            } else {
                tmdbApiKey = apikey; // Almacenamos la clave
                callback.onComplete(tmdbApiKey, null); // La devolvemos al callback
            }
        });
    }
}
