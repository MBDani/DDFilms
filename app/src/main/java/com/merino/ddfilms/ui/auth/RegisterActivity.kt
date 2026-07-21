package com.merino.ddfilms.ui.auth

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.merino.ddfilms.R
import com.merino.ddfilms.model.User
import com.merino.ddfilms.ui.MainActivity
import com.merino.ddfilms.utils.EdgeToEdgeHelper

class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseFirestore: FirebaseFirestore
    private lateinit var preferences: SharedPreferences
    private lateinit var edtEmail: EditText
    private lateinit var edtPw: EditText
    private lateinit var edtUser: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE)

        edtUser = findViewById(R.id.user_edt)
        edtEmail = findViewById(R.id.email_edt)
        edtPw = findViewById(R.id.pw_edt)

        EdgeToEdgeHelper.applyWindowInsets(
            findViewById(R.id.register_container),
            true, true, true, true
        )
    }

    fun btnNavigateToLoginActivity(view: View) {
        finish()
    }

    fun btnRegisterUser(view: View) {
        val user = edtUser.text.toString().trim()
        val email = edtEmail.text.toString().trim()
        val password = edtPw.text.toString()

        if (!areFieldsEmpties(email, user, password)) return

        registro(user, email, password)
    }

    private fun areFieldsEmpties(email: String, usuario: String, password: String): Boolean {
        var isValid = true
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(usuario)) {
            Toast.makeText(this, R.string.fill_all_fields, Toast.LENGTH_SHORT).show()
            isValid = false
        } else if (password.length < 6) {
            Toast.makeText(this, R.string.password_length_error, Toast.LENGTH_LONG).show()
            isValid = false
        }
        return isValid
    }

    private fun registro(user: String, email: String, password: String) {
        try {
            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.account_registered, email), Toast.LENGTH_SHORT).show()

                    val firebaseUser = mAuth.currentUser
                    if (firebaseUser == null) {
                        Toast.makeText(this, R.string.error_getting_user, Toast.LENGTH_SHORT).show()
                    } else {
                        createUser(firebaseUser, user, email, password)
                    }
                } else {
                    if (task.exception is FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, R.string.email_already_in_use, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, R.string.registration_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.db_connection_failed, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createUser(firebaseUser: FirebaseUser, user: String, email: String, password: String) {
        val userModel = User(user, email)

        try {
            firebaseFirestore.collection("users").document(firebaseUser.uid).set(userModel)
                .addOnSuccessListener {
                    Toast.makeText(this, R.string.data_stored_cloud, Toast.LENGTH_SHORT).show()

                    val editor = preferences.edit()
                    editor.putString("user_email", email)
                    editor.putString("user_pw", password)
                    editor.putString("user_name", user)
                    editor.apply()

                    navigateToMainActivity()
                }
                .addOnFailureListener {
                    Toast.makeText(this, R.string.error_saving_data, Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            Toast.makeText(this, R.string.db_connection_failed, Toast.LENGTH_SHORT).show()
        }
    }

    fun navigateToMainActivity() {
        val i = Intent(applicationContext, MainActivity::class.java)
        startActivity(i)
        finish()
    }
}
