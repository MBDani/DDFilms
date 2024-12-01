package com.merino.ddfilms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.configuration.ApiKeyManager;
import com.merino.ddfilms.ui.auth.LoginActivity;

public class LauncherActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private FirebaseAuth mAuth;

    private TMDBService tmdbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("LauncherActivity", "onCreate");
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);

        ApiKeyManager.getInstance().fetchApiKey((result, error) -> {
            if (error != null) {
                Log.e("MainActivity", "Error al obtener la API key: " + error.getMessage());
            } else {
                Log.d("MainActivity", "API Key obtenida: " + result);
                // Aquí puedes inicializar TMDBService si lo necesitas
                tmdbService = TMDBClient.getClient(result).create(TMDBService.class);
            }
        });

        tryLogin();
    }

    private void tryLogin() {
        String email = preferences.getString("usuario_email", null);
        String pw = preferences.getString("usuario_contraseña", null);

        if (email == null && pw == null) {
            Log.d("LauncherActivity", "No hay credenciales guardadas");
            navigateToLoginActivity();
        }
        login(email, pw);
    }

    private void login(String email, String pw) {
        mAuth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                navigateToMainActivity();
            } else {
                Log.d("LauncherActivity", "Error al iniciar sesión: " + task.getException());
                navigateToLoginActivity();
            }
        });
    }

    private void navigateToMainActivity() {
        Intent i = new Intent(getApplication(), MainActivity.class);
        startActivity(i);
        finish();
    }

    private void navigateToLoginActivity() {
        Intent i = new Intent(getApplication(), LoginActivity.class);
        startActivity(i);
        finish();
    }
}
