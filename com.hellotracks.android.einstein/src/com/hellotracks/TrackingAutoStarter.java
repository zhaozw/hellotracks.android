package com.hellotracks;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class TrackingAutoStarter extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.d("boot received");
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context.getApplicationContext());
		if (settings.getBoolean(Prefs.STATUS_ONOFF, true)) {
			context.startService(new Intent(context, TrackingService.class));
		}
	}
}