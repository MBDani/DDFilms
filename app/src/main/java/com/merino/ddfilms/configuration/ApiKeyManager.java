package com.merino.ddfilms.configuration;

import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.utils.TaskCompletionCallback;

import lombok.Getter;

public class ApiKeyManager {
    private static ApiKeyManager instance;

    @Getter
    private String apiKey;
    private boolean isApiKeyFetched = false;

    private final FirebaseManager firebaseManager = new FirebaseManager();

    private ApiKeyManager() {}

    public static synchronized ApiKeyManager getInstance() {
        if (instance == null) {
            instance = new ApiKeyManager();
        }
        return instance;
    }

    public void fetchApiKey(TaskCompletionCallback<String> callback) {
        if (isApiKeyFetched && apiKey != null) {
            // Si ya se obtuvo, devolvemos directamente la clave
            callback.onComplete(apiKey, null);
            return;
        }

        // Lógica para obtener la API key desde Firebase
        firebaseManager.getTmdbApiKey((result, error) -> {
            if (error != null) {
                callback.onComplete(null, error); // Pasar el error
            } else {
                apiKey = result; // Guardar la clave
                isApiKeyFetched = true;
                callback.onComplete(apiKey, null); // Devolver el resultado
            }
        });
    }

}
