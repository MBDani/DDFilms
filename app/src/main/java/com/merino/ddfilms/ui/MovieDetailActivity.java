package com.merino.ddfilms.ui;

import static com.merino.ddfilms.model.Credits.Crew.getDirector;
import static com.merino.ddfilms.utils.Utils.showMessage;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.CastAdapter;
import com.merino.ddfilms.adapters.CrewAdapter;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.configuration.ApiKeyManager;
import com.merino.ddfilms.model.Credits;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.MovieDetails;
import com.merino.ddfilms.transitions.DetailsTransition;
import com.merino.ddfilms.ui.fragment.MovieListDialogFragment;
import com.merino.ddfilms.ui.fragment.WriteReviewDialogFragment;
import com.merino.ddfilms.ui.utils.CustomFabMenu;
import com.merino.ddfilms.utils.ReviewUtil;

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
    private TextView movieDirector;
    private TextView duration;
    private ImageButton backButton;
    private RecyclerView castRecyclerView, crewRecyclerView, reviewsRecyclerView;
    private CastAdapter castAdapter;
    private CrewAdapter crewAdapter;
    private Movie currentMovie;
    private FirebaseManager firebaseManager = new FirebaseManager();
    private String userId;
    private String userName;
    private CustomFabMenu fabMenu;
    private NestedScrollView nestedScrollView;
    private AppBarLayout appBarLayout;
    private ReviewUtil reviewUtil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService.class);
        reviewUtil = new ReviewUtil(getApplicationContext());

        // Inicializamos las vistas
        initViews();
        setUpButtons();
        setupRecyclerViews();

        // Recuperamos el objeto Movie de los extras
        Movie movie = getIntent().getParcelableExtra("movie");

        getMovieCredits(movie.getId());
        getMovieDetails(movie.getId());

        // Recuperamos el objeto Movie de los extras
        currentMovie = getIntent().getParcelableExtra("movie");

        if (currentMovie != null) {
            getMovieCredits(currentMovie.getId());
            getMovieDetails(currentMovie.getId());
            initReviews();
            setupMovieData(currentMovie);
        }
        getUserData();
        setupCustomFabMenu();
    }

    private void initReviews() {
        reviewUtil.setScrollTargets(appBarLayout, nestedScrollView, reviewsRecyclerView);
        reviewUtil.setCurrentMovie(currentMovie);
        reviewUtil.loadMovieReviews(currentMovie.getId());
    }

    private void getUserData() {
        userId = firebaseManager.getCurrentUserUID();
        firebaseManager.getUserName(userId, (userName, error) -> this.userName = userName);
        if (reviewUtil != null) reviewUtil.setUserName(userName);
    }

    private void initViews() {
        backdropImageView = findViewById(R.id.backdrop_image_view);
        posterImageView = findViewById(R.id.poster_image_view);
        movieTitle = findViewById(R.id.movie_title);
        movieYearDuration = findViewById(R.id.movie_release_date);
        movieOverview = findViewById(R.id.movie_overview);
        movieOverview.setMovementMethod(new ScrollingMovementMethod());
        movieDirector = findViewById(R.id.movie_director);
        duration = findViewById(R.id.duration);
        backButton = findViewById(R.id.back_button);
        nestedScrollView = findViewById(R.id.nested_scroll_view);
        appBarLayout = findViewById(R.id.appbar_layout);

        castRecyclerView = findViewById(R.id.cast_recycler_view);
        crewRecyclerView = findViewById(R.id.crew_recycler_view);
        reviewsRecyclerView = findViewById(R.id.reviews_recycler_view);
    }

    private void setUpButtons() {
        backButton.setOnClickListener(v -> {
            onBackPressed();
        });
    }

    private void setupRecyclerViews() {
        // Inicializar adaptadores con listas vacías
        castAdapter = new CastAdapter(new ArrayList<>());
        crewAdapter = new CrewAdapter(new ArrayList<>());

        // Configurar RecyclerView para cast
        castRecyclerView.setAdapter(castAdapter);
        castRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Configurar RecyclerView para crew
        crewRecyclerView.setAdapter(crewAdapter);
        crewRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Configurar RecyclerView para reviews
        reviewsRecyclerView.setAdapter(reviewUtil.getReviewAdapter());
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewsRecyclerView.setNestedScrollingEnabled(false);
    }

    private void setupMovieData(Movie movie) {
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

    private void setupCustomFabMenu() {
        ViewGroup container = findViewById(R.id.coordinator_layout_main_container);

        fabMenu = new CustomFabMenu(this, container)
                .setMainFabIcon(R.drawable.ic_menu)
                .setMainFabColor(R.color.primary_dark_light)
                .setOverlayColor(R.color.black_50)
                .setLabelBackground(R.drawable.fab_label_background)
                .setBaseMarginBottom(40f)
                .setItemSpacing(64f)

                // Agregar items del menú
                .addMenuItem(
                        R.drawable.ic_review,
                        getString(R.string.write_review),
                        R.color.accent_orange,
                        getString(R.string.add_review),
                        this::openWriteReviewActivity
                )
                .addMenuItem(
                        R.drawable.ic_add,
                        getString(R.string.add_to_list),
                        R.color.accent_red,
                        getString(R.string.add_to_list),
                        this::showAddToListDialog
                )
                .addMenuItem(
                        R.drawable.ic_bookmark,
                        getString(R.string.pending_to_watch),
                        R.color.blue_500,
                        getString(R.string.add_to_watchlist),
                        this::addToWatchlist
                )
                .addMenuItem(
                        R.drawable.ic_visibility,
                        getString(R.string.watched),
                        R.color.green_500,
                        getString(R.string.mark_as_watched),
                        this::markAsWatched
                );

        fabMenu.build();
    }

    private void openWriteReviewActivity() {
        WriteReviewDialogFragment dialog = new WriteReviewDialogFragment(currentMovie, reviewUtil.getUserReview());
        dialog.setOnReviewSubmittedListener(reviewUtil);
        dialog.show(getSupportFragmentManager(), "WriteReviewDialog");
    }

    private void showAddToListDialog() {
        MovieListDialogFragment dialog = new MovieListDialogFragment(currentMovie);
        dialog.show(getSupportFragmentManager(), "MovieListDialog");
    }

    private void addToWatchlist() {
        firebaseManager.addMovieToWatchList(currentMovie, (result, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
            } else {
                showMessage(getApplicationContext(), result);
            }
        });
    }

    private void markAsWatched() {
        showMessage(getApplicationContext(), "Esta en proceso... \nDame tiempo Daniela 😭");
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