package com.merino.ddfilms.utils;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.merino.ddfilms.adapters.ReviewAdapter;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.Review;
import com.merino.ddfilms.ui.fragment.WriteReviewDialogFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

public class ReviewUtil implements
        ReviewAdapter.OnReviewInteractionListener,
        WriteReviewDialogFragment.OnReviewSubmittedListener {

    private List<Review> reviewsList;
    @Getter
    private ReviewAdapter reviewAdapter;
    @Getter
    private Review userReview;
    private final FirebaseManager firebaseManager = new FirebaseManager();
    private String userId;
    private Context context;
    @Setter
    private String userName;
    @Setter
    private Movie currentMovie;

    // Scroll targets (se setean desde la Activity)
    private AppBarLayout appBarLayout;
    private NestedScrollView nestedScrollView;
    private RecyclerView recyclerView;

    public ReviewUtil(Context context) {
        this.context = context;
        this.userId = firebaseManager.getCurrentUserUID();
        this.reviewsList = new ArrayList<>();
        this.reviewAdapter = new ReviewAdapter(reviewsList, this);
    }

    public void setScrollTargets(AppBarLayout appBarLayout,
                                 NestedScrollView nestedScrollView,
                                 RecyclerView recyclerView) {
        this.appBarLayout = appBarLayout;
        this.nestedScrollView = nestedScrollView;
        this.recyclerView = recyclerView;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadAllReviews() {
        firebaseManager.getAllReviews((reviews, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
            } else if (reviews != null) {
                getCurrenUserReview(reviews);
                setUpLikeAndDislikeReviews(reviews);
                reviewsList.clear();
                reviewsList.addAll(reviews.stream()
                        .sorted((r1, r2) -> r2.getReviewDate().compareTo(r1.getReviewDate()))
                        .collect(Collectors.toList()));
                runOnMain(() -> reviewAdapter.notifyDataSetChanged());
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadMovieReviews(Integer movieId) {
        firebaseManager.getReviews(movieId, (reviews, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
            } else if (reviews != null) {
                getCurrenUserReview(reviews);
                setUpLikeAndDislikeReviews(reviews);
                reviewsList.addAll(reviews.stream()
                        .sorted((r1, r2) -> r2.getReviewDate().compareTo(r1.getReviewDate()))
                        .collect(Collectors.toList()));
                runOnMain(() -> reviewAdapter.notifyDataSetChanged());
            }
        });
    }

    public void postReview(Review review) {
        review.setUserId(userId);
        review.setUserName(userName);
        review.setMovieId(currentMovie.getId());
        review.setReviewDate(new Date().toString());
        review.setLikeCount(new ArrayList<>());
        review.setDislikeCount(new ArrayList<>());

        firebaseManager.postReview(review, (reviewResponse, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
                return;
            }
            runOnMain(() -> {
                addReviewToRecycler(reviewResponse);
                if (appBarLayout != null && nestedScrollView != null && recyclerView != null) {
                    scrollToNewReview();
                }
            });
        });
    }

    public void addReviewToRecycler(Review review) {
        if (userReview != null)
            reviewsList.remove(userReview);

        reviewsList.add(0, review);
        runOnMain(() -> {
            reviewAdapter.setReviewList(reviewsList);
            reviewAdapter.notifyItemInserted(0);
        });


        userReview = review;
    }

    @Override
    public void onReviewSubmitted(Review review) {
        // Si review tiene id -> actualización; si no -> post
        if (review.getId() != null) {
            updateReview(review);
        } else {
            postReview(review);
        }
    }

    private void updateReview(Review review) {
        review.setReviewDate(new Date().toString());
        review.setLikeCount(new ArrayList<>());
        review.setDislikeCount(new ArrayList<>());

        firebaseManager.updateReview(review, (reviewResponse, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
                return;
            }
            runOnMain(() -> {
                // Reemplace o inserte en la posición 0
                addReviewToRecycler(reviewResponse);

                // Si tenemos targets para scroll, ejecutamos el scroll animado
                if (appBarLayout != null && nestedScrollView != null && recyclerView != null) {
                    scrollToNewReview();
                }
            });
        });
    }

    private void getCurrenUserReview(List<Review> reviews) {
        for (Review review : reviews) {
            if (review.getUserId().equals(userId)) {
                userReview = review;
                break;
            }
        }
    }

    // --------------------------------- Scroll  ------------------------------------------
    public void scrollToNewReview() {
        // 1) Colapsar AppBar
        appBarLayout.setExpanded(false, true);

        // 2) Escuchar cuando el RecyclerView termine de hacer layout
        recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v,
                                       int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                recyclerView.removeOnLayoutChangeListener(this);

                // 3) Calculamos destino de scroll
                nestedScrollView.post(() -> {
                    int startY = nestedScrollView.getScrollY();
                    int targetY = recyclerView.getTop();

                    // 4) Animador de scroll personalizado
                    ValueAnimator scrollAnim = ValueAnimator.ofInt(startY, targetY);
                    scrollAnim.setDuration(1200);
                    scrollAnim.setInterpolator(new DecelerateInterpolator());
                    scrollAnim.addUpdateListener(anim -> {
                        int y = (int) anim.getAnimatedValue();
                        nestedScrollView.scrollTo(0, y);
                    });
                    scrollAnim.start();

                    // 5) Animamos la nueva review: slide + fade-in
                    View newItem = recyclerView.getLayoutManager().findViewByPosition(0);
                    if (newItem != null) {
                        newItem.setAlpha(0f);
                        newItem.setTranslationY(50f);  // empieza 50px abajo
                        newItem.animate()
                                .alpha(1f)
                                .translationY(0f)
                                .setDuration(1500)
                                .setInterpolator(new DecelerateInterpolator())
                                .start();
                    }
                });
            }
        });
    }

    // --------------------- Likes / dislikes  ------------------------------------------

    @Override
    public void onLikeClicked(Review review, int position) {
        // Lógica para el botón de "Me gusta"
        if (review.getLikeCount().contains(userId)) {
            // Si ya le gustaba, quitar el like
            removeLike(review, position);
        } else {
            // Si no le gustaba, añadir like
            addLike(review, position);
        }
    }

    @Override
    public void onDislikeClicked(Review review, int position) {
        // Lógica para el botón de "No me gusta"
        if (review.getDislikeCount().contains(userId)) {
            removeDislike(review, position);
        } else {
            // Si no tenía dislike, añadirlo
            addDislike(review, position);
        }
    }

    private void setUpLikeAndDislikeReviews(List<Review> reviews) {
        for (Review review : reviews) {
            review.setLikedByCurrentUser(review.getLikeCount().contains(userId));
            review.setDislikedByCurrentUser(review.getDislikeCount().contains(userId));
        }
    }

    private void addDislike(Review review, int position) {
        firebaseManager.reviewAddDislike(review.getId(), userId, (result, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
                return;
            }
            review.getDislikeCount().add(this.userId);
            review.setDislikedByCurrentUser(true);

            // Si tenía like, quitarlo
            if (review.getLikeCount().contains(userId)) {
                removeLike(review, position);
            } else {
                reviewAdapter.updateReview(position, review);
                reviewAdapter.notifyItemChanged(position);
            }
        });
    }

    private void addLike(Review review, int position) {
        firebaseManager.reviewAddLike(review.getId(), userId, (result, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
                return;
            }
            review.getLikeCount().add(this.userId);
            review.setLikedByCurrentUser(true);

            // Si tenía dislike, quitarlo
            if (review.getDislikeCount().contains(userId)) {
                removeDislike(review, position);
            } else {
                runOnMain(() -> {
                    reviewAdapter.updateReview(position, review);
                    reviewAdapter.notifyItemChanged(position);
                });
            }
        });
    }

    private void removeDislike(Review review, int position) {
        firebaseManager.reviewRemoveDislike(review.getId(), userId, (result, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
                return;
            }
            review.getDislikeCount().remove(this.userId);
            review.setDislikedByCurrentUser(false);
            runOnMain(() -> {
                reviewAdapter.updateReview(position, review);
                reviewAdapter.notifyItemChanged(position);
            });
        });
    }

    private void removeLike(Review review, int position) {
        firebaseManager.reviewRemoveLike(review.getId(), userId, (result, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
                return;
            }
            review.getLikeCount().remove(this.userId);
            review.setLikedByCurrentUser(false);
            runOnMain(() -> reviewAdapter.notifyItemChanged(position));
        });
    }

    private void runOnMain(Runnable r) {
        if (r == null) return;
        new Handler(Looper.getMainLooper()).post(r);
    }
}
