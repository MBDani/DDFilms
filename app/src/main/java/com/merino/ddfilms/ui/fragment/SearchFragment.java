package com.merino.ddfilms.ui.fragment;

import static com.merino.ddfilms.utils.Utils.showMessage;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.configuration.ApiKeyManager;
import com.merino.ddfilms.model.Movie;
import com.merino.ddfilms.model.SearchResponse;
import com.merino.ddfilms.adapters.MovieAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {

    private TMDBService tmdbService;
    private MovieAdapter movieAdapter;

    private EditText searchEditText;
    private RecyclerView movieListRecyclerView;

    private static final String API_KEY = ApiKeyManager.getInstance().getApiKey();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        // Infla el layout del fragmento
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        // Inicializa TMDBService
        tmdbService = TMDBClient.getClient(API_KEY).create(TMDBService.class);

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
        tmdbService.searchMovies(query, false, "es-ES", 1).enqueue(new Callback<SearchResponse>() {
            @Override
            public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMoviesList(response.body().getResults());
                } else {
                    showMessage(getContext() ,"Error en la búsqueda");
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                showMessage(getContext() ,"Error de conexión: " + t.getMessage());
            }
        });
    }

    private void loadPopularMovies() {
        tmdbService.getPopularMovies(API_KEY, "es-ES").enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<SearchResponse> call, @NonNull Response<SearchResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    updateMoviesList(response.body().getResults());
                } else {
                    showMessage(getContext(), "Error al cargar películas populares");
                }
            }

            @Override
            public void onFailure(Call<SearchResponse> call, Throwable t) {
                showMessage(getContext(), "Error de conexión: " + t.getMessage());
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
