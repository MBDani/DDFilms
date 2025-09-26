package com.merino.ddfilms.ui.fragment;

import static com.merino.ddfilms.utils.StringUtils.DIARY_LIST;
import static com.merino.ddfilms.utils.Utils.showMessage;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
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

public class DiaryFragment extends Fragment implements FabHost, ShowsFab {

    private MovieAdapter movieAdapter;
    private RecyclerView movieListRecyclerView;
    private boolean isEditMode = false;
    private MenuItem doneMenuItem;
    private final FirebaseManager firebaseManager = new FirebaseManager();
    private List<Movie> movieList = new ArrayList<>();
    private final String userID = firebaseManager.getCurrentUserUID();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchlist, container, false);
        setHasOptionsMenu(true);

        setupViews(view);
        setupRecyclerView();
        loadMoviesFromList();

        return view;
    }

    private void setupViews(View view) {
        movieListRecyclerView = view.findViewById(R.id.movie_list_recycler_view);
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
            firebaseManager.deleteMovieFromList(DIARY_LIST, userID, movie, (result, error) -> {
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        // Inflamos los dos menús: edit_mode_menu y list_menu (mismos ids que en la Activity)
        inflater.inflate(R.menu.edit_mode_menu, menu);
        inflater.inflate(R.menu.list_menu, menu);

        // Guardamos referencias a los items para poder togglear su visibilidad
        doneMenuItem = menu.findItem(R.id.action_done);
        menu.findItem(R.id.action_more).setVisible(false);

        // Estado inicial según isEditMode
        if (doneMenuItem != null) doneMenuItem.setVisible(isEditMode);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (isEditMode)
                    exitEditMode();
                else
                    requireActivity().onBackPressed();
                return true;
            case R.id.action_done:
                exitEditMode();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupAddMovieFragment() {
        Intent intent = new Intent(getContext(), SearchActivity.class);
        intent.putExtra("collection", DIARY_LIST);
        intent.putExtra("documentID", firebaseManager.getCurrentUserUID());
        intent.putExtra("listName", "Diario");

        int[] moviesID = movieList.stream()
                .mapToInt(Movie::getId)
                .toArray();
        intent.putExtra("moviesID", moviesID);
        startActivity(intent);
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
        firebaseManager.getMoviesFromList(userID, DIARY_LIST, (movies, error) -> {
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
        movieAdapter.setEditMode(true);
        movieAdapter.setListID(userID);
        ((MainActivity) requireActivity()).setFabVisibility(false);
    }

    private void exitEditMode() {
        isEditMode = false;
        doneMenuItem.setVisible(false);
        movieAdapter.setEditMode(false);
        ((MainActivity) requireActivity()).setFabVisibility(true);
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
