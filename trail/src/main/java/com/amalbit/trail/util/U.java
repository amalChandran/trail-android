package com.amalbit.trail.util;

import android.util.Log;

public class U {

    public static void log(String message) {
        Log.i("Trail", "" + message);
    }

    public static void log(String title, String message) {
        Log.i(title, message);
    }

}
