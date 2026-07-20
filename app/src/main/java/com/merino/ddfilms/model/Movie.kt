package com.merino.ddfilms.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Movie(
    var adult: Boolean = false,
    @SerializedName("backdrop_path") var backdropPath: String? = null,
    @SerializedName("genre_ids") var genreIds: ArrayList<Int>? = null,
    var id: Int = 0,
    @SerializedName("original_language") var originalLanguage: String? = null,
    @SerializedName("original_title") var originalTitle: String? = null,
    var overview: String? = null,
    var popularity: Double = 0.0,
    @SerializedName("poster_path") var posterPath: String? = null,
    @SerializedName("release_date") var releaseDate: String? = null,
    var title: String? = null,
    var video: Boolean = false,
    @SerializedName("vote_average") var voteAverage: Double = 0.0,
    @SerializedName("vote_count") var voteCount: Int = 0,
    var createdAt: String? = null,
    var addedBy: String? = null
) : Parcelable {

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "adult" to adult,
            "backdropPath" to backdropPath,
            "genreIds" to genreIds,
            "id" to id,
            "originalLanguage" to originalLanguage,
            "originalTitle" to originalTitle,
            "overview" to overview,
            "popularity" to popularity,
            "posterPath" to posterPath,
            "releaseDate" to releaseDate,
            "title" to title,
            "video" to video,
            "voteAverage" to voteAverage,
            "voteCount" to voteCount,
            "createdAt" to createdAt,
            "addedBy" to addedBy
        )
    }

    companion object {
        @JvmStatic
        fun mapToMovie(movieMap: Map<String, Any?>): Movie {
            val movie = Movie()
            movie.adult = movieMap["adult"] as? Boolean ?: false
            movie.backdropPath = movieMap["backdropPath"] as? String
            @Suppress("UNCHECKED_CAST")
            movie.genreIds = movieMap["genreIds"] as? ArrayList<Int>
            movie.id = (movieMap["id"] as? Long)?.toInt() ?: (movieMap["id"] as? Int) ?: 0
            movie.originalLanguage = movieMap["originalLanguage"] as? String
            movie.originalTitle = movieMap["originalTitle"] as? String
            movie.overview = movieMap["overview"] as? String
            movie.popularity = movieMap["popularity"] as? Double ?: 0.0
            movie.posterPath = movieMap["posterPath"] as? String
            movie.releaseDate = movieMap["releaseDate"] as? String
            movie.title = movieMap["title"] as? String
            movie.video = movieMap["video"] as? Boolean ?: false
            movie.voteAverage = movieMap["voteAverage"] as? Double ?: 0.0
            movie.voteCount = (movieMap["voteCount"] as? Long)?.toInt() ?: (movieMap["voteCount"] as? Int) ?: 0
            movie.createdAt = movieMap["createdAt"] as? String
            movie.addedBy = movieMap["addedBy"] as? String
            return movie
        }
    }
}
