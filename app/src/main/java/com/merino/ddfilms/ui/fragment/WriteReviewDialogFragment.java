package com.merino.ddfilms.ui.fragment;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.merino.ddfilms.R;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.Review;

public class WriteReviewDialogFragment extends DialogFragment {

    private Movie movie;
    private Review review;
    private float currentRating = 0f;
    private ImageView[] ratingStars = new ImageView[5];
    private TextInputEditText reviewEditText;
    private OnReviewSubmittedListener listener;

    public interface OnReviewSubmittedListener {
        void onReviewSubmitted(Review review);
    }

    public WriteReviewDialogFragment(Movie movie, Review userReview) {
        this.movie = movie;
        this.review = userReview;
    }

    public void setOnReviewSubmittedListener(OnReviewSubmittedListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_write_review, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);

        if (review != null) {
            if (review.getReviewText() != null) {
                reviewEditText.setText(review.getReviewText());
            }
            setRating(review.getRating());
        }

        setupStarRating();
        setupButtons(view);

        // Configurar título de la película
        TextView movieTitleReview = view.findViewById(R.id.movie_title_review);
        if (movie != null) {
            movieTitleReview.setText((movie.getTitle()));
        }
    }

    private void initViews(View view) {
        reviewEditText = view.findViewById(R.id.review_edit_text);

        // Inicializar array de estrellas
        ratingStars[0] = view.findViewById(R.id.rating_star_1);
        ratingStars[1] = view.findViewById(R.id.rating_star_2);
        ratingStars[2] = view.findViewById(R.id.rating_star_3);
        ratingStars[3] = view.findViewById(R.id.rating_star_4);
        ratingStars[4] = view.findViewById(R.id.rating_star_5);
    }

    private void setupStarRating() {
        for (int i = 0; i < ratingStars.length; i++) {
            final int starIndex = i;
            final ImageView star = ratingStars[i];

            star.setOnClickListener(v -> {
                // Si ya está seleccionada esta estrella completa, hacer media estrella
                float newRating;
                if (currentRating == (starIndex + 1)) {
                    newRating = starIndex + 0.5f;
                } else {
                    newRating = starIndex + 1;
                }

                setRating(newRating);
            });

            // Long click para hacer media estrella directamente
            star.setOnLongClickListener(v -> {
                setRating(starIndex + 0.5f);
                return true;
            });
        }

        updateStarDisplay();
    }

    private void setRating(float rating) {
        currentRating = Math.max(0f, Math.min(5f, rating));
        updateStarDisplay();
    }

    private void updateStarDisplay() {
        int fullStars = (int) currentRating;
        boolean hasHalfStar = (currentRating % 1) == 0.5f;

        for (int i = 0; i < 5; i++) {
            ImageView star = ratingStars[i];

            if (i < fullStars) {
                // Estrella completa
                star.setImageResource(R.drawable.ic_star);
                star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gold_dark));
            } else if (i == fullStars && hasHalfStar) {
                // Media estrella
                star.setImageResource(R.drawable.ic_star_half);
                star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gold_dark));
            } else {
                // Estrella vacía
                star.setImageResource(R.drawable.ic_star);
                star.setColorFilter(ContextCompat.getColor(requireContext(), R.color.gray_medium));
            }
        }
    }

    private void setupButtons(View view) {
        MaterialButton cancelButton = view.findViewById(R.id.cancel_button);
        MaterialButton publishButton = view.findViewById(R.id.publish_button);

        cancelButton.setOnClickListener(v -> dismiss());

        publishButton.setOnClickListener(v -> {
            if (currentRating > 0) {
                submitReview();
            } else {
                showMessage(getContext(), "Es obligatoria la puntucación");
            }
        });
    }

    private void submitReview() {
        if (listener != null && movie != null) {
            String reviewTextContent = reviewEditText.getText() != null ?
                    reviewEditText.getText().toString().trim() : "";

            if (this.review == null) this.review = new Review();
            this.review.setRating(currentRating);
            this.review.setReviewText(reviewTextContent);
            listener.onReviewSubmitted(this.review);
        }

        dismiss();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            // Configurar el tamaño del diálogo
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
