package com.merino.ddfilms.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class MovieDetails(
    var id: Int = 0,
    var title: String? = null,
    var overview: String? = null,
    @SerializedName("release_date") var releaseDate: String? = null,
    var runtime: Int = 0,
    var tagline: String? = null,
    var status: String? = null,
    @SerializedName("original_language") var originalLanguage: String? = null,
    @SerializedName("backdrop_path") var backdropPath: String? = null,
    @SerializedName("poster_path") var posterPath: String? = null,
    @SerializedName("vote_average") var voteAverage: Double = 0.0,
    @SerializedName("vote_count") var voteCount: Int = 0,
    var genres: List<Genre>? = null
) : Parcelable {

    fun getDuration(): String {
        val hours = runtime / 60
        val minutes = runtime % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }

    @Parcelize
    data class Genre(
        var id: Int = 0,
        var name: String? = null
    ) : Parcelable
}
