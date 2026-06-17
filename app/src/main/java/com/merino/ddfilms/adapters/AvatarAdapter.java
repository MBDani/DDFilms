package com.merino.ddfilms.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;

import java.util.List;

public class AvatarAdapter extends RecyclerView.Adapter<AvatarAdapter.AvatarViewHolder> {

    public interface OnAvatarClickListener {
        void onAvatarClick(String avatarPath);
    }

    private final List<String> avatarPaths;
    private final OnAvatarClickListener listener;
    private int selectedPosition = -1;
    private Context context;

    public AvatarAdapter(List<String> avatarPaths, String currentAvatarPath, OnAvatarClickListener listener) {
        this.avatarPaths = avatarPaths;
        this.listener = listener;

        if (currentAvatarPath != null) {
            for (int i = 0; i < avatarPaths.size(); i++) {
                String fullPath = "file:///android_asset/avatars/" + avatarPaths.get(i);
                if (avatarPaths.get(i).equals(currentAvatarPath) || 
                    fullPath.equals(currentAvatarPath) ||
                    currentAvatarPath.contains(avatarPaths.get(i))) {
                    selectedPosition = i;
                    break;
                }
            }
        }
    }

    public String getSelectedAvatarPath() {
        if (selectedPosition >= 0 && selectedPosition < avatarPaths.size()) {
            return "file:///android_asset/avatars/" + avatarPaths.get(selectedPosition);
        }
        return null;
    }

    @NonNull
    @Override
    public AvatarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_avatar_thumbnail, parent, false);
        return new AvatarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AvatarViewHolder holder, int position) {
        String fileName = avatarPaths.get(position);
        String fullPath = "file:///android_asset/avatars/" + fileName;

        // Load image using Glide from assets
        Glide.with(context)
                .load(fullPath)
                .placeholder(R.drawable.ic_default_profile)
                .error(R.drawable.ic_default_profile)
                .into(holder.avatarThumbnail);

        // Highlight selected state
        boolean isSelected = position == selectedPosition;
        holder.checkMark.setVisibility(isSelected ? View.VISIBLE : View.GONE);

        float density = context.getResources().getDisplayMetrics().density;
        if (isSelected) {
            holder.avatarThumbnail.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gold_dark)));
            holder.avatarThumbnail.setStrokeWidth(3.0f * density);
        } else {
            holder.avatarThumbnail.setStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.gray_divider)));
            holder.avatarThumbnail.setStrokeWidth(1.5f * density);
        }

        // Click selection logic
        holder.avatarContainer.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();

            if (previousSelected != selectedPosition) {
                notifyItemChanged(previousSelected);
                notifyItemChanged(selectedPosition);
            }

            if (listener != null) {
                listener.onAvatarClick(fullPath);
            }
        });
    }

    @Override
    public int getItemCount() {
        return avatarPaths.size();
    }

    static class AvatarViewHolder extends RecyclerView.ViewHolder {
        com.google.android.material.imageview.ShapeableImageView avatarThumbnail;
        ImageView checkMark;
        View avatarContainer;

        public AvatarViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarThumbnail = itemView.findViewById(R.id.avatar_thumbnail);
            checkMark = itemView.findViewById(R.id.check_mark);
            avatarContainer = itemView.findViewById(R.id.avatar_container);
        }
    }
}
