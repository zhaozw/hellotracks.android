package com.hellotracks.util;

import android.app.Activity;
import android.location.Location;
import android.os.Build;

import com.hellotracks.Prefs;

public class Utils {

    public static final long TWO_MINUTES = 2 * Time.MIN;
    
    public static boolean hasFroyo() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
    }

    public static boolean hasGingerbread() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
    }

    public static boolean hasHoneycomb() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
    }

    public static boolean hasHoneycombMR1() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
    }

    public static boolean hasICS() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;
    }

    public static boolean hasJellyBean() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     * 
     * @param location
     *            The new Location that you want to evaluate
     * @param currentBestLocation
     *            The current Location fix, to which you want to compare the new one
     */
    public static boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use
        // the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be
            // worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(), currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and
        // accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /** Checks whether two providers are the same */
    private static boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public static String createMaresi(String username, String password) {
        StringBuilder sb = new StringBuilder();

        String concat = username + password;
        long c = 0;
        long u = 0;
        long p = 0;
        for (int i = 0; i <= concat.length(); i++) {
            c *= concat.charAt(i) * 7;
            u *= (concat.charAt(i) + i) * 31;
            p *= (concat.charAt(i) + concat.length() * 7) * 11;
        }
        int first = (int) (c % 100l);
        int second = (int) (u % 100l);
        int third = (int) (p % 100l);

        konsonant(sb, first);
        vokal(sb, first);
        konsonant(sb, second);
        vokal(sb, second);
        konsonant(sb, third);
        vokal(sb, third);
        return sb.toString();
    }

    private static void vokal(StringBuilder sb, int first) {
        switch (first % 5) {
        case 0:
            sb.append("a");
            break;
        case 1:
            sb.append("e");
            break;
        case 2:
            sb.append("i");
            break;
        case 3:
            sb.append("o");
            break;
        case 4:
            sb.append("u");
            break;
        }
    }

    private static void konsonant(StringBuilder sb, int first) {
        switch (first % 20) {
        case 0:
            sb.append("b");
            break;
        case 1:
            sb.append("d");
            break;
        case 2:
            sb.append("f");
            break;
        case 3:
            sb.append("g");
            break;
        case 4:
            sb.append("h");
        case 5:
            sb.append("j");
        case 6:
            sb.append("l");
        case 7:
            sb.append("m");
        case 8:
            sb.append("n");
        case 9:
            sb.append("p");
        case 10:
            sb.append("q");
        case 11:
            sb.append("r");
        case 12:
            sb.append("s");
        case 13:
            sb.append("t");
        case 14:
            sb.append("u");
        case 15:
            sb.append("v");
        case 16:
            sb.append("w");
        case 17:
            sb.append("x");
        case 18:
            sb.append("y");
        case 19:
            sb.append("z");

            break;
        }
    }

    public static String getDeviceAccountUsername(Activity activity) {
        int hash = Prefs.id(activity);
        int i = (int) (hash % 1000000000L);
        StringBuilder sb = new StringBuilder();
        sb.append(i);
        while(sb.length() < 6) {
            sb.insert(0, "0");
        }
        return "#" + sb;
    }

    public static String getDeviceAccountPassword(Activity activity) {
        int hash = Prefs.id(activity);
        
        int i = (int) (hash % 1000000L);
        
        StringBuilder sb = new StringBuilder();
        sb.append(i);
        while(sb.length() < 6) {
            sb.insert(0, "0");
        }
        return sb.toString();
    }

}
