package com.merino.ddfilms.api

import com.merino.ddfilms.model.Credits
import com.merino.ddfilms.model.MovieDetails
import com.merino.ddfilms.model.SearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TMDBService {
    @GET("movie/{id}/credits")
    fun getMovieCredits(
        @Path("id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String
    ): Call<Credits>

    @GET("search/movie")
    fun searchMovies(
        @Query("query") query: String,
        @Query("include_adult") includeAdult: Boolean,
        @Query("language") language: String,
        @Query("page") page: Int
    ): Call<SearchResponse>

    @GET("movie/{movie_id}")
    fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String
    ): Call<MovieDetails>

    @GET("movie/popular")
    fun getPopularMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String
    ): Call<SearchResponse>

    @GET("discover/movie")
    fun discoverMovies(
        @Query("api_key") apiKey: String,
        @Query("language") language: String,
        @Query("sort_by") sortBy: String? = null,
        @Query("with_genres") withGenres: String? = null,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
        @Query("vote_average.gte") voteAverageGte: Float? = null,
        @Query("page") page: Int = 1
    ): Call<SearchResponse>
}

