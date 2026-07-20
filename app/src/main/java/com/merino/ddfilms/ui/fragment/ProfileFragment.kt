package com.merino.ddfilms.ui.fragment

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.chip.ChipGroup
import com.google.android.material.switchmaterial.SwitchMaterial
import com.merino.ddfilms.DDFilmsApplication
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.ui.MainActivity

class ProfileFragment : Fragment() {

    private var currentProfileImageUrl: String? = null
    private lateinit var preferences: SharedPreferences
    private var switchNotifications: SwitchMaterial? = null
    private lateinit var profileImage: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            saveNotificationsEnabled(isGranted)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bind Views
        profileImage = view.findViewById(R.id.profile_image)
        val editProfileIcon = view.findViewById<ImageView>(R.id.edit_profile_icon)
        profileName = view.findViewById(R.id.profile_name)
        profileEmail = view.findViewById(R.id.profile_email)
        val themeChipGroup = view.findViewById<ChipGroup>(R.id.theme_chip_group)
        switchNotifications = view.findViewById(R.id.switch_notifications)

        preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE)

        // Load User Details from Firebase
        val uid = FirebaseManager.getInstance().getCurrentUserUID()
        if (uid != null) {
            FirebaseManager.getInstance().getUserName(uid) { userName, _ ->
                if (userName != null && isAdded) {
                    profileName.text = userName
                }
            }

            FirebaseManager.getInstance().getUserMail(uid) { userEmail, _ ->
                if (userEmail != null && isAdded) {
                    profileEmail.text = userEmail
                }
            }

            FirebaseManager.getInstance().getUserProfileImageUrl(uid) { url, _ ->
                if (url != null && isAdded) {
                    currentProfileImageUrl = url
                    Glide.with(this)
                        .load(url)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(profileImage)
                } else if (isAdded) {
                    profileImage.setImageResource(R.drawable.ic_default_profile)
                }
            }
        }

        // Edit Profile Picture Click
        editProfileIcon.setOnClickListener {
            val dialog = ProfilePicturePickerDialog.newInstance(currentProfileImageUrl)
            dialog.setOnProfileImageUpdatedListener { newImagePath ->
                currentProfileImageUrl = newImagePath
                if (isAdded) {
                    Glide.with(this)
                        .load(newImagePath)
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(profileImage)
                }

                // Sync navigation header in MainActivity
                val currentActivity = activity
                if (currentActivity is MainActivity) {
                    currentActivity.refreshNavHeader()
                }
            }
            dialog.show(parentFragmentManager, "ProfilePicturePickerDialog")
        }

        // Initialize Theme Chip Selection
        val themeMode = preferences.getString("theme_mode", "system")
        when (themeMode) {
            "light" -> themeChipGroup.check(R.id.chip_theme_light)
            "dark" -> themeChipGroup.check(R.id.chip_theme_dark)
            else -> themeChipGroup.check(R.id.chip_theme_system)
        }

        // Set Theme Change Listener
        themeChipGroup.setOnCheckedChangeListener { _, checkedId ->
            var newThemeMode = "system"
            if (checkedId == R.id.chip_theme_light) {
                newThemeMode = "light"
            } else if (checkedId == R.id.chip_theme_dark) {
                newThemeMode = "dark"
            } else if (checkedId == R.id.chip_theme_system) {
                newThemeMode = "system"
            }

            val currentTheme = preferences.getString("theme_mode", "system")
            if (currentTheme != newThemeMode) {
                preferences.edit().putString("theme_mode", newThemeMode).apply()
                DDFilmsApplication.applyTheme(newThemeMode)
            }
        }

        // Initialize Notification Switch Selection
        val notificationsEnabled = preferences.getBoolean("notifications_enabled", false)
        switchNotifications?.isChecked = notificationsEnabled

        // Set Notification Switch Listener
        switchNotifications?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            requireContext(),
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        saveNotificationsEnabled(true)
                    }
                } else {
                    saveNotificationsEnabled(true)
                }
            } else {
                saveNotificationsEnabled(false)
            }
        }
    }

    private fun saveNotificationsEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean("notifications_enabled", enabled)
            .putBoolean("notifications_prompted", true)
            .apply()
        val currentSwitch = switchNotifications
        if (currentSwitch != null && currentSwitch.isChecked != enabled) {
            currentSwitch.isChecked = enabled
        }
    }
}
