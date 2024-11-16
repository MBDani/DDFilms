package com.merino.ddfilms.ui;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.merino.ddfilms.R;

public class ForgotPasswordActivity extends AppCompatActivity {


    private EditText edt_email;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);
        mAuth = FirebaseAuth.getInstance();
        edt_email = findViewById(R.id.editText_email2);
    }

    public void ResetPassword(View view) {
        String email = edt_email.getText().toString();

        if (!email.isEmpty()) {
            Toast.makeText(ForgotPasswordActivity.this, "Cargando...", Toast.LENGTH_SHORT).show();
            mAuth.setLanguageCode("es");
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(ForgotPasswordActivity.this, "Se ha enviado un correo para restablecer tu contraseña. Redirigiendo a la pantalla de Login.", Toast.LENGTH_SHORT).show();

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
                    Toast.makeText(ForgotPasswordActivity.this, "No se puedo enviar el correo de restablecer contraseña", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(ForgotPasswordActivity.this, "Rellena el email", Toast.LENGTH_SHORT).show();
        }


    }


}
