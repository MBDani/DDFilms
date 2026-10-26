package com.merino.ddfilms.model

import com.google.gson.annotations.SerializedName

data class WatchProvidersResponse(
    @SerializedName("id") val id: Int,
    @SerializedName("results") val results: Map<String, CountryProviders>?
)

data class CountryProviders(
    @SerializedName("link") val link: String?,
    @SerializedName("flatrate") val flatrate: List<ProviderItem>?,
    @SerializedName("rent") val rent: List<ProviderItem>?,
    @SerializedName("buy") val buy: List<ProviderItem>?
)

data class ProviderItem(
    @SerializedName("provider_id") val providerId: Int,
    @SerializedName("provider_name") val providerName: String,
    @SerializedName("logo_path") val logoPath: String?,
    @SerializedName("display_priority") val displayPriority: Int?
)

enum class TargetPlatform(
    val idName: String,
    val displayName: String,
    val brandColorHex: Long,
    val defaultLogoPath: String,
    val keywords: List<String>
) {
    NETFLIX(
        idName = "netflix",
        displayName = "Netflix",
        brandColorHex = 0xFFE50914,
        defaultLogoPath = "/t2yyOv40HZeVlLjYsCsPHnWLk4W.jpg",
        keywords = listOf("netflix")
    ),
    PRIME_VIDEO(
        idName = "prime",
        displayName = "Prime Video",
        brandColorHex = 0xFF00A8E1,
        defaultLogoPath = "/dQeAar5H991VYporEjUspolDarG.jpg",
        keywords = listOf("prime video", "amazon prime", "amazon video")
    ),
    DISNEY_PLUS(
        idName = "disney",
        displayName = "Disney+",
        brandColorHex = 0xFF113CCF,
        defaultLogoPath = "/97yvRBw1GzX7fXprcF80er19ot.jpg",
        keywords = listOf("disney plus", "disney+", "disney")
    ),
    MAX(
        idName = "max",
        displayName = "Max",
        brandColorHex = 0xFF2A52BE,
        defaultLogoPath = "/fksCUZ9QDWZMUwL2LgMtLckROUN.jpg",
        keywords = listOf("max", "hbo max", "hbo")
    );

    fun matches(providerName: String): Boolean {
        val lower = providerName.lowercase()
        return keywords.any { lower.contains(it) }
    }
}

data class PlatformAvailability(
    val platform: TargetPlatform,
    val isAvailable: Boolean,
    val logoUrl: String,
    val watchLink: String? = null
)
