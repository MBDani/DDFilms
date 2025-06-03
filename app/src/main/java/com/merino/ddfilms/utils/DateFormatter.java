package com.merino.ddfilms.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DateFormatter {

    private String reviewDate;

    public DateFormatter(String reviewDate) {
        this.reviewDate = reviewDate;
    }

    public String getFormattedDate() {
        // El formato de entrada esperado: "Sun Jun 01 23:23:24 GMT+02:00 2025"
        SimpleDateFormat inputFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
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
}
