package com.merino.ddfilms.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.utils.EdgeToEdgeHelper
import com.merino.ddfilms.utils.Utils.showMessage
import java.util.ArrayList
import java.util.HashMap

class EditListActivity : AppCompatActivity() {

    private lateinit var listNameInput: EditText
    private lateinit var usersCountText: TextView
    private var listID: String? = null
    private var currentListName: String? = null
    private var mapUsers: HashMap<String, String>? = null
    private val firebaseManager = FirebaseManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_list)

        listNameInput = findViewById(R.id.list_name_input)
        usersCountText = findViewById(R.id.users_count_text)

        listID = intent.getStringExtra("listID")
        currentListName = intent.getStringExtra("listName")

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editar lista"

        listNameInput.setText(currentListName)
        loadListUsers()

        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(R.id.appbar_layout), true, false)
        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(R.id.edit_list_root), false, true)
    }

    private fun loadListUsers() {
        firebaseManager.getListUsersIDsAndNames(listID!!) { mapUsersResult, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (mapUsersResult != null) {
                mapUsers = mapUsersResult
                val userNames = ArrayList(mapUsersResult.values)
                showUserInList(userNames)
            }
        }
    }

    private fun showUserInList(users: List<String>) {
        val usersText = StringBuilder()
        for (user in users) {
            usersText.append(user).append("\n")
        }
        usersCountText.text = usersText.toString()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            handleBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun handleBackPressed() {
        val newName = listNameInput.text.toString().trim()
        if (newName != currentListName) {
            saveListName(newName)
        } else {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        handleBackPressed()
    }

    private fun saveListName(newName: String) {
        firebaseManager.updateListName(listID!!, newName) { result, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (result == true) {
                showMessage(applicationContext, getString(R.string.list_name_updated_success))
                finish()
            }
        }
    }
}
