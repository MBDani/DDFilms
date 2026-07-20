package com.merino.ddfilms.model

import com.google.gson.annotations.SerializedName

data class SearchResponse(
    var page: Int = 0,
    var results: List<Movie>? = null,
    @SerializedName("total_pages") var totalPages: Int = 0,
    @SerializedName("total_results") var totalResults: Int = 0
)
