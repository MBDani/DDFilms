package com.merino.ddfilms.model

import com.google.firebase.firestore.Exclude
import com.merino.ddfilms.utils.DateFormatter

data class Review(
    var id: String? = null,
    var userId: String? = null,
    var userName: String? = null,
    var userProfileImageUrl: String? = null,
    var movieId: Int = 0,
    var movieTitle: String? = null,
    var posterPath: String? = null,
    var backdropPath: String? = null,
    var rating: Float = 0f,
    var reviewText: String? = null,
    var reviewDate: String? = null,
    var likeCount: List<String>? = null,
    var dislikeCount: List<String>? = null
) {
    @get:Exclude
    @set:Exclude
    var isLikedByCurrentUser: Boolean = false

    @get:Exclude
    @set:Exclude
    var isDislikedByCurrentUser: Boolean = false

    @get:Exclude
    val formattedDate: String
        get() {
            val formatter = DateFormatter()
            return formatter.getFormattedDate(this.reviewDate)
        }
}
