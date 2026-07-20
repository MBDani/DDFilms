package com.merino.ddfilms.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.MovieListAdapter
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.MovieLists
import com.merino.ddfilms.ui.MovieListActivity
import com.merino.ddfilms.ui.viewModel.MovieListViewModel
import com.merino.ddfilms.utils.Utils.showMessage
import java.util.ArrayList

class ListsFragment : Fragment() {

    private lateinit var adapter: MovieListAdapter
    private val listsList: MutableList<MovieLists> = ArrayList()
    private var lastVisible: DocumentSnapshot? = null
    private var isLoading = false
    private var isLastPage = false
    private val PAGE_SIZE = 10

    private lateinit var initialLoadingProgress: ProgressBar
    private lateinit var loadMoreProgress: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lists, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.lists_recycler_view)
        val createListButton = view.findViewById<LinearLayout>(R.id.create_list_button)
        initialLoadingProgress = view.findViewById(R.id.lists_loading_progress)
        loadMoreProgress = view.findViewById(R.id.lists_load_more_progress)

        adapter = MovieListAdapter(requireContext(), listsList, this::navigateToListMoviesActivity)
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.adapter = adapter

        // Scroll listener for pagination
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager
                if (layoutManager != null && !isLoading && !isLastPage) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.getItemCount()
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= PAGE_SIZE
                    ) {
                        loadNextPage()
                    }
                }
            }
        })

        createListButton.setOnClickListener { showCreateListDialog() }

        return view
    }

    private fun resetPaginationAndLoad() {
        val isFirstLoad = listsList.isEmpty()

        lastVisible = null
        isLastPage = false

        if (isFirstLoad) {
            listsList.clear()
            adapter.notifyDataSetChanged()
            initialLoadingProgress.visibility = View.VISIBLE
        }

        isLoading = true

        FirebaseManager.getInstance().getMovieListsPaginated(
            FirebaseManager.getInstance().getCurrentUserUID(),
            null,
            PAGE_SIZE
        ) { pair, error ->
            isLoading = false
            initialLoadingProgress.visibility = View.GONE
            if (error != null) {
                showMessage(context, getString(R.string.error_loading_lists, error.message))
            } else if (pair != null) {
                val newLists = pair.first
                lastVisible = pair.second

                if (newLists.size < PAGE_SIZE) {
                    isLastPage = true
                }

                listsList.clear()
                listsList.addAll(newLists)
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadNextPage() {
        isLoading = true
        loadMoreProgress.visibility = View.VISIBLE

        FirebaseManager.getInstance().getMovieListsPaginated(
            FirebaseManager.getInstance().getCurrentUserUID(),
            lastVisible,
            PAGE_SIZE
        ) { pair, error ->
            isLoading = false
            loadMoreProgress.visibility = View.GONE
            if (error != null) {
                showMessage(context, getString(R.string.error_generic, error.message))
            } else if (pair != null) {
                val newLists = pair.first
                lastVisible = pair.second

                if (newLists.isEmpty() || newLists.size < PAGE_SIZE) {
                    isLastPage = true
                }

                val startSize = listsList.size
                listsList.addAll(newLists)
                adapter.notifyItemRangeInserted(startSize, newLists.size)
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
