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
import androidx.compose.foundation.border
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
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
import com.merino.ddfilms.utils.ReviewUtil
import com.merino.ddfilms.utils.StringUtils.DIARY_LIST
import com.merino.ddfilms.utils.StringUtils.WATCH_LIST
import com.merino.ddfilms.utils.Utils.showMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var tmdbService: TMDBService
    private val firebaseManager = FirebaseManager.getInstance()
    private lateinit var reviewUtil: ReviewUtil

    private var currentMovie: Movie? = null
    private var userId: String? = null
    private var userName: String? = null

    // Native Views for Header (outside Compose to preserve shared element enter/exit transitions)
    private lateinit var backdropImageView: ImageView
    private lateinit var posterImageView: ImageView
    private lateinit var titleTextView: TextView
    private lateinit var metaTextView: TextView
    private lateinit var directorTextView: TextView
    private lateinit var ratingTextView: TextView

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

        // 1. Header FrameLayout (Backdrop with smooth multi-stop gradient)
        val headerHeight = (280 * density).toInt()
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

        val midBgColor1 = AndroidColor.argb(80, AndroidColor.red(bgColor), AndroidColor.green(bgColor), AndroidColor.blue(bgColor))
        val midBgColor2 = AndroidColor.argb(200, AndroidColor.red(bgColor), AndroidColor.green(bgColor), AndroidColor.blue(bgColor))
        val endBgColor = AndroidColor.argb(255, AndroidColor.red(bgColor), AndroidColor.green(bgColor), AndroidColor.blue(bgColor))

        val gradientView = View(this).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(
                    AndroidColor.TRANSPARENT,
                    midBgColor1,
                    midBgColor2,
                    endBgColor
                )
            )
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Modern Glassmorphism Back Button
        val backButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_arrow_back)
            setColorFilter(AndroidColor.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(AndroidColor.argb(140, 15, 14, 19))
                setStroke((1 * density).toInt(), AndroidColor.argb(60, 255, 255, 255))
            }
            elevation = 16f * density
            val p = (10 * density).toInt()
            setPadding(p, p, p, p)
            setOnClickListener { supportFinishAfterTransition() }

            val size = (42 * density).toInt()
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

        // 2. Poster & Info Header Row (LinearLayout Horizontal with Glass Elevation)
        val posterRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            val margin = (16 * density).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                leftMargin = margin
                rightMargin = margin
                topMargin = (-60 * density).toInt()
            }
            layoutParams = params
        }

        val posterW = (120 * density).toInt()
        val posterH = (180 * density).toInt()
        posterImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            transitionName = "moviePosterTransition"
            elevation = 12f * density
            clipToOutline = true
            outlineProvider = object : android.view.ViewOutlineProvider() {
                override fun getOutline(v: View, outline: android.graphics.Outline) {
                    val radius = 14f * density
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
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(textPrimaryColor)
            maxLines = 4
            ellipsize = android.text.TextUtils.TruncateAt.END
            androidx.core.widget.TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(
                this,
                13,
                20,
                1,
                android.util.TypedValue.COMPLEX_UNIT_SP
            )
        }

        // Rating Badge Pill Container
        val ratingContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (6 * density).toInt()
                bottomMargin = (4 * density).toInt()
            }
            layoutParams = params
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 8f * density
                setColor(AndroidColor.argb(40, 229, 9, 20))
                setStroke((1 * density).toInt(), AndroidColor.argb(100, 229, 9, 20))
            }
            setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
        }

        val starIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_star_filled)
            setColorFilter(AndroidColor.rgb(255, 193, 7)) // Amber Gold Star
            val size = (14 * density).toInt()
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                rightMargin = (4 * density).toInt()
            }
        }

        ratingTextView = TextView(this).apply {
            val vote = currentMovie?.voteAverage ?: 0.0
            text = if (vote > 0) String.format(Locale.getDefault(), "%.1f", vote) else "N/A"
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(textPrimaryColor)
        }

        ratingContainer.addView(starIcon)
        ratingContainer.addView(ratingTextView)

        metaTextView = TextView(this).apply {
            text = ""
            textSize = 13f
            setTextColor(textSecondaryColor)
        }

        directorTextView = TextView(this).apply {
            text = "Director: Cargando..."
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(accentColor)
        }

        infoContainer.addView(titleTextView)
        infoContainer.addView(ratingContainer)
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
                    val vote = details.voteAverage
                    ratingTextView.text = if (vote > 0) String.format(Locale.getDefault(), "%.1f", vote) else "N/A"

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
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
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
        // Genres Chips Carrousel
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
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons Row (Compact, Single-Line Responsive Fit)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = onAddToListClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1.1f)
                    .height(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.add_to_list),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onWatchlistClick,
                enabled = !isProcessingAction,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2196F3).copy(alpha = 0.6f),
                            Color(0xFF2196F3).copy(alpha = 0.3f)
                        )
                    )
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_bookmark),
                    contentDescription = null,
                    tint = Color(0xFF44B3FF),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.action_watchlist),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            OutlinedButton(
                onClick = onWatchedClick,
                enabled = !isProcessingAction,
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF4CAF50).copy(alpha = 0.6f),
                            Color(0xFF4CAF50).copy(alpha = 0.3f)
                        )
                    )
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_visibility),
                    contentDescription = null,
                    tint = Color(0xFF66BB6A),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = stringResource(R.string.action_diary),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Overview / Synopsis Section
        val overview = details?.overview
        if (!overview.isNullOrEmpty()) {
            SectionHeader(title = stringResource(R.string.synopsis_title))
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Text(
                    text = overview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                    lineHeight = 22.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Cast Section
        val castList = credits?.cast
        if (!castList.isNullOrEmpty()) {
            SectionHeader(title = stringResource(R.string.main_cast_title))
            Spacer(modifier = Modifier.height(14.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(castList.take(12)) { actor ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(84.dp)
                    ) {
                        val profileUrl = if (!actor.profilePath.isNullOrEmpty()) {
                            "https://image.tmdb.org/t/p/w185${actor.profilePath}"
                        } else null

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .border(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                    CircleShape
                                )
                        ) {
                            AsyncImage(
                                model = profileUrl,
                                contentDescription = actor.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                                placeholder = painterResource(R.drawable.ic_default_profile),
                                error = painterResource(R.drawable.ic_default_profile)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = actor.name ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = actor.character ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // Reviews Section Header & List
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            SectionHeader(
                title = stringResource(R.string.user_reviews_title),
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = onWriteReviewClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_edit),
                    contentDescription = null,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.write_review_btn),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (reviews.isEmpty()) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(14.dp)
                    )
            ) {
                Text(
                    text = stringResource(R.string.first_review_prompt),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
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

        Spacer(modifier = Modifier.height(36.dp))
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                RoundedCornerShape(16.dp)
            )
            .clickable { onReviewClick(review) }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = review.userProfileImageUrl ?: R.drawable.ic_default_profile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(10.dp))
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
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

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = review.reviewText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(10.dp))
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
                        tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "$likeCount",
                    fontSize = 12.sp,
                    fontWeight = if (isLiked) FontWeight.Bold else FontWeight.Normal,
                    color = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.width(14.dp))

                IconButton(
                    onClick = { onDislikeClick(review) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_thumb_down),
                        contentDescription = "No me gusta",
                        tint = if (isDisliked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "$dislikeCount",
                    fontSize = 12.sp,
                    fontWeight = if (isDisliked) FontWeight.Bold else FontWeight.Normal,
                    color = if (isDisliked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
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
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
