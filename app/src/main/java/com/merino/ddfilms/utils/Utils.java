package com.merino.ddfilms.utils;

import android.content.Context;
import android.widget.Toast;

public class Utils {

    public static void showError(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
