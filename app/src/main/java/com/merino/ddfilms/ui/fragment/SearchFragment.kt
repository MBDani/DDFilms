package com.merino.ddfilms.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.MovieAdapter
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.api.TMDBClient
import com.merino.ddfilms.api.TMDBService
import com.merino.ddfilms.configuration.ApiKeyManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.SearchResponse
import com.merino.ddfilms.utils.StringUtils.DIARY_LIST
import com.merino.ddfilms.utils.Utils.showMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SearchFragment : Fragment() {

    private lateinit var tmdbService: TMDBService
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var searchEditText: EditText
    private lateinit var movieListRecyclerView: RecyclerView

    private val firebaseManager = FirebaseManager()

    companion object {
        private val API_KEY = ApiKeyManager.getInstance().apiKey ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService::class.java)

        searchEditText = view.findViewById(R.id.search_edit_text)
        movieListRecyclerView = view.findViewById(R.id.movie_list_recycler_view)

        setupRecyclerView()
        setupSearchListener()

        loadPopularMovies()

        val argumentos = arguments
        if (argumentos != null) {
            val collection = argumentos.getString("collection")
            val documentID = argumentos.getString("documentID")
            val listName = argumentos.getString("listName")
            val moviesID = argumentos.getIntArray("moviesID")
            if (moviesID != null && collection != null && documentID != null && listName != null) {
                val moviesIDList = moviesID.toList().toMutableList()
                setupViewAddMode(collection, documentID, listName, moviesIDList)
            }
        }

        return view
    }

    private fun setupViewAddMode(
        collection: String,
        documentID: String,
        listName: String,
        moviesIDList: MutableList<Int>
    ) {
        movieAdapter.isAddMode = true
        movieAdapter.moviesIdList = moviesIDList
        movieAdapter.setOnAddClickListener { _, movie ->
            addMovie(collection, documentID, listName, moviesIDList, movie)
            if (collection == DIARY_LIST) {
                deleteMovie(collection, documentID, listName, moviesIDList, movie)
            }
        }

        movieAdapter.setOnCheckClickListener { _, movie ->
            deleteMovie(collection, documentID, listName, moviesIDList, movie)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteMovie(
        collection: String,
        documentID: String,
        listName: String,
        moviesIDList: MutableList<Int>,
        movie: Movie
    ) {
        firebaseManager.deleteMovieFromList(collection, documentID, movie) { result, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (result != null) {
                showMessage(context, getString(R.string.removed_from_list, listName))
                moviesIDList.removeIf { it == movie.id }
                movieAdapter.moviesIdList = moviesIDList
                movieAdapter.notifyDataSetChanged()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun addMovie(
        collection: String,
        documentID: String,
        listName: String,
        moviesIDList: MutableList<Int>,
        movie: Movie
    ) {
        firebaseManager.addMovieToList(collection, documentID, movie) { result, error ->
            if (error != null) {
                showMessage(context, error.message)
            } else if (result != null) {
                showMessage(context, getString(R.string.added_to_list_message, listName))
                moviesIDList.add(movie.id)
                movieAdapter.moviesIdList = moviesIDList
                movieAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter()
        movieListRecyclerView.adapter = movieAdapter
        movieListRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString()?.trim() ?: ""
                if (query.isNotEmpty()) {
                    searchMovies(query)
                } else {
                    loadPopularMovies()
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun searchMovies(query: String) {
        tmdbService.searchMovies(query, false, getString(R.string.tmdb_api_language), 1).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    updateMoviesList(response.body()!!.results ?: emptyList())
                } else {
                    showMessage(context, getString(R.string.error_searching))
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                showMessage(context, getString(R.string.error_connection, t.message))
            }
        })
    }

    private fun loadPopularMovies() {
        tmdbService.getPopularMovies(API_KEY, getString(R.string.tmdb_api_language)).enqueue(object : Callback<SearchResponse> {
            override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    updateMoviesList(response.body()!!.results ?: emptyList())
                } else {
                    showMessage(context, getString(R.string.error_loading_popular))
                }
            }

            override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                showMessage(context, getString(R.string.error_connection, t.message))
            }
        })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateMoviesList(movies: List<Movie>) {
        activity?.runOnUiThread {
            movieAdapter.setMovies(movies)
            movieAdapter.notifyDataSetChanged()
        }
    }
}
