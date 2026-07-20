package com.merino.ddfilms.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.merino.ddfilms.R
import com.merino.ddfilms.ui.fragment.SearchFragment
import com.merino.ddfilms.utils.EdgeToEdgeHelper

class SearchActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        toolbar = findViewById(R.id.toolbar)
        setupToolbar()

        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(R.id.appbar_layout), true, false)
        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(R.id.fragment_container), false, true)

        if (savedInstanceState == null) {
            val bundle = intent.extras
            val searchFragment = SearchFragment().apply {
                arguments = bundle
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, searchFragment)
                .commit()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val listName = intent.getStringExtra("listName") ?: ""
        supportActionBar?.title = "Añadiendo películas a $listName"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}
