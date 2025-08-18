package com.merino.ddfilms.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.utils.ReviewUtil;

public class ReviewsFragment extends Fragment{

    private RecyclerView reviewsRecyclerView;
    private ReviewUtil reviewUtil;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reviews, container, false);
        reviewUtil = new ReviewUtil(requireContext());

        initViews(view);
        setupRecyclerViews();

        reviewUtil.loadAllReviews();

        return view;
    }

    private void initViews(View view) {
        reviewsRecyclerView = view.findViewById(R.id.recyclerView_fragment_reviews);
    }

    private void setupRecyclerViews() {
        // Configurar RecyclerView para reviews
        reviewsRecyclerView.setAdapter(reviewUtil.getReviewAdapter());
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        reviewsRecyclerView.setNestedScrollingEnabled(false);
    }
}
