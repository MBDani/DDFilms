package com.merino.ddfilms.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.ui.components.Fab.ActivityFabController
import com.merino.ddfilms.ui.components.Fab.FabHost
import com.merino.ddfilms.ui.components.Fab.ShowsFab
import com.merino.ddfilms.ui.fragment.DiaryFragment
import com.merino.ddfilms.ui.fragment.ListsFragment
import com.merino.ddfilms.ui.fragment.PopularFragment
import com.merino.ddfilms.ui.fragment.ProfileFragment
import com.merino.ddfilms.ui.fragment.ProfilePicturePickerDialog
import com.merino.ddfilms.ui.fragment.ReviewsFragment
import com.merino.ddfilms.ui.fragment.SearchFragment
import com.merino.ddfilms.ui.fragment.SettingsFragment
import com.merino.ddfilms.ui.fragment.WatchlistFragment
import com.merino.ddfilms.utils.EdgeToEdgeHelper
import java.util.Objects

class MainActivity : AppCompatActivity(), ActivityFabController {

    private lateinit var drawerLayout: DrawerLayout
    private val firebaseManager = FirebaseManager()
    private lateinit var activityFab: FloatingActionButton
    private var currentProfileImageUrl: String? = null

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

        drawerLayout = findViewById(R.id.drawer_layout)
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        toggle.drawerArrowDrawable.color = ContextCompat.getColor(this, R.color.primary_light)
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout) { _, insets ->
            ViewCompat.onApplyWindowInsets(findViewById(R.id.app_bar_layout), insets)
            ViewCompat.onApplyWindowInsets(navigationView, insets)
            ViewCompat.onApplyWindowInsets(findViewById(R.id.main_content_scroll), insets)
            insets
        }

        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(R.id.app_bar_layout), true, false)
        EdgeToEdgeHelper.applyWindowInsetsPending(navigationView, true, true)
        EdgeToEdgeHelper.applyWindowInsetsPending(findViewById(R.id.main_content_scroll), false, true)

        personalizedNavHeader(navigationView)

        navigationView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_popular -> loadFragment(PopularFragment())
                R.id.nav_search -> loadFragment(SearchFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
                R.id.nav_watchlist -> loadFragment(WatchlistFragment())
                R.id.nav_lists -> loadFragment(ListsFragment())
                R.id.nav_diary -> loadFragment(DiaryFragment())
                R.id.nav_reviews -> loadFragment(ReviewsFragment())
                R.id.nav_settings -> loadFragment(SettingsFragment())
                R.id.nav_sign_out -> showLogoutConfirmationDialog()
            }
            drawerLayout.closeDrawers()
            true
        }

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
            loadFragment(SearchFragment())
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

    fun refreshNavHeader() {
        val navigationView = findViewById<NavigationView>(R.id.navigation_view)
        if (navigationView != null) {
            personalizedNavHeader(navigationView)
        }
    }

    private fun personalizedNavHeader(navigationView: NavigationView) {
        val headerView = navigationView.getHeaderView(0)

        val profileImage = headerView.findViewById<ImageView>(R.id.profile_image)
        val profileName = headerView.findViewById<TextView>(R.id.profile_name)
        val profileMail = headerView.findViewById<TextView>(R.id.profile_email)

        val uid = firebaseManager.getCurrentUserUID() ?: return
        firebaseManager.getUserName(uid) { userName, error ->
            if (error != null) {
                Log.e("FirebaseManager", "Error: " + error.message)
                return@getUserName
            }
            profileName.text = userName
        }
        firebaseManager.getUserMail(uid) { userEmail, error ->
            if (error != null) {
                Log.e("FirebaseManager", "Error: " + error.message)
                return@getUserMail
            }
            profileMail.text = userEmail
        }

        firebaseManager.getUserProfileImageUrl(uid) { url, _ ->
            if (url != null) {
                currentProfileImageUrl = url
                Glide.with(this@MainActivity)
                    .load(url)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(profileImage)
            } else {
                profileImage.setImageResource(R.drawable.ic_default_profile)
            }
        }

        val editProfileIcon = headerView.findViewById<ImageView>(R.id.edit_profile_icon)
        editProfileIcon?.setOnClickListener {
            val dialog = ProfilePicturePickerDialog.newInstance(currentProfileImageUrl)
            dialog.setOnProfileImageUpdatedListener { newImagePath ->
                currentProfileImageUrl = newImagePath
                Glide.with(this@MainActivity)
                    .load(newImagePath)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(profileImage)
            }
            dialog.show(supportFragmentManager, "ProfilePicturePickerDialog")
        }
    }

    fun loadFragment(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.nav_host_fragment, fragment)
            .commit()

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

    private fun showLogoutConfirmationDialog() {
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val navView = findViewById<NavigationView>(R.id.navigation_view)
        if (drawerLayout.isDrawerOpen(navView)) {
            drawerLayout.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }

    fun setToolbarTitle(title: String) {
        supportActionBar?.title = title
    }
}
