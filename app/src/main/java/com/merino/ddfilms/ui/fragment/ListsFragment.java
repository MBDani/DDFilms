package com.merino.ddfilms.ui.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.MovieListAdapter;
import com.merino.ddfilms.ui.viewModel.MovieListViewModel;

import java.util.ArrayList;

public class ListsFragment extends Fragment {

    private MovieListViewModel viewModel;
    private MovieListAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lists, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.lists_recycler_view);

        viewModel = new ViewModelProvider(this).get(MovieListViewModel.class);
        adapter = new MovieListAdapter(getContext(), new ArrayList<>(), () -> {
            // Aquí defines la acción al seleccionar una lista
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getMovieLists().observe(getViewLifecycleOwner(), movieLists -> {
            adapter.updateData(movieLists);
        });

        viewModel.loadMovieLists();

        return view;
    }
}
