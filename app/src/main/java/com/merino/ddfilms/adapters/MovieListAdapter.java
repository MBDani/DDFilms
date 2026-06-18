package com.merino.ddfilms.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.model.MovieLists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieListAdapter extends RecyclerView.Adapter<MovieListAdapter.ViewHolder> {

    public interface OnListClickListener {
        void onListClick(MovieLists list);
    }

    private final Context context;
    private final List<MovieLists> movieLists;
    private final OnListClickListener onItemClick;
    private final Map<String, String> avatarCache = new HashMap<>();

    public MovieListAdapter(Context context, List<MovieLists> movieLists, OnListClickListener onItemClick) {
        this.context = context;
        this.movieLists = movieLists;
        this.onItemClick = onItemClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie_list_redesign, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MovieLists list = movieLists.get(position);
        holder.listTitle.setText(list.getName());

        int count = list.getMoviesCount();

        // Bind covers (last 3 movies)
        List<String> covers = list.getCoverPreviews();
        holder.cover1.setVisibility(View.GONE);
        holder.cover2.setVisibility(View.GONE);
        holder.cover3.setVisibility(View.GONE);
        holder.coverMore.setVisibility(View.GONE);

        if (covers != null && !covers.isEmpty()) {
            for (int i = 0; i < covers.size(); i++) {
                String path = covers.get(i);
                String fullPath = path.startsWith("http") || path.startsWith("file") ? path : "https://image.tmdb.org/t/p/w200" + path;
                ImageView target = null;
                if (i == 0) target = holder.cover1;
                else if (i == 1) target = holder.cover2;
                else if (i == 2) target = holder.cover3;

                if (target != null) {
                    target.setVisibility(View.VISIBLE);
                    Glide.with(context)
                            .load(fullPath)
                            .placeholder(R.drawable.placeholder_poster)
                            .error(R.drawable.placeholder_poster)
                            .into(target);
                }
            }

            if (count > 3) {
                holder.coverMore.setVisibility(View.VISIBLE);
                int extra = count - 3;
                holder.txtMoreCovers.setText("+" + Math.min(extra, 99));
            }
        } else {
            // Show default placeholder poster
            holder.cover1.setVisibility(View.VISIBLE);
            holder.cover1.setImageResource(R.drawable.ic_empty_list_placeholder);
        }

        // Bind member avatars preview using user IDs list, desnormalizados, and cache
        List<String> userIDs = list.getUserID();
        List<String> denormalizedAvatars = list.getMemberAvatarsPreview();
        holder.avatar1.setVisibility(View.GONE);
        holder.avatar2.setVisibility(View.GONE);
        holder.avatar3.setVisibility(View.GONE);
        holder.avatar4.setVisibility(View.GONE);
        holder.txtMoreMembers.setVisibility(View.GONE);

        if (userIDs != null && !userIDs.isEmpty()) {
            android.util.Log.d("MovieListAdapter", "List: " + list.getName() + " has userIDs: " + userIDs + ", denormalized: " + denormalizedAvatars);
            for (int i = 0; i < Math.min(userIDs.size(), 4); i++) {
                final String uid = userIDs.get(i);
                ImageView target = null;
                if (i == 0) target = holder.avatar1;
                else if (i == 1) target = holder.avatar2;
                else if (i == 2) target = holder.avatar3;
                else if (i == 3) target = holder.avatar4;

                if (target != null) {
                    target.setVisibility(View.VISIBLE);
                    Glide.with(context).clear(target);
                    target.setTag(R.id.member_avatar_1, uid);

                    // Check if we have it in the memory cache
                    if (avatarCache.containsKey(uid)) {
                        String cachedUrl = avatarCache.get(uid);
                        String resolvedUrl = resolveAvatarPath(cachedUrl);
                        android.util.Log.d("MovieListAdapter", "Cache hit for uid: " + uid + ", url: " + resolvedUrl);
                        if (resolvedUrl != null && !resolvedUrl.isEmpty()) {
                            Glide.with(context)
                                    .load(resolvedUrl)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .into(target);
                        } else {
                            target.setImageResource(R.drawable.ic_default_profile);
                        }
                    } else {
                        // Not in memory cache.
                        // Check if we have a denormalized avatar URL for this index
                        String initialUrl = null;
                        if (denormalizedAvatars != null && i < denormalizedAvatars.size()) {
                            initialUrl = denormalizedAvatars.get(i);
                        }

                        // Load initial/denormalized avatar instantly if present
                        if (initialUrl != null && !initialUrl.isEmpty()) {
                            String resolvedInitial = resolveAvatarPath(initialUrl);
                            android.util.Log.d("MovieListAdapter", "Loading denormalized avatar instantly for uid: " + uid + ", url: " + resolvedInitial);
                            avatarCache.put(uid, initialUrl); // Put in cache tentatively
                            Glide.with(context)
                                    .load(resolvedInitial)
                                    .placeholder(R.drawable.ic_default_profile)
                                    .error(R.drawable.ic_default_profile)
                                    .into(target);
                        } else {
                            target.setImageResource(R.drawable.ic_default_profile);
                        }

                        // Query Firestore in the background to get the latest live picture
                        final ImageView finalTarget = target;
                        final String finalInitialUrl = initialUrl;
                        FirebaseManager.getInstance().getUserProfileImageUrl(uid, (avatarUrl, error) -> {
                            if (error != null) {
                                android.util.Log.e("MovieListAdapter", "Error fetching avatar for uid: " + uid, error);
                            }
                            android.util.Log.d("MovieListAdapter", "Firestore response for uid: " + uid + ", url: " + avatarUrl);
                            if (error == null && avatarUrl != null) {
                                // If the live URL is different from what we had initially, or if we had nothing, update cache and load
                                if (!avatarUrl.equals(finalInitialUrl)) {
                                    avatarCache.put(uid, avatarUrl);
                                    String resolvedLive = resolveAvatarPath(avatarUrl);
                                    if (uid.equals(finalTarget.getTag(R.id.member_avatar_1))) {
                                        android.util.Log.d("MovieListAdapter", "Loading live updated imageUrl for uid: " + uid + ", url: " + resolvedLive);
                                        Glide.with(context)
                                                .load(resolvedLive)
                                                .placeholder(R.drawable.ic_default_profile)
                                                .error(R.drawable.ic_default_profile)
                                                .into(finalTarget);
                                    }
                                } else {
                                    // Live url is same as denormalized, just ensure cache is clean
                                    avatarCache.put(uid, avatarUrl);
                                }
                            } else {
                                // If query returned null or failed, and we had no denormalized image, cache empty string
                                if (finalInitialUrl == null) {
                                    avatarCache.put(uid, "");
                                }
                            }
                        });
                    }
                }
            }

            if (userIDs.size() > 4) {
                holder.txtMoreMembers.setVisibility(View.VISIBLE);
                int extraMembers = userIDs.size() - 4;
                holder.txtMoreMembers.setText("+" + extraMembers);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (onItemClick != null) {
                onItemClick.onListClick(list);
            }
        });
    }

    @Override
    public int getItemCount() {
        return movieLists.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView listTitle;
        ImageView cover1;
        ImageView cover2;
        ImageView cover3;
        FrameLayout coverMore;
        TextView txtMoreCovers;

        ImageView avatar1;
        ImageView avatar2;
        ImageView avatar3;
        ImageView avatar4;
        TextView txtMoreMembers;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            listTitle = itemView.findViewById(R.id.list_title);
            cover1 = itemView.findViewById(R.id.cover_image_1);
            cover2 = itemView.findViewById(R.id.cover_image_2);
            cover3 = itemView.findViewById(R.id.cover_image_3);
            coverMore = itemView.findViewById(R.id.cover_more_container);
            txtMoreCovers = itemView.findViewById(R.id.txt_more_covers);

            avatar1 = itemView.findViewById(R.id.member_avatar_1);
            avatar2 = itemView.findViewById(R.id.member_avatar_2);
            avatar3 = itemView.findViewById(R.id.member_avatar_3);
            avatar4 = itemView.findViewById(R.id.member_avatar_4);
            txtMoreMembers = itemView.findViewById(R.id.txt_more_members);
        }
    }

    private String resolveAvatarPath(String path) {
        if (path == null || path.isEmpty()) return null;
        if (path.startsWith("http") || path.startsWith("file")) {
            return path;
        }
        String filename = path;
        if (!filename.contains("/")) {
            if (!filename.endsWith(".png")) {
                filename = filename + ".png";
            }
            return "file:///android_asset/avatars/" + filename;
        }
        if (filename.startsWith("avatars/")) {
            return "file:///android_asset/" + filename;
        }
        return path;
    }

    public void updateData(List<MovieLists> newMovieLists) {
        this.movieLists.clear();
        this.movieLists.addAll(newMovieLists);
        notifyDataSetChanged();
    }
}
