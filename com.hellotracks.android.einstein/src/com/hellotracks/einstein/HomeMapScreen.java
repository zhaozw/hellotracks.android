package com.hellotracks.einstein;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingService;
import com.hellotracks.TrackingService.Mode;
import com.hellotracks.activities.AbstractMapScreen;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.RegisterPlaceScreen;
import com.hellotracks.activities.SignUpScreen;
import com.hellotracks.activities.TracksScreen;
import com.hellotracks.activities.WelcomeScreen;
import com.hellotracks.c2dm.C2DMReceiver;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.types.GPS;
import com.hellotracks.util.BadgeView;
import com.hellotracks.util.ContactAccessor;
import com.hellotracks.util.ContactInfo;
import com.hellotracks.util.ImageCache;
import com.hellotracks.util.ImageCache.ImageCallback;
import com.hellotracks.util.MapGestureDetectorOverlay;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class HomeMapScreen extends AbstractMapScreen {

	private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

		@Override
		public void onSharedPreferenceChanged(final SharedPreferences prefs,
				final String key) {
			if (Prefs.STATUS_ONOFF.equals(key) || Prefs.MODE.equals(key)) {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						updateButtons(prefs, Prefs.STATUS_ONOFF.equals(key));
						updateMyLocationOverlay();
					}
				});
			}
		}
	};

	class UpdateTimeTask extends TimerTask {

		int count = 0;

		public void run() {
			updateModeBackground();

			switch (count % 3) {
			case 0:
				doLogin();
				break;
			case 1:
				refillMap();
				break;
			case 2:
				refillTracks();
				break;
			}
			count++;
		}
	}

	private Timer timer;

	private BroadcastReceiver mWifiStateChangedReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateMyLocationOverlay();
				}
			});
		}
	};

	private BroadcastReceiver mPowerReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					updateMyLocationOverlay();
				}
			});
		}
	};

	protected void onStart() {
		super.onStart();
		FlurryAgent.onStartSession(this, "3TJ7YYSYK4C4HB983H27");
	};

	protected void onResume() {
		timer = new Timer();
		timer.schedule(new UpdateTimeTask(), 4000, 5000);
		new Thread() {
			public void run() {
				try {
					doLogin();

					Thread.sleep(500);

					final String cache = Prefs.get(HomeMapScreen.this)
							.getString(createMarkerCacheId(), null);
					if (cache != null && !cache.equals(lastMarkers)) {
						runOnUiThread(new Runnable() {

							@Override
							public void run() {
								updateMap(cache);
							}

						});
					}
					refillMap();

					Thread.sleep(500);
					refillTracks();
				} catch (Exception exc) {
				}
			};
		}.start();
		updateMyLocationOverlay();
		updateButtons(Prefs.get(this), false);
		super.onResume();
	}

	private void updateMyLocationOverlay() {
		if (true) {
			mMap.setMyLocationEnabled(true);
		} else {
			boolean enable = isEnableMyLocation();
			if (enable && !mMap.isMyLocationEnabled()) {
				mMap.setMyLocationEnabled(true);
				showMyLocation();
			} else if (!enable && mMap.isMyLocationEnabled()) {
				mMap.setMyLocationEnabled(false);
			}
		}
	};

	private boolean isEnableMyLocation() {
		boolean active = Prefs.get(this).getBoolean(Prefs.STATUS_ONOFF, false);
		if (!active)
			return false;

		ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		android.net.NetworkInfo wifi = connec
				.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		if (wifi.isConnected()) {
			return false;
		}

		String mode = Prefs.get(this).getString(Prefs.MODE, null);
		if (Mode.isOutdoor(mode))
			return true;
		if (Mode.isFuzzy(mode))
			return false;

		Intent intent = registerReceiver(null, new IntentFilter(
				Intent.ACTION_BATTERY_CHANGED));

		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
		boolean power = plugged == BatteryManager.BATTERY_PLUGGED_AC
				|| plugged == BatteryManager.BATTERY_PLUGGED_USB;
		return power;
	}

	@Override
	protected void onPause() {
		if (timer != null)
			timer.cancel();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mWifiStateChangedReceiver);
		unregisterReceiver(mPowerReceiver);
		Prefs.get(this).unregisterOnSharedPreferenceChangeListener(
				prefChangeListener);
		super.onDestroy();
	}

	public void activate() {
		Prefs.get(this).edit().putBoolean(Prefs.STATUS_ONOFF, true).commit();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		C2DMReceiver.refreshAppC2DMRegistrationState(getApplicationContext());

		if (Prefs.get(this).getBoolean(Prefs.ACTIVATE_ON_LOGIN, true)) {
			activate();
		}
		maybeStartService();

		String mode = Prefs.get(this).getString(Prefs.MODE, null);
		if (mode == null || mode.length() == 0) {
			mode = Mode.sport.toString();
			final String m = mode;
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					stopService();
					Prefs.get(HomeMapScreen.this).edit()
							.putString(Prefs.MODE, m).commit();
					maybeStartService();
				}
			});
		}

		Prefs.get(this).registerOnSharedPreferenceChangeListener(
				prefChangeListener);
		registerReceiver(mWifiStateChangedReceiver, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));
		registerReceiver(mPowerReceiver, new IntentFilter(
				Intent.ACTION_POWER_CONNECTED));
		registerReceiver(mPowerReceiver, new IntentFilter(
				Intent.ACTION_POWER_DISCONNECTED));

		setContentView(R.layout.screen_homemap);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		TextView name = (TextView) findViewById(R.id.name);
		name.setTypeface(tf);

		setUpMapIfNeeded();

		refillMap();
		refillContactList();

		badgeMessages = new BadgeView(this, findViewById(R.id.buttonMessages));

		showMyLocation();
	}

	private String lastMarkers = null;
	private BadgeView badgeMessages;
	private BadgeView badgeContacts;

	private String createMarkerCacheId() {
		String cacheId = "cache_markers_"
				+ Prefs.get(this).getString(Prefs.USERNAME, "");
		return cacheId;
	}

	private void refillMap() {
		try {
			JSONObject obj = AbstractScreen.prepareObj(this);
			obj.put(C.account, null);
			AbstractScreen.doAction(this, AbstractScreen.ACTION_MARKERS, obj,
					null, new ResultWorker() {

						@Override
						public void onResult(final String result,
								Context context) {
							if (!result.equals(lastMarkers)) {
								lastMarkers = result;
								Prefs.get(HomeMapScreen.this)
										.edit()
										.putString(createMarkerCacheId(),
												lastMarkers).commit();
								runOnUiThread(new Runnable() {

									@Override
									public void run() {
										updateMap(result);
									}

								});
							}
						}
					});
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	private String lastTracks = null;

	private void refillTracks() {
		try {
			JSONObject obj = AbstractScreen.prepareObj(this);
			obj.put(C.account, Prefs.get(this).getString(Prefs.USERNAME, ""));
			obj.put("count", 1);
			obj.put("fromts", System.currentTimeMillis() * 2);
			AbstractScreen.doAction(this, AbstractScreen.ACTION_TRACKS, obj,
					null, new ResultWorker() {

						@Override
						public void onResult(final String result,
								Context context) {
							if (!result.equals(lastTracks)) {
								lastTracks = result;

								if (result != null && result.length() > 0) {
									try {
										JSONArray array = new JSONArray(result);

										for (int i = 0; i < array.length();) {
											JSONObject obj = array
													.getJSONObject(i);
											String url = obj.getString("url");

											ImageCache.getInstance().loadAsync(
													url, new ImageCallback() {

														@Override
														public void onImageLoaded(
																final Bitmap bmp,
																String url) {
															runOnUiThread(new Runnable() {

																@Override
																public void run() {
																	ImageButton imageButton = (ImageButton) findViewById(R.id.buttonTracks);
																	imageButton
																			.setImageBitmap(bmp);
																}

															});
														}
													}, context);
											break;
										}
									} catch (Exception exc) {
										Log.w(exc);
									}
								}
							}
						}
					});
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	@SuppressWarnings("serial")
	private class NameSortedSet extends TreeSet<Integer> {

		public NameSortedSet() {
			super(new Comparator<Integer>() {

				@Override
				public int compare(Integer i1, Integer i2) {
					try {
						return names[i1].toLowerCase().compareTo(
								names[i2].toLowerCase());
					} catch (Exception exc) {
						return 0;
					}
				}
			});
		}

	}

	private void updateMap(final String markers) {
		if (markers != null && markers.length() > 0) {
			try {
				JSONArray array = new JSONArray(markers);
				final String[] names = new String[array.length()];
				final String[] urls = new String[array.length()];
				final LatLng[] points = new LatLng[array.length()];
				final String[] accounts = new String[array.length()];
				final long[] timestamps = new long[array.length()];
				final int[] radius = new int[array.length()];
				final String[] infos = new String[array.length()];
				final int[] accuracies = new int[array.length()];

				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					urls[i] = obj.getString("url");
					names[i] = obj.getString("name");
					accounts[i] = obj.getString("account");
					timestamps[i] = obj.getLong("ts");
					radius[i] = obj.has("radius") ? obj.getInt("radius") : -1;
					infos[i] = obj.getString("info");
					accuracies[i] = obj.getInt("acc");
					double lat = obj.getDouble("lat");
					double lng = obj.getDouble("lng");
					points[i] = new LatLng(lat, lng);
				}

				new OverlayTask(this) {

					@Override
					public void onPostExecute(Void unused) {
						HomeMapScreen.this.names = names;
						HomeMapScreen.this.urls = urls;
						HomeMapScreen.this.accounts = accounts;
						HomeMapScreen.this.timestamps = timestamps;
						HomeMapScreen.this.points = points;
						HomeMapScreen.this.radius = radius;
						HomeMapScreen.this.infos = infos;
						HomeMapScreen.this.accuracies = accuracies;

						buildMarkers();

						refillContactList();
					}
				}.execute(urls);
			} catch (Exception exc) {
				Log.w(exc);
			}
		}
	}

	private void refillContactList() {
		LinearLayout container = (LinearLayout) findViewById(R.id.contactsContainer);
		container.removeAllViews();
		fillMyLocAction(container);
		fillLayersAction(container);
		fillContactListAction(container);
		fillAddContactAction(container);
		if (accounts != null && accounts.length > 0)
			fillContactActions(container);
	}

	protected final ContactAccessor mContactAccessor = ContactAccessor
			.getInstance();

	private void loadContactInfo(Uri contactUri) {

		AsyncTask<Uri, Void, ContactInfo> task = new AsyncTask<Uri, Void, ContactInfo>() {

			@Override
			protected ContactInfo doInBackground(Uri... uris) {
				return mContactAccessor.loadContact(getContentResolver(),
						uris[0]);
			}

			@Override
			protected void onPostExecute(ContactInfo result) {
				if (result.getEmail() != null && result.getEmail().length() > 0) {
					AbstractScreen.sendInvitation(HomeMapScreen.this,
							result.getEmail());
				}
				AbstractScreen.prepareSMS(HomeMapScreen.this,
						result.getPhoneNumber());
				AbstractScreen.sendPendingInvitation(HomeMapScreen.this,
						result.getDisplayName(), null, result.getPhoneNumber());
			}
		};

		task.execute(contactUri);
	}

	private void fillAddContactAction(LinearLayout container) {
		View addView = getLayoutInflater()
				.inflate(R.layout.quick_contact, null);

		final ImageButton image = (ImageButton) addView
				.findViewById(R.id.quickImage);
		image.setImageResource(R.drawable.btn_add);
		image.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				FlurryAgent.logEvent("AddContact");
				ActionItem findItem = new ActionItem(HomeMapScreen.this,
						R.string.NearbyMe);
				ActionItem inviteItem = new ActionItem(HomeMapScreen.this,
						R.string.InviteContact);
				ActionItem searchItem = new ActionItem(HomeMapScreen.this,
						R.string.SearchForPeopleOrPlaces);
				QuickAction quick = new QuickAction(HomeMapScreen.this);
				quick.addActionItem(searchItem);
				quick.addActionItem(findItem);
				quick.addActionItem(inviteItem);
				quick.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						switch (pos) {
						case 0:
							openSearchDialog();
							break;
						case 1:
							openFindDialog();
							break;
						case 2:
							FlurryAgent.logEvent("PickContact");
							startActivityForResult(
									mContactAccessor.getPickContactIntent(),
									C.REQUESTCODE_PICK_CONTACT);
							break;
						}
					}
				});
				quick.show(image);
			}
		});

		TextView text = (TextView) addView.findViewById(R.id.quickText);
		text.setText(R.string.Add);

		container.addView(addView);
	}

	private void openFindDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.NearbyMe);
		Resources r = getResources();
		String[] names = new String[] { r.getString(R.string.People),
				r.getString(R.string.Places) };
		builder.setItems(names, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				Intent intent = new Intent(HomeMapScreen.this,
						NetworkScreen.class);
				if (item == 0) {
					intent.putExtra(C.type, "person");
					FlurryAgent.logEvent("NearbyPerson");
				} else {
					intent.putExtra(C.type, "place");
					FlurryAgent.logEvent("NearbyPlace");
				}
				intent.putExtra(C.action, AbstractScreen.ACTION_FIND);
				startActivity(intent);
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	protected void openSearchDialog() {
		FlurryAgent.logEvent("Search");
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setMessage(R.string.EnterSearch);
		final EditText input = new EditText(this);
		input.setHint(R.string.NameOrPlace);
		alert.setView(input);
		alert.setPositiveButton(getResources().getString(R.string.Search),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString().trim();
						if (value.length() >= 2) {
							Intent intent = new Intent(HomeMapScreen.this,
									NetworkScreen.class);
							intent.putExtra(C.type, C.search);
							intent.putExtra(C.search, value);
							intent.putExtra(C.action,
									AbstractScreen.ACTION_SEARCH);
							startActivity(intent);
						}
					}
				});
		alert.setNegativeButton(getResources().getString(R.string.Cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		alert.show();
	}

	private void fillLayersAction(LinearLayout container) {
		View addView = getLayoutInflater()
				.inflate(R.layout.quick_contact, null);

		ImageButton image = (ImageButton) addView.findViewById(R.id.quickImage);
		image.setImageResource(R.drawable.btn_layers);
		image.setOnClickListener(new View.OnClickListener() {
			int count = 0;

			@Override
			public void onClick(View v) {
				FlurryAgent.logEvent("Layers");
				switch (++count % 3) {
				case 0:
					mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
					break;
				case 1:
					mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
					break;
				case 2:
					mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
					break;
				}
			}
		});

		TextView text = (TextView) addView.findViewById(R.id.quickText);
		text.setText("");

		container.addView(addView);
	}

	private void fillMyLocAction(LinearLayout container) {
		View addView = getLayoutInflater()
				.inflate(R.layout.quick_contact, null);

		ImageButton image = (ImageButton) addView.findViewById(R.id.quickImage);
		image.setImageResource(R.drawable.btn_myloc);
		image.setOnClickListener(new View.OnClickListener() {

			int count = 0;

			@Override
			public void onClick(View v) {
				FlurryAgent.logEvent("MyLocation");
				if (count++ % 2 == 0)
					showMyLocation();
				else
					doShowAll();
			}
		});

		TextView text = (TextView) addView.findViewById(R.id.quickText);
		text.setText("");

		container.addView(addView);
	}

	private void fillContactActions(LinearLayout container) {
		Set<Integer> live = new NameSortedSet();
		Set<Integer> today = new NameSortedSet();
		Set<Integer> contacts = new NameSortedSet();
		Set<Integer> places = new NameSortedSet();
		for (int i = 0; i < accounts.length; i++) {
			long ts = timestamps[i];
			if (radius[i] > 0) {
				places.add(i);
			} else if (ts > System.currentTimeMillis() - 1000 * 60 * 60 * 12) {
				today.add(i);
				if (ts > System.currentTimeMillis() - 60000 * 18)
					live.add(i);
			} else {
				contacts.add(i);
			}
		}

		LinkedList<Integer> list = new LinkedList<Integer>();
		list.addAll(today);
		list.addAll(contacts);
		list.addAll(places);

		for (int i : list) {
			final int item = i;
			View contactView = getLayoutInflater().inflate(
					R.layout.quick_contact, null);
			final ImageButton image = (ImageButton) contactView
					.findViewById(R.id.quickImage);
			if (live.contains(item)) {
				image.setBackgroundResource(R.drawable.custom_button_trans_blue);
			} else if (places.contains(item)) {
				image.setBackgroundResource(R.drawable.custom_button_trans_red);
			} else {
				image.setBackgroundResource(R.drawable.custom_button_trans);
			}
			image.setOnLongClickListener(new View.OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {
					try {
						FlurryAgent.logEvent("ProfileLongClick");
						Marker m = getMarker(item);
						CameraPosition cameraPosition = new CameraPosition.Builder()
								.target(m.getPosition()).zoom(14).tilt(30)
								.build();
						mMap.moveCamera(CameraUpdateFactory
								.newCameraPosition(cameraPosition));
						Intent intent = new Intent(HomeMapScreen.this,
								ProfileScreen.class);
						intent.putExtra(C.account, accounts[item]);
						intent.putExtra(C.name, names[item]);
						startActivityForResult(intent, C.REQUESTCODE_CONTACT);
					} catch (Exception exc) {
					}
					return true;
				}
			});
			image.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					try {
						Marker m = getMarker(item);
						m.showInfoWindow();
						CameraPosition cameraPosition = new CameraPosition.Builder()
								.target(m.getPosition()).zoom(14).tilt(30)
								.build();
						mMap.animateCamera(CameraUpdateFactory
								.newCameraPosition(cameraPosition));
					} catch (Exception exc) {
					}
				}
			});
			String url = urls[i];
			if (url.endsWith("marker.png")) {
				url = url.substring(0, url.length() - "marker.png".length())
						+ "mini.jpg";
			}
			ImageCache.getInstance().loadAsync(url, new ImageCallback() {

				@Override
				public void onImageLoaded(final Bitmap bmp, String url) {
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							image.setImageBitmap(bmp);
						}
					});

				}
			}, getApplicationContext());

			TextView text = (TextView) contactView.findViewById(R.id.quickText);

			String name = names[i];
			if (name.length() > 12) {
				name = name.substring(0, 10) + "...";
			}
			text.setText(name);

			container.addView(contactView);
		}
	}

	public void onOnOff(View view) {
		FlurryAgent.logEvent("OnOff");
		boolean stat = Prefs.get(this).getBoolean(Prefs.STATUS_ONOFF, false);
		Prefs.get(this).edit().putBoolean(Prefs.STATUS_ONOFF, !stat).commit();
	}

	private void fillContactListAction(LinearLayout container) {
		View contactsView = getLayoutInflater().inflate(R.layout.quick_contact,
				null);

		ImageButton image = (ImageButton) contactsView
				.findViewById(R.id.quickImage);
		image.setImageResource(R.drawable.btn_list);
		image.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(HomeMapScreen.this,
						NetworkScreen.class);
				startActivity(intent);
			}
		});

		TextView text = (TextView) contactsView.findViewById(R.id.quickText);
		text.setText(R.string.Contacts);

		container.addView(contactsView);

		badgeContacts = new BadgeView(this, contactsView);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == C.REQUESTCODE_CONTACT) {
			new Thread() {
				public void run() {
					try {
						Thread.sleep(1500);
					} catch (InterruptedException e) {
					}
					refillMap();
				};
			}.start();
		} else if (requestCode == C.REQUESTCODE_PICK_CONTACT
				&& resultCode == RESULT_OK) {
			loadContactInfo(data.getData());
			return;
		}

		if (resultCode < 0) {
			realLogout();
			return;
		}
	}

	protected void realLogout() {
		Prefs.get(this).edit().putString(C.account, null)
				.putBoolean(Prefs.STATUS_ONOFF, false)
				.putString(Prefs.PASSWORD, "").commit();
		stopService(new Intent(this, TrackingService.class));
		setResult(-1);
		startActivity(new Intent(this, WelcomeScreen.class));
		finish();
	}

	public void onMenu(View view) {
		FlurryAgent.logEvent("Menu");
		startActivityForResult(new Intent(HomeMapScreen.this,
				ProfileMenuScreen.class), C.REQUESTCODE_CONTACT);
	}

	private void showMyLocation() {
		if (mMap.isMyLocationEnabled() && mMap.getMyLocation() != null) {
			try {
				LatLng pos = new LatLng(mMap.getMyLocation().getLatitude(),
						mMap.getMyLocation().getLongitude());
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.zoom(14).target(pos).tilt(30).build();
				mMap.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
			} catch (Exception exc) {
				Log.w(exc);
			}
		} else {
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location loc = locationManager
					.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			if (loc != null) {
				LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.zoom(14).target(pos).tilt(30).build();
				mMap.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (quickView != null) {
				// FIXME mapView.removeView(quickView);
				// if (sitesOverlay != null) {
				// mapView.getOverlays().remove(sitesOverlay);
				// mapView.invalidate();
				// }
				quickView = null;
			} else {
				finish();
			}
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_SEARCH) {
			startActivity(new Intent(HomeMapScreen.this, NetworkScreen.class));
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			startActivityForResult(new Intent(HomeMapScreen.this,
					ProfileMenuScreen.class), C.REQUESTCODE_CONTACT);
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	public void onCockpit(View view) {
		FlurryAgent.logEvent("Cockpit");
		Intent intent = new Intent(HomeMapScreen.this, Cockpit3Screen.class);
		startActivity(intent);
	}

	public void onTracks(View view) {
		FlurryAgent.logEvent("Tracks");
		Intent intent = new Intent(HomeMapScreen.this, TracksScreen.class);
		startActivity(intent);
	}

	public void onPanic(View view) {
		try {
			FlurryAgent.logEvent("Panic");
			Intent intent = new Intent(HomeMapScreen.this, PanicScreen.class);
			String msg = "";
			LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
			Location loc = locationManager
					.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			if (loc != null) {
				msg = "@uri geo:0,0?q=";
				String location = loc.getLatitude() + "," + loc.getLongitude()
						+ "(" + getResources().getString(R.string.IAmHere)
						+ ")";
				msg += location + " text:";
			}
			msg += getResources().getString(R.string.INeedHelp);
			intent.putExtra("message", msg);

			HashSet<String> receivers = new HashSet<String>();
			for (int i = 1; i < accounts.length; i++) {
				if (radius[i] <= 0) {
					receivers.add(accounts[i]);
				}
			}
			intent.putExtra("receivers", receivers.toArray(new String[0]));
			startActivity(intent);
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	private void updateButtons(final SharedPreferences prefs, boolean change) {
		ImageButton buttonMode = (ImageButton) findViewById(R.id.buttonMode);

		boolean active = prefs.getBoolean(Prefs.STATUS_ONOFF, false);
		String mode = prefs.getString(Prefs.MODE, Mode.sport.name());

		if (Mode.isFuzzy(mode)) {
			buttonMode.setImageResource(R.drawable.mode_fuzzy);
		} else if (Mode.isTransport(mode)) {
			buttonMode.setImageResource(R.drawable.mode_transport);
		} else if (Mode.isOutdoor(mode)) {
			buttonMode.setImageResource(R.drawable.mode_sport);
		}

		if (!active) {
			ImageButton button = (ImageButton) findViewById(R.id.buttonStatus);
			button.setImageResource(R.drawable.btn_off);
			button.setBackgroundResource(R.drawable.custom_button_trans_attention);
		} else {
			ImageButton button = (ImageButton) findViewById(R.id.buttonStatus);
			button.setImageResource(R.drawable.btn_on);
			button.setBackgroundResource(R.drawable.custom_button_trans_green);
		}

		if (change && !Mode.isFuzzy(mode)) {
			if (active) {
				int r = Prefs.isDistanceUS(this) ? R.string.TrackStartedFeet
						: R.string.TrackStartedMeter;
				Toast.makeText(this, r, Toast.LENGTH_LONG).show();
			} else {
				insertTrackEndGPS();
				Toast.makeText(this, R.string.TrackEnded, Toast.LENGTH_SHORT)
						.show();
			}
		}

		if (active) {
			maybeStartService();
		} else {
			stopService();
		}
	}

	private void insertTrackEndGPS() {
		DbAdapter dbAdapter = null;
		try {
			dbAdapter = new DbAdapter(this);
			dbAdapter.open();
			GPS gps = new GPS();
			gps.ts = System.currentTimeMillis();
			gps.alt = 0;
			gps.lat = 1;
			gps.lng = 1;
			gps.vacc = 0;
			gps.head = 0;
			gps.speed = 0;
			gps.sensor = GPS.SENSOR_TRACKEND;
			dbAdapter.insertGPS(gps);
		} catch (Exception exc) {
			Log.w(exc);
		} finally {
			try {
				dbAdapter.close();
			} catch (Exception exc) {
			}
		}
	}

	private void stopService() {
		stopService(new Intent(this, TrackingService.class));
	}

	private void maybeStartService() {
		if (!isMyServiceRunning()) {
			Log.i("service not running -> start it");
			Intent serviceIntent = new Intent(this, TrackingService.class);
			startService(serviceIntent);
		}
	}

	@Override
	protected void onStop() {
		FlurryAgent.onEndSession(this);
		boolean tracking = Prefs.get(this)
				.getBoolean(Prefs.STATUS_ONOFF, false);
		if (!tracking) {
			stopService();
		}
		super.onStop();
	}

	private boolean isMyServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if (TrackingService.class.getCanonicalName().equals(
					service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	private void doLogin() {
		try {
			JSONObject data = AbstractScreen.prepareObj(this);
			data.put("man", Build.MANUFACTURER);
			data.put("mod", Build.MODEL);
			data.put("os", "Android " + Build.VERSION.RELEASE);
			data.put(
					"ver",
					this.getPackageManager().getPackageInfo(
							this.getPackageName(), 0).versionCode);
			data.put(
					"vername",
					this.getPackageManager().getPackageInfo(
							this.getPackageName(), 0).versionName);

			AbstractScreen.doAction(this, AbstractScreen.ACTION_LOGIN, data,
					null, new ResultWorker() {

						@Override
						public void onResult(final String result,
								Context context) {
							try {
								doLoginResult(new JSONObject(result));
							} catch (Exception exc) {
								Log.w(exc);
							}
						}

						@Override
						public void onFailure(final int status,
								final Context context) {
							runOnUiThread(new Runnable() {

								@Override
								public void run() {
									doLoginFailure(status, context);
								}

							});

						}
					});
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	private void doLoginResult(final JSONObject node) throws JSONException {
		final int unreadMsgCount = node.has("unreadmsg") ? node
				.getInt("unreadmsg") : 0;
		final int contactReqCount = node.has("requests") ? node
				.getInt("requests") : 0;
		final int suggestionsCount = node.has("suggestions") ? node
				.getInt("suggestions") : 0;

		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (unreadMsgCount > 0) {
					badgeMessages.setText(String.valueOf(unreadMsgCount));
					badgeMessages.show();
				} else {
					badgeMessages.setText("");
					badgeMessages.hide();
				}
				if (badgeContacts != null
						&& contactReqCount + suggestionsCount > 0) {
					badgeContacts.setText(String.valueOf(contactReqCount
							+ suggestionsCount));
					badgeContacts.show();
				} else {
					badgeContacts.setText("");
					badgeContacts.hide();
				}
			}

		});

		final SharedPreferences settings = Prefs.get(this);
		final int mod = Prefs.get(this).getInt("bookmod", 0);
		if (mod < 20) {
			new Thread() {
				public void run() {
					try {
						JSONArray array = loadBook(mod % 10);
						if (array.length() > 0) {
							JSONObject data = AbstractScreen
									.prepareObj(HomeMapScreen.this);
							data.put("book", array);
							AbstractScreen.doAction(HomeMapScreen.this,
									AbstractScreen.ACTION_SETVALUE, data, null,
									new ResultWorker() {
										@Override
										public void onFailure(int failure,
												Context context) {
										}

										public void onResult(String result,
												Context context) {

										};
									});
							settings.edit().putInt("bookmod", mod + 1).commit();
						}

					} catch (Exception exc) {
						Log.w(exc);
					}
				};
			}.start();
		}
	}

	private void doLoginFailure(int status, Context context) {
		final SharedPreferences settings = Prefs.get(this);
		int txt = R.string.unkownError;
		int bg = R.color.red;

		if (status == ResultWorker.STATUS_NORESULT) {
			txt = R.string.PleaseCheckInternetConnection;
			bg = R.color.orange;
		} else if (status == ResultWorker.ERROR_FORMAT)
			return;
		else if (status == ResultWorker.ERROR_USERUNKNOWN)
			txt = R.string.unkownUser;
		else if (status == ResultWorker.ERROR_PASSWORDMISMATCH)
			txt = R.string.passwordMismatch;
		else if (status == ResultWorker.ERROR_USERALREADYEXISTS)
			txt = R.string.userAlreadyExists;

		if (txt == R.string.unkownError)
			return;

		String text = context.getResources().getString(txt);
		if (bg != R.color.green) {

			if (bg == R.color.red) {
				final String username = settings.getString(Prefs.USERNAME, "");
				if (status == -2) {
					alertUserDoesNotExist(text, username);
				} else {
					final AlertDialog.Builder alert = new AlertDialog.Builder(
							this);
					alert.setMessage(text);
					alert.setPositiveButton(
							getResources().getString(R.string.logout),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									settings.edit()
											.putBoolean(Prefs.STATUS_ONOFF,
													false)
											.putString(Prefs.PASSWORD, "")
											.commit();
									setResult(-1);
									startActivity(new Intent(
											HomeMapScreen.this,
											WelcomeScreen.class));
									finish();
								}
							});
					alert.setCancelable(false);
					try {
						alert.show();
					} catch (Exception exc) {
						Log.i("exc catch");
					}

				}
			}
		}
	}

	private JSONArray loadBook(int mod) {
		long start = System.currentTimeMillis();
		int count = 0;
		JSONArray array = new JSONArray();
		Cursor people = null;
		try {
			people = getContentResolver().query(
					ContactsContract.Contacts.CONTENT_URI, null, null, null,
					null);

			while (people.moveToNext()) {
				long contactId = people.getLong(people
						.getColumnIndex(ContactsContract.Contacts._ID));
				if (contactId % 10 == mod) {
					Uri uri = ContentUris.withAppendedId(People.CONTENT_URI,
							contactId);
					ContactInfo info = mContactAccessor.loadContact(
							getContentResolver(), uri);
					if (info != null && info.getPhoneNumber() != null) {
						try {
							long encrypted = encrypt(normalize(info
									.getPhoneNumber()));
							if (encrypted > 0) {
								array.put(count++, encrypted);
							}
						} catch (Exception exc) {
						}
					}
				}
			}
		} catch (Exception exc) {
			Log.w(exc);
		} finally {
			try {
				people.close();
			} catch (Exception exc) {
			}
		}
		return array;
	}

	public static long encrypt(long normal) {
		String orig = String.valueOf(normal);
		StringBuilder real = new StringBuilder();
		for (int i = 0; i < orig.length(); i++) {
			int n = Integer.parseInt(orig.substring(i, i + 1));
			real.append(n == 9 ? 0 : n + 1);
		}
		return Long.parseLong(real.toString());
	}

	public static long normalize(String phone) throws Exception {
		if (phone.length() < 10)
			throw new Exception();
		phone = phone.trim().replaceAll("\\+", "00");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < phone.length(); i++) {
			if (Character.isDigit(phone.charAt(i))) {
				sb.append(phone.charAt(i));
			}
		}
		try {
			return Long.parseLong(sb.toString().substring(sb.length() - 10));
		} catch (Exception exc) {
			return -1;
		}
	}

	private void alertUserDoesNotExist(String text, final String username) {
		final SharedPreferences settings = Prefs.get(this);
		final AlertDialog.Builder alert = new AlertDialog.Builder(this);
		String msg = getResources().getString(R.string.CreateNewAccount);
		String title = text + ": " + username;
		alert.setTitle(title);
		alert.setMessage(msg);
		alert.setPositiveButton(getResources().getString(R.string.NewUser),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						settings.edit().putBoolean(Prefs.STATUS_ONOFF, false)
								.putString(Prefs.PASSWORD, "").commit();
						Intent intent = new Intent(HomeMapScreen.this,
								SignUpScreen.class);
						startActivity(intent);
						finish();
					}
				});
		alert.setCancelable(false);
		alert.setNegativeButton(getResources().getString(R.string.logout),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						settings.edit().putBoolean(Prefs.STATUS_ONOFF, false)
								.putString(Prefs.PASSWORD, "").commit();
						setResult(-1);
						startActivity(new Intent(HomeMapScreen.this,
								WelcomeScreen.class));
						finish();
					}
				});
		alert.show();
	}

	public void updateModeBackground() {
		try {
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					ImageButton modeButton = (ImageButton) findViewById(R.id.buttonMode);

					boolean active = Prefs.get(HomeMapScreen.this).getBoolean(
							Prefs.STATUS_ONOFF, false);
					if (!active) {
						modeButton
								.setBackgroundResource(R.drawable.custom_button_trans_nopadding);
						return;
					}

					String mode = Prefs.get(HomeMapScreen.this).getString(
							Prefs.MODE, null);

					Intent intent = registerReceiver(null, new IntentFilter(
							Intent.ACTION_BATTERY_CHANGED));
					int plugged = intent.getIntExtra(
							BatteryManager.EXTRA_PLUGGED, -1);
					boolean power = plugged == BatteryManager.BATTERY_PLUGGED_AC
							|| plugged == BatteryManager.BATTERY_PLUGGED_USB;
					int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL,
							-1);
					if (level < 10) {
						modeButton
								.setBackgroundResource(R.drawable.custom_button_trans_attention);
						return;
					} else if (Mode.isTransport(mode) && !power) {
						modeButton
								.setBackgroundResource(R.drawable.custom_button_trans_orange);
						return;
					}

					ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
					android.net.NetworkInfo wifi = connec
							.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
					if (wifi.isConnected()) {
						modeButton
								.setBackgroundResource(R.drawable.custom_button_trans_orange);
						return;
					}

					LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

					Location gpsLoc = locationManager
							.getLastKnownLocation(LocationManager.GPS_PROVIDER);

					if ((Mode.isOutdoor(mode) || (Mode.isTransport(mode) && power))) {
						if (gpsLoc == null
								|| System.currentTimeMillis()
										- gpsLoc.getTime() > 60000) {
							modeButton
									.setBackgroundResource(R.drawable.custom_button_trans_attention);
							return;
						}
					}

					if (Mode.isFuzzy(mode)
							|| (Mode.isTransport(mode) && !power)) {

						boolean net = locationManager != null
								&& locationManager
										.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
						if (!net) {
							modeButton
									.setBackgroundResource(R.drawable.custom_button_trans_attention);
							return;
						} else {
							Location netLoc = locationManager
									.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
							if (netLoc == null) {
								modeButton
										.setBackgroundResource(R.drawable.custom_button_trans_attention);
								return;
							}
						}
					}

					modeButton
							.setBackgroundResource(R.drawable.custom_button_trans_nopadding);
				}
			});
		} catch (Exception exc) {
		}
	}

	public void onMessages(View view) {
		FlurryAgent.logEvent("Messages");
		startActivity(new Intent(this, ConversationsScreen.class));
	}

}