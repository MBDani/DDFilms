package com.merino.ddfilms.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.model.Review;

import de.hdodenhof.circleimageview.CircleImageView;
import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;
    private Context context;
    private OnReviewInteractionListener listener;

    public interface OnReviewInteractionListener {
        void onLikeClicked(Review review, int position);
        void onDislikeClicked(Review review, int position);
    }

    public ReviewAdapter(List<Review> reviewList, OnReviewInteractionListener listener) {
        this.reviewList = reviewList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);
        holder.bind(review, position);
    }

    @Override
    public int getItemCount() {
        return reviewList.size();
    }

    public void setReviewList(List<Review> reviewList) {
        this.reviewList = reviewList;
        notifyDataSetChanged();
    }

    public void updateReview(int position, Review updatedReview) {
        if (position >= 0 && position < reviewList.size()) {
            reviewList.set(position, updatedReview);
            notifyItemChanged(position);
        }
    }

    class ReviewViewHolder extends RecyclerView.ViewHolder {

        private CircleImageView userProfileImage;
        private TextView userName;
        private TextView reviewDate;
        private LinearLayout reviewStarsLayout;
        private TextView reviewText;
        private LinearLayout likeButton;
        private LinearLayout dislikeButton;
        private ImageView likeIcon;
        private ImageView dislikeIcon;
        private TextView likeCount;
        private TextView dislikeCount;

        private ImageView[] stars = new ImageView[5];

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);

            userProfileImage = itemView.findViewById(R.id.user_profile_image);
            userName = itemView.findViewById(R.id.user_name);
            reviewDate = itemView.findViewById(R.id.review_date);
            reviewStarsLayout = itemView.findViewById(R.id.review_stars_layout);
            reviewText = itemView.findViewById(R.id.review_text);
            likeButton = itemView.findViewById(R.id.like_button);
            dislikeButton = itemView.findViewById(R.id.dislike_button);
            likeIcon = itemView.findViewById(R.id.like_icon);
            dislikeIcon = itemView.findViewById(R.id.dislike_icon);
            likeCount = itemView.findViewById(R.id.like_count);
            dislikeCount = itemView.findViewById(R.id.dislike_count);

            // Inicializar array de estrellas
            stars[0] = itemView.findViewById(R.id.star_1);
            stars[1] = itemView.findViewById(R.id.star_2);
            stars[2] = itemView.findViewById(R.id.star_3);
            stars[3] = itemView.findViewById(R.id.star_4);
            stars[4] = itemView.findViewById(R.id.star_5);
        }

        public void bind(Review review, int position) {
            // Configurar información del usuario
            userName.setText(review.getUserName());
            reviewDate.setText(review.getFormattedDate());

            // Cargar imagen de perfil
            if (review.getUserProfileImageUrl() != null && !review.getUserProfileImageUrl().isEmpty()) {
                Glide.with(context)
                        .load(review.getUserProfileImageUrl())
                        .placeholder(R.drawable.ic_default_profile)
                        .error(R.drawable.ic_default_profile)
                        .into(userProfileImage);
            } else {
                userProfileImage.setImageResource(R.drawable.ic_default_profile);
            }

            // Configurar estrellas
            setupStars(review.getRating());

            // Configurar texto de reseña
            if (review.getReviewText() != null && !review.getReviewText().trim().isEmpty()) {
                reviewText.setText(review.getReviewText());
                reviewText.setVisibility(View.VISIBLE);
            } else {
                reviewText.setVisibility(View.GONE);
            }

            // Configurar botones de like/dislike
            setupLikeDislikeButtons(review, position);
        }

        private void setupStars(float rating) {
            int fullStars = (int) rating;
            boolean hasHalfStar = (rating % 1) == 0.5f;

            for (int i = 0; i < 5; i++) {
                if (i < fullStars) {
                    // Estrella completa
                    stars[i].setImageResource(R.drawable.ic_star);
                    stars[i].setColorFilter(ContextCompat.getColor(context, R.color.gold_dark));
                } else if (i == fullStars && hasHalfStar) {
                    // Media estrella
                    stars[i].setImageResource(R.drawable.ic_star_half);
                    stars[i].setColorFilter(ContextCompat.getColor(context, R.color.gold_dark));
                } else {
                    // Estrella vacía
                    stars[i].setImageResource(R.drawable.ic_star);
                    stars[i].setColorFilter(ContextCompat.getColor(context, R.color.gray_medium));
                }
            }
        }

        private void setupLikeDislikeButtons(Review review, int position) {
            // Configurar contadores
            likeCount.setText(String.valueOf(review.getLikeCount().size()));
            dislikeCount.setText(String.valueOf(review.getDislikeCount().size()));

            // Configurar colores según estado
            if (review.isLikedByCurrentUser()) {
                likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_green_dark));
                likeCount.setTextColor(ContextCompat.getColor(context, R.color.accent_green_dark));
            } else {
                likeIcon.setColorFilter(ContextCompat.getColor(context, R.color.gold_light));
                likeCount.setTextColor(ContextCompat.getColor(context, R.color.gold_light));
            }

            if (review.isDislikedByCurrentUser()) {
                dislikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.accent_red));
                dislikeCount.setTextColor(ContextCompat.getColor(context, R.color.accent_red));
            } else {
                dislikeIcon.setColorFilter(ContextCompat.getColor(context, R.color.gold_light));
                dislikeCount.setTextColor(ContextCompat.getColor(context, R.color.gold_light));
            }

            // Configurar listeners
            likeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLikeClicked(review, position);
                }
            });

            dislikeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDislikeClicked(review, position);
                }
            });
        }
    }
}