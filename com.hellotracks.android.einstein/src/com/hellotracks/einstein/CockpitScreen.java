package com.hellotracks.einstein;

import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONObject;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingSender;
import com.hellotracks.TrackingService.Mode;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class CockpitScreen extends AbstractScreen {

	private static final int MODE_OFF = R.id.offButton;
	private static final int MODE_FUZZY = R.id.roughLocatingButton;
	private static final int MODE_TRANSPORT = R.id.transportButton;
	private static final int MODE_OUTDOOR = R.id.outdoorButton;

	private SharedPreferences prefs;

	private Timer timer = null;

	protected void onResume() {
		super.onResume();
		timer = new Timer();
		timer.schedule(new UpdateTimeTask(), 1000, 3000);

		locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

		prefs.edit().putLong(Prefs.LASTLOG, System.currentTimeMillis())
				.commit();

		String mode = prefs.getString(Prefs.MODE, null);

		if (!isActive()) {
			offBtn.setChecked(true);
		} else if (Mode.isOutdoor(mode)) {
			outdoorBtn.setChecked(true);
			onModeChanged(outdoorBtn.getId());
		} else if (Mode.isTransport(mode)) {
			transportBtn.setChecked(true);
			onModeChanged(transportBtn.getId());
		} else if (Mode.isFuzzy(mode)) {
			roughBtn.setChecked(true);
			onModeChanged(roughBtn.getId());
		}
	};

	@Override
	protected void onPause() {
		if (timer != null)
			timer.cancel();
		super.onPause();
	}

	class UpdateTimeTask extends TimerTask {

		public void run() {
			try {
				runOnUiThread(new Runnable() {

					@Override
					public void run() {
						DbAdapter dbAdapter = null;
						try {
							dbAdapter = new DbAdapter(getApplicationContext());
							dbAdapter.open();
							block4top
									.setText(String.valueOf(dbAdapter.count()));
						} catch (Exception exc) {
							Log.w(exc);
							block4top.setText("-");
						} finally {
							try {
								dbAdapter.close();
							} catch (Exception exc) {
							}
						}

						int total = prefs.getInt(Prefs.LOCATIONS_TOTAL, 0);
						block4bottom.setText(String.valueOf(total));

						ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
						android.net.NetworkInfo wifi = connec
								.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
						android.net.NetworkInfo mobile = connec
								.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

						if (wifi.isConnected()) {
							block2top.setText("Wi-Fi");
							block2bottom
									.setBackgroundResource(R.drawable.block_bottom);
						} else if (mobile != null && mobile.isConnected()) {
							block2top.setText("3G");
							block2bottom
									.setBackgroundResource(R.drawable.block_bottom);
						} else {
							block2top.setText("-");
							block2bottom
									.setBackgroundResource(R.drawable.block_bottom_red);
						}

						Intent intent = registerReceiver(null,
								new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

						int plugged = intent.getIntExtra(
								BatteryManager.EXTRA_PLUGGED, -1);
						boolean power = plugged == BatteryManager.BATTERY_PLUGGED_AC
								|| plugged == BatteryManager.BATTERY_PLUGGED_USB;
						int level = intent.getIntExtra(
								BatteryManager.EXTRA_LEVEL, -1);
						block3top.setText(level + "%");
						if (level < 10) {
							block3bottom
									.setBackgroundResource(R.drawable.block_bottom_red);
						} else if (power || !isActive()) {
							block3bottom
									.setBackgroundResource(R.drawable.block_bottom);
						} else if (isModeTransport()) {
							block3bottom
									.setBackgroundResource(R.drawable.block_bottom_orange);
						} else if (level < 30) {
							block3bottom
									.setBackgroundResource(R.drawable.block_bottom_orange);
						} else {
							block3bottom
									.setBackgroundResource(R.drawable.block_bottom);
						}

						Location gpsLoc = locationManager
								.getLastKnownLocation(LocationManager.GPS_PROVIDER);

						if (gpsLoc != null) {
							if (System.currentTimeMillis() - gpsLoc.getTime() < 60000) {
								if (gpsLoc.getAccuracy() < 50) {
									block1bottom
											.setBackgroundResource(R.drawable.block_bottom);
								} else {
									block1bottom
											.setBackgroundResource(R.drawable.block_bottom_orange);
								}
								if (Prefs.isDistanceUS(CockpitScreen.this)) {
									int ft = (int) (gpsLoc.getAccuracy() * 3.2808399);
									block1top.setText(ft + "ft");
								} else {
									int meter = (int) gpsLoc.getAccuracy();
									block1top.setText(meter + "m");
								}
							} else {
								block1top.setText("-");
								block1bottom
										.setBackgroundResource(R.drawable.block_bottom_orange);
							}
						} else {
							block1top.setText("-");
							block1bottom
									.setBackgroundResource(R.drawable.block_bottom_red);
						}

						if (!isActive()) {
							block1bottom
									.setBackgroundResource(R.drawable.block_bottom);
						}
						if (isModeFuzzy() || isModeTransport() && !power) {
							block1bottom.setText(R.string.Locating);
							boolean net = locationManager != null
									&& locationManager
											.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
							if (!net) {
								block1top.setText("-");
								block1bottom
										.setBackgroundResource(R.drawable.block_bottom_red);
							} else {
								Location netLoc = locationManager
										.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
								if (netLoc != null) {
									if (Prefs.isDistanceUS(CockpitScreen.this)) {
										int ft = (int) (netLoc.getAccuracy() * 3.2808399);
										if (ft > 900)
											block1top.setText(">900ft");
										else
											block1top.setText(ft + "ft");
										block1bottom
												.setBackgroundResource(R.drawable.block_bottom);
									} else {
										int meter = (int) netLoc.getAccuracy();
										if (meter > 300)
											block1top.setText(">300m");
										else
											block1top.setText(meter + "m");

										block1bottom
												.setBackgroundResource(R.drawable.block_bottom);
									}
								} else {
									block1top.setText("-");
									block1bottom
											.setBackgroundResource(R.drawable.block_bottom_red);
								}
							}
						} else {
							block1bottom.setText(R.string.GPS);
						}
					}
				});
			} catch (Exception exc) {
			}
		}
	}

	@Override
	protected void onDestroy() {
		timer = null;
		super.onDestroy();
	}

	private LocationManager locationManager = null;
	private TextView block1top = null;
	private TextView block2top = null;
	private TextView block3top = null;
	private TextView block4top = null;
	private TextView block1bottom = null;
	private TextView block2bottom = null;
	private TextView block3bottom = null;
	private TextView block4bottom = null;

	public void onPane(View view) {
		finish();
	}

	private RadioButton roughBtn;
	private RadioButton transportBtn;
	private RadioButton outdoorBtn;
	private RadioButton offBtn;
	private RadioGroup group;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_cockpit);

		group = (RadioGroup) findViewById(R.id.modeGroup);
		group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int id) {
				onModeChanged(id);
			}

		});

		roughBtn = (RadioButton) findViewById(R.id.roughLocatingButton);
		transportBtn = (RadioButton) findViewById(R.id.transportButton);
		outdoorBtn = (RadioButton) findViewById(R.id.outdoorButton);
		offBtn = (RadioButton) findViewById(R.id.offButton);

		offBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				int color = getResources().getColor(
						isChecked ? R.color.shine : R.color.transparent);
				buttonView.setBackgroundColor(color);
			}
		});

		roughBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				findViewById(R.id.roughLocatingText).setVisibility(
						isChecked ? View.VISIBLE : View.GONE);
				int color = getResources().getColor(
						isChecked ? R.color.shine : R.color.transparent);
				findViewById(R.id.roughLocatingText).setBackgroundColor(color);
				buttonView.setBackgroundColor(color);
			}
		});

		transportBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				findViewById(R.id.transportText).setVisibility(
						isChecked ? View.VISIBLE : View.GONE);

				int color = getResources().getColor(
						isChecked ? R.color.shine : R.color.transparent);
				findViewById(R.id.transportText).setBackgroundColor(color);
				buttonView.setBackgroundColor(color);
			}
		});

		outdoorBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				findViewById(R.id.outdoorText).setVisibility(
						isChecked ? View.VISIBLE : View.GONE);
				int color = getResources().getColor(
						isChecked ? R.color.shine : R.color.transparent);
				findViewById(R.id.outdoorText).setBackgroundColor(color);
				buttonView.setBackgroundColor(color);
			}
		});

		block1top = (TextView) findViewById(R.id.block1top);
		block2top = (TextView) findViewById(R.id.block2top);
		block3top = (TextView) findViewById(R.id.block3top);
		block4top = (TextView) findViewById(R.id.block4top);

		block1bottom = (TextView) findViewById(R.id.block1bottom);
		block2bottom = (TextView) findViewById(R.id.block2bottom);
		block3bottom = (TextView) findViewById(R.id.block3bottom);
		block4bottom = (TextView) findViewById(R.id.block4bottom);

		prefs = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (resultCode == -2) {
			realLogout();
			return;
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBack(null);
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			onBack(null);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	private void onModeChanged(int i) {
		final String newMode;

		switch (i) {
		case MODE_OFF:
			prefs.edit().putBoolean(Prefs.STATUS_ONOFF, false).commit();
			return;
		case MODE_TRANSPORT:
			newMode = Mode.transport.toString();
			break;
		case MODE_OUTDOOR:
			newMode = Mode.sport.toString();
			break;
		case MODE_FUZZY:
			newMode = Mode.fuzzy.toString();
			break;
		default:
			return;
		}
		prefs.edit().putString(Prefs.MODE, newMode)
				.putBoolean(Prefs.STATUS_ONOFF, true).commit();
	}

	private boolean isActive() {
		return prefs.getBoolean(Prefs.STATUS_ONOFF, true);
	}

	public void onBack(View view) {
		finish();
	}

	private boolean isModeTransport() {
		return group.getCheckedRadioButtonId() == MODE_TRANSPORT;
	}

	private boolean isModeFuzzy() {
		return group.getCheckedRadioButtonId() == MODE_FUZZY;
	}

	private boolean isModeOutdoor() {
		return group.getCheckedRadioButtonId() == MODE_OUTDOOR;
	}

	public void onBlock1(View view) {
		int r = R.string.EnableGPS;

		boolean gps = locationManager != null
				&& locationManager
						.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean net = locationManager != null
				&& locationManager
						.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		Intent intent = registerReceiver(null, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));
		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean power = plugged == BatteryManager.BATTERY_PLUGGED_AC
				|| plugged == BatteryManager.BATTERY_PLUGGED_USB;
		if (!isActive()) {
			r = R.string.OffModeNoGPS;
		} else if (isModeFuzzy() || (isModeTransport() && !power)) {
			r = net ? R.string.LocationAccuracyOnFuzzy
					: R.string.EnableNetworkLocating;
		} else if (!gps && (isModeTransport() || isModeOutdoor())) {
			r = R.string.EnableGPS;
		} else {
			ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			android.net.NetworkInfo wifi = connec
					.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (wifi.isConnected()) {
				r = R.string.GPSIsNotNeededWhileWifiConnected;
			} else {
				Location location = locationManager
						.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				if (location != null) {
					if (System.currentTimeMillis() - location.getTime() < 60000) {
						if (location.getAccuracy() < 10) {
							r = R.string.GPSIsVeryGood;
						} else if (location.getAccuracy() < 20) {
							r = R.string.GPSIsGood;
						} else if (location.getAccuracy() < 50) {
							r = R.string.GPSIsOk;
						} else {
							r = R.string.GPSIsWeak;
						}
					} else {
						r = R.string.GPSIsWeak;
					}
				}
			}
		}

		final boolean openLocationSettings = r == R.string.EnableGPS
				|| r == R.string.EnableNetworkLocating;
		QuickAction quick = new QuickAction(this);
		quick.setOnActionItemClickListener(new OnActionItemClickListener() {

			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				if (openLocationSettings) {
					Intent intent = new Intent(
							Settings.ACTION_LOCATION_SOURCE_SETTINGS);
					startActivity(intent);
				}
			}
		});

		quick.addActionItem(new ActionItem(this, r));
		quick.show(block1top);
	}

	public void onBlock2(View view) {
		ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo wifi = connec
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		android.net.NetworkInfo mobile = connec
				.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
		int r = R.string.InternetDoesntWork;
		if (wifi != null && wifi.isConnected()) {
			r = R.string.InternetWorksOverWifi;
		} else if (mobile != null && mobile.isConnected()) {
			r = R.string.InternetWorksOverMobileNetwork;
		}

		ActionItem stateItem = new ActionItem(this, r);
		ActionItem checkItem = new ActionItem(this,
				R.string.CheckServerConnection);
		final QuickAction quick = new QuickAction(this);
		quick.addActionItem(stateItem);
		quick.addActionItem(checkItem);
		quick.setOnActionItemClickListener(new OnActionItemClickListener() {

			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				if (pos > 0) {
					try {
						JSONObject data = prepareObj();
						data.put("man", Build.MANUFACTURER);
						data.put("mod", Build.MODEL);
						data.put("os", "Android " + Build.VERSION.RELEASE);
						data.put(
								"ver",
								getPackageManager().getPackageInfo(
										getPackageName(), 0).versionCode);
						data.put(
								"vername",
								getPackageManager().getPackageInfo(
										getPackageName(), 0).versionName);
						doAction(ACTION_LOGIN, data, new ResultWorker() {
							@Override
							public void onResult(String result, Context context) {
								runOnUiThread(new Runnable() {
									public void run() {
										ActionItem item = new ActionItem(
												CockpitScreen.this,
												R.string.ConnectionToServerWorks);
										QuickAction mQuickAction = new QuickAction(
												CockpitScreen.this);
										mQuickAction.addActionItem(item);
										mQuickAction.show(block2top);
									};
								});
							}

							public void onFailure(int failure, Context context) {
								runOnUiThread(new Runnable() {
									public void run() {
										ActionItem item = new ActionItem(
												CockpitScreen.this,
												R.string.ConnectionToServerDoesntWork);
										QuickAction mQuickAction = new QuickAction(
												CockpitScreen.this);
										mQuickAction.addActionItem(item);
										mQuickAction.show(block2top);
									};
								});
							};
						});
					} catch (Exception exc) {
						Log.w(exc);
						Toast.makeText(getApplicationContext(),
								R.string.ConnectionToServerDoesntWork,
								Toast.LENGTH_LONG).show();
						ActionItem item = new ActionItem(CockpitScreen.this,
								R.string.ConnectionToServerDoesntWork);
						QuickAction mQuickAction = new QuickAction(
								CockpitScreen.this);
						mQuickAction.addActionItem(item);
						mQuickAction.show(block2top);
					}
				}
			}
		});
		quick.show(block2top);
	}

	public void onBlock3(View view) {
		int r;
		Intent intent = registerReceiver(null, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));

		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean power = plugged == BatteryManager.BATTERY_PLUGGED_AC
				|| plugged == BatteryManager.BATTERY_PLUGGED_USB;
		int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

		if (power) {
			r = R.string.BatteryCharging;
		} else if (isModeTransport()) {
			r = R.string.BatteryNotChargingButTransport;
		} else if (level < 15) {
			r = R.string.BatteryLow;
		} else if (level < 60) {
			r = R.string.BatteryMid;
		} else {
			r = R.string.BatteryHigh;
		}

		ActionItem resetItem = new ActionItem(this, r);
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction.addActionItem(resetItem);
		mQuickAction.show(block3top);
	}

	public void onBlock4(View view) {
		int total = prefs.getInt(Prefs.LOCATIONS_TOTAL, 0);
		Resources r = getResources();
		String totalText = r.getString(R.string.TotalUploaded) + ": " + total
				+ " " + r.getString(R.string.locations);

		int upload = 0;
		DbAdapter dbAdapter = null;
		try {
			dbAdapter = new DbAdapter(getApplicationContext());
			dbAdapter.open();
			upload = dbAdapter.count();
		} catch (Exception exc) {
			Log.w(exc);
		} finally {
			try {
				dbAdapter.close();
			} catch (Exception exc) {
			}
		}
		String uploadText = r.getString(R.string.UploadNow) + ": " + upload
				+ " " + r.getString(R.string.locations);
		ActionItem totalItem = new ActionItem(this, totalText);
		ActionItem uploadItem = new ActionItem(this, uploadText);
		ActionItem resetItem = new ActionItem(this, R.string.ResetCounter);

		QuickAction quick = new QuickAction(this);
		quick.setOnActionItemClickListener(new OnActionItemClickListener() {

			@Override
			public void onItemClick(QuickAction source, int pos, int actionId) {
				if (pos == 1) {
					Intent intent = new Intent(CockpitScreen.this,
							TrackingSender.class);
					intent.setAction(TrackingSender.ACTION_SEND);
					PendingIntent sendIntent = PendingIntent.getBroadcast(
							CockpitScreen.this, 0, intent, 0);
					AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

					long triggerAtTime = SystemClock.elapsedRealtime();

					if (prefs.getBoolean(Prefs.STATUS_ONOFF, false)) {
						alarmManager.setInexactRepeating(
								AlarmManager.ELAPSED_REALTIME_WAKEUP,
								triggerAtTime, TrackingSender.SEND_INTERVAL,
								sendIntent);
					} else {
						alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
								triggerAtTime, sendIntent);
					}
				} else if (pos == 2) {
					prefs.edit().putInt(Prefs.LOCATIONS_TOTAL, 0).commit();
				}
			}
		});
		quick.addActionItem(totalItem);
		quick.addActionItem(uploadItem);
		quick.addActionItem(resetItem);
		quick.show(block4top);
	}

	private void showModeDescription(View v) {
		int txt = 0;
		if (isModeOutdoor()) {
			txt = R.string.OutdoorDescription;
		} else if (isModeTransport()) {
			txt = R.string.TransportDescription;
		} else {
			txt = R.string.trackingInactive;
		}

		ActionItem resetItem = new ActionItem(CockpitScreen.this, txt);
		QuickAction mQuickAction = new QuickAction(CockpitScreen.this);
		mQuickAction.addActionItem(resetItem);
		mQuickAction.show(v);
	}

	public void onTrackingDescription(View view) {
		showModeDescription(view);
	}
	

}
