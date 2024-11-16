package com.merino.ddfilms.ui;


import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.merino.ddfilms.R;
import com.merino.ddfilms.api.TMDBClient;
import com.merino.ddfilms.api.TMDBService;
import com.merino.ddfilms.model.SearchResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {
    private TMDBService tmdbService;
    private MovieAdapter movieAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tmdbService = TMDBClient.getClient().create(TMDBService.class);
        EditText searchEditText = findViewById(R.id.search_edit_text);
        RecyclerView movieListRecyclerView = findViewById(R.id.movie_list_recycler_view);

        movieAdapter = new MovieAdapter();
        movieListRecyclerView.setAdapter(movieAdapter);
        movieListRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchMovies(s.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }

            // Override other TextWatcher methods as needed
        });
    }

    private void searchMovies(String query) {
        String apiKey = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiIzZjQ0MjA3YmU5MzVhYmJiOWRiYzRjNzhmMjJjYWJmMCIsIm5iZiI6MTczMTM0ODY2NC45MzIyMzUyLCJzdWIiOiI1ZWRhNThkY2IzZjZmNTAwMjA5ODk1YjQiLCJzY29wZXMiOlsiYXBpX3JlYWQiXSwidmVyc2lvbiI6MX0.2GVXLVK1vviDpW26p8x8WrJduG7S6oIYJfLKD25NoCw";
        tmdbService.searchMovies(query, apiKey)
                .enqueue(new Callback<SearchResponse>() {
                    @Override
                    public void onResponse(Call<SearchResponse> call, Response<SearchResponse> response) {
                        if (response.isSuccessful()) {
                            SearchResponse searchResponse = response.body();
                            movieAdapter.setMovies(searchResponse.getResults());
                            movieAdapter.notifyDataSetChanged();
                        } else {
                            // Handle the error
                        }
                    }

                    @Override
                    public void onFailure(Call<SearchResponse> call, Throwable t) {
                        // Handle the error
                    }
                });
    }
}