package com.merino.ddfilms.ui.auth;


import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.merino.ddfilms.R;
import com.merino.ddfilms.model.User;
import com.merino.ddfilms.ui.MainActivity;
import com.merino.ddfilms.utils.EdgeToEdgeHelper;


public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firebaseFirestore;
    private SharedPreferences preferences;
    private EditText edt_email, edt_pw, edt_user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE);

        edt_user = findViewById(R.id.user_edt);
        edt_email = findViewById(R.id.email_edt);
        edt_email = findViewById(R.id.email_edt);
        edt_pw = findViewById(R.id.pw_edt);

        // Fix for Edge-to-Edge (Android 15+)
        EdgeToEdgeHelper.applyWindowInsets(findViewById(R.id.register_container), true, true, true, true);
    }

    public void btnNavigateToLoginActivity(View view) {
        Intent i = new Intent(getApplication(), LoginActivity.class);
        startActivity(i);
    }

    public void btnRegisterUser(View view) {
        // Obtén los valores de los campos de texto
        final String user = edt_user.getText().toString();
        final String email = edt_email.getText().toString();
        final String password = edt_pw.getText().toString();

        if (!areFieldsEmpties(email, user, password)) return;

        registro(user, email, password);
    }

    private boolean areFieldsEmpties(String email, String usuario, String password) {
        boolean isValid = true;
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(usuario)) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show();
            isValid = false;
        } else if (password.length() < 6) {
            Toast.makeText(this, R.string.password_length_error, Toast.LENGTH_LONG).show();
            isValid = false;
        }
        return isValid;
    }

    private void registro(String user, String email, String password) {
        try {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {

                if (task.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, getString(R.string.account_registered, email), Toast.LENGTH_SHORT).show();

                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser == null) {
                        Toast.makeText(RegisterActivity.this, R.string.error_getting_user, Toast.LENGTH_SHORT).show();
                    } else {
                        createUser(firebaseUser, user, email, password);
                    }

                } else {
                    if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(RegisterActivity.this, R.string.email_already_in_use, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(RegisterActivity.this, R.string.registration_failed, Toast.LENGTH_SHORT).show();

                    }
                }
            });
        } catch (Exception e) {
            Toast.makeText(RegisterActivity.this, R.string.db_connection_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private void createUser(FirebaseUser firebaseUser, String user, String email, String password) {
        User userModel = new User(user, email);

        try {
            firebaseFirestore.collection("users").document(firebaseUser.getUid()).set(userModel).addOnSuccessListener(aVoid -> {
                Toast.makeText(RegisterActivity.this, R.string.data_stored_cloud, Toast.LENGTH_SHORT).show();

                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("user_email", email);
                editor.putString("user_pw", password);
                editor.putString("user_name", user);
                editor.apply();

                navigateToMainActivity();

            }).addOnFailureListener(e -> Toast.makeText(RegisterActivity.this, R.string.error_saving_data, Toast.LENGTH_SHORT).show());
        } catch (Exception e) {
            Toast.makeText(RegisterActivity.this, R.string.db_connection_failed, Toast.LENGTH_SHORT).show();
        }
    }

    public void navigateToMainActivity() {
        Intent i = new Intent(getApplication(), MainActivity.class);
        startActivity(i);
        finish();
    }


}
