package com.merino.ddfilms.ui

import android.os.Bundle
import android.view.View
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

        setExitSharedElementCallback(object : androidx.core.app.SharedElementCallback() {
            override fun onSharedElementEnd(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                sharedElements?.forEach { view ->
                    view.visibility = View.VISIBLE
                    view.alpha = 1f
                }
            }
        })

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
        supportActionBar?.title = getString(R.string.adding_movies_to_list, listName)
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    }
}
