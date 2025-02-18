package com.merino.ddfilms.ui;

import static com.merino.ddfilms.model.Credits.Crew.getDirector;
import static com.merino.ddfilms.utils.Utils.showMessage;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.CastAdapter;
import com.merino.ddfilms.adapters.CrewAdapter;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.configuration.ApiKeyManager;
import com.merino.ddfilms.model.Credits;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.MovieDetails;
import com.merino.ddfilms.transitions.DetailsTransition;
import com.merino.ddfilms.ui.fragment.MovieListDialogFragment;

import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity {

    private TMDBService tmdbService;

    private static final String API_KEY = ApiKeyManager.getInstance().getApiKey();

    private static Credits credits;
    private static MovieDetails movieDetails;

    private ImageView backdropImageView;
    private ImageView posterImageView;
    private TextView movieTitle;
    private TextView movieYearDuration;
    private TextView movieOverview;
    private FloatingActionButton addToListButton;
    private TextView movieDirector;
    private TextView duration;
    private ImageButton backButton;
    private RecyclerView castRecyclerView, crewRecyclerView;
    private CastAdapter castAdapter;
    private CrewAdapter crewAdapter;

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
        duration = findViewById(R.id.duration);
        backButton = findViewById(R.id.back_button);

        castRecyclerView = findViewById(R.id.cast_recycler_view);
        crewRecyclerView = findViewById(R.id.crew_recycler_view);

        // Inicializar adaptadores con listas vacías
        castAdapter = new CastAdapter(new ArrayList<>());
        crewAdapter = new CrewAdapter(new ArrayList<>());

        // Configurar RecyclerView para cast
        castRecyclerView.setAdapter(castAdapter);
        castRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Configurar RecyclerView para crew
        crewRecyclerView.setAdapter(crewAdapter);
        crewRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Recuperamos el objeto Movie de los extras
        Movie movie = getIntent().getParcelableExtra("movie");

        getMovieCredits(movie.getId());
        getMovieDetails(movie.getId());

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

    private void getMovieCredits(int id) {
        tmdbService.getMovieCredits(id, API_KEY, "es-ES").enqueue(new Callback<Credits>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<Credits> call, Response<Credits> response) {
                if (response.isSuccessful() && response.body() != null) {
                    credits = response.body();
                    String labelMovieDirector = movieDirector.getText() + getDirector(credits.getCrew());
                    movieDirector.setText(labelMovieDirector);

                    // Actualizar listas de los adaptadores
                    castAdapter.setCastList(credits.getCast());
                    crewAdapter.setCrewList(credits.getCrew());

                    // Notificar cambios a los adaptadores
                    castAdapter.notifyDataSetChanged();
                    crewAdapter.notifyDataSetChanged();

                } else {
                    Log.e("MovieDetailActivity", "Error en la respuesta: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<Credits> call, Throwable t) {
                Log.e("MovieDetailActivity", "Error al obtener créditos de la película", t);
                showMessage(getApplicationContext(), "Error al obtener créditos de la película");
            }
        });
    }

    private void getMovieDetails(int id) {
        tmdbService.getMovieDetails(id, API_KEY, "es-ES").enqueue(new Callback<MovieDetails>() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onResponse(Call<MovieDetails> call, Response<MovieDetails> response) {
                if (response.isSuccessful() && response.body() != null) {
                    movieDetails = response.body();
                    duration.setText(movieDetails.getDuration());
                } else {
                    Log.e("MovieDetailActivity", "Error en la respuesta: " + response.message());
                }
            }

            @Override
            public void onFailure(Call<MovieDetails> call, Throwable t) {
                Log.e("MovieDetailActivity", "Error al obtener detalles de la película", t);
                showMessage(getApplicationContext(), "Error al obtener detalles de la película");
            }
        });
    }
}