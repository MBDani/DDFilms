package com.merino.ddfilms.ui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.android.material.appbar.AppBarLayout
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.CastAdapter
import com.merino.ddfilms.adapters.CrewAdapter
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.api.TMDBClient
import com.merino.ddfilms.api.TMDBService
import com.merino.ddfilms.configuration.ApiKeyManager
import com.merino.ddfilms.model.Credits
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.MovieDetails
import com.merino.ddfilms.transitions.DetailsTransition
import com.merino.ddfilms.ui.fragment.MovieListDialogFragment
import com.merino.ddfilms.ui.fragment.WriteReviewDialogFragment
import com.merino.ddfilms.ui.utils.CustomFabMenu
import com.merino.ddfilms.utils.EdgeToEdgeHelper
import com.merino.ddfilms.utils.ReviewUtil
import com.merino.ddfilms.utils.StringUtils.DIARY_LIST
import com.merino.ddfilms.utils.StringUtils.WATCH_LIST
import com.merino.ddfilms.utils.Utils.showMessage
import java.util.ArrayList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MovieDetailActivity : AppCompatActivity() {

    private lateinit var tmdbService: TMDBService
    private var credits: Credits? = null
    private var movieDetails: MovieDetails? = null
    private lateinit var backdropImageView: ImageView
    private lateinit var posterImageView: ImageView
    private lateinit var movieTitle: TextView
    private lateinit var movieYearDuration: TextView
    private lateinit var movieOverview: TextView
    private lateinit var movieDirector: TextView
    private lateinit var duration: TextView
    private lateinit var backButton: ImageButton
    private lateinit var castRecyclerView: RecyclerView
    private lateinit var crewRecyclerView: RecyclerView
    private lateinit var reviewsRecyclerView: RecyclerView
    private lateinit var castAdapter: CastAdapter
    private lateinit var crewAdapter: CrewAdapter
    private var currentMovie: Movie? = null
    private val firebaseManager = FirebaseManager()
    private var userId: String? = null
    private var userName: String? = null
    private var fabMenu: CustomFabMenu? = null
    private lateinit var nestedScrollView: NestedScrollView
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var reviewUtil: ReviewUtil
    private var isTransitionStarted = false
    private var skeletonDirector: View? = null
    private var skeletonBottomContainer: View? = null
    private var apiCallsCompleted = 0

    companion object {
        private val API_KEY = ApiKeyManager.getInstance().apiKey ?: ""
        private const val TOTAL_API_CALLS = 3
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportPostponeEnterTransition()
        setContentView(R.layout.activity_movie_detail)

        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService::class.java)
        reviewUtil = ReviewUtil(this)

        initViews()
        setUpButtons()
        setupRecyclerViews()

        currentMovie = intent.getParcelableExtra("movie")

        val movie = currentMovie
        if (movie != null) {
            getMovieCredits(movie.id)
            getMovieDetails(movie.id)
            initReviews()
            setupMovieData(movie)
        } else {
            supportStartPostponedEnterTransition()
        }
        getUserData()
        setupCustomFabMenu()
    }

    private fun initReviews() {
        val movie = currentMovie ?: return
        reviewUtil.setScrollTargets(appBarLayout, nestedScrollView, reviewsRecyclerView)
        reviewUtil.currentMovie = movie
        val highlightReviewId = intent.getStringExtra("highlight_review_id")
        if (highlightReviewId != null) {
            reviewUtil.highlightReviewId = highlightReviewId
        }
        reviewUtil.setOnReviewsLoadedListener { checkLoadingComplete() }
        reviewUtil.loadMovieReviews(movie.id)
    }

    private fun getUserData() {
        userId = firebaseManager.getCurrentUserUID()
        val currentUserId = userId
        if (currentUserId != null) {
            firebaseManager.getUserName(currentUserId) { name, _ ->
                this.userName = name
                reviewUtil.userName = name
            }
        }
    }

    private fun initViews() {
        backdropImageView = findViewById(R.id.backdrop_image_view)
        posterImageView = findViewById(R.id.poster_image_view)
        movieTitle = findViewById(R.id.movie_title)
        movieYearDuration = findViewById(R.id.movie_release_date)
        movieOverview = findViewById(R.id.movie_overview)
        movieOverview.movementMethod = ScrollingMovementMethod()
        movieDirector = findViewById(R.id.movie_director)
        duration = findViewById(R.id.duration)
        backButton = findViewById(R.id.back_button)
        nestedScrollView = findViewById(R.id.nested_scroll_view)
        appBarLayout = findViewById(R.id.appbar_layout)

        castRecyclerView = findViewById(R.id.cast_recycler_view)
        crewRecyclerView = findViewById(R.id.crew_recycler_view)
        reviewsRecyclerView = findViewById(R.id.reviews_recycler_view)

        skeletonDirector = findViewById(R.id.skeleton_director)
        skeletonBottomContainer = findViewById(R.id.skeleton_bottom_container)

        EdgeToEdgeHelper.applyWindowInsetsPending(nestedScrollView, false, true)
    }

    private fun setUpButtons() {
        backButton.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupRecyclerViews() {
        castAdapter = CastAdapter(ArrayList())
        crewAdapter = CrewAdapter(ArrayList())

        castRecyclerView.adapter = castAdapter
        castRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        crewRecyclerView.adapter = crewAdapter
        crewRecyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        reviewsRecyclerView.adapter = reviewUtil.reviewAdapter
        reviewsRecyclerView.layoutManager = LinearLayoutManager(this)
        reviewsRecyclerView.isNestedScrollingEnabled = false
    }

    private fun setupMovieData(movie: Movie) {
        posterImageView.transitionName = "moviePosterTransition"
        window.sharedElementEnterTransition = DetailsTransition()

        movieTitle.text = movie.title
        var releaseDate = movie.releaseDate
        if (releaseDate != null && releaseDate.length >= 4) {
            releaseDate = releaseDate.substring(0, 4)
        } else {
            releaseDate = ""
        }
        val details = movieDetails
        val movieYearDurationText = releaseDate + if (details != null) "  •  " + details.getDuration() else ""
        movieYearDuration.text = movieYearDurationText
        movieOverview.text = movie.overview

        if (!movie.backdropPath.isNullOrEmpty()) {
            Glide.with(this)
                .load("https://image.tmdb.org/t/p/w500/" + movie.backdropPath)
                .into(backdropImageView)
        }
        if (!movie.posterPath.isNullOrEmpty()) {
            val posterUrl = "https://image.tmdb.org/t/p/w500" + movie.posterPath
            val thumbnailUrl = "https://image.tmdb.org/t/p/w200" + movie.posterPath

            if (!isTransitionStarted) {
                val thumbnailRequest = Glide.with(this)
                    .load(thumbnailUrl)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isTransitionStarted) {
                                isTransitionStarted = true
                                supportStartPostponedEnterTransition()
                            }
                            return false
                        }
                    })

                Glide.with(this)
                    .load(posterUrl)
                    .thumbnail(thumbnailRequest)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: Target<Drawable>,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isTransitionStarted) {
                                isTransitionStarted = true
                                supportStartPostponedEnterTransition()
                            }
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable,
                            model: Any,
                            target: Target<Drawable>,
                            dataSource: DataSource,
                            isFirstResource: Boolean
                        ): Boolean {
                            if (!isTransitionStarted) {
                                isTransitionStarted = true
                                supportStartPostponedEnterTransition()
                            }
                            return false
                        }
                    })
                    .into(posterImageView)
            } else {
                Glide.with(this)
                    .load(posterUrl)
                    .into(posterImageView)
            }
        } else {
            if (!isTransitionStarted) {
                isTransitionStarted = true
                supportStartPostponedEnterTransition()
            }
        }
    }

    private fun setupCustomFabMenu() {
        val container = findViewById<ViewGroup>(R.id.coordinator_layout_main_container)

        fabMenu = CustomFabMenu(this, container)
            .setMainFabIcon(R.drawable.ic_menu)
            .setMainFabColor(R.color.main_fab_bg)
            .setOverlayColor(R.color.black_50)
            .setLabelBackground(R.drawable.fab_label_background)
            .setBaseMarginBottom(40f)
            .setItemSpacing(64f)
            .addMenuItem(
                R.drawable.ic_review,
                getString(R.string.write_review),
                R.color.accent_orange,
                getString(R.string.add_review)
            ) { openWriteReviewActivity() }
            .addMenuItem(
                R.drawable.ic_add,
                getString(R.string.add_to_list),
                R.color.accent_red,
                getString(R.string.add_to_list)
            ) { showAddToListDialog() }
            .addMenuItem(
                R.drawable.ic_bookmark,
                getString(R.string.pending_to_watch),
                R.color.blue_500,
                getString(R.string.add_to_watchlist)
            ) { addToWatchlist() }
            .addMenuItem(
                R.drawable.ic_visibility,
                getString(R.string.watched),
                R.color.green_500,
                getString(R.string.mark_as_watched)
            ) { markAsWatched() }

        fabMenu?.build()
    }

    private fun openWriteReviewActivity() {
        val dialog = WriteReviewDialogFragment(currentMovie, reviewUtil.userReview)
        dialog.setOnReviewSubmittedListener(reviewUtil)
        dialog.show(supportFragmentManager, "WriteReviewDialog")
    }

    private fun showAddToListDialog() {
        val dialog = MovieListDialogFragment(currentMovie!!)
        dialog.show(supportFragmentManager, "MovieListDialog")
    }

    private fun addToWatchlist() {
        firebaseManager.addMovieToWatchOrDiaryList(WATCH_LIST, currentMovie!!) { result, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else {
                showMessage(applicationContext, result)
            }
        }
    }

    private fun markAsWatched() {
        firebaseManager.addMovieToWatchOrDiaryList(DIARY_LIST, currentMovie!!) { result, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else {
                showMessage(applicationContext, result)
                deleteMovieFromWatchList()
            }
        }
    }

    private fun deleteMovieFromWatchList() {
        firebaseManager.deleteMovieFromList(WATCH_LIST, userId!!, currentMovie!!) { _, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            }
        }
    }

    private fun getMovieCredits(id: Int) {
        tmdbService.getMovieCredits(id, API_KEY, getString(R.string.tmdb_api_language)).enqueue(object : Callback<Credits> {
            @SuppressLint("NotifyDataSetChanged")
            override fun onResponse(call: Call<Credits>, response: Response<Credits>) {
                if (response.isSuccessful && response.body() != null) {
                    val responseCredits = response.body()!!
                    credits = responseCredits
                    val director = Credits.Crew.getDirector(responseCredits.crew)
                    val labelMovieDirector = director ?: getString(R.string.no_director_found)
                    movieDirector.text = labelMovieDirector

                    castAdapter.castList = responseCredits.cast ?: ArrayList()
                    crewAdapter.crewList = responseCredits.crew ?: ArrayList()

                    castAdapter.notifyDataSetChanged()
                    crewAdapter.notifyDataSetChanged()
                } else {
                    Log.e("MovieDetailActivity", "Error en la respuesta: " + response.message())
                }
                checkLoadingComplete()
            }

            override fun onFailure(call: Call<Credits>, t: Throwable) {
                Log.e("MovieDetailActivity", "Error al obtener créditos de la película", t)
                showMessage(applicationContext, getString(R.string.error_getting_credits))
                checkLoadingComplete()
            }
        })
    }

    private fun getMovieDetails(id: Int) {
        tmdbService.getMovieDetails(id, API_KEY, getString(R.string.tmdb_api_language)).enqueue(object : Callback<MovieDetails> {
            override fun onResponse(call: Call<MovieDetails>, response: Response<MovieDetails>) {
                if (response.isSuccessful && response.body() != null) {
                    val details = response.body()!!
                    movieDetails = details

                    val movie = currentMovie
                    if (movie != null) {
                        movie.title = details.title
                        movie.overview = details.overview
                        movie.releaseDate = details.releaseDate
                        movie.backdropPath = details.backdropPath

                        setupMovieData(movie)
                        reviewUtil.currentMovie = movie
                    }
                } else {
                    Log.e("MovieDetailActivity", "Error en la respuesta: " + response.message())
                }
                checkLoadingComplete()
            }

            override fun onFailure(call: Call<MovieDetails>, t: Throwable) {
                Log.e("MovieDetailActivity", "Error al obtener detalles de la película", t)
                showMessage(applicationContext, getString(R.string.error_getting_details))
                checkLoadingComplete()
            }
        })
    }

    private fun checkLoadingComplete() {
        apiCallsCompleted++
        if (apiCallsCompleted >= TOTAL_API_CALLS) {
            showSkeleton(false)
        }
    }

    private fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonDirector?.alpha = 1f
            skeletonDirector?.visibility = View.VISIBLE
            skeletonBottomContainer?.alpha = 1f
            skeletonBottomContainer?.visibility = View.VISIBLE

            movieDirector.visibility = View.GONE
            findViewById<View>(R.id.cast_title).visibility = View.GONE
            findViewById<View>(R.id.cast_recycler_view).visibility = View.GONE
            findViewById<View>(R.id.crew_title).visibility = View.GONE
            findViewById<View>(R.id.crew_recycler_view).visibility = View.GONE
            findViewById<View>(R.id.reviews_title).visibility = View.GONE
            findViewById<View>(R.id.reviews_recycler_view).visibility = View.GONE
        } else {
            val skDir = skeletonDirector
            if (skDir != null && skDir.visibility == View.VISIBLE) {
                skDir.animate().alpha(0f).setDuration(200).withEndAction {
                    skDir.visibility = View.GONE
                    movieDirector.alpha = 0f
                    movieDirector.visibility = View.VISIBLE
                    movieDirector.animate().alpha(1f).setDuration(200).start()
                }.start()
            } else {
                movieDirector.visibility = View.VISIBLE
            }

            val skBottom = skeletonBottomContainer
            if (skBottom != null && skBottom.visibility == View.VISIBLE) {
                skBottom.animate().alpha(0f).setDuration(200).withEndAction {
                    skBottom.visibility = View.GONE

                    val contentViews = arrayOf(
                        findViewById<View>(R.id.cast_title),
                        findViewById<View>(R.id.cast_recycler_view),
                        findViewById<View>(R.id.crew_title),
                        findViewById<View>(R.id.crew_recycler_view),
                        findViewById<View>(R.id.reviews_title),
                        findViewById<View>(R.id.reviews_recycler_view)
                    )
                    for (view in contentViews) {
                        if (view != null) {
                            view.alpha = 0f
                            view.visibility = View.VISIBLE
                            view.animate().alpha(1f).setDuration(200).start()
                        }
                    }
                }.start()
            } else {
                val contentViews = arrayOf(
                    findViewById<View>(R.id.cast_title),
                    findViewById<View>(R.id.cast_recycler_view),
                    findViewById<View>(R.id.crew_title),
                    findViewById<View>(R.id.crew_recycler_view),
                    findViewById<View>(R.id.reviews_title),
                    findViewById<View>(R.id.reviews_recycler_view)
                )
                for (view in contentViews) {
                    view?.visibility = View.VISIBLE
                }
            }
        }
    }
}
