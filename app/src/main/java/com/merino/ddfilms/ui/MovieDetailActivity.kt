package com.merino.ddfilms.ui

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.widget.NestedScrollView
import coil.compose.AsyncImage
import com.bumptech.glide.Glide
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
import com.merino.ddfilms.ui.fragment.MovieListDialogFragment
import com.merino.ddfilms.ui.fragment.WriteReviewDialogFragment
import com.merino.ddfilms.ui.theme.CinematicTheme
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
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

    // Native Views for Header (outside Compose to prevent recomposition disposal during transitions)
    private lateinit var backdropImageView: ImageView
    private lateinit var posterImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var metaTextView: TextView
    private lateinit var directorTextView: TextView

    // Compose states for body content
    private val movieState = mutableStateOf<Movie?>(null)
    private val detailsState = mutableStateOf<MovieDetails?>(null)
    private val creditsState = mutableStateOf<Credits?>(null)
    private val directorState = mutableStateOf<String?>(null)
    private val reviewsListState = mutableStateOf<List<Review>>(emptyList())
    private val isLoadingState = mutableStateOf(true)
    private val isProcessingActionState = mutableStateOf(false)

    companion object {
        private val API_KEY = ApiKeyManager.getInstance().apiKey ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportPostponeEnterTransition()

        window.sharedElementEnterTransition = com.merino.ddfilms.transitions.DetailsTransition()
        window.sharedElementReturnTransition = com.merino.ddfilms.transitions.DetailsTransition()

        setEnterSharedElementCallback(object : androidx.core.app.SharedElementCallback() {
            override fun onSharedElementEnd(
                sharedElementNames: MutableList<String>?,
                sharedElements: MutableList<View>?,
                sharedElementSnapshots: MutableList<View>?
            ) {
                sharedElements?.forEach { view ->
                    view.visibility = View.VISIBLE
                    view.alpha = 1f
                }
            }
        })

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

        val density = resources.displayMetrics.density

        val bgColor = ContextCompat.getColor(this, R.color.primary_dark)
        val textPrimaryColor = ContextCompat.getColor(this, R.color.text_primary)
        val textSecondaryColor = ContextCompat.getColor(this, R.color.text_secondary)
        val accentColor = ContextCompat.getColor(this, R.color.gold_dark)

        val rootFrameLayout = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(bgColor)
        }

        // Root Scroll Container
        val nestedScrollView = NestedScrollView(this).apply {
            isFillViewport = true
            setBackgroundColor(bgColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        // 1. Header FrameLayout (Backdrop)
        val headerHeight = (260 * density).toInt()
        val headerFrameLayout = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                headerHeight
            )
        }

        backdropImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            transitionName = "movieBackdropTransition"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val midBgColor = AndroidColor.argb(128, AndroidColor.red(bgColor), AndroidColor.green(bgColor), AndroidColor.blue(bgColor))
        val endBgColor = AndroidColor.argb(255, AndroidColor.red(bgColor), AndroidColor.green(bgColor), AndroidColor.blue(bgColor))

        val gradientView = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    AndroidColor.TRANSPARENT,
                    midBgColor,
                    endBgColor
                )
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val backButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_arrow_back)
            setColorFilter(AndroidColor.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(AndroidColor.argb(128, 0, 0, 0))
            }
            elevation = 16f * density
            val p = (10 * density).toInt()
            setPadding(p, p, p, p)
            setOnClickListener { supportFinishAfterTransition() }

            val size = (40 * density).toInt()
            val margin = (16 * density).toInt()
            val statusBarPadding = (24 * density).toInt()
            val params = FrameLayout.LayoutParams(size, size).apply {
                leftMargin = margin
                topMargin = margin + statusBarPadding
            }
            layoutParams = params
        }

        headerFrameLayout.addView(backdropImageView)
        headerFrameLayout.addView(gradientView)

        // 2. Poster & Info Header Row (LinearLayout Horizontal)
        val posterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val margin = (16 * density).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = margin
                rightMargin = margin
                topMargin = (-40 * density).toInt()
            }
            layoutParams = params
        }

        val posterW = (110 * density).toInt()
        val posterH = (165 * density).toInt()
        posterImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            transitionName = "moviePosterTransition"
            elevation = 8f * density
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(v: View, outline: android.graphics.Outline) {
                    val radius = 12f * density
                    outline.setRoundRect(0, 0, v.width, v.height, radius)
                }
            }
            layoutParams = LinearLayout.LayoutParams(posterW, posterH)
        }

        val infoContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val params = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                leftMargin = (16 * density).toInt()
                gravity = Gravity.BOTTOM
            }
            layoutParams = params
        }

        titleTextView = TextView(this).apply {
            text = currentMovie?.title ?: ""
            textSize = 22f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(textPrimaryColor)
        }

        metaTextView = TextView(this).apply {
            text = ""
            textSize = 14f
            setTextColor(textSecondaryColor)
        }

        directorTextView = TextView(this).apply {
            text = "Director: Cargando..."
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(accentColor)
        }

        infoContainer.addView(titleTextView)
        infoContainer.addView(metaTextView)
        infoContainer.addView(directorTextView)

        posterRow.addView(posterImageView)
        posterRow.addView(infoContainer)

        // 3. ComposeView for Body Content
        val composeView = ComposeView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setContent {
                CinematicTheme {
                    MovieDetailBody(
                        details = detailsState.value,
                        credits = creditsState.value,
                        reviews = reviewsListState.value,
                        isLoading = isLoadingState.value,
                        isProcessingAction = isProcessingActionState.value,
                        currentUserId = userId,
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
                        }
                    )
                }
            }
        }

        mainContainer.addView(headerFrameLayout)
        mainContainer.addView(posterRow)
        mainContainer.addView(composeView)

        nestedScrollView.addView(mainContainer)

        rootFrameLayout.addView(nestedScrollView)
        rootFrameLayout.addView(backButton)

        setContentView(rootFrameLayout)

        // Load images with Glide and start postponed transition when poster is ready
        val posterPath = currentMovie?.posterPath
        if (!posterPath.isNullOrEmpty()) {
            val posterUrl = "https://image.tmdb.org/t/p/w500$posterPath"
            Glide.with(this)
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
                        supportStartPostponedEnterTransition()
                        return false
                    }
                })
                .into(posterImageView)
        } else {
            posterImageView.setImageResource(R.drawable.placeholder_poster)
            supportStartPostponedEnterTransition()
        }

        val backdropPath = currentMovie?.backdropPath ?: currentMovie?.posterPath
        if (!backdropPath.isNullOrEmpty()) {
            val backdropUrl = "https://image.tmdb.org/t/p/w780$backdropPath"
            Glide.with(this)
                .load(backdropUrl)
                .placeholder(R.drawable.placeholder_poster)
                .error(R.drawable.placeholder_poster)
                .into(backdropImageView)
        } else {
            backdropImageView.setImageResource(R.drawable.placeholder_poster)
        }

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

                    titleTextView.text = details.title ?: currentMovie?.title ?: ""
                    val relDate = details.releaseDate
                    val year = if (!relDate.isNullOrEmpty() && relDate.length >= 4) {
                        relDate.substring(0, 4)
                    } else ""
                    val durationText = details.getDuration() ?: ""
                    metaTextView.text = year + if (durationText.isNotEmpty()) "  •  $durationText" else ""
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
                    val dir = Credits.Crew.getDirector(resCredits.crew)
                    directorState.value = dir
                    directorTextView.text = "Director: ${dir ?: "Desconocido"}"
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
        if (isProcessingActionState.value) return
        isProcessingActionState.value = true
        firebaseManager.addMovieToWatchOrDiaryList(WATCH_LIST, movieState.value!!) { result, error ->
            isProcessingActionState.value = false
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else {
                showMessage(applicationContext, result)
            }
        }
    }

    private fun markAsWatched() {
        if (isProcessingActionState.value) return
        isProcessingActionState.value = true
        firebaseManager.addMovieToWatchOrDiaryList(DIARY_LIST, movieState.value!!) { result, error ->
            if (error != null) {
                isProcessingActionState.value = false
                showMessage(applicationContext, error.message)
            } else {
                showMessage(applicationContext, result)
                deleteMovieFromWatchList()
            }
        }
    }

    private fun deleteMovieFromWatchList() {
        val uid = userId
        if (uid == null) {
            isProcessingActionState.value = false
            return
        }
        firebaseManager.deleteMovieFromList(WATCH_LIST, uid, movieState.value!!) { _, error ->
            isProcessingActionState.value = false
            if (error != null) {
                showMessage(applicationContext, error.message)
            }
        }
    }
}

@Composable
fun MovieDetailBody(
    details: MovieDetails?,
    credits: Credits?,
    reviews: List<Review>,
    isLoading: Boolean,
    isProcessingAction: Boolean = false,
    currentUserId: String?,
    onWriteReviewClick: () -> Unit,
    onAddToListClick: () -> Unit,
    onWatchlistClick: () -> Unit,
    onWatchedClick: () -> Unit,
    onLikeClick: (Review) -> Unit,
    onDislikeClick: (Review) -> Unit,
    onDeleteClick: (Review) -> Unit,
    onReviewClick: (Review) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Genres Chips
        val genres = details?.genres
        if (!genres.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(genres) { genre ->
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                text = genre.name ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = onAddToListClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.add_to_list), fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onWatchlistClick,
                enabled = !isProcessingAction,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark),
                    contentDescription = null,
                    tint = Color(0xFF2196F3),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.action_watchlist), fontSize = 13.sp)
            }

            OutlinedButton(
                onClick = onWatchedClick,
                enabled = !isProcessingAction,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_visibility),
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = stringResource(R.string.action_diary), fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Overview / Synopsis
        val overview = details?.overview
        if (!overview.isNullOrEmpty()) {
            Text(
                text = "Sinopsis",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Cast Section
        val castList = credits?.cast
        if (!castList.isNullOrEmpty()) {
            Text(
                text = "Reparto Principal",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(castList.take(10)) { actor ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(80.dp)
                    ) {
                        val profileUrl = if (!actor.profilePath.isNullOrEmpty()) {
                            "https://image.tmdb.org/t/p/w185${actor.profilePath}"
                        } else null

                        AsyncImage(
                            model = profileUrl,
                            contentDescription = actor.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape),
                            placeholder = painterResource(R.drawable.ic_default_profile),
                            error = painterResource(R.drawable.ic_default_profile)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = actor.name ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = actor.character ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Reviews Section Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Reseñas de Usuarios",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onWriteReviewClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Escribir", fontSize = 13.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (reviews.isEmpty()) {
            Text(
                text = "Sé el primero en escribir una reseña sobre esta película.",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                reviews.forEach { review ->
                    ReviewItemCard(
                        review = review,
                        currentUserId = currentUserId,
                        onLikeClick = onLikeClick,
                        onDislikeClick = onDislikeClick,
                        onDeleteClick = onDeleteClick,
                        onReviewClick = onReviewClick
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
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
                    val formattedDate = remember(review.reviewDate) {
                        com.merino.ddfilms.utils.DateFormatter().getFormattedDate(review.reviewDate)
                    }
                    Text(
                        text = formattedDate,
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

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val likeCount = review.likeCount?.size ?: 0
                val dislikeCount = review.dislikeCount?.size ?: 0

                val isLiked = currentUserId != null && review.likeCount?.contains(currentUserId) == true
                val isDisliked = currentUserId != null && review.dislikeCount?.contains(currentUserId) == true

                IconButton(
                    onClick = { onLikeClick(review) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_thumb_up),
                        contentDescription = "Me gusta",
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "$likeCount",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.width(12.dp))

                IconButton(
                    onClick = { onDislikeClick(review) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_thumb_down),
                        contentDescription = "No me gusta",
                        tint = if (isDisliked) MaterialTheme.colorScheme.error else Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "$dislikeCount",
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.weight(1f))

                if (currentUserId != null && currentUserId == review.userId) {
                    IconButton(
                        onClick = { onDeleteClick(review) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete),
                            contentDescription = "Eliminar",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
