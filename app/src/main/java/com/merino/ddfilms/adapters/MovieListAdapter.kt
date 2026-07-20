package com.merino.ddfilms.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.MovieLists
import java.util.HashMap

class MovieListAdapter(
    private val context: Context,
    private val movieLists: MutableList<MovieLists>,
    private val onItemClick: OnListClickListener?
) : RecyclerView.Adapter<MovieListAdapter.ViewHolder>() {

    fun interface OnListClickListener {
        fun onListClick(list: MovieLists)
    }

    private val avatarCache: MutableMap<String, String> = HashMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_movie_list_redesign, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val list = movieLists[position]
        holder.listTitle.text = list.name

        val count = list.moviesCount

        // Bind covers (last 3 movies)
        val covers = list.coverPreviews
        holder.cover1.visibility = View.GONE
        holder.cover2.visibility = View.GONE
        holder.cover3.visibility = View.GONE
        holder.coverMore.visibility = View.GONE

        if (!covers.isNullOrEmpty()) {
            for (i in covers.indices) {
                val path = covers[i]
                val fullPath = if (path.startsWith("http") || path.startsWith("file")) path else "https://image.tmdb.org/t/p/w200$path"
                var target: ImageView? = null
                when (i) {
                    0 -> target = holder.cover1
                    1 -> target = holder.cover2
                    2 -> target = holder.cover3
                }

                if (target != null) {
                    target.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(fullPath)
                        .placeholder(R.drawable.placeholder_poster)
                        .error(R.drawable.placeholder_poster)
                        .into(target)
                }
            }

            if (count > 3) {
                holder.coverMore.visibility = View.VISIBLE
                val extra = count - 3
                holder.txtMoreCovers.text = "+${Math.min(extra, 99)}"
            }
        } else {
            // Show default placeholder poster
            holder.cover1.visibility = View.VISIBLE
            holder.cover1.setImageResource(R.drawable.ic_empty_list_placeholder)
        }

        // Bind member avatars preview using user IDs list, desnormalizados, and cache
        val userIDs = list.userID
        val denormalizedAvatars = list.memberAvatarsPreview
        holder.avatar1.visibility = View.GONE
        holder.avatar2.visibility = View.GONE
        holder.avatar3.visibility = View.GONE
        holder.avatar4.visibility = View.GONE
        holder.txtMoreMembers.visibility = View.GONE

        if (!userIDs.isNullOrEmpty()) {
            Log.d("MovieListAdapter", "List: ${list.name} has userIDs: $userIDs, denormalized: $denormalizedAvatars")
            for (i in 0 until Math.min(userIDs.size, 4)) {
                val uid = userIDs[i]
                var target: ImageView? = null
                when (i) {
                    0 -> target = holder.avatar1
                    1 -> target = holder.avatar2
                    2 -> target = holder.avatar3
                    3 -> target = holder.avatar4
                }

                if (target != null) {
                    target.visibility = View.VISIBLE
                    Glide.with(context).clear(target)
                    target.setTag(R.id.member_avatar_1, uid)

                    // Check if we have it in the memory cache
                    if (avatarCache.containsKey(uid)) {
                        val cachedUrl = avatarCache[uid]
                        val resolvedUrl = resolveAvatarPath(cachedUrl)
                        Log.d("MovieListAdapter", "Cache hit for uid: $uid, url: $resolvedUrl")
                        if (!resolvedUrl.isNullOrEmpty()) {
                            Glide.with(context)
                                .load(resolvedUrl)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(target)
                        } else {
                            target.setImageResource(R.drawable.ic_default_profile)
                        }
                    } else {
                        // Not in memory cache.
                        // Check if we have a denormalized avatar URL for this index
                        var initialUrl: String? = null
                        if (denormalizedAvatars != null && i < denormalizedAvatars.size) {
                            initialUrl = denormalizedAvatars[i]
                        }

                        // Load initial/denormalized avatar instantly if present
                        if (!initialUrl.isNullOrEmpty()) {
                            val resolvedInitial = resolveAvatarPath(initialUrl)
                            Log.d("MovieListAdapter", "Loading denormalized avatar instantly for uid: $uid, url: $resolvedInitial")
                            avatarCache[uid] = initialUrl // Put in cache tentatively
                            Glide.with(context)
                                .load(resolvedInitial)
                                .placeholder(R.drawable.ic_default_profile)
                                .error(R.drawable.ic_default_profile)
                                .into(target)
                        } else {
                            target.setImageResource(R.drawable.ic_default_profile)
                        }

                        // Query Firestore in the background to get the latest live picture
                        val finalTarget = target
                        val finalInitialUrl = initialUrl
                        FirebaseManager.getInstance().getUserProfileImageUrl(uid) { avatarUrl, error ->
                            if (error != null) {
                                Log.e("MovieListAdapter", "Error fetching avatar for uid: $uid", error)
                            }
                            Log.d("MovieListAdapter", "Firestore response for uid: $uid, url: $avatarUrl")
                            if (error == null && avatarUrl != null) {
                                // If the live URL is different from what we had initially, or if we had nothing, update cache and load
                                if (avatarUrl != finalInitialUrl) {
                                    avatarCache[uid] = avatarUrl
                                    val resolvedLive = resolveAvatarPath(avatarUrl)
                                    if (uid == finalTarget.getTag(R.id.member_avatar_1)) {
                                        Log.d("MovieListAdapter", "Loading live updated imageUrl for uid: $uid, url: $resolvedLive")
                                        Glide.with(context)
                                            .load(resolvedLive)
                                            .placeholder(R.drawable.ic_default_profile)
                                            .error(R.drawable.ic_default_profile)
                                            .into(finalTarget)
                                    }
                                } else {
                                    // Live url is same as denormalized, just ensure cache is clean
                                    avatarCache[uid] = avatarUrl
                                }
                            } else {
                                // If query returned null or failed, and we had no denormalized image, cache empty string
                                if (finalInitialUrl == null) {
                                    avatarCache[uid] = ""
                                }
                            }
                        }
                    }
                }
            }

            if (userIDs.size > 4) {
                holder.txtMoreMembers.visibility = View.VISIBLE
                val extraMembers = userIDs.size - 4
                holder.txtMoreMembers.text = "+$extraMembers"
            }
        }

        holder.itemView.setOnClickListener {
            onItemClick?.onListClick(list)
        }
    }

    override fun getItemCount(): Int = movieLists.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val listTitle: TextView = itemView.findViewById(R.id.list_title)
        val cover1: ImageView = itemView.findViewById(R.id.cover_image_1)
        val cover2: ImageView = itemView.findViewById(R.id.cover_image_2)
        val cover3: ImageView = itemView.findViewById(R.id.cover_image_3)
        val coverMore: FrameLayout = itemView.findViewById(R.id.cover_more_container)
        val txtMoreCovers: TextView = itemView.findViewById(R.id.txt_more_covers)

        val avatar1: ImageView = itemView.findViewById(R.id.member_avatar_1)
        val avatar2: ImageView = itemView.findViewById(R.id.member_avatar_2)
        val avatar3: ImageView = itemView.findViewById(R.id.member_avatar_3)
        val avatar4: ImageView = itemView.findViewById(R.id.member_avatar_4)
        val txtMoreMembers: TextView = itemView.findViewById(R.id.txt_more_members)
    }

    private fun resolveAvatarPath(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        if (path.startsWith("http") || path.startsWith("file")) {
            return path
        }
        var filename = path
        if (!filename.contains("/")) {
            if (!filename.endsWith(".png")) {
                filename = "$filename.png"
            }
            return "file:///android_asset/avatars/$filename"
        }
        if (filename.startsWith("avatars/")) {
            return "file:///android_asset/$filename"
        }
        return path
    }

    fun updateData(newMovieLists: List<MovieLists>) {
        movieLists.clear()
        movieLists.addAll(newMovieLists)
        notifyDataSetChanged()
    }
}
