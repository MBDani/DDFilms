package com.merino.ddfilms.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.merino.ddfilms.R

class AvatarAdapter(
    private val avatarPaths: List<String>,
    currentAvatarPath: String?,
    private val listener: OnAvatarClickListener?
) : RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder>() {

    fun interface OnAvatarClickListener {
        fun onAvatarClick(avatarPath: String)
    }

    private var selectedPosition = -1
    private lateinit var context: Context

    init {
        if (currentAvatarPath != null) {
            for (i in avatarPaths.indices) {
                val fullPath = "file:///android_asset/avatars/${avatarPaths[i]}"
                if (avatarPaths[i] == currentAvatarPath ||
                    fullPath == currentAvatarPath ||
                    currentAvatarPath.contains(avatarPaths[i])
                ) {
                    selectedPosition = i
                    break
                }
            }
        }
    }

    val selectedAvatarPath: String?
        get() = if (selectedPosition in avatarPaths.indices) {
            "file:///android_asset/avatars/${avatarPaths[selectedPosition]}"
        } else null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AvatarViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context).inflate(R.layout.item_avatar_thumbnail, parent, false)
        return AvatarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AvatarViewHolder, position: Int) {
        val fileName = avatarPaths[position]
        val fullPath = "file:///android_asset/avatars/$fileName"

        // Load image using Glide from assets
        Glide.with(context)
            .load(fullPath)
            .placeholder(R.drawable.ic_default_profile)
            .error(R.drawable.ic_default_profile)
            .into(holder.avatarThumbnail)

        // Highlight selected state
        val isSelected = position == selectedPosition
        holder.checkMark.visibility = if (isSelected) View.VISIBLE else View.GONE

        val density = context.resources.displayMetrics.density
        if (isSelected) {
            holder.avatarThumbnail.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gold_dark))
            holder.avatarThumbnail.strokeWidth = 3.0f * density
        } else {
            holder.avatarThumbnail.strokeColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_divider))
            holder.avatarThumbnail.strokeWidth = 1.5f * density
        }

        // Click selection logic
        holder.avatarContainer.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.bindingAdapterPosition

            if (previousSelected != selectedPosition) {
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
            }

            listener?.onAvatarClick(fullPath)
        }
    }

    override fun getItemCount(): Int = avatarPaths.size

    class AvatarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatarThumbnail: ShapeableImageView = itemView.findViewById(R.id.avatar_thumbnail)
        val checkMark: ImageView = itemView.findViewById(R.id.check_mark)
        val avatarContainer: View = itemView.findViewById(R.id.avatar_container)
    }
}
