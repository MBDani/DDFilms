package com.merino.ddfilms.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityOptionsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.model.Movie;

import java.util.ArrayList;
import java.util.List;

public class MovieAdapter extends RecyclerView.Adapter<MovieAdapter.MovieViewHolder> {
    private static List<Movie> movies = new ArrayList<>();

    public void setMovies(List<Movie> movies) {
        this.movies = movies;
    }

    @NonNull
    @Override
    public MovieViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie, parent, false);
        return new MovieViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MovieViewHolder holder, int position) {
        Movie movie = movies.get(position);
        holder.bind(movie);
    }

    @Override
    public int getItemCount() {
        return movies.size();
    }

    static class MovieViewHolder extends RecyclerView.ViewHolder {
        private final ImageView posterImageView;
        private final TextView titleTextView;
        private final TextView overviewTextView;
        private final TextView yearTextView;
        private final TextView voteAverageTextView;

        public MovieViewHolder(@NonNull View itemView) {
            super(itemView);
            posterImageView = itemView.findViewById(R.id.poster_image_view);
            titleTextView = itemView.findViewById(R.id.title_text_view);
            overviewTextView = itemView.findViewById(R.id.overview_text_view);
            yearTextView = itemView.findViewById(R.id.year_text_view);
            voteAverageTextView = itemView.findViewById(R.id.vote_average_text_view);

            handleClick(itemView);
        }

        private void handleClick(View itemView) {
            itemView.setOnClickListener(v -> {
                // Obtener la película seleccionada
                Movie movie = getMovieAtPosition(getAdapterPosition());

                // Asegurarnos de que no sea null
                if (movie != null) {
                    Intent intent = new Intent(itemView.getContext(), MovieDetailActivity.class);
                    intent.putExtra("movie", movie); // Pasar el objeto de la película

                    // Realizar la animación de transición compartida
                    ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            (Activity) itemView.getContext(),
                            posterImageView, "moviePosterTransition"
                    );

                    // Iniciar la nueva actividad con la animación
                    itemView.getContext().startActivity(intent, options.toBundle());
                }
            });
        }

        @SuppressLint("DefaultLocale")
        public void bind(Movie movie) {
            Glide.with(itemView.getContext())
                    .load("https://image.tmdb.org/t/p/w500" + movie.getPosterPath())
                    .into(posterImageView);
            titleTextView.setText(movie.getTitle());
            yearTextView.setText("(" + movie.getReleaseDate() + ")");
            overviewTextView.setText(movie.getOverview());
            voteAverageTextView.setText(String.format("%.1f", movie.getVoteAverage()));
        }
    }

    // Obtenemos la posición específica
    public static Movie getMovieAtPosition(int position) {
        if (position >= 0 && position < movies.size()) {
            return movies.get(position);
        } else {
            return null;  // Si la posición es inválida, retornamos null
        }
    }
}
