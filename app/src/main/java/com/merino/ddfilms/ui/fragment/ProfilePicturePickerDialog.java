package com.merino.ddfilms.ui.fragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.snackbar.Snackbar;
import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.AvatarAdapter;
import com.merino.ddfilms.api.FirebaseManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ProfilePicturePickerDialog extends DialogFragment {

    public interface OnProfileImageUpdatedListener {
        void onProfileImageUpdated(String newImagePath);
    }

    private OnProfileImageUpdatedListener listener;
    private AvatarAdapter adapter;
    private ImageView avatarPreview;
    private View loadingOverlay;
    private String selectedAvatarPath;
    private String currentAvatarPath;

    public static ProfilePicturePickerDialog newInstance(String currentAvatarPath) {
        ProfilePicturePickerDialog fragment = new ProfilePicturePickerDialog();
        Bundle args = new Bundle();
        args.putString("current_avatar_path", currentAvatarPath);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnProfileImageUpdatedListener(OnProfileImageUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            currentAvatarPath = getArguments().getString("current_avatar_path");
            selectedAvatarPath = currentAvatarPath;
        }
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_profile_picture_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        avatarPreview = view.findViewById(R.id.avatar_preview);
        loadingOverlay = view.findViewById(R.id.loading_overlay);
        RecyclerView recyclerView = view.findViewById(R.id.avatars_recycler_view);
        ImageView closeDialog = view.findViewById(R.id.close_dialog);
        MaterialButton btnCancel = view.findViewById(R.id.btn_cancel);
        MaterialButton btnConfirm = view.findViewById(R.id.btn_confirm);

        // Set content descriptions for screen readers
        btnCancel.setContentDescription(getString(R.string.cancel_profile_pic_desc));
        btnConfirm.setContentDescription(getString(R.string.confirm_profile_pic_desc));

        if (currentAvatarPath != null && !currentAvatarPath.isEmpty()) {
            Glide.with(this)
                    .load(currentAvatarPath)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(avatarPreview);
        } else {
            avatarPreview.setImageResource(R.drawable.ic_default_profile);
        }

        List<String> avatarFiles = new ArrayList<>();
        try {
            String[] files = requireContext().getAssets().list("avatars");
            if (files != null) {
                avatarFiles.addAll(Arrays.asList(files));
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), R.string.error_loading_avatars, Toast.LENGTH_SHORT).show();
        }

        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        adapter = new AvatarAdapter(avatarFiles, currentAvatarPath, path -> {
            selectedAvatarPath = path;
            Glide.with(this)
                    .load(path)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(avatarPreview);
        });
        recyclerView.setAdapter(adapter);

        closeDialog.setOnClickListener(v -> dismiss());
        btnCancel.setOnClickListener(v -> dismiss());

        btnConfirm.setOnClickListener(v -> {
            String finalPath = adapter.getSelectedAvatarPath();
            if (finalPath == null) {
                Toast.makeText(getContext(), R.string.please_select_avatar, Toast.LENGTH_SHORT).show();
                return;
            }

            loadingOverlay.setVisibility(View.VISIBLE);
            FirebaseManager fm = FirebaseManager.getInstance();
            String userId = fm.getCurrentUserUID();

            fm.updateUserProfileImage(userId, finalPath, (success, error) -> {
                if (isAdded()) {
                    loadingOverlay.setVisibility(View.GONE);
                    if (error != null) {
                        Snackbar.make(view, getString(R.string.error_prefix, error.getMessage()), Snackbar.LENGTH_LONG).show();
                    } else if (success != null && success) {
                        Toast.makeText(getContext(), R.string.profile_picture_updated, Toast.LENGTH_SHORT).show();
                        if (listener != null) {
                            listener.onProfileImageUpdated(finalPath);
                        }
                        dismiss();
                    }
                }
            });
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            Window window = dialog.getWindow();
            window.setWindowAnimations(R.style.DialogScaleFadeAnimation);
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }
}
