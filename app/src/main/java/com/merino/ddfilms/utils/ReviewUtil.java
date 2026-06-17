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
import com.merino.ddfilms.R;
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
    private List<Review> allReviewsList;
    private int currentPage = 1;
    private boolean isPageLoading = false;
    private static final int PAGE_SIZE = 15;
    private java.util.Comparator<Review> currentComparator;
    @Getter
    private ReviewAdapter reviewAdapter;
    @Getter
    private Review userReview;
    private final FirebaseManager firebaseManager = new FirebaseManager();
    DateFormatter dateFormatter = new DateFormatter();
    private String userId;
    private Context context;
    @Setter
    private String userName;
    @Setter
    private Movie currentMovie;

    @Setter
    private String highlightReviewId;

    public void setHighlightReviewId(String highlightReviewId) {
        this.highlightReviewId = highlightReviewId;
    }

    // Scroll targets (se setean desde la Activity)
    private AppBarLayout appBarLayout;
    private NestedScrollView nestedScrollView;
    private RecyclerView recyclerView;

    public interface OnReviewsLoadedListener {
        void onReviewsLoaded();
    }
    private OnReviewsLoadedListener reviewsLoadedListener;

    public void setOnReviewsLoadedListener(OnReviewsLoadedListener listener) {
        this.reviewsLoadedListener = listener;
    }

    public ReviewUtil(Context context) {
        this.context = context;
        this.userId = firebaseManager.getCurrentUserUID();
        this.reviewsList = new ArrayList<>();
        this.allReviewsList = new ArrayList<>();
        this.reviewAdapter = new ReviewAdapter(reviewsList, this);
        this.currentComparator = dateFormatter.reviewDateDescComparator();
    }

    public void setScrollTargets(AppBarLayout appBarLayout,
                                 NestedScrollView nestedScrollView,
                                 RecyclerView recyclerView) {
        this.appBarLayout = appBarLayout;
        this.nestedScrollView = nestedScrollView;
        this.recyclerView = recyclerView;
        
        if (nestedScrollView != null) {
            nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                // Si scroll llegó cerca del final (por ejemplo, a 300px o menos)
                View child = v.getChildAt(v.getChildCount() - 1);
                if (child != null) {
                    int diff = (child.getBottom() - (v.getHeight() + scrollY));
                    if (diff <= 300) {
                        loadNextPage();
                    }
                }
            });
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadAllReviews() {
        firebaseManager.getAllReviews((reviews, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
            } else if (reviews != null) {
                getCurrenUserReview(reviews);
                setUpLikeAndDislikeReviews(reviews);
                
                allReviewsList.clear();
                allReviewsList.addAll(reviews);
                
                // Sort with current comparator
                allReviewsList.sort(currentComparator != null ? currentComparator : dateFormatter.reviewDateDescComparator());
                
                currentPage = 1;
                isPageLoading = false;
                reviewsList.clear();
                int end = Math.min(PAGE_SIZE, allReviewsList.size());
                reviewsList.addAll(allReviewsList.subList(0, end));
                
                runOnMain(() -> reviewAdapter.notifyDataSetChanged());
            }
        });
    }

    /**
     * Carga la siguiente página de reseñas de forma perezosa (Lazy Load).
     */
    public void loadNextPage() {
        if (allReviewsList == null || reviewsList == null) return;
        if (isPageLoading) return;
        
        int start = currentPage * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, allReviewsList.size());
        
        if (start < end) {
            isPageLoading = true;
            List<Review> nextPageItems = allReviewsList.subList(start, end);
            reviewsList.addAll(nextPageItems);
            currentPage++;
            runOnMain(() -> {
                reviewAdapter.notifyItemRangeInserted(start, nextPageItems.size());
                new Handler(Looper.getMainLooper()).post(() -> isPageLoading = false);
            });
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void loadMovieReviews(Integer movieId) {
        firebaseManager.getReviews(movieId, (reviews, error) -> {
            if (error != null) {
                showMessage(context, error.getMessage());
                if (reviewsLoadedListener != null) {
                    runOnMain(() -> reviewsLoadedListener.onReviewsLoaded());
                }
            } else if (reviews != null) {
                getCurrenUserReview(reviews);
                setUpLikeAndDislikeReviews(reviews);
                
                allReviewsList.clear();
                allReviewsList.addAll(reviews);
                
                // Sort with current comparator
                allReviewsList.sort(currentComparator != null ? currentComparator : dateFormatter.reviewDateDescComparator());
                
                currentPage = 1;
                isPageLoading = false;
                reviewsList.clear();
                int end = Math.min(PAGE_SIZE, allReviewsList.size());
                reviewsList.addAll(allReviewsList.subList(0, end));
                
                runOnMain(() -> {
                    reviewAdapter.notifyDataSetChanged();
                    if (reviewsLoadedListener != null) {
                        reviewsLoadedListener.onReviewsLoaded();
                    }
                });
            } else {
                if (reviewsLoadedListener != null) {
                    runOnMain(() -> reviewsLoadedListener.onReviewsLoaded());
                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void sortReviews(java.util.Comparator<Review> comparator) {
        this.currentComparator = comparator;
        if (allReviewsList != null && !allReviewsList.isEmpty()) {
            allReviewsList.sort(comparator);
            currentPage = 1;
            isPageLoading = false;
            reviewsList.clear();
            int end = Math.min(PAGE_SIZE, allReviewsList.size());
            reviewsList.addAll(allReviewsList.subList(0, end));
            runOnMain(() -> reviewAdapter.notifyDataSetChanged());
        }
    }

    public void postReview(Review review) {
        if (userId == null) userId = firebaseManager.getCurrentUserUID();
        
        // Ensure username is present
        if (userName == null) {
            firebaseManager.getUserName(userId, (name, error) -> {
                userName = name != null ? name : "Usuario";
                review.setUserName(userName);
                proceedToPost(review);
            });
        } else {
            review.setUserName(userName);
            proceedToPost(review);
        }
    }

    private void proceedToPost(Review review) {
        review.setUserId(userId);
        if (currentMovie != null) {
            review.setMovieId(currentMovie.getId());
            review.setMovieTitle(currentMovie.getTitle());
            review.setPosterPath(currentMovie.getPosterPath());
            review.setBackdropPath(currentMovie.getBackdropPath());
        }
        review.setReviewDate(new Date().toString());
        if (review.getLikeCount() == null) review.setLikeCount(new ArrayList<>());
        if (review.getDislikeCount() == null) review.setDislikeCount(new ArrayList<>());

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
        if (userReview != null) {
            allReviewsList.removeIf(r -> r.getId().equals(userReview.getId()));
            reviewsList.removeIf(r -> r.getId().equals(userReview.getId()));
        }

        allReviewsList.add(0, review);
        allReviewsList.sort(currentComparator != null ? currentComparator : dateFormatter.reviewDateDescComparator());

        currentPage = 1;
        isPageLoading = false;
        reviewsList.clear();
        int end = Math.min(PAGE_SIZE, allReviewsList.size());
        reviewsList.addAll(allReviewsList.subList(0, end));

        runOnMain(() -> {
            reviewAdapter.notifyDataSetChanged();
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

    @Override
    public void onReviewDeleted(Review review) {
        if (review.getId() != null) {
            firebaseManager.deleteReview(review.getId(), (success, error) -> {
                if (error != null) {
                    showMessage(context, error.getMessage());
                    return;
                }
                runOnMain(() -> {
                    allReviewsList.removeIf(r -> r.getId().equals(review.getId()));
                    reviewsList.removeIf(r -> r.getId().equals(review.getId()));
                    if (userReview != null && userReview.getId().equals(review.getId())) {
                        userReview = null;
                    }
                    reviewAdapter.notifyDataSetChanged();
                    showMessage(context, "Reseña borrada con éxito");
                });
            });
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

    public void scrollToReview(String reviewId) {
        if (reviewsList == null || reviewsList.isEmpty() || reviewId == null) return;

        int index = -1;
        for (int i = 0; i < reviewsList.size(); i++) {
            if (reviewsList.get(i).getId().equals(reviewId)) {
                index = i;
                break;
            }
        }

        if (index != -1) {
            final int targetIndex = index;
            recyclerView.postDelayed(() -> {
                // Collapse AppBar first
                if (appBarLayout != null) appBarLayout.setExpanded(false, true);

                // Find view holder
                View view = recyclerView.getLayoutManager().findViewByPosition(targetIndex);
                if (view == null) {
                    // Item not currently visible, scroll closer first
                    recyclerView.scrollToPosition(targetIndex);
                }

                // Allow layout to happen then precise scroll
                recyclerView.postDelayed(() -> {
                    View targetView = recyclerView.getLayoutManager().findViewByPosition(targetIndex);

                    if (targetView != null && nestedScrollView != null) {
                        int startY = nestedScrollView.getScrollY();
                        int targetY = (int) (recyclerView.getY() + targetView.getTop()); // Changed getY to getTop for relative position in recycler

                        ValueAnimator scrollAnim = ValueAnimator.ofInt(startY, targetY);
                        scrollAnim.setDuration(1200);
                        scrollAnim.setInterpolator(new DecelerateInterpolator());
                        scrollAnim.addUpdateListener(anim -> {
                            int y = (int) anim.getAnimatedValue();
                            nestedScrollView.scrollTo(0, y);
                        });
                        scrollAnim.start();

                        // Highlight animation
                        runOnMain(() -> {
                            targetView.setAlpha(0.2f);
                            targetView.animate().alpha(1f).setDuration(1000).start();

                            int originalColor = android.graphics.Color.TRANSPARENT;
                            int highlightColor = androidx.core.content.ContextCompat.getColor(context, R.color.white_60);

                            ValueAnimator colorAnim = ValueAnimator.ofArgb(highlightColor, originalColor);
                            colorAnim.setDuration(2000);
                            colorAnim.addUpdateListener(anim -> targetView.setBackgroundColor((int) anim.getAnimatedValue()));
                            colorAnim.start();
                        });
                    } else if (targetView != null) {
                        // Fallback if no nested scroll view (e.g. standard recycler)
                        recyclerView.smoothScrollToPosition(targetIndex);
                    }
                }, 200);

            }, 500);
        }
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

    @Override
    public void onReviewClicked(Review review, View sharedElement) {
        if (review.getUserId() != null && review.getUserId().equals(userId)) {
            if (context instanceof androidx.fragment.app.FragmentActivity) {
                // Reconstruct basic Movie object or use currentMovie
                Movie movie = currentMovie;
                if (movie == null) {
                    movie = new Movie();
                    movie.setId(review.getMovieId());
                    movie.setTitle(review.getMovieTitle());
                    movie.setPosterPath(review.getPosterPath());
                    movie.setBackdropPath(review.getBackdropPath());
                }

                WriteReviewDialogFragment dialog = new WriteReviewDialogFragment(movie, review);
                dialog.setOnReviewSubmittedListener(this);

                androidx.fragment.app.FragmentManager fm = ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager();
                dialog.show(fm, "WriteReviewDialogFragment");
            }
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
