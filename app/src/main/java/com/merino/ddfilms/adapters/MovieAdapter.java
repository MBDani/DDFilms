package com.merino.ddfilms.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.ui.MovieDetailActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    @Setter
    @Getter
    private List<Movie> movies = new ArrayList<>();
    private boolean isEditMode = false;
    private OnItemLongClickListener longClickListener;

    private OnDeleteClickListener deleteClickListener;

    @Setter
    private String listID;

    public interface OnItemLongClickListener {
        boolean onItemLongClick(int position);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(int position, Movie movie);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    public void setEditMode(boolean editMode) {
        isEditMode = editMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setMovies(List<Movie> movies) {
        Map<String, List<Movie>> moviesByDate = new LinkedHashMap<>();

        for (Movie movie : movies) {
            if (movie.getCreatedAt() != null) {
                moviesByDate.computeIfAbsent(movie.getCreatedAt(), k -> new ArrayList<>()).add(movie);
            }
        }

        if (!moviesByDate.isEmpty()) {
            List<Movie> sortedMovies = new ArrayList<>();
            for (Map.Entry<String, List<Movie>> entry : moviesByDate.entrySet()) {
                sortedMovies.addAll(entry.getValue());
            }
            this.movies = sortedMovies;
        } else {
            this.movies = movies;
        }
        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.bind(movie);

        // Determine if we should show the createdAt TextView
        if (movie.getCreatedAt() != null && (position == 0 || !Objects.equals(movies.get(position - 1).getCreatedAt(), movie.getCreatedAt()))) {
            holder.createdAtTextView.setText("Añadido el " + movie.getCreatedAt());
            holder.createdAtTextView.setVisibility(View.VISIBLE);
        } else {
            holder.createdAtTextView.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    public void removeMovie(int position) {
        movies.remove(position);
        notifyItemRemoved(position);
    }

    class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ImageView posterImageView;
        private final TextView titleTextView;
        private final TextView overviewTextView;
        private final TextView yearTextView;
        private final TextView voteAverageTextView;
        private final TextView createdAtTextView;
        private final ImageButton deleteButton;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImageView = itemView.findViewById(R.id.poster_image_view);
            titleTextView = itemView.findViewById(R.id.title_text_view);
            overviewTextView = itemView.findViewById(R.id.overview_text_view);
            yearTextView = itemView.findViewById(R.id.year_text_view);
            voteAverageTextView = itemView.findViewById(R.id.vote_average_text_view);
            createdAtTextView = itemView.findViewById(R.id.created_at_text_view);
            deleteButton = itemView.findViewById(R.id.delete_button);

            setupClickListeners();
        }

        private void setupClickListeners() {
            itemView.setOnClickListener(v -> {
                if (!isEditMode) {
                    Movie movie = getMovieAtPosition(getAdapterPosition());
                    if (movie != null) {
                        Intent intent = new Intent(itemView.getContext(), MovieDetailActivity.class);
                        intent.putExtra("movie", movie);

                        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) itemView.getContext(), posterImageView, "moviePosterTransition");

                        itemView.getContext().startActivity(intent, options.toBundle());
                    }
                }
            });

            itemView.setOnLongClickListener(v -> {
                if (longClickListener != null) {
                    return longClickListener.onItemLongClick(getBindingAdapterPosition());
                }
                return false;
            });

            deleteButton.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    deleteClickListener.onDeleteClick(position, movies.get(position));
                }
            });
        }

        @SuppressLint("DefaultLocale")
        public void bind(Movie movie) {
            Glide.with(itemView.getContext()).load("https://image.tmdb.org/t/p/w500" + movie.getPosterPath()).into(posterImageView);
            titleTextView.setText(movie.getTitle());
            yearTextView.setText("(" + movie.getReleaseDate() + ")");
            overviewTextView.setText(movie.getOverview());
            voteAverageTextView.setText(String.format("%.1f", movie.getVoteAverage()));

            // Manejo de la fecha de creación
            if (movie.getCreatedAt() != null) {
                createdAtTextView.setVisibility(View.VISIBLE);
                createdAtTextView.setText("Añadido el " + movie.getCreatedAt());
            } else {
                createdAtTextView.setVisibility(View.GONE);
            }

            deleteButton.setVisibility(isEditMode ? View.VISIBLE : View.GONE);
        }
    }

    public Movie getMovieAtPosition(int position) {
        if (position >= 0 && position < movies.size()) {
            return movies.get(position);
        }
        return null;
    }
}
