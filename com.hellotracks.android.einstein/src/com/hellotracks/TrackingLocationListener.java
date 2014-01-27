package com.hellotracks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.hellotracks.types.GPS;

public class TrackingLocationListener implements LocationListener,
		GpsStatus.Listener {

	static OldTrackingService trackService;

	public TrackingLocationListener() {
	}

	public void setService(OldTrackingService service) {
		trackService = service;
	}

	private static long lastTimestamp = 0;

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

		if (LocationManager.GPS_PROVIDER.equals(loc.getProvider())) {
			gps.sensor = GPS.SENSOR_GPS;
		} else {
			gps.sensor = GPS.SENSOR_NETWORK;
		}

		trackService.insertGPS(gps);

		SharedPreferences settings = Prefs.get(trackService
				.getApplicationContext());
		int total = settings.getInt(Prefs.LOCATIONS_TOTAL, 0);
		if (total < 5) {
			try {
				Intent intent = new Intent(
						trackService.getApplicationContext(),
						TrackingSender.class);
				intent.setAction(TrackingSender.ACTION_SEND);
				PendingIntent sendIntent = PendingIntent.getBroadcast(
						trackService.getApplicationContext(), 0, intent, 0);
				AlarmManager alarmManager = (AlarmManager) trackService
						.getApplicationContext().getSystemService(
								Context.ALARM_SERVICE);
				long triggerAtTime = SystemClock.elapsedRealtime();
				alarmManager.setInexactRepeating(
						AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime,
						TrackingSender.SEND_INTERVAL, sendIntent);
			} catch (Exception exc) {
				Logger.w(exc);
			}
		}
	}

	public void onProviderDisabled(String provider) {
		Logger.i("provider disabled > " + provider);
	}

	public void onProviderEnabled(String provider) {
		Logger.i("provider enabled > " + provider);
		trackService.reregister();
	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
	}

	public void onGpsStatusChanged(int event) {

	}
}
