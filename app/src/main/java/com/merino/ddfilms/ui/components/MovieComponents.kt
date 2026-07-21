package com.merino.ddfilms.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.view.View
import androidx.compose.ui.draw.clip
import androidx.compose.ui.viewinterop.AndroidView
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.merino.ddfilms.R
import com.merino.ddfilms.model.Movie

@Composable
fun MoviePosterCard(
    movie: Movie,
    onClick: (View?) -> Unit,
    modifier: Modifier = Modifier,
    onResourceReady: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "ScaleAnimation")

    val posterPath = movie.posterPath
    val imageUrl = if (!posterPath.isNullOrEmpty()) {
        "https://image.tmdb.org/t/p/w500$posterPath"
    } else null

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        AndroidView(
            factory = { ctx ->
                val imageView = ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    transitionName = "moviePosterTransition"
                    clipToOutline = true
                    outlineProvider = object : android.view.ViewOutlineProvider() {
                        override fun getOutline(v: View, outline: android.graphics.Outline) {
                            val radius = 12f * ctx.resources.displayMetrics.density
                            outline.setRoundRect(0, 0, v.width, v.height, radius)
                        }
                    }
                    setOnClickListener { onClick(this) }
                }

                // Load image directly into the ImageView in factory (like the original Java code)
                // This ensures Glide owns the ImageView lifecycle, not Compose state
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(ctx)
                        .load(imageUrl)
                        .placeholder(R.drawable.placeholder_poster)
                        .error(R.drawable.placeholder_poster)
                        .listener(object : com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                            override fun onLoadFailed(
                                e: com.bumptech.glide.load.engine.GlideException?,
                                model: Any?,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                onResourceReady?.invoke()
                                return false
                            }

                            override fun onResourceReady(
                                resource: android.graphics.drawable.Drawable,
                                model: Any,
                                target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>,
                                dataSource: com.bumptech.glide.load.DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                onResourceReady?.invoke()
                                return false
                            }
                        })
                        .into(imageView)
                } else {
                    imageView.setImageResource(R.drawable.placeholder_poster)
                    onResourceReady?.invoke()
                }

                imageView
            },
            // No update block needed — Glide manages the ImageView directly
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        )
    }
}

@Composable
fun ShimmerBrush(showShimmer: Boolean = true, targetValue: Float = 1000f): Brush {
    return if (showShimmer) {
        val shimmerColors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        )

        val transition = rememberInfiniteTransition(label = "ShimmerTransition")
        val translateAnimation by transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "ShimmerTranslate"
        )

        Brush.linearGradient(
            colors = shimmerColors,
            start = Offset.Zero,
            end = Offset(x = translateAnimation, y = translateAnimation)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.Transparent),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

@Composable
fun CinematicRatingBar(
    rating: Float,
    onRatingChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
    maxRating: Int = 5,
    starSize: Dp = 24.dp
) {
    Row(modifier = modifier) {
        for (i in 1..maxRating) {
            val starIndex = i.toFloat()
            val iconRes = when {
                rating >= starIndex -> R.drawable.ic_star_filled
                rating >= starIndex - 0.5f -> R.drawable.ic_star_half
                else -> R.drawable.ic_star_empty
            }
            val tintColor = if (rating >= starIndex - 0.5f) {
                Color(0xFFFFC107) // Gold Color
            } else {
                Color.Gray
            }

            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = tintColor,
                modifier = Modifier
                    .size(starSize)
                    .clickable {
                        val newRating = if (rating == starIndex) {
                            starIndex - 0.5f
                        } else {
                            starIndex
                        }
                        onRatingChanged(newRating)
                    }
            )
        }
    }
}
