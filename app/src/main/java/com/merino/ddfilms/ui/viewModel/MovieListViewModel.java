package com.merino.ddfilms.ui.viewModel;

import static com.merino.ddfilms.utils.Utils.showMessage;

import static java.security.AccessController.getContext;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.utils.TaskCompletionCallback;

import java.util.List;

public class MovieListViewModel extends ViewModel {

    private final MutableLiveData<List<String>> movieLists = new MutableLiveData<>();

    FirebaseManager firebaseManager = new FirebaseManager();

    public LiveData<List<String>> getMovieLists() {
        return movieLists;
    }

    public void loadMovieListNames() {
        String userID = firebaseManager.getCurrentUser();
        firebaseManager.getMovieLists(userID, (listMovies, error) -> {
            if (listMovies != null) {
                movieLists.setValue(listMovies);
            }
        });
    }

    public void createNewMovieList(String listName, TaskCompletionCallback<String> callback) {
        String userID = firebaseManager.getCurrentUser();
        firebaseManager.createNewMovieList(listName, userID, callback);
    }

    public void addMovieToList(String listName, Movie movie, TaskCompletionCallback<String> callback) {
        String userID = firebaseManager.getCurrentUser();
        firebaseManager.addMovieToList(listName, movie, userID, callback);
    }
}

