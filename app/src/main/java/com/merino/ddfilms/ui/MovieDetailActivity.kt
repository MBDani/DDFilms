package com.merino.ddfilms.ui

import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.api.TMDBClient
import com.merino.ddfilms.api.TMDBService
import com.merino.ddfilms.configuration.ApiKeyManager
import com.merino.ddfilms.model.Credits
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.MovieDetails
import com.merino.ddfilms.model.Review
import com.merino.ddfilms.ui.components.CinematicRatingBar
import com.merino.ddfilms.ui.components.MoviePosterCard
import com.merino.ddfilms.ui.fragment.MovieListDialogFragment
import com.merino.ddfilms.ui.fragment.WriteReviewDialogFragment
import com.merino.ddfilms.ui.theme.CinematicTheme
import com.merino.ddfilms.utils.ReviewUtil
import com.merino.ddfilms.utils.StringUtils.DIARY_LIST
import com.merino.ddfilms.utils.StringUtils.WATCH_LIST
import com.merino.ddfilms.utils.Utils.showMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var tmdbService: TMDBService
    private val firebaseManager = FirebaseManager.getInstance()
    private lateinit var reviewUtil: ReviewUtil

    private var currentMovie: Movie? = null
    private var userId: String? = null
    private var userName: String? = null

    // Compose states
    private val movieState = mutableStateOf<Movie?>(null)
    private val detailsState = mutableStateOf<MovieDetails?>(null)
    private val creditsState = mutableStateOf<Credits?>(null)
    private val directorState = mutableStateOf<String?>(null)
    private val reviewsListState = mutableStateOf<List<Review>>(emptyList())
    private val isLoadingState = mutableStateOf(true)

    companion object {
        private val API_KEY = ApiKeyManager.getInstance().apiKey ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportPostponeEnterTransition()
        window.sharedElementEnterTransition = com.merino.ddfilms.transitions.DetailsTransition()
        window.sharedElementReturnTransition = com.merino.ddfilms.transitions.DetailsTransition()

        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService::class.java)
        reviewUtil = ReviewUtil(this)

        currentMovie = intent.getParcelableExtra("movie")
        movieState.value = currentMovie

        userId = firebaseManager.getCurrentUserUID()
        userId?.let { uid ->
            firebaseManager.getUserName(uid) { name, _ ->
                userName = name
                reviewUtil.userName = name
            }
        }

        // Create the poster ImageView OUTSIDE of Compose — owned by the Activity
        // This is the shared element target. It must be a stable, long-lived view
        // that the transition framework can manage without Compose interfering.
        val posterImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            transitionName = "moviePosterTransition"
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(v: View, outline: android.graphics.Outline) {
                    val radius = 12f * resources.displayMetrics.density
                    outline.setRoundRect(0, 0, v.width, v.height, radius)
                }
            }
            visibility = View.INVISIBLE // Start invisible, will be shown by transition
        }

        // Load the poster image with Glide, then start the postponed transition
        val posterPath = currentMovie?.posterPath
        if (!posterPath.isNullOrEmpty()) {
            val posterUrl = "https://image.tmdb.org/t/p/w500$posterPath"
            com.bumptech.glide.Glide.with(this)
                .load(posterUrl)
                .placeholder(R.drawable.placeholder_poster)
                .error(R.drawable.placeholder_poster)
                .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        posterImageView.visibility = View.VISIBLE
                        supportStartPostponedEnterTransition()
                        return false
                    }

                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                        dataSource: com.bumptech.glide.load.DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        posterImageView.visibility = View.VISIBLE
                        supportStartPostponedEnterTransition()
                        return false
                    }
                })
                .into(posterImageView)
        } else {
            posterImageView.setImageResource(R.drawable.placeholder_poster)
            posterImageView.visibility = View.VISIBLE
            supportStartPostponedEnterTransition()
        }

        // Build the layout: ComposeView + poster ImageView overlaid on top
        val density = resources.displayMetrics.density
        val composeView = androidx.compose.ui.platform.ComposeView(this).apply {
            setContent {
                CinematicTheme {
                    MovieDetailScreen(
                        movie = movieState.value,
                        details = detailsState.value,
                        credits = creditsState.value,
                        director = directorState.value,
                        reviews = reviewsListState.value,
                        isLoading = isLoadingState.value,
                        currentUserId = userId,
                        onBackClick = { supportFinishAfterTransition() },
                        onWriteReviewClick = { openWriteReviewActivity() },
                        onAddToListClick = { showAddToListDialog() },
                        onWatchlistClick = { addToWatchlist() },
                        onWatchedClick = { markAsWatched() },
                        onLikeClick = { review ->
                            val index = reviewUtil.reviewsList.indexOfFirst { it.id == review.id }
                            if (index != -1) {
                                reviewUtil.onLikeClicked(review, index)
                            }
                        },
                        onDislikeClick = { review ->
                            val index = reviewUtil.reviewsList.indexOfFirst { it.id == review.id }
                            if (index != -1) {
                                reviewUtil.onDislikeClicked(review, index)
                            }
                        },
                        onDeleteClick = { review ->
                            reviewUtil.onReviewDeleted(review)
                        },
                        onReviewClick = { review ->
                            reviewUtil.onReviewClicked(review, window.decorView)
                        },
                        onPosterResourceReady = {
                            // No-op: transition is started from Glide listener above
                        }
                    )
                }
            }
        }

        // FrameLayout with ComposeView filling the screen + poster overlaid
        val rootLayout = android.widget.FrameLayout(this).apply {
            addView(composeView, android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            ))

            // Position the poster at (16dp from left, 220dp from top) with size 110x165dp
            val posterWidth = (110 * density).toInt()
            val posterHeight = (165 * density).toInt()
            val posterMarginLeft = (16 * density).toInt()
            val posterMarginTop = (220 * density).toInt()  // 260dp backdrop - 40dp overlap offset
            val posterParams = android.widget.FrameLayout.LayoutParams(posterWidth, posterHeight).apply {
                leftMargin = posterMarginLeft
                topMargin = posterMarginTop
            }
            addView(posterImageView, posterParams)
        }

        setContentView(rootLayout)

        val movie = currentMovie
        if (movie != null) {
            getMovieDetails(movie.id)
            getMovieCredits(movie.id)
            initReviews(movie)
        }
    }

    private fun initReviews(movie: Movie) {
        reviewUtil.currentMovie = movie
        val highlightReviewId = intent.getStringExtra("highlight_review_id")
        if (highlightReviewId != null) {
            reviewUtil.highlightReviewId = highlightReviewId
        }
        reviewUtil.setOnReviewsLoadedListener {
            reviewsListState.value = reviewUtil.reviewsList.toList()
            checkLoadingState()
        }
        reviewUtil.loadMovieReviews(movie.id)

        // Sync review changes back to Compose state
        reviewUtil.reviewAdapter.registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                reviewsListState.value = reviewUtil.reviewsList.toList()
            }
        })
    }

    private fun checkLoadingState() {
        if (detailsState.value != null && creditsState.value != null) {
            isLoadingState.value = false
        }
    }

    private fun getMovieDetails(id: Int) {
        tmdbService.getMovieDetails(id, API_KEY, getString(R.string.tmdb_api_language)).enqueue(object : Callback<MovieDetails> {
            override fun onResponse(call: Call<MovieDetails>, response: Response<MovieDetails>) {
                if (response.isSuccessful && response.body() != null) {
                    val details = response.body()!!
                    detailsState.value = details
                    movieState.value?.apply {
                        title = details.title
                        overview = details.overview
                        releaseDate = details.releaseDate
                        backdropPath = details.backdropPath
                    }
                    reviewUtil.currentMovie = movieState.value
                }
                checkLoadingState()
            }

            override fun onFailure(call: Call<MovieDetails>, t: Throwable) {
                checkLoadingState()
            }
        })
    }

    private fun getMovieCredits(id: Int) {
        tmdbService.getMovieCredits(id, API_KEY, getString(R.string.tmdb_api_language)).enqueue(object : Callback<Credits> {
            override fun onResponse(call: Call<Credits>, response: Response<Credits>) {
                if (response.isSuccessful && response.body() != null) {
                    val resCredits = response.body()!!
                    creditsState.value = resCredits
                    directorState.value = Credits.Crew.getDirector(resCredits.crew)
                }
                checkLoadingState()
            }

            override fun onFailure(call: Call<Credits>, t: Throwable) {
                checkLoadingState()
            }
        })
    }

    private fun openWriteReviewActivity() {
        val dialog = WriteReviewDialogFragment(movieState.value, reviewUtil.userReview)
        dialog.setOnReviewSubmittedListener(reviewUtil)
        dialog.show(supportFragmentManager, "WriteReviewDialog")
    }

    private fun showAddToListDialog() {
        val dialog = MovieListDialogFragment(movieState.value!!)
        dialog.show(supportFragmentManager, "MovieListDialog")
    }

    private fun addToWatchlist() {
        firebaseManager.addMovieToWatchOrDiaryList(WATCH_LIST, movieState.value!!) { result, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else {
                showMessage(applicationContext, result)
            }
        }
    }

    private fun markAsWatched() {
        firebaseManager.addMovieToWatchOrDiaryList(DIARY_LIST, movieState.value!!) { result, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else {
                showMessage(applicationContext, result)
                deleteMovieFromWatchList()
            }
        }
    }

    private fun findViewByTransitionName(view: View, transitionName: String): View? {
        if (view.transitionName == transitionName) return view
        if (view is android.view.ViewGroup) {
            for (i in 0 until view.childCount) {
                val found = findViewByTransitionName(view.getChildAt(i), transitionName)
                if (found != null) return found
            }
        }
        return null
    }

    private fun deleteMovieFromWatchList() {
        firebaseManager.deleteMovieFromList(WATCH_LIST, userId!!, movieState.value!!) { _, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            }
        }
    }
}

@Composable
fun MovieDetailScreen(
    movie: Movie?,
    details: MovieDetails?,
    credits: Credits?,
    director: String?,
    reviews: List<Review>,
    isLoading: Boolean,
    currentUserId: String?,
    onBackClick: () -> Unit,
    onWriteReviewClick: () -> Unit,
    onAddToListClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onLikeClick: (Review) -> Unit,
    onDislikeClick: (Review) -> Unit,
    onDeleteClick: (Review) -> Unit,
    onReviewClick: (Review) -> Unit,
    onPosterResourceReady: () -> Unit
) {
    if (movie == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 96.dp)
            ) {
            // Backdrop Header
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    val backdropUrl = if (!movie.backdropPath.isNullOrEmpty()) {
                        "https://image.tmdb.org/t/p/w780${movie.backdropPath}"
                    } else if (!movie.posterPath.isNullOrEmpty()) {
                        "https://image.tmdb.org/t/p/w500${movie.posterPath}"
                    } else null

                    AsyncImage(
                        model = backdropUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        placeholder = painterResource(R.drawable.placeholder_poster),
                        error = painterResource(R.drawable.placeholder_poster)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    )

                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .statusBarsPadding()
                            .padding(16.dp)
                            .size(40.dp)
                            .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_arrow_back),
                            contentDescription = "Atrás",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Poster & Title Info Overlapped
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-40).dp)
                ) {
                    // Poster space — the actual poster ImageView is overlaid
                    // by the Activity outside of Compose for stable transitions
                    Spacer(
                        modifier = Modifier
                            .width(110.dp)
                            .height(165.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .align(Alignment.Bottom)
                            .padding(bottom = 8.dp)
                    ) {
                        Text(
                            text = movie.title ?: "",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            fontSize = 22.sp
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val year = if (!movie.releaseDate.isNullOrEmpty() && movie.releaseDate!!.length >= 4) {
                            movie.releaseDate!!.substring(0, 4)
                        } else ""
                        val durationText = details?.getDuration() ?: ""
                        val metaText = year + if (durationText.isNotEmpty()) "  •  $durationText" else ""

                        Text(
                            text = metaText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "Director: ${director ?: "Cargando..."}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Overview Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .offset(y = (-20).dp)
                ) {
                    Text(
                        text = "Sinopsis",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = movie.overview ?: "No hay sinopsis disponible.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )
                }
            }

            // Cast List Horizontal
            if (credits?.cast?.isNotEmpty() == true) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .offset(y = (-10).dp)
                    ) {
                        Text(
                            text = "Reparto Principal",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(credits.cast ?: emptyList()) { castMember ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(75.dp)
                                ) {
                                    val imageUrl = if (!castMember.profilePath.isNullOrEmpty()) {
                                        "https://image.tmdb.org/t/p/w185${castMember.profilePath}"
                                    } else null

                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray),
                                        placeholder = painterResource(R.drawable.ic_default_profile),
                                        error = painterResource(R.drawable.ic_default_profile)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = castMember.name ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = castMember.character ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Crew List Horizontal
            if (credits?.crew?.isNotEmpty() == true) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .offset(y = (-10).dp)
                    ) {
                        Text(
                            text = "Equipo Técnico",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            ),
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(credits.crew ?: emptyList()) { crewMember ->
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(75.dp)
                                ) {
                                    val imageUrl = if (!crewMember.profilePath.isNullOrEmpty()) {
                                        "https://image.tmdb.org/t/p/w185${crewMember.profilePath}"
                                    } else null

                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(70.dp)
                                            .clip(CircleShape)
                                            .background(Color.LightGray),
                                        placeholder = painterResource(R.drawable.ic_default_profile),
                                        error = painterResource(R.drawable.ic_default_profile)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = crewMember.name ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = crewMember.job ?: "",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Reviews list header
            item {
                Text(
                    text = "Reseñas de la Comunidad",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
                )
            }

            if (reviews.isEmpty()) {
                item {
                    Text(
                        text = "Sé el primero en escribir una reseña.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                items(reviews) { review ->
                    ReviewItemCard(
                        review = review,
                        currentUserId = currentUserId,
                        onLikeClick = onLikeClick,
                        onDislikeClick = onDislikeClick,
                        onDeleteClick = onDeleteClick,
                        onReviewClick = onReviewClick,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Floating Quick Actions Bar (Glassmorphic card aligned to bottom)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .widthIn(max = 380.dp)
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxSize()
                ) {
                    IconButton(onClick = onWriteReviewClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_review),
                            contentDescription = "Reseñar",
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onAddToListClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Guardar",
                            tint = Color(0xFFE91E63),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onWatchlistClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_bookmark),
                            contentDescription = "Watchlist",
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = onWatchedClick) {
                        Icon(
                            painter = painterResource(R.drawable.ic_visibility),
                            contentDescription = "Visto",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun ReviewItemCard(
    review: Review,
    currentUserId: String?,
    onLikeClick: (Review) -> Unit,
    onDislikeClick: (Review) -> Unit,
    onDeleteClick: (Review) -> Unit,
    onReviewClick: (Review) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier.clickable { onReviewClick(review) }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = review.userProfileImageUrl ?: R.drawable.ic_default_profile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = review.userName ?: "Usuario",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = review.reviewDate ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                val reviewRating = review.rating ?: 0f
                CinematicRatingBar(
                    rating = reviewRating,
                    onRatingChanged = {},
                    starSize = 14.dp,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.reviewText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val liked = review.isLikedByCurrentUser
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onLikeClick(review) }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_thumb_up),
                            contentDescription = "Me gusta",
                            tint = if (liked) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (review.likeCount?.size ?: 0).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (liked) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    val disliked = review.isDislikedByCurrentUser
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onDislikeClick(review) }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_thumb_down),
                            contentDescription = "No me gusta",
                            tint = if (disliked) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = (review.dislikeCount?.size ?: 0).toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (disliked) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }

                if (review.userId == currentUserId) {
                    IconButton(
                        onClick = { onDeleteClick(review) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = "Eliminar reseña",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
