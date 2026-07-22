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
        if (reviewDate.isNullOrEmpty()) return ""

        // If already formatted as dd/MM/yyyy, return directly
        if (reviewDate.matches(Regex("""\d{2}/\d{2}/\d{4}"""))) {
            return reviewDate
        }

        val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        try {
            val inputFormat = SimpleDateFormat(SDF_PATTERN, SDF_LOCALE)
            val date = inputFormat.parse(reviewDate)
            if (date != null) {
                return outputFormat.format(date)
            }
        } catch (ignored: Exception) {}

        try {
            val timestamp = reviewDate.toLongOrNull()
            if (timestamp != null) {
                return outputFormat.format(Date(timestamp))
            }
        } catch (ignored: Exception) {}

        try {
            val date = Date(reviewDate)
            return outputFormat.format(date)
        } catch (ignored: Exception) {}

        return reviewDate
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
