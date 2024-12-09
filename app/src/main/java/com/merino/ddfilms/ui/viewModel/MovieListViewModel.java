package com.merino.ddfilms.ui.viewModel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.merino.ddfilms.api.FirebaseManager;

import java.util.ArrayList;
import java.util.List;

public class MovieListViewModel extends ViewModel {

    private final MutableLiveData<List<String>> movieLists = new MutableLiveData<>();

    FirebaseManager firebaseManager = new FirebaseManager();

    public LiveData<List<String>> getMovieLists() {
        return movieLists;
    }

    public void loadMovieLists() {
        String userID = firebaseManager.getCurrentUser();
        firebaseManager.getMovieLists(userID).addOnSuccessListener(queryDocumentSnapshots -> {
            List<String> lists = new ArrayList<>();
            for (DocumentSnapshot document : queryDocumentSnapshots) {
                lists.add(document.getString("name"));
            }
            movieLists.setValue(lists);
        });
    }

    public Task<Void> createNewMovieList(String listName) {
        String userID = firebaseManager.getCurrentUser();
        return firebaseManager.createNewMovieList(listName, userID).continueWith(task -> null);
    }
}

