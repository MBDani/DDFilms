package com.merino.ddfilms.ui;

import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.transitions.DetailsTransition;

public class MovieDetailActivity extends AppCompatActivity {

    private ImageView backdropImageView;
    private ImageView posterImageView;
    private TextView movieTitle;
    private TextView movieYearDuration;
    private TextView movieOverview;
    private Button addToListButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        // Inicializamos las vistas
        backdropImageView = findViewById(R.id.backdrop_image_view);
        posterImageView = findViewById(R.id.poster_image_view);
        movieTitle = findViewById(R.id.movie_title);
        movieYearDuration = findViewById(R.id.movie_release_date);
        movieOverview = findViewById(R.id.movie_overview);
        addToListButton = findViewById(R.id.add_to_list_button);

        // Recuperamos el objeto Movie de los extras
        Movie movie = getIntent().getParcelableExtra("movie");

        // Rellenamos los datos en las vistas
        if (movie != null) {

            // Asignar el nombre de la transición
            posterImageView.setTransitionName("moviePosterTransition");
            getWindow().setSharedElementEnterTransition(new DetailsTransition());

            movieTitle.setText(movie.getTitle());
            movieYearDuration.setText("Fecha estreno: " + movie.getReleaseDate());
            movieOverview.setText(movie.getOverview());

            // Usamos Glide para cargar las imágenes
            Glide.with(this)
                    .load("https://image.tmdb.org/t/p/w500/" + movie.getBackdropPath())
                    .into(backdropImageView);
            Glide.with(this)
                    .load("https://image.tmdb.org/t/p/w500/" + movie.getPosterPath())
                    .into(posterImageView);
        }

        // Lógica para el botón "Añadir a la lista"
        addToListButton.setOnClickListener(v -> {
            // Aquí iría la lógica para añadir la película a la lista
        });
    }
}