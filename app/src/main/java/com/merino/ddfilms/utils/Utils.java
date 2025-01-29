package com.merino.ddfilms.utils;

import android.content.Context;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Utils {

    public static void showMessage(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }

    public static String formatDate(Date date) {
        String formattedDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
        return formattedDate;
    }
}
