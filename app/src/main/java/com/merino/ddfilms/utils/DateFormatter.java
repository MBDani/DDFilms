package com.merino.ddfilms.utils;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.merino.ddfilms.model.Review;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

@SafeParcelable.Constructor
public class DateFormatter {

    private String reviewDate;
    private final String SDF_PATTERN = "EEE MMM dd HH:mm:ss z yyyy";
    private static final Locale SDF_LOCALE = Locale.ENGLISH;

    public String getFormattedDate(String reviewDate) {
        // El formato de entrada esperado: "Sun Jun 01 23:23:24 GMT+02:00 2025"
        SimpleDateFormat inputFormat = new SimpleDateFormat(SDF_PATTERN, SDF_LOCALE);
        try {
            Date date = inputFormat.parse(reviewDate);
            Date now = new Date();
            long diffMillis = now.getTime() - date.getTime();

            // Evitar valores negativos (en caso de fechas futuras)
            if (diffMillis < 0) {
                diffMillis = 0;
            }

            long diffSeconds = diffMillis / 1000;
            if (diffSeconds < 10) {
                return "Ahora mismo";
            } else if (diffSeconds < 60) {
                return "Hace unos segundos";
            }

            long diffMinutes = diffSeconds / 60;
            if (diffMinutes < 60) {
                return diffMinutes == 1 ? "Hace 1 minuto" : "Hace " + diffMinutes + " minutos";
            }

            long diffHours = diffMinutes / 60;
            if (diffHours < 24) {
                return diffHours == 1 ? "Hace 1 hora" : "Hace " + diffHours + " horas";
            }

            long diffDays = diffHours / 24;
            if (diffDays == 1) {
                return "Ayer";
            } else if (diffDays < 7) {
                return "Hace " + diffDays + " días";
            } else {
                // Para intervalos mayores a una semana, se muestra la fecha formateada
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH);
                return outputFormat.format(date);
            }
        } catch (ParseException e) {
            // En caso de error en el parseo, devolver el string original.
            e.printStackTrace();
            return reviewDate;
        }
    }

    public Comparator<Review> reviewDateDescComparator() {
        return (r1, r2) -> {
            Date d1 = parseDateToString(r1.getReviewDate());
            Date d2 = parseDateToString(r2.getReviewDate());
            return d2.compareTo(d1);
        };
    }

    public Date parseDateToString(String dateString) {
        if (dateString == null) return new Date(0);
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(SDF_PATTERN, SDF_LOCALE);
            return sdf.parse(dateString);
        } catch (ParseException e) {
            return new Date(0);
        }
    }
}
