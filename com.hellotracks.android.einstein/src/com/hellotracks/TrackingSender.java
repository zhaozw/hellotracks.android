package com.hellotracks;

import java.security.MessageDigest;
import java.util.HashMap;

import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.hellotracks.api.API;
import com.hellotracks.api.StringRequest;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.types.GPS;
import com.hellotracks.types.Track;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Time;

import de.greenrobot.event.EventBus;

public class TrackingSender extends BroadcastReceiver {

    private static final int MAX = 200;
    public static final String HOST_REAL = "http://hellotracks.com/port/";
    public static final String ACTION_SEND = "com.hellotracks.send";
    public static final long SEND_INTERVAL_FAST = 30000;
    public static final long SEND_INTERVAL = 45000;

    private static long lastTransmission = 0;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (ACTION_SEND.equals(intent.getAction())) {
            long diff = Math.abs(System.currentTimeMillis() - lastTransmission);
            if (diff > 1000) {
                Logger.d("handle alarm");
                handle(context);
                lastTransmission = System.currentTimeMillis();
            }
            final SharedPreferences prefs = Prefs.get(context);
            boolean status = prefs.getBoolean(Prefs.STATUS_ONOFF, false);
            String mode = prefs.getString(Prefs.MODE, Mode.sport.toString());
            if (status && Mode.isAutomatic(mode)) {
                if (!AbstractScreen.isMyServiceRunning(context, BestTrackingService.class)) {
                    com.hellotracks.Logger.w("restoring BestTrackingService out of TrackingSender");
                    Intent serviceIntent = new Intent(context, BestTrackingService.class);
                    context.startService(serviceIntent);
                } else {
                    EventBus.getDefault().post(Mode.automatic);
                }
            }
        }
    }

    private void handle(Context context) {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String username = preferences.getString(Prefs.USERNAME, "");
        String password = preferences.getString(Prefs.PASSWORD, "");

        GPS[] locations = selectGPS(context);

        if (locations.length == 0) {
            locations = maybeSendLastKnownLocation(context, locations);
        }

        String txt = "sending " + locations.length + " locations to " + username;
        Logger.d(txt);

        if (locations.length > 0) {
            sendAsync(context, username, password, locations);
            logTrackingToGA(context, locations);
            if (preferences.contains(Prefs.SEND_LOCATION_TO)) {
                handleForceSendLocationTo(context, preferences, locations);
            }
        }

        long autostop = preferences.getLong(Prefs.TRACKING_AUTOSTOP_AT, 0);
        if (autostop > 0) {
            long now = System.currentTimeMillis();
            if (now > autostop) {
                preferences.edit().putBoolean(Prefs.STATUS_ONOFF, false).putLong(Prefs.TRACKING_AUTOSTOP_AT, 0)
                        .commit();
            }
        }
    }

    protected GPS[] maybeSendLastKnownLocation(Context context, GPS[] locations) {
        try {
            SharedPreferences prefs = Prefs.get(context);
            long ts = prefs.getLong(Prefs.LAST_TRANSMISSION, 0);
            if (System.currentTimeMillis() - ts > Time.MIN * 30) {
                Logger.d("forcing to send last known location");
                final LocationManager mlocManager = (LocationManager) context
                        .getSystemService(Context.LOCATION_SERVICE);
                final Location loc = mlocManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) {
                    GPS gps = GPS.fromLocation(loc);
                    locations = new GPS[] { gps };
                    prefs.edit().putLong(Prefs.LAST_TRANSMISSION, System.currentTimeMillis()); // force it
                }
            }
        } catch (Exception exc) {
            Logger.e(exc);
        }
        return locations;
    }

    protected void logTrackingToGA(Context context, GPS[] locations) {
        try {
            EasyTracker.getInstance(context).send(
                    MapBuilder.createEvent("tracking", "sender", "gps", (long) locations.length).build());
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    protected void handleForceSendLocationTo(Context context, final SharedPreferences preferences, GPS[] locations) {
        try {
            GPS gps = locations[locations.length - 1];
            JSONObject data = AbstractScreen.prepareObj(context);
            String msg = "@uri geo:0,0?q=";
            String loc = gps.lat + "," + gps.lng + "(" + preferences.getString(Prefs.NAME, "") + ")";
            msg += loc + " text: " + context.getResources().getString(R.string.AutoLocation);
            data.put("msg", msg);
            data.put("receiver", preferences.getString(Prefs.SEND_LOCATION_TO, ""));
            API.doAction(context, AbstractScreen.ACTION_SENDMSG, data, null, new ResultWorker() {
                @Override
                public void onResult(String result, Context context) {
                    preferences.edit().remove(Prefs.SEND_LOCATION_TO).commit();
                }
            });
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    private GPS[] selectGPS(final Context context) {
        try {
            GPS[] locations = DbAdapter.getInstance(context).selectGPS(MAX);
            return locations;
        } catch (Exception exc) {
            Logger.e("exception while sending locations", exc);
        }
        return new GPS[0];
    }

    private int cycle = 0;

    private void sendAsync(final Context context, String username, String password, final GPS... locations) {
        try {
            if (username == null || username.length() == 0) {
                return;
            }

            final Track track = new Track();
            for (GPS location : locations) {
                track.add(location);
            }

            final long to = track.lastAny().ts;
            final JSONObject json = createJson(username, password, track);
            Logger.d("sending " + track.size() + " locations");

            Listener<String> listener = new Listener<String>() {
                @Override
                public void onResponse(String response) {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getInt("status") == 0) {
                            boolean exceptionOcurred = false;
                            try {
                                DbAdapter.getInstance(context).deleteGPS(to);
                            } catch (Exception exc) {
                                exceptionOcurred = true;
                                Logger.w(exc);
                            }

                            SharedPreferences settings = Prefs.get(context);

                            int total = settings.getInt(Prefs.LOCATIONS_TOTAL, 0) + track.size();
                            settings.edit().putInt(Prefs.LOCATIONS_TOTAL, total)
                                    .putLong(Prefs.LAST_TRANSMISSION, System.currentTimeMillis()).commit();

                            if (locations.length >= MAX && ++cycle < 10 && !exceptionOcurred) {
                                callAgain(context);
                            } else {
                                cycle = 0;
                            }
                        }

                    } catch (Exception e) {
                        Logger.e(e);
                    }
                }
            };

            ErrorListener errorListener = new ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    // callAgain(context);
                }
            };

            TrackingSender.Scan.process(context, json, listener, errorListener);
        } catch (Exception exc) {
            Logger.e("sending error: " + exc.getMessage(), exc);
        }
    }

    private void callAgain(final Context context) {
        lastTransmission = 0;
        Intent intent = new Intent(context, TrackingSender.class);
        intent.setAction(ACTION_SEND);
        PendingIntent sendIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long triggerAtTime = SystemClock.elapsedRealtime();
        alarmManager
                .setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, SEND_INTERVAL, sendIntent);
    }

    public static JSONObject createJson(String username, String password, Track track) {
        try {
            String n = String.valueOf(System.currentTimeMillis());

            JSONObject root = new JSONObject();
            root.put("cnt", track.size());
            root.put("tok", n);
            root.put("usr", username);
            root.put("pwd", md5(n, password));
            root.put("gps", track.encodePoints());
            root.put("dts", track.encodeTimes());
            root.put("alt", track.encodeAltitudes());
            root.put("spd", track.encodeSpeeds());
            root.put("sen", track.encodePoints());
            root.put("dir", track.encodeHeadings());
            root.put("hac", track.encodeHAccs());
            root.put("sen", track.encodeSensors());
            root.put("src", "aos" + Build.VERSION.SDK_INT);
            root.put("ver", "1.1.0");

            return root;
        } catch (Exception exc) {
            Logger.w(exc);
            return null;
        }
    }

    public static String md5(String n, String password) {
        try {
            // Create MD5 Hash
            MessageDigest digest = MessageDigest.getInstance("MD5");
            String s = n + password;
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++)
                hexString.append(Integer.toHexString(0xFF & messageDigest[i]));
            return hexString.toString();

        } catch (Exception exc) {
            Logger.e("md5 could not be created", exc);
        }
        return "";
    }

    public static class Scan {

        private static HashMap<Context, RequestQueue> queues = new HashMap<Context, RequestQueue>();

        public static void process(Context context, JSONObject json, Listener<String> listener,
                ErrorListener errorListener) {
            RequestQueue queue = queues.get(context);
            if (queue == null) {
                queue = Volley.newRequestQueue(context);
                queues.put(context, queue);
            }

            String url = HOST_REAL + "?format=json";
            StringRequest request = new StringRequest(url, json, listener, errorListener);
            request.setRetryPolicy(new DefaultRetryPolicy(API.TIMEOUT, 0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            request.setShouldCache(false);
            queue.add(request);
        }
    }
}
