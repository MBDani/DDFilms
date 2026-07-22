package com.merino.ddfilms.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

data class GenreItem(val id: Int?, val name: String)

data class SortOption(val key: String, val label: String)

class SearchFragment : Fragment() {

    private lateinit var tmdbService: TMDBService
    private val firebaseManager = FirebaseManager()

    private val searchQueryState = mutableStateOf("")
    private val moviesListState = mutableStateOf<List<Movie>>(emptyList())
    private val isLoadingState = mutableStateOf(false)

    // Filter states
    private val selectedGenreIdState = mutableStateOf<Int?>(null)
    private val selectedSortByState = mutableStateOf("popularity.desc")
    private val selectedYearState = mutableStateOf<Int?>(null)
    private val minRatingState = mutableStateOf<Float?>(null)
    private val isFilterSheetOpenState = mutableStateOf(false)

    // Add Mode properties
    private var isAddMode = false
    private val moviesIdListState = mutableStateOf<List<Int>>(emptyList())
    private var collection: String? = null
    private var documentID: String? = null
    private var listName: String? = null

    private val genresList = listOf(
        GenreItem(null, "Todas"),
        GenreItem(28, "Acción"),
        GenreItem(35, "Comedia"),
        GenreItem(18, "Drama"),
        GenreItem(878, "Sci-Fi"),
        GenreItem(27, "Terror"),
        GenreItem(16, "Animación"),
        GenreItem(53, "Thriller"),
        GenreItem(12, "Aventura"),
        GenreItem(10749, "Romance"),
        GenreItem(99, "Documental")
    )

    private val sortOptions = listOf(
        SortOption("popularity.desc", "Más Populares"),
        SortOption("vote_average.desc", "Mejor Valoradas"),
        SortOption("primary_release_date.desc", "Más Recientes")
    )

    private val yearOptions = listOf<Int?>(null, 2026, 2025, 2024, 2023, 2020, 2010)

    private val ratingOptions = listOf<Float?>(null, 7.0f, 8.0f)

    private lateinit var voiceSearchLauncher: ActivityResultLauncher<Intent>

    companion object {
        private val API_KEY = ApiKeyManager.getInstance().apiKey ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService::class.java)

        voiceSearchLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = matches?.firstOrNull()
                if (!spokenText.isNullOrBlank()) {
                    searchQueryState.value = spokenText
                }
            }
        }

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
                        onSearchQueryChange = { query -> searchQueryState.value = query },
                        genres = genresList,
                        selectedGenreId = selectedGenreIdState.value,
                        onGenreSelected = { genreId ->
                            selectedGenreIdState.value = genreId
                        },
                        selectedSortBy = selectedSortByState.value,
                        onSortBySelected = { sortKey ->
                            selectedSortByState.value = sortKey
                        },
                        selectedYear = selectedYearState.value,
                        onYearSelected = { year ->
                            selectedYearState.value = year
                        },
                        minRating = minRatingState.value,
                        onMinRatingSelected = { rating ->
                            minRatingState.value = rating
                        },
                        sortOptions = sortOptions,
                        yearOptions = yearOptions,
                        ratingOptions = ratingOptions,
                        isFilterSheetOpen = isFilterSheetOpenState.value,
                        onOpenFilterSheet = { isFilterSheetOpenState.value = true },
                        onDismissFilterSheet = { isFilterSheetOpenState.value = false },
                        onResetFilters = { resetAllFilters() },
                        onVoiceSearchClick = { launchVoiceSearch() },
                        onPerformFetch = {
                            executeFetchMovies()
                        },
                        moviesList = moviesListState.value,
                        isLoading = isLoadingState.value,
                        isAddMode = isAddMode,
                        addedMoviesIds = moviesIdListState.value,
                        onAddClick = { movie -> addMovie(movie) },
                        onRemoveClick = { movie -> deleteMovie(movie) },
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

    private fun launchVoiceSearch() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, getString(R.string.tmdb_api_language))
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla para buscar películas...")
            }
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            showMessage(context, "El servicio de voz no está disponible")
        }
    }

    private fun resetAllFilters() {
        selectedGenreIdState.value = null
        selectedSortByState.value = "popularity.desc"
        selectedYearState.value = null
        minRatingState.value = null
    }

    private fun executeFetchMovies() {
        val query = searchQueryState.value.trim()
        val genreId = selectedGenreIdState.value
        val sortBy = selectedSortByState.value
        val year = selectedYearState.value
        val rating = minRatingState.value

        isLoadingState.value = true

        if (query.isNotEmpty()) {
            tmdbService.searchMovies(
                query = query,
                includeAdult = false,
                language = getString(R.string.tmdb_api_language),
                page = 1
            ).enqueue(object : Callback<SearchResponse> {
                override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                    isLoadingState.value = false
                    if (response.isSuccessful && response.body() != null) {
                        var results = response.body()!!.results ?: emptyList()
                        if (rating != null) {
                            results = results.filter { it.voteAverage >= rating }
                        }
                        moviesListState.value = results
                    } else {
                        showMessage(context, getString(R.string.error_searching))
                    }
                }

                override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                    isLoadingState.value = false
                    showMessage(context, getString(R.string.error_connection, t.message))
                }
            })
        } else {
            tmdbService.discoverMovies(
                apiKey = API_KEY,
                language = getString(R.string.tmdb_api_language),
                sortBy = sortBy,
                withGenres = genreId?.toString(),
                primaryReleaseYear = year,
                voteAverageGte = rating,
                page = 1
            ).enqueue(object : Callback<SearchResponse> {
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
    genres: List<GenreItem>,
    selectedGenreId: Int?,
    onGenreSelected: (Int?) -> Unit,
    selectedSortBy: String,
    onSortBySelected: (String) -> Unit,
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    minRating: Float?,
    onMinRatingSelected: (Float?) -> Unit,
    sortOptions: List<SortOption>,
    yearOptions: List<Int?>,
    ratingOptions: List<Float?>,
    isFilterSheetOpen: Boolean,
    onOpenFilterSheet: () -> Unit,
    onDismissFilterSheet: () -> Unit,
    onResetFilters: () -> Unit,
    onVoiceSearchClick: () -> Unit,
    onPerformFetch: () -> Unit,
    moviesList: List<Movie>,
    isLoading: Boolean,
    isAddMode: Boolean,
    addedMoviesIds: List<Int>,
    onAddClick: (Movie) -> Unit,
    onRemoveClick: (Movie) -> Unit,
    onMovieClick: (Movie, View?) -> Unit
) {
    val hasActiveFilters = selectedGenreId != null || selectedYear != null || minRating != null || selectedSortBy != "popularity.desc"

    LaunchedEffect(searchQuery, selectedGenreId, selectedSortBy, selectedYear, minRating) {
        val trimmed = searchQuery.trim()
        if (trimmed.isNotEmpty()) {
            kotlinx.coroutines.delay(350)
        }
        onPerformFetch()
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
            // Search Input Header
            SearchHeader(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                onVoiceSearchClick = onVoiceSearchClick,
                onFilterClick = onOpenFilterSheet,
                hasActiveFilters = hasActiveFilters
            )

            // Genre Capsules Bar
            GenreChipRow(
                genres = genres,
                selectedGenreId = selectedGenreId,
                onGenreSelected = onGenreSelected
            )

            // Active Filters Summary Bar
            AnimatedVisibility(visible = hasActiveFilters) {
                ActiveFiltersSummary(
                    selectedGenreName = genres.find { it.id == selectedGenreId }?.name,
                    selectedSortLabel = sortOptions.find { it.key == selectedSortBy }?.label,
                    selectedYear = selectedYear,
                    minRating = minRating,
                    onResetFilters = onResetFilters
                )
            }

            // Results count or status
            if (!isLoading && moviesList.isNotEmpty()) {
                Text(
                    text = "${moviesList.size} películas encontradas",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }

            // Main Content Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Buscando las mejores películas...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (moviesList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Sin resultados cinematográficos",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (hasActiveFilters) "Prueba a cambiar o reiniciar los filtros aplicados." else "Prueba a escribir otra búsqueda.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                            if (hasActiveFilters) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onResetFilters,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Limpiar Filtros", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
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
                            MovieGridItem(
                                movie = movie,
                                isAddMode = isAddMode,
                                isAdded = addedMoviesIds.contains(movie.id),
                                onAddClick = { onAddClick(movie) },
                                onRemoveClick = { onRemoveClick(movie) },
                                onMovieClick = { view -> onMovieClick(movie, view) }
                            )
                        }
                    }
                }
            }
        }

        // Filter Bottom Sheet
        if (isFilterSheetOpen) {
            FilterBottomSheet(
                selectedSortBy = selectedSortBy,
                onSortBySelected = onSortBySelected,
                selectedYear = selectedYear,
                onYearSelected = onYearSelected,
                minRating = minRating,
                onMinRatingSelected = onMinRatingSelected,
                sortOptions = sortOptions,
                yearOptions = yearOptions,
                ratingOptions = ratingOptions,
                onDismiss = onDismissFilterSheet,
                onResetFilters = onResetFilters
            )
        }
    }
}

@Composable
fun SearchHeader(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onVoiceSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    hasActiveFilters: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Buscar películas...", color = Color.Gray) },
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Limpiar texto",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onVoiceSearchClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_mic),
                            contentDescription = "Búsqueda por Voz",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box {
            IconButton(
                onClick = onFilterClick,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (hasActiveFilters) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (hasActiveFilters) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(14.dp)
                    )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter_list),
                    contentDescription = "Filtros",
                    tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (hasActiveFilters) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}

@Composable
fun GenreChipRow(
    genres: List<GenreItem>,
    selectedGenreId: Int?,
    onGenreSelected: (Int?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(genres) { genre ->
            val isSelected = (selectedGenreId == genre.id)
            val backgroundColor = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surface
            }
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .border(
                        width = if (isSelected) 0.dp else 1.dp,
                        color = Color.Gray.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onGenreSelected(genre.id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = genre.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
fun ActiveFiltersSummary(
    selectedGenreName: String?,
    selectedSortLabel: String?,
    selectedYear: Int?,
    minRating: Float?,
    onResetFilters: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Filtros:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 6.dp)
        )
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selectedGenreName != null && selectedGenreName != "Todas") {
                item { ActiveBadge("Género: $selectedGenreName") }
            }
            if (selectedSortLabel != null && selectedSortLabel != "Más Populares") {
                item { ActiveBadge("Orden: $selectedSortLabel") }
            }
            if (selectedYear != null) {
                item { ActiveBadge("Año: $selectedYear") }
            }
            if (minRating != null) {
                item { ActiveBadge("Nota: $minRating+") }
            }
        }
        Text(
            text = "Borrar",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onResetFilters() }
                .padding(start = 6.dp)
        )
    }
}

@Composable
fun ActiveBadge(label: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun MovieGridItem(
    movie: Movie,
    isAddMode: Boolean,
    isAdded: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onMovieClick: (View?) -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Column {
            Box(modifier = Modifier.fillMaxWidth()) {
                MoviePosterCard(
                    movie = movie,
                    onClick = onMovieClick,
                    modifier = Modifier.fillMaxWidth()
                )

                // Rating Badge (Top Right Overlay)
                if (movie.voteAverage > 0.0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.75f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = String.format("%.1f", movie.voteAverage),
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = movie.title ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            val year = movie.releaseDate?.take(4) ?: ""
            if (year.isNotEmpty()) {
                Text(
                    text = year,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        if (isAddMode) {
            IconButton(
                onClick = {
                    if (isAdded) onRemoveClick() else onAddClick()
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheet(
    selectedSortBy: String,
    onSortBySelected: (String) -> Unit,
    selectedYear: Int?,
    onYearSelected: (Int?) -> Unit,
    minRating: Float?,
    onMinRatingSelected: (Float?) -> Unit,
    sortOptions: List<SortOption>,
    yearOptions: List<Int?>,
    ratingOptions: List<Float?>,
    onDismiss: () -> Unit,
    onResetFilters: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtros y Ordenación",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                TextButton(onClick = onResetFilters) {
                    Text("Restablecer", color = MaterialTheme.colorScheme.primary)
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = Color.Gray.copy(alpha = 0.2f)
            )

            // 1. Sort Section
            Text(
                text = "Ordenar por",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                sortOptions.forEach { option ->
                    val isSelected = selectedSortBy == option.key
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSortBySelected(option.key) },
                        label = { Text(option.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Release Year Section
            Text(
                text = "Año de Estreno",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(yearOptions) { year ->
                    val label = year?.toString() ?: "Todos"
                    val isSelected = selectedYear == year
                    FilterChip(
                        selected = isSelected,
                        onClick = { onYearSelected(year) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Minimum Rating Section
            Text(
                text = "Puntuación Mínima",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ratingOptions.forEach { rating ->
                    val label = if (rating != null) "⭐ $rating+" else "Todas"
                    val isSelected = minRating == rating
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMinRatingSelected(rating) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(
                    text = "Aplicar Filtros",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
