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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.fragment.app.Fragment
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.api.TMDBClient
import com.merino.ddfilms.api.TMDBService
import com.merino.ddfilms.configuration.ApiKeyManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.SearchResponse
import com.merino.ddfilms.ui.MovieDetailActivity
import com.merino.ddfilms.ui.components.MoviePosterCard
import com.merino.ddfilms.ui.theme.CinematicTheme
import com.merino.ddfilms.utils.Utils.showMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private lateinit var tmdbService: TMDBService
    private val firebaseManager = FirebaseManager()

    private val searchQueryState = mutableStateOf("")
    private val moviesListState = mutableStateOf<List<Movie>>(emptyList())
    private val isLoadingState = mutableStateOf(false)

    // Add Mode properties
    private var isAddMode = false
    private val moviesIdListState = mutableStateOf<List<Int>>(emptyList())
    private var collection: String? = null
    private var documentID: String? = null
    private var listName: String? = null

    companion object {
        private val API_KEY = ApiKeyManager.getInstance().apiKey ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService::class.java)

        val args = arguments
        if (args != null) {
            collection = args.getString("collection")
            documentID = args.getString("documentID")
            listName = args.getString("listName")
            val moviesID = args.getIntArray("moviesID")
            if (moviesID != null && collection != null && documentID != null && listName != null) {
                isAddMode = true
                moviesIdListState.value = moviesID.toList()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CinematicTheme {
                    SearchScreen(
                        searchQuery = searchQueryState.value,
                        onSearchQueryChange = { query ->
                            searchQueryState.value = query
                        },
                        onPerformSearch = { query ->
                            if (query.isNotEmpty()) {
                                searchMovies(query)
                            } else {
                                loadPopularMovies()
                            }
                        },
                        moviesList = moviesListState.value,
                        isLoading = isLoadingState.value,
                        isAddMode = isAddMode,
                        addedMoviesIds = moviesIdListState.value,
                        onAddClick = { movie ->
                            addMovie(movie)
                        },
                        onRemoveClick = { movie ->
                            deleteMovie(movie)
                        },
                        onMovieClick = { movie, view ->
                            if (!isAddMode) {
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
                        }
                    )
                }
            }
        }
    }

    private fun searchMovies(query: String) {
        isLoadingState.value = true
        tmdbService.searchMovies(query, false, getString(R.string.tmdb_api_language), 1).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                isLoadingState.value = false
                if (response.isSuccessful && response.body() != null) {
                    moviesListState.value = response.body()!!.results ?: emptyList()
                } else {
                    showMessage(context, getString(R.string.error_searching))
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                isLoadingState.value = false
                showMessage(context, getString(R.string.error_connection, t.message))
            }
        })
    }

    private fun loadPopularMovies() {
        isLoadingState.value = true
        tmdbService.getPopularMovies(API_KEY, getString(R.string.tmdb_api_language)).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                isLoadingState.value = false
                if (response.isSuccessful && response.body() != null) {
                    moviesListState.value = response.body()!!.results ?: emptyList()
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

    private fun addMovie(movie: Movie) {
        val col = collection ?: return
        val doc = documentID ?: return
        val name = listName ?: return

        firebaseManager.addMovieToList(col, doc, movie) { result, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (result != null) {
                showMessage(context, getString(R.string.added_to_list_message, name))
                val currentList = moviesIdListState.value.toMutableList()
                currentList.add(movie.id)
                moviesIdListState.value = currentList
            }
        }
    }

    private fun deleteMovie(movie: Movie) {
        val col = collection ?: return
        val doc = documentID ?: return
        val name = listName ?: return

        firebaseManager.deleteMovieFromList(col, doc, movie) { result, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (result != null) {
                showMessage(context, getString(R.string.removed_from_list, name))
                val currentList = moviesIdListState.value.toMutableList()
                currentList.remove(movie.id)
                moviesIdListState.value = currentList
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onPerformSearch: (String) -> Unit,
    moviesList: List<Movie>,
    isLoading: Boolean,
    isAddMode: Boolean,
    addedMoviesIds: List<Int>,
    onAddClick: (Movie) -> Unit,
    onRemoveClick: (Movie) -> Unit,
    onMovieClick: (Movie, View?) -> Unit
) {
    LaunchedEffect(searchQuery) {
        val trimmed = searchQuery.trim()
        if (trimmed.isEmpty()) {
            onPerformSearch("")
        } else {
            kotlinx.coroutines.delay(350)
            onPerformSearch(trimmed)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Buscar películas...", color = Color.Gray) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

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
                Text("No se encontraron resultados", color = MaterialTheme.colorScheme.onBackground)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(moviesList, key = { it.id }) { movie ->
                    Box(modifier = Modifier.fillMaxWidth()) {
                        MoviePosterCard(
                            movie = movie,
                            onClick = { view -> onMovieClick(movie, view) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (isAddMode) {
                            val isAdded = addedMoviesIds.contains(movie.id)
                            IconButton(
                                onClick = {
                                    if (isAdded) {
                                        onRemoveClick(movie)
                                    } else {
                                        onAddClick(movie)
                                    }
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(28.dp)
                                    .background(
                                        color = if (isAdded) Color(0xFF2E7D32) else Color(0xFFBA1A1A),
                                        shape = RoundedCornerShape(14.dp)
                                    )
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (isAdded) R.drawable.ic_done else R.drawable.ic_add_simple
                                    ),
                                    contentDescription = null,
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
