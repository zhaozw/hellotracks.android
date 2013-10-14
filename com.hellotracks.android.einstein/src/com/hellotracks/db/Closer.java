package com.hellotracks.db;

import android.database.Cursor;

import com.google.analytics.tracking.android.Log;

public class Closer {
    public static void close(Cursor c) {
        try {
            if (c != null)
                c.close();
        } catch (Exception exc) {
            Log.e(exc);
        }
    }
}
