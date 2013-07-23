package com.hellotracks.util;

import java.text.DateFormat;

import android.content.Context;
import android.content.res.Resources;

import com.hellotracks.R;

public class Time {

    public static final long SEC = 1000;
    public static final long MIN = 60 * SEC;
    public static final long H = 60 * MIN;
    public static final long D = 24 * H;

    public static final long SECONDS = SEC;
    public static final long MINUTE = MIN;
    public static final long HOURS = H;
    public static final long YEAR = 365 * D;

    public static final long hours(int count) {
        return count * H;
    }

    public static final long minutes(int count) {
        return count * MIN;
    }
    
    public static final DateFormat FORMAT_TIME = DateFormat.getTimeInstance();

    public static final String formatTimePassed(Context context, long timestamp) {
        long now = System.currentTimeMillis();

        long time = (now - timestamp);
        int seconds = (int) (time / 1000L);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        int days = hours / 24;
        int months = days / 30;

        Resources r = context.getResources();

        if (time < 10) {
            return r.getString(R.string.JustNow);
        }

        // Less than one minute
        if (time < 60 * SECONDS)
            return seconds == 1 ? r.getString(R.string.OneSecondAgo) : r.getString(R.string.SecondsAgo, seconds);

        if (time < 60 * MINUTE)
            return minutes == 1 ? r.getString(R.string.OneMinuteAgo) : r.getString(R.string.MinutesAgo, minutes);

        if (time < 120 * MINUTE)
            return r.getString(R.string.AboutAnHourAgo);

        if (hours < 48) {
            return r.getString(R.string.HoursAgo, hours);
        }

        if (days < 100) {
            return r.getString(R.string.DaysAgo, days);
        }

        return r.getString(R.string.MonthsAgo, months);
    }
}
