package com.merino.ddfilms.utils

import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.merino.ddfilms.model.Review
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Comparator
import java.util.Date
import java.util.Locale

@SafeParcelable.Constructor
class DateFormatter {

    private val SDF_PATTERN = "EEE MMM dd HH:mm:ss z yyyy"

    fun getFormattedDate(reviewDate: String?): String {
        if (reviewDate == null) return ""
        val inputFormat = SimpleDateFormat(SDF_PATTERN, SDF_LOCALE)
        try {
            val date = inputFormat.parse(reviewDate) ?: return reviewDate
            var diffMillis = Date().time - date.time
            if (diffMillis < 0) {
                diffMillis = 0
            }

            val diffSeconds = diffMillis / 1000
            if (diffSeconds < 10) {
                return "Ahora mismo"
            } else if (diffSeconds < 60) {
                return "Hace unos segundos"
            }

            val diffMinutes = diffSeconds / 60
            if (diffMinutes < 60) {
                return if (diffMinutes == 1L) "Hace 1 minuto" else "Hace $diffMinutes minutos"
            }

            val diffHours = diffMinutes / 60
            if (diffHours < 24) {
                return if (diffHours == 1L) "Hace 1 hora" else "Hace $diffHours horas"
            }

            val diffDays = diffHours / 24
            if (diffDays == 1L) {
                return "Ayer"
            } else if (diffDays < 7) {
                return "Hace $diffDays días"
            } else {
                val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH)
                return outputFormat.format(date)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
            return reviewDate
        }
    }

    fun reviewDateDescComparator(): Comparator<Review> {
        return Comparator { r1, r2 ->
            val d1 = parseDateToString(r1.reviewDate)
            val d2 = parseDateToString(r2.reviewDate)
            d2.compareTo(d1)
        }
    }

    fun reviewDateAscComparator(): Comparator<Review> {
        return Comparator { r1, r2 ->
            val d1 = parseDateToString(r1.reviewDate)
            val d2 = parseDateToString(r2.reviewDate)
            d1.compareTo(d2)
        }
    }

    fun reviewLikesDescComparator(): Comparator<Review> {
        return Comparator { r1, r2 ->
            val size2 = r2.likeCount?.size ?: 0
            val size1 = r1.likeCount?.size ?: 0
            size2.compareTo(size1)
        }
    }

    fun reviewDislikesDescComparator(): Comparator<Review> {
        return Comparator { r1, r2 ->
            val size2 = r2.dislikeCount?.size ?: 0
            val size1 = r1.dislikeCount?.size ?: 0
            size2.compareTo(size1)
        }
    }

    fun reviewRatingDescComparator(): Comparator<Review> {
        return Comparator { r1, r2 ->
            r2.rating.compareTo(r1.rating)
        }
    }

    fun reviewRatingAscComparator(): Comparator<Review> {
        return Comparator { r1, r2 ->
            r1.rating.compareTo(r2.rating)
        }
    }

    fun parseDateToString(dateString: String?): Date {
        if (dateString == null) return Date(0)
        try {
            val sdf = SimpleDateFormat(SDF_PATTERN, SDF_LOCALE)
            return sdf.parse(dateString) ?: Date(0)
        } catch (e: ParseException) {
            return Date(0)
        }
    }

    companion object {
        private val SDF_LOCALE = Locale.ENGLISH
    }
}
