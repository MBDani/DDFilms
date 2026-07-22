package com.merino.ddfilms.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import coil.compose.AsyncImage
import com.google.firebase.firestore.DocumentSnapshot
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.MovieLists
import com.merino.ddfilms.ui.MovieListActivity
import com.merino.ddfilms.ui.viewModel.MovieListViewModel
import com.merino.ddfilms.ui.theme.CinematicTheme
import com.merino.ddfilms.utils.Utils.showMessage

class ListsFragment : Fragment() {

    private val firebaseManager = FirebaseManager.getInstance()
    private val listsListState = mutableStateOf<List<MovieLists>>(emptyList())
    private val isLoadingState = mutableStateOf(false)
    private val isLastPageState = mutableStateOf(false)
    private var lastVisible: DocumentSnapshot? = null
    private val PAGE_SIZE = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        resetPaginationAndLoad()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CinematicTheme {
                    ListsScreen(
                        listsList = listsListState.value,
                        isLoading = isLoadingState.value,
                        pageSize = PAGE_SIZE,
                        onCreateListClick = { showCreateListDialog() },
                        onListClick = { list -> navigateToListMoviesActivity(list) },
                        onLoadMore = {
                            if (!isLoadingState.value && !isLastPageState.value) {
                                loadNextPage()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun resetPaginationAndLoad() {
        lastVisible = null
        isLastPageState.value = false
        if (listsListState.value.isEmpty()) {
            isLoadingState.value = true
        }

        firebaseManager.getMovieListsPaginated(
            firebaseManager.getCurrentUserUID(),
            null,
            PAGE_SIZE
        ) { pair, error ->
            isLoadingState.value = false
            if (error != null) {
                showMessage(context, getString(R.string.error_loading_lists, error.message))
            } else if (pair != null) {
                val newLists = pair.first
                lastVisible = pair.second
                if (newLists.size < PAGE_SIZE) {
                    isLastPageState.value = true
                }
                listsListState.value = newLists
            }
        }
    }

    private fun loadNextPage() {
        if (isLoadingState.value || isLastPageState.value) return
        isLoadingState.value = true
        firebaseManager.getMovieListsPaginated(
            firebaseManager.getCurrentUserUID(),
            lastVisible,
            PAGE_SIZE
        ) { pair, error ->
            isLoadingState.value = false
            if (error != null) {
                showMessage(context, getString(R.string.error_generic, error.message))
            } else if (pair != null) {
                val newLists = pair.first
                lastVisible = pair.second
                if (newLists.isEmpty() || newLists.size < PAGE_SIZE) {
                    isLastPageState.value = true
                }
                val existingIds = listsListState.value.mapNotNull { it.id }.toSet()
                val uniqueNewLists = newLists.filter { it.id != null && !existingIds.contains(it.id) }
                if (uniqueNewLists.isNotEmpty()) {
                    val current = listsListState.value.toMutableList()
                    current.addAll(uniqueNewLists)
                    listsListState.value = current
                }
            }
        }
    }

    private fun showCreateListDialog() {
        val createListDialog = CreateListDialogFragment()
        createListDialog.setOnListCreatedListener(this::createNewList)
        createListDialog.show(childFragmentManager, "CreateListDialog")
    }

    private fun createNewList(listName: String) {
        val vm = ViewModelProvider(this)[MovieListViewModel::class.java]
        vm.createNewMovieList(listName) { result, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (result != null) {
                showMessage(context, result)
                resetPaginationAndLoad()
            }
        }
    }

    private fun navigateToListMoviesActivity(list: MovieLists) {
        val intent = Intent(context, MovieListActivity::class.java).apply {
            putExtra("listID", list.id)
            putExtra("listName", list.name)
        }
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        resetPaginationAndLoad()
    }
}

@Composable
fun ListsScreen(
    listsList: List<MovieLists>,
    isLoading: Boolean,
    pageSize: Int = 10,
    onCreateListClick: () -> Unit,
    onListClick: (MovieLists) -> Unit,
    onLoadMore: () -> Unit
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
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.my_movie_lists),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = onCreateListClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.create_list_btn),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (isLoading && listsList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (listsList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.empty_lists_message),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = listsList,
                        key = { _, list -> list.id ?: list.name ?: System.identityHashCode(list) }
                    ) { index, list ->
                        if (index >= listsList.size - 1 && listsList.size >= pageSize) {
                            LaunchedEffect(index) {
                                onLoadMore()
                            }
                        }

                        MovieListCard(
                            list = list,
                            onClick = { onListClick(list) }
                        )
                    }

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MovieListCard(
    list: MovieLists,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = list.name ?: stringResource(R.string.unnamed_list),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Stack of movie cover circles
                val covers = list.coverPreviews ?: emptyList()
                val count = list.moviesCount

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                ) {
                    if (covers.isEmpty()) {
                        AsyncImage(
                            model = R.drawable.ic_empty_list_placeholder,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        covers.take(3).forEachIndexed { i, path ->
                            val fullPath = if (path.startsWith("http") || path.startsWith("file")) path else "https://image.tmdb.org/t/p/w200$path"
                            AsyncImage(
                                model = fullPath,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .padding(start = (i * 34).dp)
                                    .size(58.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            )
                        }

                        if (count > 3) {
                            val extra = count - 3
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .padding(start = (3 * 34).dp)
                                    .size(58.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            ) {
                                Text(
                                    text = "+${if (extra > 99) 99 else extra}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Stack of user profile avatars with Primary accent border stroke
                val userIDs = list.userID ?: emptyList()
                val denormalizedAvatars = list.memberAvatarsPreview ?: emptyList()

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-8).dp)
                ) {
                    userIDs.take(4).forEachIndexed { i, uid ->
                        var avatarUrl by remember { mutableStateOf<String?>(null) }

                        LaunchedEffect(uid) {
                            if (i < denormalizedAvatars.size) {
                                avatarUrl = denormalizedAvatars[i]
                            }
                            FirebaseManager.getInstance().getUserProfileImageUrl(uid) { liveUrl, _ ->
                                if (liveUrl != null) {
                                    avatarUrl = liveUrl
                                }
                            }
                        }

                        val resolvedUrl = if (avatarUrl.isNullOrEmpty()) {
                            R.drawable.ic_default_profile
                        } else if (avatarUrl!!.startsWith("http") || avatarUrl!!.startsWith("file")) {
                            avatarUrl
                        } else if (avatarUrl!!.startsWith("avatars/")) {
                            "file:///android_asset/$avatarUrl"
                        } else {
                            "file:///android_asset/avatars/${if (avatarUrl!!.endsWith(".png")) avatarUrl else "$avatarUrl.png"}"
                        }

                        AsyncImage(
                            model = resolvedUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface)
                                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        )
                    }

                    if (userIDs.size > 4) {
                        Text(
                            text = "+${userIDs.size - 4}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 12.dp)
                        )
                    }
                }
            }
        }
    }
}
