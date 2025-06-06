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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.CastAdapter;
import com.merino.ddfilms.adapters.CrewAdapter;
import com.merino.ddfilms.adapters.ReviewAdapter;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.configuration.ApiKeyManager;
import com.merino.ddfilms.model.Credits;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.MovieDetails;
import com.merino.ddfilms.model.Review;
import com.merino.ddfilms.transitions.DetailsTransition;
import com.merino.ddfilms.ui.fragment.MovieListDialogFragment;
import com.merino.ddfilms.ui.fragment.WriteReviewDialogFragment;
import com.merino.ddfilms.ui.utils.CustomFabMenu;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MovieDetailActivity extends AppCompatActivity implements
        WriteReviewDialogFragment.OnReviewSubmittedListener,
        ReviewAdapter.OnReviewInteractionListener {

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
    private ReviewAdapter reviewAdapter;
    private Movie currentMovie;
    private List<Review> reviewsList = new ArrayList<>();
    private FirebaseManager firebaseManager = new FirebaseManager();
    private String userId;
    private String userName;
    private Review userReview;
    private CustomFabMenu fabMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_detail);

        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService.class);

        // Inicializamos las vistas
        initViews();
//        initializeFabMenu();
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
            loadMovieReviews(currentMovie.getId());
            setupMovieData(currentMovie);
        }
        getUserData();
        setupCustomFabMenu();
    }

    private void getUserData() {
        userId = firebaseManager.getCurrentUserUID();
        firebaseManager.getUserName(userId, (userName, error) -> this.userName = userName);
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

        castRecyclerView = findViewById(R.id.cast_recycler_view);
        crewRecyclerView = findViewById(R.id.crew_recycler_view);
        reviewsRecyclerView = findViewById(R.id.reviews_recycler_view);
    }

    private void setupRecyclerViews() {
        // Inicializar adaptadores con listas vacías
        castAdapter = new CastAdapter(new ArrayList<>());
        crewAdapter = new CrewAdapter(new ArrayList<>());
        reviewAdapter = new ReviewAdapter(reviewsList, this);

        // Configurar RecyclerView para cast
        castRecyclerView.setAdapter(castAdapter);
        castRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Configurar RecyclerView para crew
        crewRecyclerView.setAdapter(crewAdapter);
        crewRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Configurar RecyclerView para reviews
        reviewsRecyclerView.setAdapter(reviewAdapter);
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
                .setMainFabColor(R.color.primary_dark)
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
        WriteReviewDialogFragment dialog = new WriteReviewDialogFragment(currentMovie, userReview);
        dialog.setOnReviewSubmittedListener(this);
        dialog.show(getSupportFragmentManager(), "WriteReviewDialog");
    }

    private void showAddToListDialog() {
        MovieListDialogFragment dialog = new MovieListDialogFragment(currentMovie);
        dialog.show(getSupportFragmentManager(), "MovieListDialog");
    }

    private void addToWatchlist() {
        showMessage(getApplicationContext(),"Esta en proceso... \nDame tiempo Daniela 😭");
    }

    private void markAsWatched() {
        showMessage(getApplicationContext(),"Esta en proceso... \nDame tiempo Daniela 😭");
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

    @SuppressLint("NotifyDataSetChanged")
    private void loadMovieReviews(Integer movieId) {
        firebaseManager.getReviews(movieId, (reviews, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
            } else if (reviews != null) {
                getCurrenUserReview(reviews);
                setUpLikeAndDislikeReviews(reviews);
                reviewsList.addAll(reviews.stream()
                        .sorted((r1, r2) -> r2.getReviewDate().compareTo(r1.getReviewDate()))
                        .collect(Collectors.toList()));
                reviewAdapter.notifyDataSetChanged();
            }
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

    private void setUpLikeAndDislikeReviews(List<Review> reviews) {
        for (Review review : reviews) {
            review.setLikedByCurrentUser(review.getLikeCount().contains(userId));
            review.setDislikedByCurrentUser(review.getDislikeCount().contains(userId));
        }
    }

    @Override
    public void onReviewSubmitted(Review review) {
        if (review.getId() != null) {
            updateReview(review);
        } else {
            postReview(review);
        }
    }

    private void postReview(Review review) {
        review.setUserId(userId);
        review.setUserName(userName);
        review.setMovieId(currentMovie.getId());
        review.setReviewDate(new Date().toString());
        review.setLikeCount(new ArrayList<>());
        review.setDislikeCount(new ArrayList<>());

        firebaseManager.postReview(review, (reviewResponse, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
                return;
            }
            addReviewToRecycler(reviewResponse);
        });
    }

    private void updateReview(Review review) {
        review.setReviewDate(new Date().toString());
        review.setLikeCount(new ArrayList<>());
        review.setDislikeCount(new ArrayList<>());

        firebaseManager.updateReview(review, (reviewResponse, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
                return;
            }
            addReviewToRecycler(reviewResponse);
        });
    }

    public void addReviewToRecycler(Review review) {
        if (userReview != null)
            reviewsList.remove(userReview);

        reviewsList.add(0, review);
        reviewAdapter.setReviewList(reviewsList);
        // Hacer scroll al principio para mostrar la nueva reseña
        reviewsRecyclerView.scrollToPosition(0);
        userReview = review;
    }

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

    private void addDislike(Review review, int position) {
        firebaseManager.reviewAddDislike(review.getId(), userId, (result, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
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
                showMessage(getApplicationContext(), error.getMessage());
                return;
            }
            review.getLikeCount().add(this.userId);
            review.setLikedByCurrentUser(true);

            // Si tenía dislike, quitarlo
            if (review.getDislikeCount().contains(userId)) {
                removeDislike(review, position);
            } else {
                reviewAdapter.updateReview(position, review);
                reviewAdapter.notifyItemChanged(position);
            }
        });
    }

    private void removeDislike(Review review, int position) {
        firebaseManager.reviewRemoveDislike(review.getId(), userId, (result, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
                return;
            }
            review.getDislikeCount().remove(this.userId);
            review.setDislikedByCurrentUser(false);
            reviewAdapter.updateReview(position, review);
            reviewAdapter.notifyItemChanged(position);
        });
    }

    private void removeLike(Review review, int position) {
        firebaseManager.reviewRemoveLike(review.getId(), userId, (result, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
                return;
            }
            review.getLikeCount().remove(this.userId);
            review.setLikedByCurrentUser(false);
            reviewAdapter.notifyItemChanged(position);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (reviewsList != null) {
            reviewsList.clear();
        }
    }
}