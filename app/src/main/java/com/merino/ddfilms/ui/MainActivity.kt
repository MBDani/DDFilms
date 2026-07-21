package com.merino.ddfilms.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.ui.components.Fab.ActivityFabController
import com.merino.ddfilms.ui.components.Fab.FabHost
import com.merino.ddfilms.ui.components.Fab.ShowsFab
import com.merino.ddfilms.ui.fragment.ListsFragment
import com.merino.ddfilms.ui.fragment.PopularFragment
import com.merino.ddfilms.ui.fragment.ProfileFragment
import com.merino.ddfilms.ui.fragment.SearchFragment
import com.merino.ddfilms.utils.EdgeToEdgeHelper

class MainActivity : AppCompatActivity(), ActivityFabController {

    private lateinit var activityFab: FloatingActionButton
    private lateinit var bottomNavigation: BottomNavigationView

    private val requestPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                Log.d("MainActivity", "Notification permission granted")
            } else {
                Log.d("MainActivity", "Notification permission denied")
                getSharedPreferences("Preferences", MODE_PRIVATE).edit()
                    .putBoolean("notifications_enabled", false)
                    .apply()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = getString(R.string.app_name)

        bottomNavigation = findViewById(R.id.bottom_navigation)
        bottomNavigation.setOnItemSelectedListener { item ->
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            when (item.itemId) {
                R.id.nav_popular -> loadFragment(PopularFragment())
                R.id.nav_search -> loadFragment(SearchFragment())
                R.id.nav_lists -> loadFragment(ListsFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        val rootLayout = findViewById<View>(R.id.main_content_root)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            ViewCompat.onApplyWindowInsets(findViewById(R.id.app_bar_layout), insets)
            ViewCompat.onApplyWindowInsets(bottomNavigation, insets)
            insets
        }

        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(R.id.app_bar_layout), true, false)
        EdgeToEdgeHelper.applyWindowInsetsPending(bottomNavigation, false, true)

        activityFab = findViewById(R.id.activity_fab_add)
        activityFab.setOnClickListener {
            val current = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            if (current is FabHost) {
                current.onFabClicked()
            }
        }

        supportFragmentManager.registerFragmentLifecycleCallbacks(
            object : FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    super.onFragmentResumed(fm, f)
                    updateFabVisibility(f)
                }
            }, false
        )

        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_popular
            loadFragment(PopularFragment())
        }

        val prefs = getSharedPreferences("Preferences", MODE_PRIVATE)
        if (!prefs.contains("notifications_prompted")) {
            showNotificationOptInDialog()
        }
    }

    private fun showNotificationOptInDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.notifications_dialog_title)
            .setMessage(R.string.notifications_dialog_message)
            .setCancelable(false)
            .setPositiveButton(R.string.notifications_dialog_allow) { _, _ ->
                getSharedPreferences("Preferences", MODE_PRIVATE).edit()
                    .putBoolean("notifications_enabled", true)
                    .putBoolean("notifications_prompted", true)
                    .apply()

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
            .setNegativeButton(R.string.notifications_dialog_deny) { _, _ ->
                getSharedPreferences("Preferences", MODE_PRIVATE).edit()
                    .putBoolean("notifications_enabled", false)
                    .putBoolean("notifications_prompted", true)
                    .apply()
            }
            .show()
    }

    fun loadFragment(fragment: Fragment, addToBackStack: Boolean = false) {
        val transaction = supportFragmentManager.beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
        if (addToBackStack) {
            transaction.addToBackStack(null)
        }
        transaction.commit()

        supportFragmentManager.executePendingTransactions()

        val current = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (current != null) updateFabVisibility(current)
    }

    private fun updateFabVisibility(fragment: Fragment) {
        if (fragment is ShowsFab) {
            showFab()
        } else {
            hideFab()
        }
    }

    override fun showFab() {
        if (activityFab.visibility != View.VISIBLE) {
            activityFab.show()
            activityFab.visibility = View.VISIBLE
        }
    }

    override fun hideFab() {
        if (activityFab.visibility == View.VISIBLE) {
            activityFab.hide()
            activityFab.visibility = View.GONE
        }
    }

    fun setFabVisibility(visible: Boolean) {
        if (visible) showFab() else hideFab()
    }

    fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.logout_dialog_title)
            .setMessage(R.string.logout_dialog_message)
            .setPositiveButton(R.string.dialog_yes) { _, _ ->
                FirebaseManager.getInstance().logoutUser(this)
            }
            .setNegativeButton(R.string.dialog_no) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }
}
