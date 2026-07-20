package com.merino.ddfilms.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.merino.ddfilms.R
import com.merino.ddfilms.ui.MainActivity
import com.merino.ddfilms.utils.EdgeToEdgeHelper

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var preferences: SharedPreferences
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE)

        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        val buttonLogin = findViewById<Button>(R.id.button_login)

        buttonLogin.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString()
            loginWithEmailAndPassword(email, password)
        }

        EdgeToEdgeHelper.applyWindowInsets(
            findViewById(R.id.login_container),
            true, true, true, true
        )
    }

    private fun loginWithEmailAndPassword(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    setSharedPreferences()
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@LoginActivity, getString(R.string.login_failed), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun setSharedPreferences() {
        val editor = preferences.edit()
        editor.putString("usuario_email", emailEditText.text.toString().trim())
        editor.putString("usuario_contraseña", passwordEditText.text.toString())
        editor.apply()
    }

    fun navigateToRegisterActivity(view: View) {
        val i = Intent(applicationContext, RegisterActivity::class.java)
        startActivity(i)
    }

    fun navigateToForgotPasswordActivity(view: View) {
        val i = Intent(applicationContext, ForgotPasswordActivity::class.java)
        startActivity(i)
    }
}
