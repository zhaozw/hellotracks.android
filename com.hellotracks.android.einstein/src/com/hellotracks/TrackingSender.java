package com.hellotracks;

import java.io.IOException;
import java.security.MessageDigest;

import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.hellotracks.db.DbAdapter;
import com.hellotracks.types.GPS;
import com.hellotracks.types.Track;

public class TrackingSender extends BroadcastReceiver {

    private static final int MAX = 200;
    public static final String HOST_REAL = "http://hellotracks.com/port/";
    public static final String ACTION_SEND = "com.hellotracks.send";
    public static final long SEND_INTERVAL = 45000;

    private static long lastTransmission = 0;

    @Override
    public void onReceive(final Context context, Intent intent) {
        if (ACTION_SEND.equals(intent.getAction())) {
            long diff = Math.abs(System.currentTimeMillis() - lastTransmission);
            if (diff > 1000) {
                Log.d("handle alarm");
                handle(context);
                lastTransmission = System.currentTimeMillis();
            }
        }
    }

    private void handle(Context context) {
		SharedPreferences preferences = PreferenceManager
				.getDefaultSharedPreferences(context);
		String username = preferences.getString(Prefs.USERNAME, "");
		String password = preferences.getString(Prefs.PASSWORD, "");

		GPS[] locations = selectGPS(context);

		String txt = "sending " + locations.length + " locations to "
				+ username;
		Log.d(txt);

		if (locations.length > 0) {
			sendAsync(context, username, password, locations);
		}
		
		long autostop = preferences.getLong(Prefs.TRACKING_AUTOSTOP_AT, 0);
		if (autostop > 0) {
		    long now = System.currentTimeMillis();
		    if (now > autostop) {
		        preferences.edit().putBoolean(Prefs.STATUS_ONOFF, false).putLong(Prefs.TRACKING_AUTOSTOP_AT, 0).commit();
		    }
		}
	}

    private GPS[] selectGPS(final Context context) {
        DbAdapter dbAdapter = null;
        try {
            dbAdapter = new DbAdapter(context);
            dbAdapter.open();
            GPS[] locations = dbAdapter.selectGPS(MAX);
            return locations;
        } catch (Exception exc) {
            Log.e("exception while sending locations", exc);
        } finally {
            try {
                dbAdapter.close();
            } catch (Exception exc) {
            }
        }
        return new GPS[0];
    }

    private int cycle = 0;

    private void sendAsync(final Context context, String username, String password, final GPS... locations) {
        try {

            final Track track = new Track();
            for (GPS location : locations) {
                track.add(location);
            }

            final long to = track.lastAny().ts;
            final JSONObject json = createJson(username, password, track);
            Log.d("sending " + track.size() + " locations");

            new Thread() {
                public void run() {
                    StringBuilder log = new StringBuilder();

                    log.append(json);

                    boolean response = TrackingSender.Scan.process(json);

                    log.append("-->" + response);

                    if (response) {
                        boolean exceptionOcurred = false;
                        DbAdapter dbAdapter = null;
                        try {
                            dbAdapter = new DbAdapter(context);
                            dbAdapter.open();
                            dbAdapter.deleteGPS(to);
                            log.append("-->delOK");
                        } catch (Exception exc) {
                            log.append("-->delFail:" + exc.getMessage());
                            exceptionOcurred = true;
                            Log.w(exc);
                        } finally {
                            dbAdapter.close();
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
                    } else {
                        Log.w("response failed: " + response);
                        /*
                         * try { JSONObject obj =
                         * AbstractScreen.prepareObj(context);
                         * obj.put("logging", "senderFail=" + response);
                         * AbstractScreen.doAction(context,
                         * AbstractScreen.ACTION_SETVALUE, obj, null, null); }
                         * catch (Exception exc) { Log.w(exc); }
                         */
                    }

                };
            }.start();
        } catch (Exception exc) {
            Log.e("sending error: " + exc.getMessage(), exc);
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
            Log.w(exc);
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
            Log.e("md5 could not be created", exc);
        }
        return "";
    }

    public static class Scan {

        public static boolean process(JSONObject json) {
            boolean status = true;
            HttpParams httpParameters = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout(httpParameters, 7000);
            HttpConnectionParams.setSoTimeout(httpParameters, 7000);
            HttpClient httpclient = new DefaultHttpClient(httpParameters);
            HttpPost httppost = null;
            try {
                ResponseHandler<String> responseHandler = new BasicResponseHandler();

                String url = HOST_REAL + "?format=json";
                httppost = new HttpPost(url);
                Log.i(url);

                httppost.setEntity(new ByteArrayEntity(json.toString().getBytes("UTF8")));
                Log.w("-->" + json.toString());
                String response = httpclient.execute(httppost, responseHandler);
                status = isResponseOK(response);
            } catch (IOException exc) {
                status = false;
            } catch (RuntimeException exc) {
                Log.w(exc);
                try {
                    httppost.abort();
                } catch (Exception exc2) {
                }
            } finally {
                try {
                    httpclient.getConnectionManager().shutdown();
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
            return status;
        }

        private static boolean isResponseOK(String response) {
            try {
                JSONObject json = new JSONObject(response);
                return json.getInt("status") == 0;
            } catch (Exception e) {
                Log.e(e);
                return false;
            }
        }
    }
}
