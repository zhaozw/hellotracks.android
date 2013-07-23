package com.hellotracks.c2dm;

import org.json.JSONObject;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.hellotracks.Log;
import com.hellotracks.Mode;
import com.hellotracks.Prefs;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.einstein.C;

public class C2DMReceiver extends C2DMBaseReceiver {
	public static final String SENDER = "152563520904";

	public C2DMReceiver() {
		super(SENDER);
	}

	@Override
	public void onRegistered(Context context, String registration) {
		Log.d("onRegistered");
		try {
			JSONObject obj = AbstractScreen.prepareObj(context);
			obj.put("gcm_registration", registration);

			AbstractScreen.doBackgroundAction(AbstractScreen.ACTION_SETVALUE,
					obj);
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	@Override
	public void onUnregistered(Context context) {
		Log.d("onUnregistered");
	}

	@Override
	public void onError(Context context, String errorId) {
	}

	@Override
	public void onMessage(Context context, Intent intent) {
		Bundle extras = intent.getExtras();
		if (extras != null) {
			String msg = (String) extras.get("msg");
			String title = (String) extras.get("title");
			String account = (String) extras.get("account");
			int type;
			try {
				if (msg != null && msg.startsWith("@!")) {
					if ("@!starttrackingservice".equals(msg)) {
						Prefs.get(context).edit()
								.putBoolean(Prefs.STATUS_ONOFF, true).commit();
						maybeStartService();
					} else if ("@!stoptrackingservice".equals(msg)) {
						Prefs.get(context).edit()
								.putBoolean(Prefs.STATUS_ONOFF, false).commit();
						stopService(new Intent(this, C.trackingServiceClass));
					} else if ("@!starttransport".equals(msg)) {
						Prefs.get(context).edit()
								.putString(Prefs.MODE, Mode.transport.name())
								.putBoolean(Prefs.STATUS_ONOFF, true).commit();
						startService(new Intent(this, C.trackingServiceClass));
					} else if ("@!startoutdoor".equals(msg)) {
						Prefs.get(context).edit()
								.putString(Prefs.MODE, Mode.sport.name())
								.putBoolean(Prefs.STATUS_ONOFF, true).commit();
						startService(new Intent(this, C.trackingServiceClass));
					} else if ("@!startlogging".equals(msg)) {
						Prefs.get(context).edit()
								.putBoolean(Prefs.LOGGING, true).commit();
					} else if ("@!stoplogging".equals(msg)) {
						Prefs.get(context).edit()
								.putBoolean(Prefs.LOGGING, false).commit();
					} else if (msg.startsWith("@!deletegps")) {
						long to = System.currentTimeMillis();
						try {
							to = Long.parseLong(msg.substring("@!deletegps"
									.length()));
						} catch (Exception exc) {
						}
						DbAdapter dbAdapter = null;
						try {
							dbAdapter = new DbAdapter(context);
							dbAdapter.open();
							dbAdapter.deleteGPS(to);
						} catch (Exception exc) {
							Log.w(exc);
						} finally {
							dbAdapter.close();
						}
					} else if (msg.startsWith("@!playstore")) {
						String txt = msg.substring("@!playstore".length());
						LauncherUtils.generatePlayStoreNotification(context,
								txt.trim());
					} else if (msg.startsWith("@!uri")) {
						int space = msg.indexOf("text:");
						if (space < 0)
							space = msg.length();
						String uri = msg.substring(5, space);
						String txt = msg.substring(space + 5);
						LauncherUtils.generateUriNotification(context, title,
								uri.trim(), txt.trim());
					}
					return;
				}

				if (msg.startsWith("@uri")) {
					int space = msg.indexOf("text:");
					if (space < 0)
						space = msg.length();
					String uri = msg.substring(4, space);
					String txt = msg.substring(space + 5);
					LauncherUtils.generateUriNotification(context, title,
							uri.trim(), txt.trim());
					return;
				}

				type = Integer.parseInt((String) extras.get("type"));
			} catch (Exception exc) {
				type = 0;
			}

			Intent i = new Intent();
			i.putExtra("msg", msg);
			i.putExtra("title", title);
			i.putExtra("type", type);
			if (account != null)
				i.putExtra("account", account);
			i.setAction(Prefs.PUSH_INTENT);
			context.sendBroadcast(i);

			LauncherUtils.generateNotification(context, msg, title, account,
					type, intent);
		}
	}

	private void maybeStartService() {
		if (!isMyServiceRunning()) {
			Log.i("service not running -> start it");
			Intent serviceIntent = new Intent(this, C.trackingServiceClass);
			startService(serviceIntent);
		}
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (C.trackingServiceClass.getCanonicalName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Register or unregister based on phone sync settings. Called on each
	 * performSync by the SyncAdapter.
	 */
	public static void refreshAppC2DMRegistrationState(Context context) {
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(context);
		String username = settings.getString(Prefs.USERNAME, null);
		String password = settings.getString(Prefs.PASSWORD, null);
		if (username != null && password != null) {
			C2DMessaging.register(context, SENDER);
		} else {
			C2DMessaging.unregister(context);
		}

	}
}