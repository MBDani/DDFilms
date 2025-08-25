package com.merino.ddfilms.ui.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.utils.TaskCompletionCallback;

import java.util.HashMap;
import java.util.List;

public class MovieListViewModel extends ViewModel {

    private final MutableLiveData<List<String>> movieLists = new MutableLiveData<>();

    private final FirebaseManager firebaseManager = new FirebaseManager();

    public LiveData<List<String>> getMovieLists() {
        return movieLists;
    }

    public void setMovieLists(List<String> list) {
        movieLists.setValue(list);
    }

    public void loadMovieListNames(TaskCompletionCallback<HashMap<String, String>> callback) {
        String userID = firebaseManager.getCurrentUserUID();
        firebaseManager.getMovieListsNameAndID(userID, callback);
    }

    public void createNewMovieList(String listName, TaskCompletionCallback<String> callback) {
        String userID = firebaseManager.getCurrentUserUID();
        firebaseManager.createNewMovieList(listName, userID, callback);
    }

    public void addMovieToList(String collection, String listID, Movie movie, TaskCompletionCallback<String> callback) {
        firebaseManager.addMovieToList(collection, listID, movie, callback);
    }
}

