package com.hellotracks;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Criteria;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.hellotracks.base.C;
import com.hellotracks.c2dm.C2DMReceiver;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.map.HomeMapScreen;
import com.hellotracks.recognition.DetectionRequester;
import com.hellotracks.types.GPS;
import com.hellotracks.util.Time;

public class BestTrackingService extends Service {

    private boolean locating = false;

    private final IBinder binder = new TrackServiceBinder();

    private PendingIntent sendIntent;

    private int mActivityType = DetectedActivity.UNKNOWN;
    private int mActivityConfidence = 0;

    private BroadcastReceiver mActivityRecognitionReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            mActivityConfidence = intent.getIntExtra("confidence", 0);
            mActivityType = intent.getIntExtra("activityType", -1);
            Log.i("activity recognized: type=" + mActivityType + " confidence=" + mActivityConfidence);
            boolean isMoving = isMoving(mActivityType);
            if (locating != isMoving) {
                Log.i("trigger regregister");
                reregister();
            }
        }

    };
    private LocationClient mLocationClient;
    private DetectionRequester mDetectionRequester;
    private static int counter = 0;

    private LocationListener mLocationListener = new LocationListener() {

        private long lastTimestamp = 0;

        @Override
        public void onLocationChanged(Location loc) {
            GPS gps = new GPS();
            gps.ts = loc.getTime();
            gps.lat = loc.getLatitude();
            gps.lng = loc.getLongitude();
            gps.alt = (int) loc.getAltitude();
            gps.hacc = (int) loc.getAccuracy();
            gps.speed = (int) (loc.getSpeed() * 3.6);
            gps.head = (int) loc.getBearing();
            gps.hacc = (int) loc.getAccuracy();

            // fix for some mobile phones, unkown why loc.ts is one day before
            // current ts
            if (gps.ts > System.currentTimeMillis()) {
                gps.ts = System.currentTimeMillis();
            }

            if (Math.abs(gps.ts - lastTimestamp) < 1500) {
                Log.i("skipping new location change");
                return;
            }
            lastTimestamp = gps.ts;

            if (loc.getAccuracy() < 60) {
                gps.sensor = GPS.SENSOR_GPS;
            } else {
                gps.sensor = GPS.SENSOR_NETWORK;
            }

            insertGPS(gps);

            SharedPreferences settings = Prefs.get(getApplicationContext());
            int total = settings.getInt(Prefs.LOCATIONS_TOTAL, 0);
            if (total < 5) {
                try {
                    Intent intent = new Intent(getApplicationContext(), TrackingSender.class);
                    intent.setAction(TrackingSender.ACTION_SEND);
                    PendingIntent sendIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, intent, 0);
                    AlarmManager alarmManager = (AlarmManager) getApplicationContext().getSystemService(
                            Context.ALARM_SERVICE);
                    long triggerAtTime = SystemClock.elapsedRealtime();
                    alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime,
                            TrackingSender.SEND_INTERVAL, sendIntent);
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        }
    };

    public class TrackServiceBinder extends Binder {
        public BestTrackingService getService() {
            return BestTrackingService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private SharedPreferences preferences;

    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (Prefs.STATUS_ONOFF.equals(key) || Prefs.MODE.equals(key)) {
                reregister();
            }
        }
    };

    @Override
    public void onCreate() {
        counter++;
        super.onCreate();
        Log.d("creating best track service > " + counter);
        mLocationClient = new LocationClient(this, new ConnectionCallbacks() {

            @Override
            public void onDisconnected() {
                Log.i("disconnected to loc client");
            }

            @Override
            public void onConnected(Bundle connectionHint) {
                reregister();
                Log.i("connected to loc client");
            }
        }, new OnConnectionFailedListener() {

            @Override
            public void onConnectionFailed(ConnectionResult result) {
                Log.i("connection to loc client failed");
            }
        });
        mLocationClient.connect();

        mDetectionRequester = new DetectionRequester(getApplicationContext());
        mDetectionRequester.requestUpdates();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        C2DMReceiver.refreshAppC2DMRegistrationState(getApplicationContext());
    }

    public void insertGPS(GPS gps) {
        try {
            DbAdapter.getInstance(this).insertGPS(gps);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    @Override
    public void onDestroy() {
        counter--;
        Log.d("destroying track service > " + counter);
        stopLocationManager();
        stopSendManager();
        try {
            mLocationClient.disconnect();
            LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mActivityRecognitionReceiver);
        } catch (Exception exc) {
            Log.w(exc);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.w("starting track service onStartCommand");
        preferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
        reregister();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mActivityRecognitionReceiver,
                new IntentFilter(C.BROADCAST_ACTIVITYRECOGNIZED));
        return Service.START_STICKY;
    }

    public void startSendManager() {
        Log.d("starting send manager");
        Intent intent = new Intent(this, TrackingSender.class);
        intent.setAction(TrackingSender.ACTION_SEND);
        sendIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerTime = SystemClock.elapsedRealtime() + (10 * Time.SEC);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime,
                TrackingSender.SEND_INTERVAL, sendIntent);
    }

    public void stopSendManager() {
        if (sendIntent != null) {
            try {
                Log.d("stopping send manager");
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(sendIntent);
                sendIntent = null;
            } catch (Exception exc) {
                Log.w(exc);
            }
        }

    }

    private static final int NOTIFICATION_ID = 100;

    /** this criteria will settle for less accuracy, high power, and cost */
    public static Criteria createCoarseCriteria() {

        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_COARSE);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setSpeedRequired(false);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_HIGH);
        return c;

    }

    /** this criteria needs high accuracy, high power, and cost */
    public static Criteria createFineCriteria() {

        Criteria c = new Criteria();
        c.setAccuracy(Criteria.ACCURACY_FINE);
        c.setAltitudeRequired(false);
        c.setBearingRequired(false);
        c.setSpeedRequired(false);
        c.setCostAllowed(true);
        c.setPowerRequirement(Criteria.POWER_HIGH);
        return c;

    }

    public void startLocationManager() {
        if (!mLocationClient.isConnected()) {
            return;
        }
        LocationRequest locRequest = LocationRequest.create().setInterval(4000).setSmallestDisplacement(10)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        mLocationClient.requestLocationUpdates(locRequest, mLocationListener);
        locating = true;
    }

    private Notification createNotification(boolean trackingOn) {
        Resources res = getResources();
        String text;
        int icon = R.drawable.ic_stat_on;

        if (trackingOn) {
            switch (mActivityType) {
            case DetectedActivity.IN_VEHICLE:
                text = "Driving";
                break;
            case DetectedActivity.ON_BICYCLE:
                text = "Cycling";
                break;
            default:
                text = "Walking";
                break;
            }
        } else {
            text = res.getString(R.string.trackingInactive);
        }

        Context context = getApplicationContext();
        CharSequence contentTitle = getResources().getString(R.string.app_name);

        Intent notificationIntent = new Intent(this, HomeMapScreen.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification = new Notification(icon, null, System.currentTimeMillis());
        notification.setLatestEventInfo(context, contentTitle, text, contentIntent);
        notification.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        return notification;

    }

    public void stopLocationManager() {
        try {
            locating = false;
            if (mLocationClient.isConnected()) {
                mLocationClient.removeLocationUpdates(mLocationListener);
            }
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void reregister() {
        stopSendManager();

        Intent intent = new Intent(this, TrackingSender.class);
        intent.setAction(TrackingSender.ACTION_SEND);
        PendingIntent sendIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerAtTime = SystemClock.elapsedRealtime();
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, sendIntent);

        restartLocationManager();
    }

    public void restartLocationManager() {
        stopLocationManager();
        boolean status = preferences.getBoolean(Prefs.STATUS_ONOFF, false);
        if (status && isMoving(mActivityType)) {
            startTracking();
            startForeground(NOTIFICATION_ID, createNotification(true));
        } else {
            stopForeground(true);
        }
    }

    private void startTracking() {
        startLocationManager();
        startSendManager();
    }

    /**
     * Determine if an activity means that the user is moving.
     * 
     * @param type
     *            The type of activity the user is doing (see DetectedActivity constants)
     * @return true if the user seems to be moving from one location to another, otherwise false
     */
    private static boolean isMoving(int type) {
        switch (type) {
        // These types mean that the user is probably not moving
        case DetectedActivity.STILL:
        case DetectedActivity.TILTING:
        case DetectedActivity.UNKNOWN:
            return false;
        default:
            return true;
        }
    }
}