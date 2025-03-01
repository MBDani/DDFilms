package com.merino.ddfilms.api;

import com.merino.ddfilms.model.Credits;
import com.merino.ddfilms.model.MovieDetails;
import com.merino.ddfilms.model.SearchResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface TMDBService {
    @GET("movie/{id}/credits")
    Call<Credits> getMovieCredits(@Path("id") int movieId, @Query("api_key") String apiKey, @Query("language") String language);
    @GET("search/movie")
    Call<SearchResponse> searchMovies(@Query("query") String query, @Query("include_adult") boolean includeAdult, @Query("language") String language, @Query("page") int page);

    @GET("movie/{movie_id}")
    Call<MovieDetails> getMovieDetails(@Path("movie_id") int movieId, @Query("api_key") String apiKey, @Query("language") String language);

    @GET("movie/popular")
    Call<SearchResponse> getPopularMovies(@Query("api_key") String apiKey, @Query("language") String language);
}
