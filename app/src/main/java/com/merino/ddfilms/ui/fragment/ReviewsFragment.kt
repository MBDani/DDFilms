package com.merino.ddfilms.ui.fragment

import android.content.Intent
import com.merino.ddfilms.ui.MainActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import com.merino.ddfilms.utils.DateFormatter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import coil.compose.AsyncImage
import com.merino.ddfilms.R
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.Review
import com.merino.ddfilms.ui.MovieDetailActivity
import com.merino.ddfilms.ui.components.CinematicRatingBar
import com.merino.ddfilms.ui.theme.CinematicTheme
import com.merino.ddfilms.utils.ReviewUtil

class ReviewsFragment : Fragment() {

    private lateinit var reviewUtil: ReviewUtil
    private val reviewsState = mutableStateOf<List<Review>>(emptyList())
    private val currentFilterState = mutableStateOf("recientes")
    private val dateFormatter = DateFormatter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        reviewUtil = ReviewUtil(requireContext())
        loadReviews()

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                CinematicTheme {
                    ReviewsScreen(
                        reviews = reviewsState.value,
                        currentFilter = currentFilterState.value,
                        onFilterChange = { filter ->
                            currentFilterState.value = filter
                            applyFilter(filter)
                        },
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
                        onReviewClick = { review, view ->
                            val intent = Intent(requireContext(), MovieDetailActivity::class.java).apply {
                                val movie = Movie().apply {
                                    id = review.movieId
                                    title = review.movieTitle
                                    posterPath = review.posterPath
                                    backdropPath = review.backdropPath
                                }
                                putExtra("movie", movie)
                                putExtra("highlight_review_id", review.id)
                            }
                            if (view != null) {
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

    private fun loadReviews() {
        reviewUtil.setOnReviewsLoadedListener {
            reviewsState.value = reviewUtil.reviewsList.toList()
        }
        reviewUtil.loadAllReviews()

        reviewUtil.reviewAdapter.registerAdapterDataObserver(object : androidx.recyclerview.widget.RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                reviewsState.value = reviewUtil.reviewsList.toList()
            }
        })
    }

    private fun applyFilter(filter: String) {
        when (filter) {
            "recientes" -> reviewUtil.sortReviews(dateFormatter.reviewDateDescComparator())
            "antiguas" -> reviewUtil.sortReviews(dateFormatter.reviewDateAscComparator())
            "likes" -> reviewUtil.sortReviews(dateFormatter.reviewLikesDescComparator())
            "dislikes" -> reviewUtil.sortReviews(dateFormatter.reviewDislikesDescComparator())
            "mejor_valoracion" -> reviewUtil.sortReviews(dateFormatter.reviewRatingDescComparator())
            "peor_valoracion" -> reviewUtil.sortReviews(dateFormatter.reviewRatingAscComparator())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    reviews: List<Review>,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    onLikeClick: (Review) -> Unit,
    onDislikeClick: (Review) -> Unit,
    onReviewClick: (Review, View?) -> Unit,
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
                    text = "Reseñas de la Comunidad",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
            }

        ScrollableTabRow(
            selectedTabIndex = getFilterIndex(currentFilter),
            edgePadding = 16.dp,
            divider = {},
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            val filters = listOf(
                "recientes" to "Más Recientes",
                "antiguas" to "Más Antiguas",
                "likes" to "Likes",
                "dislikes" to "Dislikes",
                "mejor_valoracion" to "Mejor Valoración",
                "peor_valoracion" to "Peor Valoración"
            )
            filters.forEachIndexed { index, (id, label) ->
                Tab(
                    selected = currentFilter == id,
                    onClick = { onFilterChange(id) },
                    text = { Text(label, fontSize = 13.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (reviews.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No hay reseñas disponibles.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(reviews) { index, review ->
                    ReviewsItemCard(
                        review = review,
                        onLikeClick = onLikeClick,
                        onDislikeClick = onDislikeClick,
                        onCardClick = { view -> onReviewClick(review, view) }
                    )
                }
            }
        }
    }
}
}

private fun getFilterIndex(filter: String): Int {
    return when (filter) {
        "recientes" -> 0
        "antiguas" -> 1
        "likes" -> 2
        "dislikes" -> 3
        "mejor_valoracion" -> 4
        "peor_valoracion" -> 5
        else -> 0
    }
}

@Composable
fun ReviewsItemCard(
    review: Review,
    onLikeClick: (Review) -> Unit,
    onDislikeClick: (Review) -> Unit,
    onCardClick: (View?) -> Unit,
    modifier: Modifier = Modifier
) {
    var posterViewRef by remember { mutableStateOf<View?>(null) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCardClick(posterViewRef) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = review.userProfileImageUrl ?: R.drawable.ic_default_profile,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
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
                        DateFormatter().getFormattedDate(review.reviewDate)
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                CinematicRatingBar(
                    rating = review.rating ?: 0f,
                    onRatingChanged = {},
                    starSize = 12.dp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Movie Info Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp)
            ) {
                val posterUrl = if (!review.posterPath.isNullOrEmpty()) {
                    "https://image.tmdb.org/t/p/w200${review.posterPath}"
                } else null

                AndroidView(
                    factory = { ctx ->
                        ImageView(ctx).apply {
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            transitionName = "moviePosterTransition"
                            clipToOutline = true
                            outlineProvider = object : android.view.ViewOutlineProvider() {
                                override fun getOutline(v: View, outline: android.graphics.Outline) {
                                    val radius = 4f * ctx.resources.displayMetrics.density
                                    outline.setRoundRect(0, 0, v.width, v.height, radius)
                                }
                            }
                            posterViewRef = this
                        }
                    },
                    update = { imageView ->
                        posterViewRef = imageView
                        if (!posterUrl.isNullOrEmpty()) {
                            Glide.with(imageView.context)
                                .load(posterUrl)
                                .placeholder(R.drawable.placeholder_poster)
                                .error(R.drawable.placeholder_poster)
                                .into(imageView)
                        } else {
                            imageView.setImageResource(R.drawable.placeholder_poster)
                        }
                    },
                    modifier = Modifier
                        .width(40.dp)
                        .height(60.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = review.movieTitle ?: "Película",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Review Content
            Text(
                text = review.reviewText ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Likes/Dislikes Row
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
                        modifier = Modifier.size(16.dp)
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
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = (review.dislikeCount?.size ?: 0).toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (disliked) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }
    }
}
