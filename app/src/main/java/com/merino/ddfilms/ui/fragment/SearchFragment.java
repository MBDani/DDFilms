package com.merino.ddfilms.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.configuration.AppConfig;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.SearchResponse;
import com.merino.ddfilms.ui.MovieAdapter;
import com.merino.ddfilms.utils.TaskCompletionCallback;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private TMDBService tmdbService;
    private MovieAdapter movieAdapter;

    private EditText searchEditText;
    private RecyclerView movieListRecyclerView;
    private AppConfig appConfig;

    private static String API_KEY = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        getApiKey();

        // Infla el layout del fragmento
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Inicializa TMDBService
        tmdbService = TMDBClient.getClient().create(TMDBService.class);

        // Asigna vistas
        searchEditText = view.findViewById(R.id.search_edit_text);
        movieListRecyclerView = view.findViewById(R.id.movie_list_recycler_view);

        // Configurar RecyclerView y buscador
        setupRecyclerView();
        setupSearchListener();

        // Cargar películas populares al inicio
        loadPopularMovies();

        return view;
    }

    private void getApiKey() {
        AppConfig appConfig = new AppConfig();
        appConfig.getTmdbApiKey((result, error) -> {
            if (error != null) {
                // Manejar el error
            } else {
                API_KEY = result;
            }
        });
    }

    private void setupRecyclerView() {
        movieAdapter = new MovieAdapter();
        movieListRecyclerView.setAdapter(movieAdapter);
        movieListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No necesitamos implementar esto
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Realizamos la búsqueda solo si hay texto
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    searchMovies(query);
                } else {
                    loadPopularMovies(); // Si el campo está vacío, mostramos películas populares
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No necesitamos implementar esto
            }
        });
    }

    private void searchMovies(String query) {
        tmdbService.searchMovies(query, false, "en-US", 1).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMoviesList(response.body().getResults());
                } else {
                    showError("Error en la búsqueda");
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                showError("Error de conexión: " + t.getMessage());
            }
        });
    }

    private void loadPopularMovies() {
        tmdbService.getPopularMovies(API_KEY).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMoviesList(response.body().getResults());
                } else {
                    showError("Error al cargar películas populares");
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                showError("Error de conexión: " + t.getMessage());
            }
        });
    }

    private void updateMoviesList(List<Movie> movies) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                movieAdapter.setMovies(movies);
                movieAdapter.notifyDataSetChanged();
            });
        }
    }

    private void showError(String message) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            });
        }
    }
}
