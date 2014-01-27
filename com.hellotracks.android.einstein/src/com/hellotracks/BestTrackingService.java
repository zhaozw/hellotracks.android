package com.hellotracks;

import java.util.concurrent.atomic.AtomicInteger;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.hellotracks.c2dm.C2DMReceiver;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.map.HomeMapScreen;
import com.hellotracks.recognition.DetectionRemover;
import com.hellotracks.recognition.DetectionRequester;
import com.hellotracks.types.GPS;
import com.hellotracks.util.Time;

import de.greenrobot.event.EventBus;

public class BestTrackingService extends Service {

    private boolean locating = false;

    private PendingIntent sendWhileTrackingIntent;
    private PendingIntent sendAlwaysIntent;

    private AtomicInteger mActivityType = new AtomicInteger(DetectedActivity.STILL);
    private int mActivityConfidence = 0;

    /**
     * Class for clients to access. Because we know this service always runs in the same process as its clients, we
     * don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public BestTrackingService getService() {
            return BestTrackingService.this;
        }
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

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
                Logger.i("skipping new location change");
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
                    Logger.e(exc);
                }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    protected void handleActivityDetected(int type, int conf, boolean forceReregister) {
        int oldType = mActivityType.get();
        mActivityConfidence = conf;
        mActivityType.set(type);
        Logger.i("activity recognized: type=" + mActivityType + " confidence=" + mActivityConfidence);
        if (oldType != mActivityType.get() || forceReregister) {
            Logger.i("trigger regregister");
            reregister("activity type change");
        }
    }

    private SharedPreferences preferences;

    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (Prefs.STATUS_ONOFF.equals(key) || Prefs.MODE.equals(key) || Prefs.SEND_LOCATION_TO.equals(key)) {
                reregister("status or mode changed: " + key);
            }
        }
    };

    @Override
    public void onCreate() {
        counter++;
        super.onCreate();
        Logger.i("creating best track service > " + counter);
        mLocationClient = new LocationClient(this, new ConnectionCallbacks() {

            @Override
            public void onDisconnected() {
                Logger.d("disconnected to location client");
            }

            @Override
            public void onConnected(Bundle connectionHint) {
                reregister("connection to location client established");
                Logger.d("connected to location client");
            }
        }, new OnConnectionFailedListener() {

            @Override
            public void onConnectionFailed(ConnectionResult result) {
                Logger.d("connection to location client failed");
            }
        });
        mLocationClient.connect();

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        C2DMReceiver.refreshAppC2DMRegistrationState(getApplicationContext());

        preferences.registerOnSharedPreferenceChangeListener(prefChangeListener);

        EventBus.getDefault().register(this);
    }

    public void onEventMainThread(Mode event) {
        if (event == Mode.automatic) {
            ensureAllOK();
        }
    }

    public void insertGPS(GPS gps) {
        try {
            DbAdapter.getInstance(this).insertGPS(gps);
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    private void callDetectionRemover() {
        try {
            new DetectionRemover(this).removeUpdates(mDetectionRequester.getRequestPendingIntent());
            mDetectionRequester = null;
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    @Override
    public void onDestroy() {
        counter--;
        Logger.i("destroying track service > " + counter);

        callDetectionRemover();

        stopLocationManager();
        stopSendManager();
        stopAlwaysSendManager();

        preferences.unregisterOnSharedPreferenceChangeListener(prefChangeListener);

        try {
            mLocationClient.disconnect();
            EventBus.getDefault().unregister(this);
        } catch (Exception exc) {
            Logger.e(exc);
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    private void startSendManager() {
        Logger.i("starting send manager");
        Intent intent = new Intent(this, TrackingSender.class);
        intent.setAction(TrackingSender.ACTION_SEND);
        sendWhileTrackingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerTime = SystemClock.elapsedRealtime() + (10 * Time.SEC);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime,
                TrackingSender.SEND_INTERVAL, sendWhileTrackingIntent);
    }

    private void stopSendManager() {
        if (sendWhileTrackingIntent != null) {
            try {
                Logger.i("stopping send manager");
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(sendWhileTrackingIntent);
                sendWhileTrackingIntent = null;
            } catch (Exception exc) {
                Logger.w(exc);
            }
        }
    }

    private void startAlwaysSendManager() {
        Logger.i("starting always send");
        Intent intent = new Intent(this, TrackingSender.class);
        intent.setAction(TrackingSender.ACTION_SEND);
        sendAlwaysIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerTime = SystemClock.elapsedRealtime() + (10 * Time.MIN);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime, Time.H, sendAlwaysIntent);
    }

    private void stopAlwaysSendManager() {
        if (sendAlwaysIntent != null) {
            try {
                Logger.i("stopping sendAlwaysIntent");
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(sendAlwaysIntent);
                sendAlwaysIntent = null;
            } catch (Exception exc) {
                Logger.w(exc);
            }
        }
    }

    private static final int NOTIFICATION_ID = 100;

    public void startLocationManager() {
        if (!mLocationClient.isConnected()) {
            Logger.i("mLocationService not connected: returning");
            return;
        }
        LocationRequest locRequest = LocationRequest.create().setInterval(4000).setSmallestDisplacement(10)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationClient.requestLocationUpdates(locRequest, mLocationListener);

        locating = true;
    }

    private Notification createNotification() {
        Resources res = getResources();
        final String text;

        switch (mActivityType.get()) {
        case DetectedActivity.ON_FOOT:
            text = res.getString(R.string.ActivityWalking);
            break;
        case DetectedActivity.IN_VEHICLE:
            text = res.getString(R.string.ActivityDriving);
            break;
        case DetectedActivity.ON_BICYCLE:
            text = res.getString(R.string.ActivityCycling);
            break;
        default:
            text = res.getString(R.string.OnTheWay);
            break;
        }

        int icon = R.drawable.ic_stat_on;

        Context context = getApplicationContext();
        CharSequence contentTitle = getResources().getString(R.string.app_name);

        Intent notificationIntent = new Intent(this, HomeMapScreen.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context).setSmallIcon(icon)
                .setContentTitle(contentTitle).setContentText(text);
        builder.setOngoing(true).setUsesChronometer(true).setAutoCancel(false).setContentIntent(contentIntent);
        return builder.build();
    }

    public void stopLocationManager() {
        try {
            Logger.i("stopping location manager");
            if (mLocationClient.isConnected()) {
                Logger.i("removing location updates");
                mLocationClient.removeLocationUpdates(mLocationListener);
            }
            locating = false;
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    public void reregister(String reason) {
        Logger.i("reregistering because " + reason);
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
        Logger.i("restarting location manager");
        boolean status = preferences.getBoolean(Prefs.STATUS_ONOFF, false);
        
        if (sendAlwaysIntent == null && status) {
            stopAlwaysSendManager();
            startAlwaysSendManager();
        } else if (sendAlwaysIntent != null && !status) {
            stopAlwaysSendManager();
        }
        
        if (status && isMoving(mActivityType.get()) && locating) {
            Logger.i("already moving");
            maybeShowNotification();
            return;
        }

        if (status) {
            Logger.i("starting detection requester");
            mDetectionRequester = new DetectionRequester(this);
            mDetectionRequester.requestUpdates();
        }

        stopLocationManager();
        stopForeground(true);
        Logger.i("status=" + status + " activity=" + mActivityType.get());
        if ((status && isMoving(mActivityType.get())) || preferences.contains(Prefs.SEND_LOCATION_TO)) {
            startTracking();
            maybeShowNotification();
        }
    }

    private void maybeShowNotification() {
        if (mActivityType.get() < 2) {
            if (isTilting()) {
                // don't show notification if tilting
            } else {
                startForeground(NOTIFICATION_ID, createNotification());
            }
        }
    }

    public boolean isTilting() {
        return mLastOriginal != null && mLastOriginal.getType() == DetectedActivity.TILTING;
    }

    private void startTracking() {
        Logger.i("start tracking");
        startLocationManager();
        startSendManager();
    }

    private static boolean isMoving(int type) {
        return type <= 2;
    }

    private DetectedActivity mLastOriginal;

    public void activityDetected(int type, int confidence, DetectedActivity original) {
        boolean wasTilting = isTilting();
        this.mLastOriginal = original;
        handleActivityDetected(type, confidence, wasTilting);
    }

    public void ensureAllOK() {
        Logger.i("ensure all ok called");
        if (!mLocationClient.isConnected() && !mLocationClient.isConnecting()) {
            Logger.w("locClientConnected=" + mLocationClient.isConnected());
            mLocationClient.connect();
        }
    }
}