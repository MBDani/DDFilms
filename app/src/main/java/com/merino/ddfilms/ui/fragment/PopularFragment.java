package com.merino.ddfilms.ui.fragment;

import static com.merino.ddfilms.utils.Utils.getApiKey;
import static com.merino.ddfilms.utils.Utils.showError;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.SearchResponse;
import com.merino.ddfilms.ui.MovieAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PopularFragment extends Fragment {

    private TMDBService tmdbService;
    private static final String API_KEY = getApiKey();

    private MovieAdapter movieAdapter;
    private RecyclerView movieListRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Infla el layout del fragmento
        View view = inflater.inflate(R.layout.fragment_popular, container, false);

        // Inicializa TMDBService
        tmdbService = TMDBClient.getClient().create(TMDBService.class);

        // Asigna vistas
        movieListRecyclerView = view.findViewById(R.id.movie_popular_recycler_view);

        setupRecyclerView();
        loadPopularMovies();

        return view;
    }

    private void setupRecyclerView() {
        movieAdapter = new MovieAdapter();
        movieListRecyclerView.setAdapter(movieAdapter);
        movieListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }


    private void loadPopularMovies() {
        tmdbService.getPopularMovies(API_KEY).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMoviesList(response.body().getResults());
                } else {
                    showError(getContext(), "Error al cargar películas populares");
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                showError(getContext(), "Error de conexión: " + t.getMessage());
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void updateMoviesList(List<Movie> movies) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                movieAdapter.setMovies(movies);
                movieAdapter.notifyDataSetChanged();
            });
        }
    }
}
