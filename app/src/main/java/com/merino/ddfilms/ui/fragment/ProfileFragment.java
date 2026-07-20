package com.merino.ddfilms.ui.fragment;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.merino.ddfilms.DDFilmsApplication;
import com.merino.ddfilms.R;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.ui.MainActivity;

public class ProfileFragment extends Fragment {

    private String currentProfileImageUrl = null;
    private SharedPreferences preferences;
    private SwitchMaterial switchNotifications;
    private ImageView profileImage;
    private TextView profileName;
    private TextView profileEmail;

    private ActivityResultLauncher<String> requestPermissionLauncher;

    public ProfileFragment() {}

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        saveNotificationsEnabled(true);
                    } else {
                        saveNotificationsEnabled(false);
                    }
                }
        );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Bind Views
        profileImage = view.findViewById(R.id.profile_image);
        ImageView editProfileIcon = view.findViewById(R.id.edit_profile_icon);
        profileName = view.findViewById(R.id.profile_name);
        profileEmail = view.findViewById(R.id.profile_email);
        ChipGroup themeChipGroup = view.findViewById(R.id.theme_chip_group);
        switchNotifications = view.findViewById(R.id.switch_notifications);

        preferences = requireContext().getSharedPreferences("Preferences", Context.MODE_PRIVATE);

        // Load User Details from Firebase
        String uid = FirebaseManager.getInstance().getCurrentUserUID();
        if (uid != null) {
            FirebaseManager.getInstance().getUserName(uid, (userName, error) -> {
                if (userName != null && isAdded()) {
                    profileName.setText(userName);
                }
            });

            FirebaseManager.getInstance().getUserMail(uid, (userEmail, error) -> {
                if (userEmail != null && isAdded()) {
                    profileEmail.setText(userEmail);
                }
            });

            FirebaseManager.getInstance().getUserProfileImageUrl(uid, (url, error) -> {
                if (url != null && isAdded()) {
                    currentProfileImageUrl = url;
                    Glide.with(ProfileFragment.this)
                            .load(url)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .into(profileImage);
                } else if (isAdded()) {
                    profileImage.setImageResource(R.drawable.ic_default_profile);
                }
            });
        }

        // Edit Profile Picture Click
        editProfileIcon.setOnClickListener(v -> {
            ProfilePicturePickerDialog dialog = ProfilePicturePickerDialog.newInstance(currentProfileImageUrl);
            dialog.setOnProfileImageUpdatedListener(newImagePath -> {
                currentProfileImageUrl = newImagePath;
                if (isAdded()) {
                    Glide.with(ProfileFragment.this)
                            .load(newImagePath)
                            .placeholder(R.drawable.ic_default_profile)
                            .error(R.drawable.ic_default_profile)
                            .into(profileImage);
                }

                // Sync navigation header in MainActivity
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).refreshNavHeader();
                }
            });
            dialog.show(getParentFragmentManager(), "ProfilePicturePickerDialog");
        });

        // Initialize Theme Chip Selection
        String themeMode = preferences.getString("theme_mode", "system");
        if ("light".equals(themeMode)) {
            themeChipGroup.check(R.id.chip_theme_light);
        } else if ("dark".equals(themeMode)) {
            themeChipGroup.check(R.id.chip_theme_dark);
        } else {
            themeChipGroup.check(R.id.chip_theme_system);
        }

        // Set Theme Change Listener
        themeChipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String newThemeMode = "system";
            if (checkedId == R.id.chip_theme_light) {
                newThemeMode = "light";
            } else if (checkedId == R.id.chip_theme_dark) {
                newThemeMode = "dark";
            } else if (checkedId == R.id.chip_theme_system) {
                newThemeMode = "system";
            }

            String currentTheme = preferences.getString("theme_mode", "system");
            if (!currentTheme.equals(newThemeMode)) {
                preferences.edit().putString("theme_mode", newThemeMode).apply();
                DDFilmsApplication.applyTheme(newThemeMode);
            }
        });

        // Initialize Notification Switch Selection
        boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", false);
        switchNotifications.setChecked(notificationsEnabled);

        // Set Notification Switch Listener
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                    } else {
                        saveNotificationsEnabled(true);
                    }
                } else {
                    saveNotificationsEnabled(true);
                }
            } else {
                saveNotificationsEnabled(false);
            }
        });
    }

    private void saveNotificationsEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean("notifications_enabled", enabled)
                .putBoolean("notifications_prompted", true)
                .apply();
        if (switchNotifications != null && switchNotifications.isChecked() != enabled) {
            switchNotifications.setChecked(enabled);
        }
    }
}
