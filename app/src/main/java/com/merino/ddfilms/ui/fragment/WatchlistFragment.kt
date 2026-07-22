package com.merino.ddfilms.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.ui.MainActivity
import com.merino.ddfilms.ui.MovieDetailActivity
import com.merino.ddfilms.ui.SearchActivity
import com.merino.ddfilms.ui.components.Fab.FabHost
import com.merino.ddfilms.ui.components.Fab.ShowsFab
import com.merino.ddfilms.ui.components.MoviePosterCard
import com.merino.ddfilms.ui.theme.CinematicTheme
import com.merino.ddfilms.utils.StringUtils.WATCH_LIST
import com.merino.ddfilms.utils.Utils.showMessage

class WatchlistFragment : Fragment(), FabHost, ShowsFab {

    private val firebaseManager = FirebaseManager.getInstance()
    private val movieListState = mutableStateOf<List<Movie>>(emptyList())
    private val isLoadingState = mutableStateOf(true)
    private val isEditModeState = mutableStateOf(false)
    private var userID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userID = firebaseManager.getCurrentUserUID()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CinematicTheme {
                    WatchlistScreen(
                        moviesList = movieListState.value,
                        isLoading = isLoadingState.value,
                        isEditMode = isEditModeState.value,
                        onEditModeToggle = {
                            isEditModeState.value = !isEditModeState.value
                        },
                        onMovieClick = { movie, view ->
                            val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                                putExtra("movie", movie)
                            }
                            if (view != null && activity != null) {
                                val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                                    requireActivity(),
                                    view,
                                    "moviePosterTransition"
                                )
                                androidx.core.app.ActivityCompat.startActivity(requireContext(), intent, options.toBundle())
                            } else {
                                startActivity(intent)
                            }
                        },
                        onDeleteMovie = { index, movie ->
                            deleteMovie(index, movie)
                        },
                        onBackClick = {
                            if (parentFragmentManager.backStackEntryCount > 0) {
                                parentFragmentManager.popBackStack()
                            } else {
                                (activity as? MainActivity)?.loadFragment(ProfileFragment())
                            }
                        }
                    )
                }
            }
        }
    }

    private fun loadMoviesFromList(showLoading: Boolean = true) {
        val uid = userID ?: return
        if (showLoading && movieListState.value.isEmpty()) {
            isLoadingState.value = true
        }
        firebaseManager.getMoviesFromList(uid, WATCH_LIST) { movies, error ->
            isLoadingState.value = false
            if (error != null) {
                showMessage(context, error.message)
            } else if (movies != null) {
                movieListState.value = movies.reversed()
            }
        }
    }

    private fun deleteMovie(index: Int, movie: Movie) {
        val uid = userID ?: return
        // Optimistic UI update: remove item instantly from local state
        val current = movieListState.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            movieListState.value = current
        }

        firebaseManager.deleteMovieFromList(WATCH_LIST, uid, movie) { _, error ->
            if (error != null) {
                showMessage(context, error.message)
                // Revert state if remote deletion fails
                loadMoviesFromList(showLoading = false)
            }
        }
    }

    private fun setupAddMovieFragment() {
        val intent = Intent(context, SearchActivity::class.java).apply {
            putExtra("collection", WATCH_LIST)
            putExtra("documentID", userID)
            putExtra("listName", "Pendientes")
            val moviesID = movieListState.value.map { it.id }.toIntArray()
            putExtra("moviesID", moviesID)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        loadMoviesFromList()
    }

    override fun onFabClicked() {
        setupAddMovieFragment()
    }
}

@Composable
fun WatchlistScreen(
    moviesList: List<Movie>,
    isLoading: Boolean,
    isEditMode: Boolean,
    onEditModeToggle: () -> Unit,
    onMovieClick: (Movie, View?) -> Unit,
    onDeleteMovie: (Int, Movie) -> Unit,
    onBackClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_arrow_back),
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Películas Pendientes",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onEditModeToggle) {
                    Icon(
                        painter = painterResource(
                            id = if (isEditMode) R.drawable.ic_done else R.drawable.ic_edit
                        ),
                        contentDescription = "Editar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (moviesList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tu watchlist está vacía.",
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        moviesList,
                        key = { index, movie -> "${movie.id}_${movie.createdAt ?: ""}_${movie.title ?: ""}_$index" }
                    ) { index, movie ->
                        Box(modifier = Modifier.fillMaxWidth()) {
                            MoviePosterCard(
                                movie = movie,
                                onClick = { view -> onMovieClick(movie, view) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (isEditMode) {
                                IconButton(
                                    onClick = { onDeleteMovie(index, movie) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(28.dp)
                                        .background(
                                            color = Color(0xFFBA1A1A),
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_remove),
                                        contentDescription = "Eliminar",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
