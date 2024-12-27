package com.merino.ddfilms.ui;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.MovieAdapter;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.model.Movie;

import java.util.Collections;
import java.util.List;

public class ListMoviesActivity extends AppCompatActivity {

    private MovieAdapter movieAdapter;
    private RecyclerView movieListRecyclerView;

    FirebaseManager firebaseManager = new FirebaseManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        movieListRecyclerView = findViewById(R.id.movie_list_recycler_view);
        String listName = getIntent().getStringExtra("listName");

        setupRecyclerView();
        loadMoviesFromList(listName);
    }

    private void setupRecyclerView() {
        movieAdapter = new MovieAdapter();
        movieListRecyclerView.setAdapter(movieAdapter);
        movieListRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
    }

    private void loadMoviesFromList(String listName) {
        firebaseManager.loadMovieFromListName(listName, firebaseManager.getCurrentUser(), (movies, error) -> {
            if (error != null) {
                showMessage(getApplicationContext(), error.getMessage());
            } else {
                updateMoviesList(movies);
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateMoviesList(List<Movie> movies) {
        // Ponemos las películas agregadas más recientemente primero
        Collections.reverse(movies);
        movieAdapter.setMovies(movies);
        movieAdapter.notifyDataSetChanged();
    }

}


