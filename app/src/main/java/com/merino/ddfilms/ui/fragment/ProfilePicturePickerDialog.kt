package com.merino.ddfilms.ui.fragment

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.AvatarAdapter
import com.merino.ddfilms.api.FirebaseManager
import java.io.IOException
import java.util.ArrayList
import java.util.Arrays

class ProfilePicturePickerDialog : DialogFragment() {

    fun interface OnProfileImageUpdatedListener {
        fun onProfileImageUpdated(newImagePath: String)
    }

    private var listener: OnProfileImageUpdatedListener? = null
    private lateinit var adapter: AvatarAdapter
    private lateinit var avatarPreview: ImageView
    private lateinit var loadingOverlay: View
    private var selectedAvatarPath: String? = null
    private var currentAvatarPath: String? = null

    companion object {
        @JvmStatic
        fun newInstance(currentAvatarPath: String?): ProfilePicturePickerDialog {
            val fragment = ProfilePicturePickerDialog()
            val args = Bundle().apply {
                putString("current_avatar_path", currentAvatarPath)
            }
            fragment.arguments = args
            return fragment
        }
    }

    fun setOnProfileImageUpdatedListener(listener: OnProfileImageUpdatedListener?) {
        this.listener = listener
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val args = arguments
        if (args != null) {
            currentAvatarPath = args.getString("current_avatar_path")
            selectedAvatarPath = currentAvatarPath
        }
        setStyle(STYLE_NO_TITLE, 0)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_profile_picture_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        avatarPreview = view.findViewById(R.id.avatar_preview)
        loadingOverlay = view.findViewById(R.id.loading_overlay)
        val recyclerView = view.findViewById<RecyclerView>(R.id.avatars_recycler_view)
        val closeDialog = view.findViewById<ImageView>(R.id.close_dialog)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btn_cancel)
        val btnConfirm = view.findViewById<MaterialButton>(R.id.btn_confirm)

        btnCancel.contentDescription = getString(R.string.cancel_profile_pic_desc)
        btnConfirm.contentDescription = getString(R.string.confirm_profile_pic_desc)

        val currentPath = currentAvatarPath
        if (!currentPath.isNullOrEmpty()) {
            Glide.with(this)
                .load(currentPath)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(avatarPreview)
        } else {
            avatarPreview.setImageResource(R.drawable.ic_default_profile)
        }

        val avatarFiles = ArrayList<String>()
        try {
            val files = requireContext().assets.list("avatars")
            if (files != null) {
                avatarFiles.addAll(Arrays.asList(*files))
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(context, R.string.error_loading_avatars, Toast.LENGTH_SHORT).show()
        }

        recyclerView.layoutManager = GridLayoutManager(context, 3)
        adapter = AvatarAdapter(avatarFiles, currentAvatarPath) { path ->
            selectedAvatarPath = path
            Glide.with(this)
                .load(path)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(avatarPreview)
        }
        recyclerView.adapter = adapter

        closeDialog.setOnClickListener { dismiss() }
        btnCancel.setOnClickListener { dismiss() }

        btnConfirm.setOnClickListener {
            val finalPath = adapter.selectedAvatarPath
            if (finalPath == null) {
                Toast.makeText(context, R.string.please_select_avatar, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadingOverlay.visibility = View.VISIBLE
            val fm = FirebaseManager.getInstance()
            val userId = fm.getCurrentUserUID()

            fm.updateUserProfileImage(userId, finalPath) { success, error ->
                if (isAdded) {
                    loadingOverlay.visibility = View.GONE
                    if (error != null) {
                        Snackbar.make(view, getString(R.string.error_prefix, error.message), Snackbar.LENGTH_LONG).show()
                    } else if (success == true) {
                        Toast.makeText(context, R.string.profile_picture_updated, Toast.LENGTH_SHORT).show()
                        listener?.onProfileImageUpdated(finalPath)
                        dismiss()
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null && dialog.window != null) {
            val window = dialog.window!!
            window.setWindowAnimations(R.style.DialogScaleFadeAnimation)
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }
    }
}
