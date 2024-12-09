package com.merino.ddfilms.utils;

import android.content.Context;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

public class Utils {

    public static void showMessage(Context context, String message) {
        if (context != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        }
    }
}
