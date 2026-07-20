package com.merino.ddfilms.ui.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.MovieAdapter
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.ui.MainActivity
import com.merino.ddfilms.ui.SearchActivity
import com.merino.ddfilms.ui.components.Fab.FabHost
import com.merino.ddfilms.ui.components.Fab.ShowsFab
import com.merino.ddfilms.utils.StringUtils.DIARY_LIST
import com.merino.ddfilms.utils.Utils.showMessage
import java.util.ArrayList

class DiaryFragment : Fragment(), FabHost, ShowsFab {

    private lateinit var movieAdapter: MovieAdapter
    private lateinit var movieListRecyclerView: RecyclerView
    private var isEditMode = false
    private var doneMenuItem: MenuItem? = null
    private val firebaseManager = FirebaseManager()
    private var movieList: MutableList<Movie> = ArrayList()
    private val userID = firebaseManager.getCurrentUserUID()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_watchlist, container, false)
        setHasOptionsMenu(true)

        setupViews(view)
        setupRecyclerView()
        loadMoviesFromList()

        return view
    }

    private fun setupViews(view: View) {
        movieListRecyclerView = view.findViewById(R.id.movie_list_recycler_view)
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter()
        movieAdapter.setOnItemLongClickListener { _ ->
            if (!isEditMode) {
                enterEditMode()
            }
            true
        }

        movieAdapter.setOnDeleteClickListener { position, movie ->
            firebaseManager.deleteMovieFromList(DIARY_LIST, userID, movie) { result, error ->
                if (error != null) {
                    showMessage(context, error.message)
                } else if (result != null) {
                    movieAdapter.removeMovie(position)
                    movieList.remove(movie)
                }
            }
        }
        movieListRecyclerView.adapter = movieAdapter
        movieListRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.edit_mode_menu, menu)
        inflater.inflate(R.menu.list_menu, menu)

        doneMenuItem = menu.findItem(R.id.action_done)
        menu.findItem(R.id.action_more)?.isVisible = false

        doneMenuItem?.isVisible = isEditMode

        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                if (isEditMode) {
                    exitEditMode()
                } else {
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
                return true
            }
            R.id.action_done -> {
                exitEditMode()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupAddMovieFragment() {
        val intent = Intent(context, SearchActivity::class.java).apply {
            putExtra("collection", DIARY_LIST)
            putExtra("documentID", firebaseManager.getCurrentUserUID())
            putExtra("listName", "Diario")
            val moviesID = movieList.map { it.id }.toIntArray()
            putExtra("moviesID", moviesID)
        }
        startActivity(intent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateMoviesList(movies: List<Movie>) {
        val reversed = movies.reversed()
        movieAdapter.setMovies(reversed)
        movieAdapter.notifyDataSetChanged()
        movieList = ArrayList(movies)
    }

    private fun loadMoviesFromList() {
        firebaseManager.getMoviesFromList(userID, DIARY_LIST) { movies, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (movies != null) {
                updateMoviesList(movies)
            }
        }
    }

    private fun enterEditMode() {
        isEditMode = true
        doneMenuItem?.isVisible = true
        movieAdapter.isEditMode = true
        movieAdapter.listID = userID
        (requireActivity() as MainActivity).setFabVisibility(false)
    }

    private fun exitEditMode() {
        isEditMode = false
        doneMenuItem?.isVisible = false
        movieAdapter.isEditMode = false
        (requireActivity() as MainActivity).setFabVisibility(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        movieAdapter.isEditMode = false
    }

    override fun onResume() {
        super.onResume()
        loadMoviesFromList()
    }

    override fun onFabClicked() {
        setupAddMovieFragment()
    }
}
