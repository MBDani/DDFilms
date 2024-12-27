package com.merino.ddfilms.ui;

import static com.merino.ddfilms.model.Credits.Crew.getDirector;
import static com.merino.ddfilms.utils.Utils.showMessage;

import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.configuration.ApiKeyManager;
import com.merino.ddfilms.model.Credits;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.transitions.DetailsTransition;
import com.merino.ddfilms.ui.fragment.MovieListDialogFragment;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity {

    private TMDBService tmdbService;

    private static final String API_KEY = ApiKeyManager.getInstance().getApiKey();

    private static Credits credits;

    private ImageView backdropImageView;
    private ImageView posterImageView;
    private TextView movieTitle;
    private TextView movieYearDuration;
    private TextView movieOverview;
    private Button addToListButton;
    private TextView movieDirector;

    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService.class);

        // Inicializamos las vistas
        backdropImageView = findViewById(R.id.backdrop_image_view);
        posterImageView = findViewById(R.id.poster_image_view);
        movieTitle = findViewById(R.id.movie_title);
        movieYearDuration = findViewById(R.id.movie_release_date);
        movieOverview = findViewById(R.id.movie_overview);
        movieOverview.setMovementMethod(new ScrollingMovementMethod());
        addToListButton = findViewById(R.id.add_to_list_button);
        movieDirector = findViewById(R.id.movie_director);
        backButton = findViewById(R.id.back_button);

        // Recuperamos el objeto Movie de los extras
        Movie movie = getIntent().getParcelableExtra("movie");

        getDetailMovie(movie.getId());

        // Rellenamos los datos en las vistas
        if (movie != null) {

            // Asignar el nombre de la transición
            posterImageView.setTransitionName("moviePosterTransition");
            getWindow().setSharedElementEnterTransition(new DetailsTransition());

            movieTitle.setText(movie.getTitle());
            String movieYearDurationText = movieYearDuration.getText() + movie.getReleaseDate();
            movieYearDuration.setText(movieYearDurationText);
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
            MovieListDialogFragment dialog = new MovieListDialogFragment(movie);
            dialog.show(getSupportFragmentManager(), "MovieListDialog");
        });

        backButton.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void getDetailMovie(int id) {
        tmdbService.getMovieDetails(id, API_KEY, "en-US").enqueue(new Callback<Credits>() {
            @Override
            public void onResponse(Call<Credits> call, Response<Credits> response) {
                if (response.isSuccessful() && response.body() != null) {
                    credits = response.body();
                    Log.d("MovieDetailActivity", "Cast: " + credits.getCast());
                    Log.d("MovieDetailActivity", "Crew: " + credits.getCrew());

                    String labelMovieDirector = movieDirector.getText() + getDirector(credits.getCrew());
                    movieDirector.setText(labelMovieDirector);
                } else {
                    Log.e("MovieDetailActivity", "Error en la respuesta: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Credits> call, Throwable t) {
                Log.e("MovieDetailActivity", "Error al obtener detalles de la película", t);
                showMessage(getApplicationContext(), "Error al obtener detalles de la película");
            }
        });
    }
}