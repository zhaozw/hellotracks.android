package com.hellotracks.map;

import java.util.Date;

import javax.xml.datatype.Duration;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.base.C;
import com.hellotracks.util.Time;

public class ParkingReceiver extends BroadcastReceiver {

    private HomeMapScreen screen;

    public ParkingReceiver(HomeMapScreen screen) {
        this.screen = screen;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (C.BROADCAST_PARKING_UPDATE.equals(intent.getAction())) {
            try {
                Bundle extras = intent.getExtras();
                if (extras != null) {
                    double latitude = extras.getDouble("Lat");
                    double longitude = extras.getDouble("Long");
                    if ((latitude != -1000) && (longitude != -1000)) {
                        float accuracy = extras.getFloat("Accuracy");
                        long duration = extras.getLong("Duration");
                        Editor edit = Prefs.get(screen).edit();
                        edit.putLong(Prefs.PARKING_LAT, Double.doubleToRawLongBits(latitude));
                        edit.putLong(Prefs.PARKING_LNG, Double.doubleToRawLongBits(longitude));
                        edit.putFloat(Prefs.PARKING_ACC, accuracy);
                        edit.putLong(Prefs.PARKING_TS, System.currentTimeMillis() - (duration * Time.SEC));
                        edit.commit();
                        Logger.i("parking: " + latitude + " " + longitude + " " + accuracy + " " + duration);
                        screen.updateParkingMarker();
                    }
                }
            } catch (Exception exc) {
                Logger.e(exc);
            }
        }
    }

    public static void callParking(HomeMapScreen screen) {
        Intent intent = new Intent();
        intent.setAction(C.BROADCAST_PARKING_GET);
        intent.setClassName(screen.getPackageName(), C.PARKING_MOBILITY_SERVICE);
        screen.startService(intent);
    }

}
