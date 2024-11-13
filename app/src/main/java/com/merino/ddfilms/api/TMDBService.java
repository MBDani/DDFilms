package com.merino.ddfilms.api;

import com.merino.ddfilms.model.MovieResponse;
import com.merino.ddfilms.model.SearchResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TMDBService {
    @GET("movie/{id}")
    Call<MovieResponse> getMovieDetails(@Path("id") int movieId, @Query("api_key") String apiKey);

    @GET("search/movie")
    Call<SearchResponse> searchMovies(@Query("query") String query, @Query("api_key") String apiKey);
}
