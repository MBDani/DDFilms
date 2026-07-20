package com.merino.ddfilms.utils

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.ReviewAdapter
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.Review
import com.merino.ddfilms.ui.fragment.WriteReviewDialogFragment
import com.merino.ddfilms.utils.Utils.showMessage
import java.util.ArrayList
import java.util.Date

class ReviewUtil(private val context: Context) :
    ReviewAdapter.OnReviewInteractionListener,
    WriteReviewDialogFragment.OnReviewSubmittedListener {

    var reviewsList: MutableList<Review> = ArrayList()
        private set
    var allReviewsList: MutableList<Review> = ArrayList()
        private set
    private var currentPage = 1
    private var isPageLoading = false
    private val PAGE_SIZE = 15
    private var currentComparator: Comparator<Review>
    val reviewAdapter: ReviewAdapter
    var userReview: Review? = null
        private set

    private val firebaseManager = FirebaseManager()
    private val dateFormatter = DateFormatter()
    private var userId: String? = null
    var userName: String? = null
    var currentMovie: Movie? = null
    var highlightReviewId: String? = null

    // Scroll targets
    private var appBarLayout: AppBarLayout? = null
    private var nestedScrollView: NestedScrollView? = null
    private var recyclerView: RecyclerView? = null

    fun interface OnReviewsLoadedListener {
        fun onReviewsLoaded()
    }

    private var reviewsLoadedListener: OnReviewsLoadedListener? = null

    fun setOnReviewsLoadedListener(listener: OnReviewsLoadedListener?) {
        this.reviewsLoadedListener = listener
    }

    init {
        userId = firebaseManager.getCurrentUserUID()
        reviewAdapter = ReviewAdapter(reviewsList, this)
        currentComparator = dateFormatter.reviewDateDescComparator()
    }

    fun setScrollTargets(
        appBarLayout: AppBarLayout?,
        nestedScrollView: NestedScrollView?,
        recyclerView: RecyclerView?
    ) {
        this.appBarLayout = appBarLayout
        this.nestedScrollView = nestedScrollView
        this.recyclerView = recyclerView

        nestedScrollView?.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
            val child = v.getChildAt(v.childCount - 1)
            if (child != null) {
                val diff = child.bottom - (v.height + scrollY)
                if (diff <= 300) {
                    loadNextPage()
                }
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    fun loadAllReviews() {
        firebaseManager.getAllReviews { reviews, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (reviews != null) {
                getCurrentUserReview(reviews)
                setUpLikeAndDislikeReviews(reviews)

                allReviewsList.clear()
                allReviewsList.addAll(reviews)

                allReviewsList.sortWith(currentComparator)

                currentPage = 1
                isPageLoading = false
                reviewsList.clear()
                val end = Math.min(PAGE_SIZE, allReviewsList.size)
                reviewsList.addAll(allReviewsList.subList(0, end))

                runOnMain { reviewAdapter.notifyDataSetChanged() }
            }
        }
    }

    fun loadNextPage() {
        if (isPageLoading) return

        val start = currentPage * PAGE_SIZE
        val end = Math.min(start + PAGE_SIZE, allReviewsList.size)

        if (start < end) {
            isPageLoading = true
            val nextPageItems = allReviewsList.subList(start, end)
            reviewsList.addAll(nextPageItems)
            currentPage++
            runOnMain {
                reviewAdapter.notifyItemRangeInserted(start, nextPageItems.size)
                Handler(Looper.getMainLooper()).post { isPageLoading = false }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun loadMovieReviews(movieId: Int) {
        firebaseManager.getReviews(movieId) { reviews, error ->
            if (error != null) {
                showMessage(context, error.message)
                reviewsLoadedListener?.let { runOnMain { it.onReviewsLoaded() } }
            } else if (reviews != null) {
                getCurrentUserReview(reviews)
                setUpLikeAndDislikeReviews(reviews)

                allReviewsList.clear()
                allReviewsList.addAll(reviews)

                allReviewsList.sortWith(currentComparator)

                currentPage = 1
                isPageLoading = false
                reviewsList.clear()
                val end = Math.min(PAGE_SIZE, allReviewsList.size)
                reviewsList.addAll(allReviewsList.subList(0, end))

                runOnMain {
                    reviewAdapter.notifyDataSetChanged()
                    reviewsLoadedListener?.onReviewsLoaded()
                }
            } else {
                reviewsLoadedListener?.let { runOnMain { it.onReviewsLoaded() } }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun sortReviews(comparator: Comparator<Review>) {
        this.currentComparator = comparator
        if (allReviewsList.isNotEmpty()) {
            allReviewsList.sortWith(comparator)
            currentPage = 1
            isPageLoading = false
            reviewsList.clear()
            val end = Math.min(PAGE_SIZE, allReviewsList.size)
            reviewsList.addAll(allReviewsList.subList(0, end))
            runOnMain { reviewAdapter.notifyDataSetChanged() }
        }
    }

    fun postReview(review: Review) {
        if (userId == null) userId = firebaseManager.getCurrentUserUID()

        if (userName == null) {
            firebaseManager.getUserName(userId!!) { name, _ ->
                userName = name ?: "Usuario"
                review.userName = userName
                proceedToPost(review)
            }
        } else {
            review.userName = userName
            proceedToPost(review)
        }
    }

    private fun proceedToPost(review: Review) {
        review.userId = userId
        val movie = currentMovie
        if (movie != null) {
            review.movieId = movie.id
            review.movieTitle = movie.title
            review.posterPath = movie.posterPath
            review.backdropPath = movie.backdropPath
        }
        review.reviewDate = Date().toString()
        if (review.likeCount == null) review.likeCount = ArrayList()
        if (review.dislikeCount == null) review.dislikeCount = ArrayList()

        firebaseManager.getUserProfileImageUrl(userId!!) { imageUrl, _ ->
            if (imageUrl != null) {
                review.userProfileImageUrl = imageUrl
            }
            firebaseManager.postReview(review) { reviewResponse, error ->
                if (error != null) {
                    showMessage(context, error.message)
                } else {
                    runOnMain {
                        addReviewToRecycler(reviewResponse!!)
                        if (appBarLayout != null && nestedScrollView != null && recyclerView != null) {
                            scrollToNewReview()
                        }
                    }
                }
            }
        }
    }

    fun addReviewToRecycler(review: Review) {
        userReview?.let { ur ->
            allReviewsList.removeIf { it.id == ur.id }
            reviewsList.removeIf { it.id == ur.id }
        }

        allReviewsList.add(0, review)
        allReviewsList.sortWith(currentComparator)

        currentPage = 1
        isPageLoading = false
        reviewsList.clear()
        val end = Math.min(PAGE_SIZE, allReviewsList.size)
        reviewsList.addAll(allReviewsList.subList(0, end))

        runOnMain { reviewAdapter.notifyDataSetChanged() }

        userReview = review
    }

    override fun onReviewSubmitted(review: Review) {
        if (review.id != null) {
            updateReview(review)
        } else {
            postReview(review)
        }
    }

    override fun onReviewDeleted(review: Review) {
        if (review.id != null) {
            firebaseManager.deleteReview(review.id!!) { _, error ->
                if (error != null) {
                    showMessage(context, error.message)
                    return@deleteReview
                }
                runOnMain {
                    allReviewsList.removeIf { it.id == review.id }
                    reviewsList.removeIf { it.id == review.id }
                    if (userReview?.id == review.id) {
                        userReview = null
                    }
                    reviewAdapter.notifyDataSetChanged()
                    showMessage(context, context.getString(R.string.review_deleted_success))
                }
            }
        }
    }

    private fun updateReview(review: Review) {
        review.reviewDate = Date().toString()
        review.likeCount = ArrayList()
        review.dislikeCount = ArrayList()

        firebaseManager.updateReview(review) { reviewResponse, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else {
                runOnMain {
                    addReviewToRecycler(reviewResponse!!)
                    if (appBarLayout != null && nestedScrollView != null && recyclerView != null) {
                        scrollToNewReview()
                    }
                }
            }
        }
    }

    private fun getCurrentUserReview(reviews: List<Review>) {
        for (review in reviews) {
            if (review.userId == userId) {
                userReview = review
                break
            }
        }
    }

    // --------------------------------- Scroll  ------------------------------------------
    fun scrollToNewReview() {
        appBarLayout?.setExpanded(false, true)

        recyclerView?.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                recyclerView?.removeOnLayoutChangeListener(this)

                nestedScrollView?.post {
                    val scrollV = nestedScrollView ?: return@post
                    val recyclerV = recyclerView ?: return@post
                    val startY = scrollV.scrollY
                    val targetY = recyclerV.top

                    val scrollAnim = ValueAnimator.ofInt(startY, targetY).apply {
                        duration = 1200
                        interpolator = DecelerateInterpolator()
                        addUpdateListener { anim ->
                            val y = anim.animatedValue as Int
                            scrollV.scrollTo(0, y)
                        }
                    }
                    scrollAnim.start()

                    val newItem = recyclerV.layoutManager?.findViewByPosition(0)
                    newItem?.apply {
                        alpha = 0f
                        translationY = 50f
                        animate()
                            .alpha(1f)
                            .translationY(0f)
                            .setDuration(1500)
                            .setInterpolator(DecelerateInterpolator())
                            .start()
                    }
                }
            }
        })
    }

    fun scrollToReview(reviewId: String?) {
        if (reviewsList.isEmpty() || reviewId == null) return

        var index = -1
        for (i in reviewsList.indices) {
            if (reviewsList[i].id == reviewId) {
                index = i
                break
            }
        }

        if (index != -1) {
            val targetIndex = index
            recyclerView?.postDelayed({
                appBarLayout?.setExpanded(false, true)

                val lm = recyclerView?.layoutManager ?: return@postDelayed
                val view = lm.findViewByPosition(targetIndex)
                if (view == null) {
                    recyclerView?.scrollToPosition(targetIndex)
                }

                recyclerView?.postDelayed({
                    val targetView = lm.findViewByPosition(targetIndex)
                    val scrollV = nestedScrollView
                    val recyclerV = recyclerView

                    if (targetView != null && scrollV != null && recyclerV != null) {
                        val startY = scrollV.scrollY
                        val targetY = (recyclerV.y + targetView.top).toInt()

                        val scrollAnim = ValueAnimator.ofInt(startY, targetY).apply {
                            duration = 1200
                            interpolator = DecelerateInterpolator()
                            addUpdateListener { anim ->
                                val y = anim.animatedValue as Int
                                scrollV.scrollTo(0, y)
                            }
                        }
                        scrollAnim.start()

                        runOnMain {
                            targetView.alpha = 0.2f
                            targetView.animate().alpha(1f).setDuration(1000).start()

                            val originalColor = android.graphics.Color.TRANSPARENT
                            val highlightColor = ContextCompat.getColor(context, R.color.white_60)

                            val colorAnim = ValueAnimator.ofArgb(highlightColor, originalColor).apply {
                                duration = 2000
                                addUpdateListener { anim -> targetView.setBackgroundColor(anim.animatedValue as Int) }
                            }
                            colorAnim.start()
                        }
                    } else targetView?.let {
                        recyclerView?.smoothScrollToPosition(targetIndex)
                    }
                }, 200)

            }, 500)
        }
    }

    // --------------------- Likes / dislikes  ------------------------------------------

    override fun onLikeClicked(review: Review, position: Int) {
        if (review.likeCount?.contains(userId) == true) {
            removeLike(review, position)
        } else {
            addLike(review, position)
        }
    }

    override fun onDislikeClicked(review: Review, position: Int) {
        if (review.dislikeCount?.contains(userId) == true) {
            removeDislike(review, position)
        } else {
            addDislike(review, position)
        }
    }

    override fun onReviewClicked(review: Review, sharedElement: View) {
        if (review.userId != null && review.userId == userId) {
            if (context is androidx.fragment.app.FragmentActivity) {
                var movie = currentMovie
                if (movie == null) {
                    movie = Movie().apply {
                        id = review.movieId
                        title = review.movieTitle
                        posterPath = review.posterPath
                        backdropPath = review.backdropPath
                    }
                }

                val dialog = WriteReviewDialogFragment(movie, review)
                dialog.setOnReviewSubmittedListener(this)

                val fm = context.supportFragmentManager
                dialog.show(fm, "WriteReviewDialogFragment")
            }
        }
    }

    private fun setUpLikeAndDislikeReviews(reviews: List<Review>) {
        for (review in reviews) {
            review.isLikedByCurrentUser = review.likeCount?.contains(userId) == true
            review.isDislikedByCurrentUser = review.dislikeCount?.contains(userId) == true
        }
    }

    private fun addDislike(review: Review, position: Int) {
        firebaseManager.reviewAddDislike(review.id!!, userId!!) { _, error ->
            if (error != null) {
                showMessage(context, error.message)
                return@reviewAddDislike
            }
            val dislikes = review.dislikeCount?.toMutableList() ?: ArrayList()
            dislikes.add(userId!!)
            review.dislikeCount = dislikes
            review.isDislikedByCurrentUser = true

            if (review.likeCount?.contains(userId) == true) {
                removeLike(review, position)
            } else {
                reviewAdapter.updateReview(position, review)
                reviewAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun addLike(review: Review, position: Int) {
        firebaseManager.reviewAddLike(review.id!!, userId!!) { _, error ->
            if (error != null) {
                showMessage(context, error.message)
                return@reviewAddLike
            }
            val likes = review.likeCount?.toMutableList() ?: ArrayList()
            likes.add(userId!!)
            review.likeCount = likes
            review.isLikedByCurrentUser = true

            if (review.dislikeCount?.contains(userId) == true) {
                removeDislike(review, position)
            } else {
                runOnMain {
                    reviewAdapter.updateReview(position, review)
                    reviewAdapter.notifyItemChanged(position)
                }
            }
        }
    }

    private fun removeDislike(review: Review, position: Int) {
        firebaseManager.reviewRemoveDislike(review.id!!, userId!!) { _, error ->
            if (error != null) {
                showMessage(context, error.message)
                return@reviewRemoveDislike
            }
            val dislikes = review.dislikeCount?.toMutableList() ?: ArrayList()
            dislikes.remove(userId)
            review.dislikeCount = dislikes
            review.isDislikedByCurrentUser = false
            runOnMain {
                reviewAdapter.updateReview(position, review)
                reviewAdapter.notifyItemChanged(position)
            }
        }
    }

    private fun removeLike(review: Review, position: Int) {
        firebaseManager.reviewRemoveLike(review.id!!, userId!!) { _, error ->
            if (error != null) {
                showMessage(context, error.message)
                return@reviewRemoveLike
            }
            val likes = review.likeCount?.toMutableList() ?: ArrayList()
            likes.remove(userId)
            review.likeCount = likes
            review.isLikedByCurrentUser = false
            runOnMain { reviewAdapter.notifyItemChanged(position) }
        }
    }

    private fun runOnMain(r: Runnable?) {
        if (r == null) return
        Handler(Looper.getMainLooper()).post(r)
    }
}
