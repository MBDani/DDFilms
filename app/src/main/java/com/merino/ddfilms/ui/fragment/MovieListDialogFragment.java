package com.merino.ddfilms.ui.fragment;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.adapters.MovieListAdapter;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.ui.viewModel.MovieListViewModel;

import java.util.ArrayList;

public class MovieListDialogFragment extends DialogFragment {

    private MovieListViewModel viewModel;
    private MovieListAdapter adapter;

    private final Movie movie;

    public MovieListDialogFragment(Movie movieId) {
        this.movie = movieId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_movie_list, container, false);
        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        Button createListButton = view.findViewById(R.id.createListButton);

        viewModel = new ViewModelProvider(this).get(MovieListViewModel.class);
        adapter = new MovieListAdapter(getContext(), new ArrayList<>(), this::addMovieToSelectedList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        viewModel.getMovieLists().observe(getViewLifecycleOwner(), movieLists -> {
            adapter.updateData(movieLists);
        });

        createListButton.setOnClickListener(v -> showCreateListDialog());

        viewModel.loadMovieListNames();

        return view;
    }

    private void addMovieToSelectedList(String listName) {
        viewModel.addMovieToList(listName, movie, (result, error) -> {
            if (error != null) {
                showMessage(getContext(), error.getMessage());
            } else if (result != null) {
                showMessage(getContext(), result);
                dismiss();
            }
        });
    }

    private void showCreateListDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Crear nueva lista");

        final EditText input = new EditText(getContext());
        builder.setView(input);

        builder.setPositiveButton("Crear", (dialog, which) -> {
            String listName = input.getText().toString().trim();
            if (!listName.isEmpty()) {
                viewModel.createNewMovieList(listName).addOnSuccessListener(aVoid -> {
                    viewModel.loadMovieListNames();
                });
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
