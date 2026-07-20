package com.merino.ddfilms.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.ReviewAdapter
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.Review
import com.merino.ddfilms.ui.MovieDetailActivity
import com.merino.ddfilms.utils.DateFormatter
import com.merino.ddfilms.utils.ReviewUtil

class ReviewsFragment : Fragment() {

    private lateinit var reviewsRecyclerView: RecyclerView
    private var reviewUtil: ReviewUtil? = null
    private var mainNestedScrollView: NestedScrollView? = null
    private var scrollChangeListener: NestedScrollView.OnScrollChangeListener? = null
    private var lastClickTime: Long = 0
    private val CLICK_INTERVAL: Long = 1200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reviews, container, false)
        reviewUtil = ReviewUtil(requireContext())

        initViews(view)
        setupRecyclerViews()

        return view
    }

    override fun onResume() {
        super.onResume()
        reviewUtil?.loadAllReviews()
        mainNestedScrollView?.setOnScrollChangeListener(scrollChangeListener)
    }

    override fun onPause() {
        super.onPause()
        mainNestedScrollView?.setOnScrollChangeListener(null as NestedScrollView.OnScrollChangeListener?)
    }

    private fun initViews(view: View) {
        reviewsRecyclerView = view.findViewById(R.id.recyclerView_fragment_reviews)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_reviews_filter, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_filter) {
            val view = activity?.findViewById<View>(R.id.action_filter)
            showFilterMenu(view ?: activity?.findViewById(R.id.toolbar))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showFilterMenu(anchor: View?) {
        if (anchor == null) return
        val popup = PopupMenu(requireContext(), anchor)
        popup.menu.add(0, 1, 0, "Más recientes")
        popup.menu.add(0, 2, 0, "Más antiguas")
        popup.menu.add(0, 3, 0, "Likes")
        popup.menu.add(0, 4, 0, "Dislikes")
        popup.menu.add(0, 5, 0, "Mejor valoración")
        popup.menu.add(0, 6, 0, "Peor valoración")

        popup.setOnMenuItemClickListener { item ->
            val df = DateFormatter()
            val util = reviewUtil ?: return@setOnMenuItemClickListener false
            when (item.itemId) {
                1 -> {
                    util.sortReviews(df.reviewDateDescComparator())
                    true
                }
                2 -> {
                    util.sortReviews(df.reviewDateAscComparator())
                    true
                }
                3 -> {
                    util.sortReviews(df.reviewLikesDescComparator())
                    true
                }
                4 -> {
                    util.sortReviews(df.reviewDislikesDescComparator())
                    true
                }
                5 -> {
                    util.sortReviews(df.reviewRatingDescComparator())
                    true
                }
                6 -> {
                    util.sortReviews(df.reviewRatingAscComparator())
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun setupRecyclerViews() {
        val util = reviewUtil ?: return
        util.reviewAdapter.setShowMovieInfo(true)
        reviewsRecyclerView.adapter = util.reviewAdapter
        reviewsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        util.setScrollTargets(null, null, reviewsRecyclerView)

        mainNestedScrollView = activity?.findViewById(R.id.main_content_scroll)

        if (mainNestedScrollView != null) {
            scrollChangeListener = NestedScrollView.OnScrollChangeListener { v, _, scrollY, _, _ ->
                val child = v.getChildAt(v.childCount - 1)
                if (child != null) {
                    val diff = child.bottom - (v.height + scrollY)
                    if (diff <= 300) {
                        util.loadNextPage()
                    }
                }
            }
        } else {
            reviewsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)
                    if (dy > 0) {
                        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                        if (layoutManager != null) {
                            val totalItemCount = layoutManager.getItemCount()
                            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                            if (lastVisibleItemPosition != RecyclerView.NO_POSITION && lastVisibleItemPosition >= totalItemCount - 3) {
                                util.loadNextPage()
                            }
                        }
                    }
                }
            })
        }

        util.reviewAdapter.setListener(object : ReviewAdapter.OnReviewInteractionListener {
            override fun onLikeClicked(review: Review, position: Int) {
                util.onLikeClicked(review, position)
            }

            override fun onDislikeClicked(review: Review, position: Int) {
                util.onDislikeClicked(review, position)
            }

            override fun onReviewClicked(review: Review, sharedElement: View) {
                val currentTime = android.os.SystemClock.elapsedRealtime()
                if (currentTime - lastClickTime < CLICK_INTERVAL) {
                    return
                }
                lastClickTime = currentTime

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

                if (sharedElement != null) {
                    val options = android.app.ActivityOptions.makeSceneTransitionAnimation(
                        requireActivity(),
                        sharedElement,
                        "moviePosterTransition"
                    )
                    startActivity(intent, options.toBundle())
                } else {
                    startActivity(intent)
                }
            }
        })
    }
}
