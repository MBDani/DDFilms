package com.merino.ddfilms.model

data class MovieLists(
    var id: String? = null,
    var name: String? = null,
    var description: String? = null,
    var userID: List<String>? = null,
    var movies: List<Movie>? = null,
    var coverPreviews: List<String>? = null,
    var memberAvatarsPreview: List<String>? = null,
    var lastUpdated: String? = null,
    var moviesCount: Int = 0
)
