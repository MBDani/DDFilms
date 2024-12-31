package com.merino.ddfilms.ui.fragment;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.MovieListAdapter;
import com.merino.ddfilms.ui.ListMoviesActivity;
import com.merino.ddfilms.ui.viewModel.MovieListViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class ListsFragment extends Fragment {

    private MovieListViewModel viewModel;
    private MovieListAdapter adapter;
    private HashMap<String, String> mapMovieLists;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lists, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.lists_recycler_view);

        viewModel = new ViewModelProvider(this).get(MovieListViewModel.class);
        adapter = new MovieListAdapter(getContext(), new ArrayList<>(), this::navigateToListMoviesActivity);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getMovieLists().observe(getViewLifecycleOwner(), movieLists -> {
            adapter.updateData(movieLists);
        });

        return view;
    }

    private void navigateToListMoviesActivity(String listName) {
        String listID = getListIDByName(listName);
        Intent intent = new Intent(getContext(), ListMoviesActivity.class);
        intent.putExtra("listID", listID);
        intent.putExtra("listName", listName);
        startActivity(intent);
    }

    private void loadMovieListNames() {
        viewModel.loadMovieListNames((listMapMovies, error) -> {
            if (error != null) {
                viewModel.setMovieLists(new ArrayList<>());
                showMessage(getContext(), error.getMessage());
            } else {
                List<String> listMovies = new ArrayList<>(listMapMovies.values());
                mapMovieLists = listMapMovies;
                viewModel.setMovieLists(listMovies);
            }
        });
    }

    @Nullable
    private String getListIDByName(String listName) {
        String listID = null;
        for (String key : mapMovieLists.keySet()) {
            if (Objects.equals(mapMovieLists.get(key), listName)) {
                listID = key;
                break;
            }
        }
        return listID;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMovieListNames();
    }
}
