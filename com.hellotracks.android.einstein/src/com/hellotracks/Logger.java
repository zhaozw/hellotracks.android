package com.hellotracks;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.analytics.tracking.android.StandardExceptionParser;

import android.content.Context;

public class Logger {
    static final String TAG = "HelloTracks";
    static final boolean LOG = BuildConfig.DEBUG;

    private static Context context;

    public static void setContext(Context context) {
        Logger.context = context;
    }

    public static void i(String string) {
        if (LOG)
            android.util.Log.i(TAG, string);
    }

    public static void e(String string) {
        if (LOG)
            android.util.Log.e(TAG, string);
    }

    private static void sendException(Throwable exc) {
        if (context != null) {
            try {
                EasyTracker.getInstance(context).send(
                        MapBuilder.createException(
                                new StandardExceptionParser(context, null).getDescription(Thread.currentThread()
                                        .getName(), exc), false).build());
            } catch (Exception e) {
                // sanity
            }
        }
    }

    public static void e(String string, Throwable exc) {
        if (LOG)
            android.util.Log.e(TAG, string, exc);
        else
            sendException(exc);
    }

    public static void e(Throwable exc) {
        if (LOG)
            android.util.Log.e(TAG, "", exc);
        else
            sendException(exc);
    }

    public static void d(String string) {
        if (LOG)
            android.util.Log.d(TAG, string);
    }

    public static void v(String string) {
        if (LOG)
            android.util.Log.v(TAG, string);
    }

    public static void w(String string) {
        if (LOG)
            android.util.Log.w(TAG, string);
    }

    public static void w(Exception exc) {
        if (LOG)
            android.util.Log.w(TAG, exc);
    }

    public static void w(String string, Exception exc) {
        if (LOG)
            android.util.Log.w(TAG, string, exc);
    }
}