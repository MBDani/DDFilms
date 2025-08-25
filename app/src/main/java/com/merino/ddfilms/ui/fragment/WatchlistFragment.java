package com.merino.ddfilms.ui.fragment;

import static com.merino.ddfilms.utils.StringUtils.WATCH_LIST;
import static com.merino.ddfilms.utils.Utils.showMessage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.MovieAdapter;
import com.merino.ddfilms.api.FirebaseManager;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.ui.MainActivity;
import com.merino.ddfilms.ui.SearchActivity;
import com.merino.ddfilms.ui.components.Fab.FabHost;
import com.merino.ddfilms.ui.components.Fab.ShowsFab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WatchlistFragment extends Fragment implements FabHost, ShowsFab {

    private MovieAdapter movieAdapter;
    private RecyclerView movieListRecyclerView;
    private boolean isEditMode = false;
    private Toolbar toolbar;
    private MenuItem doneMenuItem;
    private MenuItem moreActionsMenuItem;
    private final FirebaseManager firebaseManager = new FirebaseManager();
    private List<Movie> movieList = new ArrayList<>();
    private final String userID = firebaseManager.getCurrentUserUID();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchlist, container, false);

        setupViews(view);
//        setupToolbar();
        setupRecyclerView();
        loadMoviesFromList();

        return view;
    }

    private void setupViews(View view) {
        movieListRecyclerView = view.findViewById(R.id.movie_list_recycler_view);
//        toolbar = view.findViewById(R.id.toolbar);
    }

    private void setupToolbar() {
        AppCompatActivity activity = (AppCompatActivity) getActivity();
        if (activity == null) return;

        activity.setSupportActionBar(toolbar);
        ActionBar bar = activity.getSupportActionBar();
        if (bar != null) {
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setTitle("Pendientes");
        }

        // Opcional: manejar clic en flecha de regreso
        toolbar.setNavigationOnClickListener(v -> activity.onBackPressed());
    }

    private void setupAddMovieFragment() {
        Intent intent = new Intent(getContext(), SearchActivity.class);
        intent.putExtra("collection", WATCH_LIST);
        intent.putExtra("documentID", firebaseManager.getCurrentUserUID());
        intent.putExtra("listName", "Pendientes");

        int[] moviesID = movieList.stream()
                .mapToInt(Movie::getId)
                .toArray();
        intent.putExtra("moviesID", moviesID);
        startActivity(intent);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isEditMode) {
                    exitEditMode();
                } else {
//                    onBackPressed();
                }
                return true;
            case R.id.action_more:
//                showBottomSheet();
                return true;
            case R.id.action_done:
                exitEditMode();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        movieAdapter = new MovieAdapter();
        movieAdapter.setOnItemLongClickListener(position -> {
            if (!isEditMode) {
                enterEditMode();
            }
            return true;
        });

        movieAdapter.setOnDeleteClickListener((position, movie) -> {
            firebaseManager.deleteMovieFromList(userID, movie, (result, error) -> {
                if (error != null) {
                    showMessage(getContext(), error.getMessage());
                } else if (result != null) {
                    movieAdapter.removeMovie(position);
                    movieList.remove(movie);
                }
            });
        });
        movieListRecyclerView.setAdapter(movieAdapter);
        movieListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateMoviesList(List<Movie> movies) {
        // Ponemos las películas agregadas más recientemente primero
        Collections.reverse(movies);
        movieAdapter.setMovies(movies);
        movieAdapter.notifyDataSetChanged();
        movieList = movies;
    }

    private void loadMoviesFromList() {
        firebaseManager.loadMovieFromList(userID, WATCH_LIST, (movies, error) -> {
            if (error != null) {
                showMessage(getContext(), error.getMessage());
            } else if (movies != null) {
                updateMoviesList(movies);
            }
        });
    }

    private void enterEditMode() {
        isEditMode = true;
        doneMenuItem.setVisible(true);
        moreActionsMenuItem.setVisible(false);
        movieAdapter.setEditMode(true);
        movieAdapter.setListID(userID);
        ((MainActivity) getActivity()).setFabVisibility(false);
    }

    private void exitEditMode() {
        isEditMode = false;
        doneMenuItem.setVisible(false);
        moreActionsMenuItem.setVisible(true);
        movieAdapter.setEditMode(false);
        ((MainActivity) getActivity()).setFabVisibility(true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        movieAdapter.setEditMode(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMoviesFromList();
    }

    @Override
    public void onFabClicked() {
        setupAddMovieFragment();
    }
}
