package com.merino.ddfilms.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.merino.ddfilms.R
import com.merino.ddfilms.api.TMDBClient
import com.merino.ddfilms.api.TMDBService
import com.merino.ddfilms.configuration.ApiKeyManager
import com.merino.ddfilms.ui.auth.LoginActivity
import com.merino.ddfilms.utils.TaskCompletionCallback
import com.merino.ddfilms.utils.Utils.showMessage

class LauncherActivity : AppCompatActivity() {

    private lateinit var preferences: SharedPreferences
    private lateinit var mAuth: FirebaseAuth
    private var numReintentos = 0
    private var tmdbService: TMDBService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("LauncherActivity", "onCreate")
        super.onCreate(savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        preferences = getSharedPreferences("Preferences", MODE_PRIVATE)

        getTMDBApiKey { result, _ ->
            if (result == true) {
                tryLogin()
            }
        }
    }

    private fun getTMDBApiKey(callback: TaskCompletionCallback<Boolean>) {
        ApiKeyManager.getInstance().fetchApiKey { result, error ->
            if (error != null) {
                showMessage(this, getString(R.string.error_getting_api_key, error.message))
                numReintentos++
                if (numReintentos < 3) {
                    getTMDBApiKey(callback)
                }
            } else {
                Log.d("LauncherActivity", "API Key obtenida: $result")
                tmdbService = TMDBClient.getClient(result!!).create(TMDBService::class.java)
                callback.onComplete(true, null)
            }
        }
    }

    private fun tryLogin() {
        val email = preferences.getString("usuario_email", null)
        val pw = preferences.getString("usuario_contraseña", null)

        if (email == null && pw == null) {
            Log.d("LauncherActivity", "No hay credenciales guardadas")
            navigateToLoginActivity()
        } else {
            login(email ?: "", pw ?: "")
        }
    }

    private fun login(email: String, pw: String) {
        mAuth.signInWithEmailAndPassword(email, pw).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                navigateToMainActivity()
            } else {
                Log.d("LauncherActivity", "Error al iniciar sesión: " + task.exception)
                navigateToLoginActivity()
            }
        }
    }

    private fun navigateToMainActivity() {
        val i = Intent(applicationContext, MainActivity::class.java)
        startActivity(i)
        finish()
    }

    private fun navigateToLoginActivity() {
        val i = Intent(applicationContext, LoginActivity::class.java)
        startActivity(i)
        finish()
    }
}
