package com.merino.ddfilms.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.ui.components.MoviePosterCard
import com.merino.ddfilms.ui.theme.CinematicTheme
import com.merino.ddfilms.utils.StringUtils.MOVIE_LIST
import com.merino.ddfilms.utils.Utils.showMessage

class MovieListActivity : AppCompatActivity() {

    private val firebaseManager = FirebaseManager.getInstance()
    private val movieListState = mutableStateOf<List<Movie>>(emptyList())
    private val usersListState = mutableStateOf<List<String>>(emptyList())
    private val isLoadingState = mutableStateOf(true)
    private val isEditModeState = mutableStateOf(false)
    private val showBottomSheetState = mutableStateOf(false)
    private val showDeleteDialogState = mutableStateOf(false)
    private val showEditTitleDialogState = mutableStateOf(false)

    private var listID: String? = null
    private var listNameState = mutableStateOf("")
    private var userID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        listID = intent.getStringExtra("listID") ?: intent.getStringExtra("documentID")
        listNameState.value = intent.getStringExtra("listName") ?: "Lista"
        userID = firebaseManager.getCurrentUserUID()

        setContent {
            CinematicTheme {
                MovieListScreen(
                    listName = listNameState.value,
                    movies = movieListState.value,
                    usersList = usersListState.value,
                    isLoading = isLoadingState.value,
                    isEditMode = isEditModeState.value,
                    showBottomSheet = showBottomSheetState.value,
                    showDeleteDialog = showDeleteDialogState.value,
                    onBackClick = { finish() },
                    onEditModeToggle = { isEditModeState.value = !isEditModeState.value },
                    onMoreClick = { showBottomSheetState.value = true },
                    onDismissBottomSheet = { showBottomSheetState.value = false },
                    onEditListClick = {
                        showBottomSheetState.value = false
                        showEditTitleDialogState.value = true
                    },
                    onShareListClick = {
                        showBottomSheetState.value = false
                        shareList()
                    },
                    onDeleteListClick = {
                        showBottomSheetState.value = false
                        showDeleteDialogState.value = true
                    },
                    onDismissDeleteDialog = { showDeleteDialogState.value = false },
                    onConfirmDeleteList = {
                        showDeleteDialogState.value = false
                        deleteList()
                    },
                    onAddMovieClick = {
                        val intent = Intent(this, SearchActivity::class.java).apply {
                            putExtra("collection", MOVIE_LIST)
                            putExtra("documentID", listID)
                            putExtra("listName", listNameState.value)
                            val moviesID = movieListState.value.map { it.id }.toIntArray()
                            putExtra("moviesID", moviesID)
                        }
                        startActivity(intent)
                    },
                    onMovieClick = { movie, view ->
                        val intent = Intent(this@MovieListActivity, MovieDetailActivity::class.java).apply {
                            putExtra("movie", movie)
                        }
                        if (view != null) {
                            val options = androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                                this@MovieListActivity,
                                view,
                                "moviePosterTransition"
                            )
                            androidx.core.app.ActivityCompat.startActivity(this@MovieListActivity, intent, options.toBundle())
                        } else {
                            startActivity(intent)
                        }
                    },
                    onDeleteMovieClick = { index, movie ->
                        deleteMovieFromList(index, movie)
                    },
                    onUpdateAddedBy = { movie, newAddedBy ->
                        updateMovieAddedBy(movie, newAddedBy)
                    }
                )
            }
        }

        loadMoviesFromList()
    }

    override fun onResume() {
        super.onResume()
        loadMoviesFromList(showLoading = false)
    }

    private fun loadMoviesFromList(showLoading: Boolean = true) {
        val id = listID ?: return
        if (showLoading && movieListState.value.isEmpty()) {
            isLoadingState.value = true
        }
        firebaseManager.getMoviesFromList(id, MOVIE_LIST) { movies, error ->
            isLoadingState.value = false
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (movies != null) {
                movieListState.value = movies.reversed()
                loadListUsers()
            }
        }
    }

    private fun loadListUsers() {
        val id = listID ?: return
        firebaseManager.getListUsersIDsAndNames(id) { mapUsersResult, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (mapUsersResult != null) {
                val rawNames = ArrayList(mapUsersResult.values)
                val uniqueUsers = ArrayList<String>()
                for (user in rawNames) {
                    if (!uniqueUsers.contains(user)) {
                        uniqueUsers.add(user)
                    }
                }
                usersListState.value = uniqueUsers
            }
        }
    }

    private fun updateMovieAddedBy(movie: Movie, newAddedBy: String) {
        val id = listID ?: return
        movie.addedBy = newAddedBy
        firebaseManager.updateMovieAddedBy(movie.id, newAddedBy, id) { result, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (result != null) {
                showMessage(applicationContext, getString(R.string.movie_updated_success))
                movieListState.value = movieListState.value.toList()
            }
        }
    }

    private fun deleteMovieFromList(index: Int, movie: Movie) {
        val id = listID ?: return
        val current = movieListState.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            movieListState.value = current
        }

        firebaseManager.deleteMovieFromList(MOVIE_LIST, id, movie) { _, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
                loadMoviesFromList(showLoading = false)
            }
        }
    }

    private fun deleteList() {
        val id = listID ?: return
        firebaseManager.deleteList(id) { result, error ->
            if (error != null) {
                showMessage(applicationContext, error.message)
            } else if (result != null) {
                finish()
            }
        }
    }

    private fun shareList() {
        val id = listID ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Lista de películas: ${listNameState.value}")
            putExtra(
                Intent.EXTRA_TEXT,
                "¡Echa un vistazo a mi lista de películas \"${listNameState.value}\" en DDFilms! Código de lista: $id"
            )
        }
        startActivity(Intent.createChooser(shareIntent, "Compartir lista vía"))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieListScreen(
    listName: String,
    movies: List<Movie>,
    usersList: List<String>,
    isLoading: Boolean,
    isEditMode: Boolean,
    showBottomSheet: Boolean,
    showDeleteDialog: Boolean,
    onBackClick: () -> Unit,
    onEditModeToggle: () -> Unit,
    onMoreClick: () -> Unit,
    onDismissBottomSheet: () -> Unit,
    onEditListClick: () -> Unit,
    onShareListClick: () -> Unit,
    onDeleteListClick: () -> Unit,
    onDismissDeleteDialog: () -> Unit,
    onConfirmDeleteList: () -> Unit,
    onAddMovieClick: () -> Unit,
    onMovieClick: (Movie, View?) -> Unit,
    onDeleteMovieClick: (Int, Movie) -> Unit,
    onUpdateAddedBy: (Movie, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        text = listName,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
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

                    IconButton(onClick = onMoreClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_more_vert),
                            contentDescription = "Opciones",
                            tint = MaterialTheme.colorScheme.onBackground
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
                } else if (movies.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Esta lista aún no contiene películas.",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 88.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            movies,
                            key = { index, movie -> "${movie.id}_${movie.createdAt ?: ""}_${movie.title ?: ""}_$index" }
                        ) { index, movie ->
                            val showDateHeader = movie.createdAt != null &&
                                    (index == 0 || movies[index - 1].createdAt != movie.createdAt)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                if (showDateHeader) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        Text(
                                            text = "🗓️ Añadida el ${movie.createdAt}",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }

                                MovieListItemCard(
                                    movie = movie,
                                    usersList = usersList,
                                    isEditMode = isEditMode,
                                    onMovieClick = onMovieClick,
                                    onDeleteClick = { onDeleteMovieClick(index, movie) },
                                    onUpdateAddedBy = { newAddedBy -> onUpdateAddedBy(movie, newAddedBy) }
                                )
                            }
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = onAddMovieClick,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add),
                    contentDescription = "Añadir película"
                )
            }
        }
    }

    // Modal Bottom Sheet Options
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = onDismissBottomSheet,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Opciones de la lista",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                ListItem(
                    headlineContent = { Text("Editar nombre de la lista") },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_edit),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { onEditListClick() }
                )

                ListItem(
                    headlineContent = { Text("Compartir lista") },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_share),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier.clickable { onShareListClick() }
                )

                ListItem(
                    headlineContent = { Text("Eliminar lista", color = Color(0xFFBA1A1A)) },
                    leadingContent = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_delete),
                            contentDescription = null,
                            tint = Color(0xFFBA1A1A)
                        )
                    },
                    modifier = Modifier.clickable { onDeleteListClick() }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = onDismissDeleteDialog,
            title = { Text("¿Eliminar lista?") },
            text = { Text("¿Estás seguro de que deseas eliminar la lista '$listName'? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = onConfirmDeleteList) {
                    Text("Eliminar", color = Color(0xFFBA1A1A), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissDeleteDialog) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun MovieListItemCard(
    movie: Movie,
    usersList: List<String>,
    isEditMode: Boolean,
    onMovieClick: (Movie, View?) -> Unit,
    onDeleteClick: () -> Unit,
    onUpdateAddedBy: (String) -> Unit
) {
    var expandedUserMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMovieClick(movie, null) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster Image
            MoviePosterCard(
                movie = movie,
                onClick = { view -> onMovieClick(movie, view) },
                modifier = Modifier
                    .width(100.dp)
                    .height(150.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Movie Info Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = movie.title ?: "",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!movie.releaseDate.isNullOrEmpty()) {
                        Text(
                            text = "(${movie.releaseDate})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    if (movie.voteAverage > 0) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "⭐ ${String.format("%.1f", movie.voteAverage)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val overview = movie.overview
                if (!overview.isNullOrEmpty()) {
                    Text(
                        text = overview,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Added By Selector Dropdown / Chip
                Box {
                    val currentAddedBy = if (!movie.addedBy.isNullOrEmpty()) movie.addedBy else "Común"
                    AssistChip(
                        onClick = { expandedUserMenu = true },
                        label = {
                            Text(
                                text = "👤 Añadida por: $currentAddedBy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_edit),
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    DropdownMenu(
                        expanded = expandedUserMenu,
                        onDismissRequest = { expandedUserMenu = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        val options = remember(usersList) {
                            val list = ArrayList<String>()
                            for (user in usersList) {
                                if (!list.contains(user)) list.add(user)
                            }
                            if (!list.contains("Común")) list.add("Común")
                            list
                        }

                        options.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontWeight = if (option == currentAddedBy) FontWeight.Bold else FontWeight.Normal,
                                        color = if (option == currentAddedBy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    expandedUserMenu = false
                                    onUpdateAddedBy(option)
                                }
                            )
                        }
                    }
                }
            }

            if (isEditMode) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_delete),
                        contentDescription = "Eliminar",
                        tint = Color(0xFFBA1A1A)
                    )
                }
            }
        }
    }
}
