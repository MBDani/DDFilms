package com.merino.ddfilms

import com.merino.ddfilms.model.Review
import org.junit.Assert.*
import org.junit.Test

class ReviewTest {

    @Test
    fun testReviewLikeStateAndEquality() {
        val review1 = Review(
            id = "review_123",
            userId = "user_author",
            likeCount = listOf("user_1"),
            dislikeCount = emptyList(),
            isLikedByCurrentUser = false,
            isDislikedByCurrentUser = false
        )

        // When liking:
        val reviewLiked = review1.copy(
            likeCount = listOf("user_1", "user_current"),
            isLikedByCurrentUser = true
        )

        assertNotEquals(review1, reviewLiked)
        assertEquals(2, reviewLiked.likeCount?.size)
        assertTrue(reviewLiked.isLikedByCurrentUser)
        assertFalse(reviewLiked.isDislikedByCurrentUser)

        // When switching from like to dislike:
        val reviewDisliked = reviewLiked.copy(
            likeCount = listOf("user_1"),
            dislikeCount = listOf("user_current"),
            isLikedByCurrentUser = false,
            isDislikedByCurrentUser = true
        )

        assertNotEquals(reviewLiked, reviewDisliked)
        assertEquals(1, reviewDisliked.likeCount?.size)
        assertEquals(1, reviewDisliked.dislikeCount?.size)
        assertFalse(reviewDisliked.isLikedByCurrentUser)
        assertTrue(reviewDisliked.isDislikedByCurrentUser)

        // When removing dislike:
        val reviewNeutral = reviewDisliked.copy(
            dislikeCount = emptyList(),
            isDislikedByCurrentUser = false
        )

        assertNotEquals(reviewDisliked, reviewNeutral)
        assertEquals(0, reviewNeutral.dislikeCount?.size)
        assertFalse(reviewNeutral.isLikedByCurrentUser)
        assertFalse(reviewNeutral.isDislikedByCurrentUser)
    }
}
