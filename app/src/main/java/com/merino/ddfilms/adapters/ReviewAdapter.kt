package com.merino.ddfilms.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.merino.ddfilms.R
import com.merino.ddfilms.model.Review
import de.hdodenhof.circleimageview.CircleImageView

class ReviewAdapter(
    private var reviewList: List<Review>,
    private var listener: OnReviewInteractionListener?
) : RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    interface OnReviewInteractionListener {
        fun onLikeClicked(review: Review, position: Int)
        fun onDislikeClicked(review: Review, position: Int)
        fun onReviewClicked(review: Review, sharedElement: View) {}
    }

    private var showMovieInfo = false

    fun setShowMovieInfo(showMovieInfo: Boolean) {
        this.showMovieInfo = showMovieInfo
    }

    fun setListener(listener: OnReviewInteractionListener?) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviewList[position]
        holder.setupLikeDislikeButtons(review, position)
        holder.bind(review, position, showMovieInfo)

        holder.itemView.setOnClickListener {
            listener?.let { l ->
                val currentUserId = FirebaseAuth.getInstance().uid
                val isOwnReview = review.userId != null && review.userId == currentUserId
                if (showMovieInfo || isOwnReview) {
                    l.onReviewClicked(review, holder.moviePoster)
                }
            }
        }
    }

    override fun getItemCount(): Int = reviewList.size

    fun setReviewList(reviewList: List<Review>) {
        this.reviewList = reviewList
        notifyDataSetChanged()
    }

    fun updateReview(position: Int, updatedReview: Review) {
        if (position in reviewList.indices) {
            if (reviewList is MutableList) {
                (reviewList as MutableList)[position] = updatedReview
            } else {
                val mutable = reviewList.toMutableList()
                mutable[position] = updatedReview
                reviewList = mutable
            }
            notifyItemChanged(position)
        }
    }

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userProfileImage: CircleImageView = itemView.findViewById(R.id.user_profile_image)
        private val userName: TextView = itemView.findViewById(R.id.user_name)
        private val reviewDate: TextView = itemView.findViewById(R.id.review_date)
        private val reviewText: TextView = itemView.findViewById(R.id.review_text)
        private val likeButton: LinearLayout = itemView.findViewById(R.id.like_button)
        private val dislikeButton: LinearLayout = itemView.findViewById(R.id.dislike_button)
        private val likeIcon: ImageView = itemView.findViewById(R.id.like_icon)
        private val dislikeIcon: ImageView = itemView.findViewById(R.id.dislike_icon)
        private val likeCount: TextView = itemView.findViewById(R.id.like_count)
        private val dislikeCount: TextView = itemView.findViewById(R.id.dislike_count)

        // Movie Info
        private val movieInfoLayout: View = itemView.findViewById(R.id.movie_info_layout)
        val moviePoster: ImageView = itemView.findViewById(R.id.movie_poster_review)
        private val movieTitle: TextView = itemView.findViewById(R.id.movie_title_review)

        private val stars = arrayOf<ImageView>(
            itemView.findViewById(R.id.star_1),
            itemView.findViewById(R.id.star_2),
            itemView.findViewById(R.id.star_3),
            itemView.findViewById(R.id.star_4),
            itemView.findViewById(R.id.star_5)
        )

        fun bind(review: Review, position: Int, showMovieInfo: Boolean) {
            val context = itemView.context
            if (showMovieInfo) {
                movieInfoLayout.visibility = View.VISIBLE
                movieTitle.text = review.movieTitle
                if (review.posterPath != null) {
                    Glide.with(context)
                        .load("https://image.tmdb.org/t/p/w200" + review.posterPath)
                        .placeholder(R.drawable.placeholder_poster)
                        .into(moviePoster)
                }
                moviePoster.transitionName = "poster_${review.movieId}"
            } else {
                movieInfoLayout.visibility = View.GONE
            }

            userName.text = review.userName
            reviewDate.text = review.formattedDate

            if (!review.userProfileImageUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(review.userProfileImageUrl)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .into(userProfileImage)
            } else {
                userProfileImage.setImageResource(R.drawable.ic_default_profile)
            }

            setupStars(review.rating)

            if (!review.reviewText.isNullOrBlank()) {
                reviewText.text = review.reviewText
                reviewText.visibility = View.VISIBLE
            } else {
                reviewText.visibility = View.GONE
            }

            setupLikeDislikeButtons(review, position)
        }

        private fun setupStars(rating: Float) {
            val context = itemView.context
            val fullStars = rating.toInt()
            val hasHalfStar = (rating % 1) == 0.5f

            for (i in 0 until 5) {
                if (i < fullStars) {
                    stars[i].setImageResource(R.drawable.ic_star_filled)
                    stars[i].setColorFilter(ContextCompat.getColor(context, R.color.gold_dark))
                } else if (i == fullStars && hasHalfStar) {
                    stars[i].setImageResource(R.drawable.ic_star_half)
                    stars[i].setColorFilter(ContextCompat.getColor(context, R.color.gold_dark))
                } else {
                    stars[i].setImageResource(R.drawable.ic_star_empty)
                    stars[i].setColorFilter(ContextCompat.getColor(context, R.color.gray_medium))
                }
            }
        }

        fun setupLikeDislikeButtons(review: Review, position: Int) {
            val context = itemView.context
            likeCount.text = (review.likeCount?.size ?: 0).toString()
            dislikeCount.text = (review.dislikeCount?.size ?: 0).toString()

            if (review.isLikedByCurrentUser) {
                likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_green_dark))
                likeCount.setTextColor(ContextCompat.getColor(context, R.color.accent_green_dark))
            } else {
                likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.gold_light))
                likeCount.setTextColor(ContextCompat.getColor(context, R.color.gold_light))
            }

            if (review.isDislikedByCurrentUser) {
                dislikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_red))
                dislikeCount.setTextColor(ContextCompat.getColor(context, R.color.accent_red))
            } else {
                dislikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.gold_light))
                dislikeCount.setTextColor(ContextCompat.getColor(context, R.color.gold_light))
            }

            likeButton.setOnClickListener {
                listener?.onLikeClicked(review, position)
            }

            dislikeButton.setOnClickListener {
                listener?.onDislikeClicked(review, position)
            }
        }
    }
}
