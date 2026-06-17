package com.merino.ddfilms.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.utils.ReviewUtil;

public class ReviewsFragment extends Fragment{

    private RecyclerView reviewsRecyclerView;
    private ReviewUtil reviewUtil;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reviews, container, false);
        reviewUtil = new ReviewUtil(requireContext());

        initViews(view);
        setupRecyclerViews();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (reviewUtil != null) {
            reviewUtil.loadAllReviews();
        }
    }

    private void initViews(View view) {
        reviewsRecyclerView = view.findViewById(R.id.recyclerView_fragment_reviews);
    }

    @Override
    public void onCreateOptionsMenu(android.view.Menu menu, android.view.MenuInflater inflater) {
        inflater.inflate(R.menu.menu_reviews_filter, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            // Find the MenuItem's view to anchor the popup if possible, otherwise use a fallback
            View view = getActivity().findViewById(R.id.action_filter);
            showFilterMenu(view != null ? view : getActivity().findViewById(R.id.toolbar));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showFilterMenu(android.view.View anchor) {
        if (anchor == null) return;
        android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), anchor);
        popup.getMenu().add(0, 1, 0, "Más recientes");
        popup.getMenu().add(0, 2, 0, "Más antiguas");
        popup.getMenu().add(0, 3, 0, "Likes");
        popup.getMenu().add(0, 4, 0, "Dislikes");
        popup.getMenu().add(0, 5, 0, "Mejor valoración");
        popup.getMenu().add(0, 6, 0, "Peor valoración");

        popup.setOnMenuItemClickListener(item -> {
            com.merino.ddfilms.utils.DateFormatter df = new com.merino.ddfilms.utils.DateFormatter();
            switch (item.getItemId()) {
                case 1:
                    reviewUtil.sortReviews(df.reviewDateDescComparator());
                    return true;
                case 2:
                    reviewUtil.sortReviews(df.reviewDateAscComparator());
                    return true;
                case 3:
                    reviewUtil.sortReviews(df.reviewLikesDescComparator());
                    return true;
                case 4:
                    reviewUtil.sortReviews(df.reviewDislikesDescComparator());
                    return true;
                case 5:
                    reviewUtil.sortReviews(df.reviewRatingDescComparator());
                    return true;
                case 6:
                    reviewUtil.sortReviews(df.reviewRatingAscComparator());
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void setupRecyclerViews() {
        // Configurar RecyclerView para reviews
        reviewUtil.getReviewAdapter().setShowMovieInfo(true);
        reviewsRecyclerView.setAdapter(reviewUtil.getReviewAdapter());
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        
        reviewUtil.getReviewAdapter().setListener(new com.merino.ddfilms.adapters.ReviewAdapter.OnReviewInteractionListener() {
            @Override
            public void onLikeClicked(com.merino.ddfilms.model.Review review, int position) {
                reviewUtil.onLikeClicked(review, position);
            }

            @Override
            public void onDislikeClicked(com.merino.ddfilms.model.Review review, int position) {
                 reviewUtil.onDislikeClicked(review, position);
            }
            
            @Override
            public void onReviewClicked(com.merino.ddfilms.model.Review review, android.view.View sharedElement) {
                android.content.Intent intent = new android.content.Intent(requireContext(), com.merino.ddfilms.ui.MovieDetailActivity.class);
                
                // Construct basic Movie object
                Movie movie = new Movie();
                movie.setId(review.getMovieId());
                movie.setTitle(review.getMovieTitle());
                movie.setPosterPath(review.getPosterPath());
                movie.setBackdropPath(review.getBackdropPath());

                intent.putExtra("movie", movie);
                intent.putExtra("highlight_review_id", review.getId());

                if (sharedElement != null) {
                    android.app.ActivityOptions options = android.app.ActivityOptions.makeSceneTransitionAnimation(
                            requireActivity(), 
                            sharedElement, 
                            "moviePosterTransition" 
                    );
                    startActivity(intent, options.toBundle());
                } else {
                    startActivity(intent);
                }
            }
        });
    }
}
