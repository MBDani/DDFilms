package com.merino.ddfilms.ui.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.merino.ddfilms.R
import com.merino.ddfilms.utils.EdgeToEdgeHelper

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)
        mAuth = FirebaseAuth.getInstance()
        edtEmail = findViewById(R.id.editText_email2)

        EdgeToEdgeHelper.applyWindowInsets(
            findViewById(R.id.forgot_password_container),
            true, true, true, true
        )
    }

    fun ResetPassword(view: View) {
        val email = edtEmail.text.toString().trim()

        if (email.isNotEmpty()) {
            Toast.makeText(this, R.string.loading, Toast.LENGTH_SHORT).show()
            mAuth.setLanguageCode(getString(R.string.auth_language_code))
            mAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, R.string.password_reset_email_sent, Toast.LENGTH_SHORT).show()

                    // Redirect in 2 seconds to login activity non-blockingly
                    Handler(Looper.getMainLooper()).postDelayed({
                        val i = Intent(applicationContext, LoginActivity::class.java)
                        startActivity(i)
                        finish()
                    }, 2000)
                } else {
                    Toast.makeText(this, R.string.password_reset_failed, Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(this, R.string.fill_email, Toast.LENGTH_SHORT).show()
        }
    }
}
