package com.hellotracks.util;

import java.io.Serializable;

import android.content.Context;
import android.location.Location;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.SphericalUtil;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;

public class UnitUtils implements Serializable {

    private static final long serialVersionUID = 5513261628590614985L;

    public static final int METER = 1;
    public static final int KM = 1000 * METER;

    public static final String DISTANCE_SI = "SI";
    public static final String DISTANCE_US = "US";

    public static final String DATE_DMY = "dd.MM.yyyy";
    public static final String DATE_MDY = "MM-dd-yyyy";
    public static final String DATE_YMD = "yyyy/MM/dd";

    public static final String TIME_12 = "12";
    public static final String TIME_24 = "24";

    public static double round(double value) {
        return ((int) (value * 10) / 10.0);
    }

    public static String distanceToKM(double distance) {
        return round(distance / 1000.0) + " km";
    }

    public static String distanceToMiles(double distance) {
        return round(distance * 0.000621371) + " mi";
    }

    public static String speedToKMH(double kmh) {
        return round(kmh) + " km/h";
    }

    public static String speedToMPH(double kmh) {
        return round(kmh * 0.621) + " mph";
    }

    public static double altToFeet(double meter) {
        return round(meter * 3.2808399);
    }

    public static String altitudeToFeet(double meter) {
        return round(meter * 3.2808399) + " ft";
    }

    public static String altitudeToMeter(double meter) {
        return round(meter) + " m";
    }

    public static double fromMeterToKM(double m) {
        return m / 1000.0;
    }

    public static double fromMeterToMiles(double m) {
        return m / 1000.0 / 1.6093;
    }

    public static CharSequence getNiceDistance(Context context, LatLng point, Location last) {
        try {
            double meter = SphericalUtil.computeDistanceBetween(new com.hellotracks.types.LatLng(last).toGoogle(),
                    point);

            if (!Prefs.isDistanceUS(context)) {
                return UnitUtils.distanceToKM(meter);
            } else {
                return UnitUtils.distanceToMiles(meter);
            }
        } catch (Exception exc) {
            Logger.e(exc);
            return "";
        }
    }
}
