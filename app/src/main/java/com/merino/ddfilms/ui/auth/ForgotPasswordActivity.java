package com.merino.ddfilms.ui.auth;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.merino.ddfilms.R;
import com.merino.ddfilms.utils.EdgeToEdgeHelper;

public class ForgotPasswordActivity extends AppCompatActivity {


    private EditText edt_email;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        mAuth = FirebaseAuth.getInstance();
        edt_email = findViewById(R.id.editText_email2);

        // Fix for Edge-to-Edge (Android 15+)
        EdgeToEdgeHelper.applyWindowInsets(findViewById(R.id.forgot_password_container), true, true, true, true);
    }

    public void ResetPassword(View view) {
        String email = edt_email.getText().toString();

        if (!email.isEmpty()) {
            Toast.makeText(ForgotPasswordActivity.this, R.string.loading, Toast.LENGTH_SHORT).show();
            mAuth.setLanguageCode(getString(R.string.auth_language_code));
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, R.string.password_reset_email_sent, Toast.LENGTH_SHORT).show();

                    // Redirigmos en 2 segundos a la pantalla de login
                    try {
                        Thread.sleep(2000);
                        Intent i = new Intent(getApplication(), LoginActivity.class);
                        startActivity(i);
                        finish();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }


                } else {
                    Toast.makeText(ForgotPasswordActivity.this, R.string.password_reset_failed, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(ForgotPasswordActivity.this, R.string.fill_email, Toast.LENGTH_SHORT).show();
        }


    }


}
