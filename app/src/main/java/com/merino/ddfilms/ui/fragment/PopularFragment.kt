package com.merino.ddfilms.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import com.merino.ddfilms.R
import com.merino.ddfilms.api.TMDBClient
import com.merino.ddfilms.api.TMDBService
import com.merino.ddfilms.configuration.ApiKeyManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.SearchResponse
import com.merino.ddfilms.ui.MovieDetailActivity
import com.merino.ddfilms.ui.components.MoviePosterCard
import com.merino.ddfilms.ui.components.ShimmerBrush
import com.merino.ddfilms.ui.theme.CinematicTheme
import com.merino.ddfilms.utils.Utils.showMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PopularFragment : Fragment() {

    private lateinit var tmdbService: TMDBService
    private val popularMoviesState = mutableStateOf<List<Movie>>(emptyList())
    private val isLoadingState = mutableStateOf(true)

    companion object {
        private val API_KEY = ApiKeyManager.getInstance().apiKey ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        loadData()
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CinematicTheme {
                    PopularScreen(
                        popularMovies = popularMoviesState.value,
                        isLoading = isLoadingState.value,
                        onMovieClick = { movie, view ->
                            val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                                putExtra("movie", movie)
                            }
                            if (view != null) {
                                val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    requireActivity(),
                                    view,
                                    "moviePosterTransition"
                                )
                                ActivityCompat.startActivity(requireContext(), intent, options.toBundle())
                            } else {
                                startActivity(intent)
                            }
                        }
                    )
                }
            }
        }
    }

    private fun loadData() {
        isLoadingState.value = true
        tmdbService.getPopularMovies(API_KEY, getString(R.string.tmdb_api_language)).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                isLoadingState.value = false
                if (response.isSuccessful && response.body() != null) {
                    popularMoviesState.value = response.body()!!.results ?: emptyList()
                } else {
                    showMessage(context, getString(R.string.error_loading_popular))
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                isLoadingState.value = false
                showMessage(context, getString(R.string.error_connection, t.message))
            }
        })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PopularScreen(
    popularMovies: List<Movie>,
    isLoading: Boolean,
    onMovieClick: (Movie, View?) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (isLoading) {
            PopularShimmerScreen()
        } else if (popularMovies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron películas populares",
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        } else {
            val carouselMovies = popularMovies.take(5)
            val rail1Movies = popularMovies.drop(5).take(7)
            val rail2Movies = popularMovies.drop(12)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
            ) {
            // Hero Pager
            item {
                val pagerState = rememberPagerState(pageCount = { carouselMovies.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) { page ->
                    val movie = carouselMovies[page]
                    val localView = LocalView.current
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onMovieClick(movie, localView) }
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

                        // Backdrop dynamic gradient
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

                        // Title/Overview Overlay
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = movie.title ?: "",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontSize = 24.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = movie.overview ?: "",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Rail 1: Tendencias
            if (rail1Movies.isNotEmpty()) {
                item {
                    Text(
                        text = "Tendencias de la Semana",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        ),
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 12.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(rail1Movies) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = { view -> onMovieClick(movie, view) },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            }

            // Rail 2: Favoritas del Público
            if (rail2Movies.isNotEmpty()) {
                item {
                    Text(
                        text = "Favoritas del Público",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        ),
                        modifier = Modifier.padding(start = 16.dp, top = 28.dp, bottom = 12.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(rail2Movies) { movie ->
                            MoviePosterCard(
                                movie = movie,
                                onClick = { view -> onMovieClick(movie, view) },
                                modifier = Modifier.width(120.dp)
                            )
                        }
                    }
                }
            }

            // Extra space at bottom
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
}

@Composable
fun PopularShimmerScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ShimmerBrush())
        )
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .width(180.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(ShimmerBrush())
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(165.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ShimmerBrush())
                )
            }
        }
    }
}
