package com.merino.ddfilms.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.merino.ddfilms.R;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private SharedPreferences preferences;
    private EditText emailEditText, passwordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);

        emailEditText = findViewById(R.id.email_edit_text);
        passwordEditText = findViewById(R.id.password_edit_text);
        Button buttonLogin = findViewById(R.id.button_login);

        buttonLogin.setOnClickListener(v -> {
            String email = emailEditText.getText().toString();
            String password = passwordEditText.getText().toString();
            loginWithEmailAndPassword(email, password);
        });
    }

    private void loginWithEmailAndPassword(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        setSharedPreferences();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        // Login failed, show an error message
                        Toast.makeText(LoginActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setSharedPreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("usuario_email", emailEditText.getText().toString());
        editor.putString("usuario_contraseña", passwordEditText.getText().toString());
        editor.apply();
    }

    public void navigateToRegisterActivity(View view){
        Intent i = new Intent(getApplication(), RegisterActivity.class);
        startActivity(i);
    }

    public void navigateToForgotPasswordActivity(View view){
        Intent i = new Intent(getApplication(), ForgotPasswordActivity.class);
        startActivity(i);
    }
}
