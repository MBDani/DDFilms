package com.merino.ddfilms.ui;

import static com.merino.ddfilms.utils.Utils.showMessage;

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
import com.merino.ddfilms.utils.TaskCompletionCallback;

public class LauncherActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private FirebaseAuth mAuth;

    private int numReintentos = 0;

    private TMDBService tmdbService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("LauncherActivity", "onCreate");
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);

        getTMDBApiKey((result, error) -> {
            if (result)
                tryLogin();
        });
    }

    private void getTMDBApiKey(TaskCompletionCallback<Boolean> callback) {
        ApiKeyManager.getInstance().fetchApiKey((result, error) -> {
            if (error != null) {
                showMessage(this, "Error al obtener la API key: " + error.getMessage());
                numReintentos++;
                if (numReintentos < 3) getTMDBApiKey(callback);
            } else {
                Log.d("LauncherActivity", "API Key obtenida: " + result);
                tmdbService = TMDBClient.getClient(result).create(TMDBService.class);
                callback.onComplete(true, null);
            }
        });
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
