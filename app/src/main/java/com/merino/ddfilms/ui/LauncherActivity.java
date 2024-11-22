package com.merino.ddfilms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.merino.ddfilms.ui.auth.LoginActivity;

public class LauncherActivity extends AppCompatActivity {

    private SharedPreferences preferences;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAuth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);

        tryLogin();
    }

    private void tryLogin() {
        String email = preferences.getString("usuario_email", null);
        String pw = preferences.getString("usuario_contraseña", null);

        if (email == null && pw == null) {
            navigateToLoginActivity();
        }
        login(email, pw);
    }

    private void login(String email, String pw) {
        mAuth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                navigateToMainActivity();
            } else {
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
