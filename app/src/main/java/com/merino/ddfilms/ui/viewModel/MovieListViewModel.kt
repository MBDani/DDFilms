package com.merino.ddfilms.ui.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.model.Movie
import com.merino.ddfilms.model.MovieLists
import com.merino.ddfilms.utils.TaskCompletionCallback

class MovieListViewModel : ViewModel() {

    private val _movieLists = MutableLiveData<List<MovieLists>>()
    val movieLists: LiveData<List<MovieLists>>
        get() = _movieLists

    private val firebaseManager = FirebaseManager()

    fun setMovieLists(list: List<MovieLists>) {
        _movieLists.value = list
    }

    fun loadMovieLists(callback: TaskCompletionCallback<List<MovieLists>>) {
        val userID = firebaseManager.getCurrentUserUID()
        firebaseManager.getMovieLists(userID, callback)
    }

    fun createNewMovieList(listName: String, callback: TaskCompletionCallback<String>) {
        val userID = firebaseManager.getCurrentUserUID()
        firebaseManager.createNewMovieList(listName, userID, callback)
    }

    fun addMovieToList(collection: String, listID: String, movie: Movie, callback: TaskCompletionCallback<String>) {
        firebaseManager.addMovieToList(collection, listID, movie, callback)
    }
}
