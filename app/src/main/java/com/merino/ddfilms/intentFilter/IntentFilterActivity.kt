package com.merino.ddfilms.intentFilter

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.ui.MovieListActivity
import com.merino.ddfilms.utils.TaskCompletionCallback
import com.merino.ddfilms.utils.Utils.showMessage

class IntentFilterActivity : AppCompatActivity() {

    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val data: Uri? = intent.data
        if (data != null) {
            val pathSegments = data.pathSegments
            val listID = if (pathSegments.isNotEmpty()) pathSegments[0] else null
            val listName = data.getQueryParameter("listName")

            if (listID != null && listName != null) {
                addListToUser(listID) { _, error ->
                    if (error != null) {
                        showMessage(applicationContext, error.message)
                    } else {
                        showMessage(applicationContext, getString(R.string.list_added_to_user))
                        navigateToMovieListActivity(listID, listName)
                    }
                }
            } else {
                Toast.makeText(this, R.string.incomplete_data, Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, R.string.invalid_link, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun addListToUser(listID: String, callback: TaskCompletionCallback<Boolean>) {
        firebaseManager.addListToUser(listID, callback)
    }

    private fun navigateToMovieListActivity(listID: String, listName: String) {
        val intent = Intent(this, MovieListActivity::class.java).apply {
            putExtra("listID", listID)
            putExtra("listName", listName)
        }
        startActivity(intent)
        finish()
    }
}
