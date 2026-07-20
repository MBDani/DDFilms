package com.merino.ddfilms.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Credits(
    var id: Int = 0,
    var cast: List<Cast>? = null,
    var crew: List<Crew>? = null
) : Parcelable {

    @Parcelize
    data class Cast(
        var adult: Boolean = true,
        var castId: Int = 0,
        var character: String? = null,
        @SerializedName("credit_id") var creditId: String? = null,
        var gender: Int = 0,
        var id: Int = 0,
        @SerializedName("known_for_department") var knownForDepartment: String? = null,
        var name: String? = null,
        @SerializedName("original_name") var originalName: String? = null,
        var popularity: Double = 0.0,
        @SerializedName("profile_path") var profilePath: String? = null,
        var order: Int = 0
    ) : Parcelable

    @Parcelize
    data class Crew(
        var adult: Boolean = true,
        var gender: Int = 0,
        var id: Int = 0,
        @SerializedName("known_for_department") var knownForDepartment: String? = null,
        var name: String? = null,
        @SerializedName("original_name") var originalName: String? = null,
        var popularity: Double = 0.0,
        @SerializedName("profile_path") var profilePath: String? = null,
        @SerializedName("credit_id") var creditId: String? = null,
        var department: String? = null,
        var job: String? = null
    ) : Parcelable {
        companion object {
            @JvmStatic
            fun getDirector(crew: List<Crew>?): String? {
                if (crew == null) return null
                return crew.firstOrNull { "Director" == it.job }?.name
            }
        }
    }
}
