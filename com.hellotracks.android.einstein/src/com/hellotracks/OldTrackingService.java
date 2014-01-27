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
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.hellotracks.c2dm.C2DMReceiver;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.map.HomeMapScreen;
import com.hellotracks.types.GPS;
import com.hellotracks.util.Time;

public class OldTrackingService extends Service {

    private static final TrackingLocationListener locationListener = new TrackingLocationListener();

    private LocationManager locationManager = null;

    private final IBinder binder = new TrackServiceBinder();

    private PendingIntent sendIntent;

    private static int counter = 0;

    public static class Settings {
        public long minTime = 30 * Time.SEC;
        public float minDistance = 50;
        public Mode mode = Mode.sport;
        public boolean gps = false;
        public boolean network = false;
        public long sendInterval = 15 * Time.MIN;
    }

    private Settings settings = new Settings();

    public Settings getSettings() {
        return settings;
    }

    public class PowerReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            setCharging(intent.getAction().equals(Intent.ACTION_POWER_CONNECTED));
            reregister();
        }
    }

    private BroadcastReceiver mWifiStateChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            reregister();
        }
    };

    public class TrackServiceBinder extends Binder {
        public OldTrackingService getService() {
            return OldTrackingService.this;
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

    private PowerReceiver powerReceiver;

    @Override
    public void onCreate() {
        counter++;
        super.onCreate();
        Logger.d("creating track service > " + counter);
        locationListener.setService(this);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        powerReceiver = new PowerReceiver();

        registerReceiver(powerReceiver, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        registerReceiver(powerReceiver, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        registerReceiver(mWifiStateChangedReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra("plugged", 0);
        charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;

        C2DMReceiver.refreshAppC2DMRegistrationState(getApplicationContext());
    }

    public void insertGPS(GPS gps) {
        try {
            DbAdapter.getInstance(this).insertGPS(gps);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(powerReceiver);
        unregisterReceiver(mWifiStateChangedReceiver);
        counter--;
        Logger.d("destroying track service > " + counter);
        stopLocationManager();
        stopSendManager();
        super.onDestroy();
    }

    @Override
    public void onStart(Intent intent, int startId) {
        Logger.d("starting track service onStart");
        preferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
        reregister();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d("starting track service onStartCommand");
        preferences.registerOnSharedPreferenceChangeListener(prefChangeListener);
        reregister();
        return Service.START_STICKY;
    }

    public void startSendManager() {
        Logger.d("starting send manager");
        Intent intent = new Intent(this, TrackingSender.class);
        intent.setAction(TrackingSender.ACTION_SEND);
        sendIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        long triggerTime = SystemClock.elapsedRealtime() + (10 * Time.SEC);

        Logger.d("starting send manager: " + settings.sendInterval);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime,
                TrackingSender.SEND_INTERVAL, sendIntent);
    }

    public void stopSendManager() {
        if (sendIntent != null) {
            try {
                Logger.d("stopping send manager");
                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(sendIntent);
                sendIntent = null;
            } catch (Exception exc) {
                Logger.w(exc);
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
        try {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            if (settings.gps) {
                ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                Logger.w("start gps if wifi not " + wifi.isConnectedOrConnecting());
                if (true || !wifi.isConnectedOrConnecting()) {
                    Logger.d("starting location manager (minTime=" + settings.minTime + ",minDistance="
                            + settings.minDistance + ", provider= " + "gps" + ")");
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, settings.minTime,
                            settings.minDistance, locationListener);
                } else {
                    Logger.w("not starting gps because wifi connected");
                }
            }
            if (settings.network) {
                Logger.d("starting location manager (minTime=" + settings.minTime + ",minDistance=" + settings.minDistance
                        + ", provider= " + "network" + ")");
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, settings.minTime,
                        settings.minDistance, locationListener);
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    private Notification createNotification(boolean trackingOn) {
        Resources res = getResources();
        String text;
        int icon = R.drawable.ic_stat_on;

        boolean gpsmsg = locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (trackingOn) {
            if (settings.mode != Mode.fuzzy && gpsmsg) {
                text = res.getString(R.string.EnableGPS);
                icon = R.drawable.ic_stat_on;
            } else {
                text = res.getString(R.string.trackingActiveInMode) + " ";
                switch (settings.mode) {
                case transport:
                    if (charging)
                        text += res.getString(R.string.Transport);
                    else {
                        text = res.getString(R.string.trackingWaitingToBeConnected) + " | " + res.getString(R.string.Transport);
                        icon = R.drawable.ic_stat_on;
                    }
                    break;
                case fuzzy:
                    text += res.getString(R.string.ModeRough);
                    break;
                case sport:
                    text += res.getString(R.string.Outdoor);
                    break;
                }
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
            if (locationManager != null) {
                Logger.i("stopping location manager");
                locationManager.removeUpdates(locationListener);
                locationManager.removeGpsStatusListener(locationListener);
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    private boolean charging = true;

    public void setCharging(boolean value) {
        this.charging = value;
        Logger.d("charging set to " + value);
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
        if (status) {
            startTracking();
            startForeground(NOTIFICATION_ID, createNotification(true));
        }
    }

    private void startTracking() {
        String mode = preferences.getString(Prefs.MODE, Mode.transport.toString());

        settings.mode = Mode.fromString(mode);
        if (Mode.isTransport(mode) && charging) {
            settings.minTime = 10 * Time.SEC;
            settings.minDistance = 75;
            settings.gps = true;
            settings.network = false;
            settings.sendInterval = 30 * Time.SEC;
        } else if (Mode.isOutdoor(mode)) {
            settings.minTime = 4000;
            settings.minDistance = 10;
            settings.gps = true;
            settings.network = true;
            settings.sendInterval = 30 * Time.SEC;
        } else {
            settings.minTime = 30 * Time.SEC;
            settings.minDistance = 10;
            settings.gps = false;
            settings.network = true;
            settings.sendInterval = 1 * Time.MIN;
        }
        startLocationManager();
        startSendManager();
    }
}