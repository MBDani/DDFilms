package com.merino.ddfilms.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.merino.ddfilms.R
import com.merino.ddfilms.adapters.MovieAdapter
import com.merino.ddfilms.api.TMDBClient
import com.merino.ddfilms.api.TMDBService
import com.merino.ddfilms.configuration.ApiKeyManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.SearchResponse
import com.merino.ddfilms.utils.Utils.showMessage
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PopularFragment : Fragment() {

    private lateinit var tmdbService: TMDBService
    private lateinit var movieAdapter: MovieAdapter
    private lateinit var movieListRecyclerView: RecyclerView

    companion object {
        private val API_KEY = ApiKeyManager.getInstance().apiKey ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_popular, container, false)

        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService::class.java)
        movieListRecyclerView = view.findViewById(R.id.movie_popular_recycler_view)

        setupRecyclerView()
        loadPopularMovies()

        return view
    }

    private fun setupRecyclerView() {
        movieAdapter = MovieAdapter()
        movieListRecyclerView.adapter = movieAdapter
        movieListRecyclerView.layoutManager = LinearLayoutManager(context)
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
