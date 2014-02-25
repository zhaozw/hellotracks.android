package com.hellotracks.map;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import org.jraf.android.backport.switchwidget.Switch;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.util.DisplayMetrics;
import android.util.Property;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.ext.SatelliteMenu;
import android.view.ext.SatelliteMenu.SateliteClickedListener;
import android.view.ext.SatelliteMenuItem;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.actionbarsherlock.view.SubMenu;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.facebook.Session;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.ui.IconGenerator;
import com.hellotracks.Logger;
import com.hellotracks.Mode;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingSender;
import com.hellotracks.account.AccountManagementActivity;
import com.hellotracks.account.LoginScreen;
import com.hellotracks.account.ManagementScreen;
import com.hellotracks.api.API;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.ActivitiesScreen;
import com.hellotracks.base.C;
import com.hellotracks.base.FeedbackScreen;
import com.hellotracks.base.WebScreen;
import com.hellotracks.billing.util.IabResult;
import com.hellotracks.billing.util.Inventory;
import com.hellotracks.c2dm.C2DMReceiver;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.events.TemporyMarkerEvent;
import com.hellotracks.messaging.MessagesScreen;
import com.hellotracks.network.AddContactScreen;
import com.hellotracks.network.AddPlaceScreen;
import com.hellotracks.network.ContactListScreen;
import com.hellotracks.network.PlaceListScreen;
import com.hellotracks.places.CheckinScreen;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.tools.InfoScreen;
import com.hellotracks.tools.PanicInfoScreen;
import com.hellotracks.tools.PanicScreen;
import com.hellotracks.tools.RemoteActivationInfoScreen;
import com.hellotracks.tracks.TrackListScreen;
import com.hellotracks.types.GPS;
import com.hellotracks.util.Async;
import com.hellotracks.util.BadgeView;
import com.hellotracks.util.CompatibilityUtils;
import com.hellotracks.util.Connectivity;
import com.hellotracks.util.ContactAccessor;
import com.hellotracks.util.ContactInfo;
import com.hellotracks.util.PlanUtils;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.SearchMap.DirectionsResult;
import com.hellotracks.util.SearchMap.LocationResult;
import com.hellotracks.util.Time;
import com.hellotracks.util.Ui;
import com.hellotracks.util.Utils;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import de.greenrobot.event.EventBus;

public class HomeMapScreen extends AbstractMapScreen {

    private Timer timer;
    private boolean isActivityRunning = false;
    private Animation blinkanimation;
    private Animation fadeInAnimation;
    private Animation fadeOutAnimation;

    private String lastMarkers = null;
    private BadgeView badgeMessages;
    private BadgeView badgeContacts;
    private View buttonMessages;
    private View clickToast;
    private View textinfo;

    private long lastNoInternetConnectionToast = 0;
    private long mLastDrivingViewSwitch = 0;

    private MenuItem mMenuItemDriving;

    private ModeHolder outdoorHolder;
    private ModeHolder transportHolder;
    private ModeHolder fuzzyHolder;

    private LinearLayout container;

    private ActionBarDrawerToggle mDrawerToggle;
    private View mViewPremiumSupport;
    private SatelliteMenu mSatteliteMenu;

    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
            if (Prefs.STATUS_ONOFF.equals(key) || Prefs.MODE.equals(key)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Prefs.get(HomeMapScreen.this).edit().putLong(Prefs.TRACKING_AUTOSTOP_AT, 0).commit();
                        updateButtons(prefs, true);
                    }
                });
            }
        }
    };

    private BroadcastReceiver parkingReceiver = new ParkingReceiver(this);

    private final class MapLocationListener implements OnMyLocationChangeListener {
        private long lastTimestamp = 0;

        @Override
        public void onMyLocationChange(final Location loc) {
            if (loc == null || Math.abs(loc.getTime() - lastTimestamp) < 8000
                    || !Prefs.get(HomeMapScreen.this).getBoolean(Prefs.STATUS_ONOFF, false)) {
                Logger.d("skipping new location change");
                return;
            }

            new Thread() {
                public void run() {
                    try {
                        Logger.d("new loc provider = " + loc.getProvider());
                        GPS gps = createGPS(loc);
                        insertGPS(gps);
                    } catch (Exception exc) {
                        Logger.e(exc);
                    }
                };
            }.start();

        }

        public GPS createGPS(Location loc) {
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

            lastTimestamp = gps.ts;

            if (loc.getAccuracy() < 60) {
                gps.sensor = GPS.SENSOR_BROWSER;
            } else {
                gps.sensor = GPS.SENSOR_NETWORK;
            }
            return gps;
        }

        public void insertGPS(GPS gps) {
            try {
                DbAdapter.getInstance(getApplicationContext()).insertGPS(gps);
            } catch (Exception exc) {
                Logger.w(exc);
            }
        }
    }

    private class UpdateTimeTask extends TimerTask {

        int count = 0;

        public void run() {
            try {
                SharedPreferences prefs = Prefs.get(HomeMapScreen.this);
                updateUnsetWaypoints();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        updateCockpitValues();
                    }

                });

                if (prefs.getString(Prefs.USERNAME, "").length() == 0) {
                    return;
                }

                switch (count % 3) {
                case 0:
                    doLogin();
                    break;
                case 1:
                    refillMap();
                    break;
                case 2:
                    updateCurrentTrack();
                    if (count % 2 == 0 && mEasyTracker != null) {
                        mEasyTracker.send(MapBuilder.createEvent("tracking", "mode", prefs.getString(Prefs.MODE, ""),
                                null).build());
                    }
                    break;
                }
                count++;

                if (count % 8 == 0) {
                    forceTrackingSenderToUpdateNow();
                }

                final Location loc = getLastLocation();

                if (loc != null && System.currentTimeMillis() - mLastDrivingViewSwitch > 5 * Time.MIN) {
                    if ((loc.getSpeed() > 10 && !drivingMode) || (loc.getSpeed() == 0 && drivingMode)) {
                        runOnUiThread(new Runnable() {

                            @Override
                            public void run() {
                                onDriving(drivingButton);
                            }

                        });
                    }
                } else if (loc != null && loc.getSpeed() > 10) {
                    mLastDrivingViewSwitch = System.currentTimeMillis();
                }

                if (drivingMode) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if (loc != null && System.currentTimeMillis() - loc.getTime() < 20000) {
                                    mLastBearing = loc.getBearing() != 0 ? loc.getBearing() : mLastBearing;
                                    CameraPosition cameraPosition = new CameraPosition.Builder()
                                            .target(new LatLng(loc.getLatitude(), loc.getLongitude())).zoom(18)
                                            .tilt(70).bearing(mLastBearing).build();
                                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                    if (Prefs.isDistanceUS(HomeMapScreen.this)) {
                                        textSpeed.setText((int) (loc.getSpeed() * 2.23694) + " mph");
                                    } else {
                                        textSpeed.setText((int) (loc.getSpeed() * 3.6) + " km/h");
                                    }
                                }
                            } catch (Exception exc) {
                                Logger.w(exc);
                            }
                        }
                    });
                }

                if (count == 4 && prefs.getInt(Prefs.INFO_READ, 0) < 1) {
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                textinfo.setVisibility(View.VISIBLE);
                                textinfo.startAnimation(fromBottomAnimation);
                            } catch (Exception exc) {
                                Logger.w(exc);
                            }
                        }

                    });
                }
            } catch (Exception exc) {
                Logger.e(exc);
            }
        }
    }

    private float mLastBearing = 0;

    protected void onStart() {
        super.onStart();
        isActivityRunning = true;
        EasyTracker.getInstance(this).activityStart(this);
        Intent serviceIntent = new Intent();
        serviceIntent.setAction("anagog.pd.service.MobilityService");
        serviceIntent.setClassName(getPackageName(), "anagog.pd.service.MobilityService");
        startService(serviceIntent);
    };

    private BaseAdapter listAdapter;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("flipping", true);
    }

    public void onEventMainThread(final SearchMap.DirectionsResult result) {
        List<LatLng> track = new LinkedList<LatLng>();
        for (DirectionsResult step : result.steps) {
            track.addAll(decodeFromGoogleToList(step.track));
        }
        TrackLine line = createTrackLine(null, result.track, track, -System.currentTimeMillis());
        line.result = result;
        showDirectionsList(result);
        showDirectionsInfo(result);
        closeMenu();
    }

    public void onEventMainThread(final TemporyMarkerEvent event) {
        addPinToCreatePlace(new LatLng(event.latitude, event.longitude), event.text, true, 600);
        closeMenu();
    }

    protected void onResume() {
        final SharedPreferences prefs = Prefs.get(this);
        String username = prefs.getString(Prefs.USERNAME, "");
        if (username.length() > 0) {
            updateButtons(Prefs.get(this), false);
            syncProfile(prefs, username);
        } else {
            Intent intent = new Intent(this, LoginScreen.class);
            startActivityForResult(intent, C.REQUESTCODE_LOGIN);
        }

        updateParkingMarker();

        timer = new Timer();
        timer.schedule(new UpdateTimeTask(), 500, 4000);

        C2DMReceiver.refreshAppC2DMRegistrationState(getApplicationContext());

        super.onResume();
    }

    public void syncProfile(final SharedPreferences prefs, String username) {
        try {
            JSONObject obj = prepareObj();
            obj.put(ACCOUNT, username);
            doAction(ACTION_PROFILE, obj, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    try {
                        updateButtons(prefs, false);
                    } catch (Exception exc) {
                        Logger.w(exc);
                    }
                }
            });
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    @Override
    public void onQueryInventoryFinished(IabResult result, Inventory inv) {
        super.onQueryInventoryFinished(result, inv);
        updatePremiumButtons(Prefs.get(this));
    }

    @Override
    protected void onPause() {
        if (timer != null)
            timer.cancel();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mMap != null)
            mMap.clear();
        unregisterReceiver(trackReceiver);
        unregisterReceiver(mShowOnMapReceiver);
        unregisterReceiver(parkingReceiver);
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        EventBus.getDefault().unregister(this);

        DbAdapter.closeDB();
        super.onDestroy();
    }

    private BroadcastReceiver trackReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent data) {
            if (data != null) {
                String trackString = data.getStringExtra("track");
                long trackId = data.getLongExtra("trackid", 0);
                if (trackString != null && trackString.length() > 0 && trackId > 0) {
                    if (visibleTracks.containsKey(trackId)) {
                        fitBounds(mMap, visibleTracks.get(trackId).track);
                        return;
                    }
                    createTrackLine(data, trackString, null, trackId);
                    closeMenu();
                }
            }
        }

    };

    private Switch mPowerSwitch;
    private View mMiniCockpit;
    private DrawerLayout mDrawerLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Logger.setContext(getApplicationContext());

        // startActivity(new Intent(this, RegisterCompanyScreen.class));

        final SharedPreferences prefs = Prefs.get(this);
        String mode = prefs.getString(Prefs.MODE, null);
        if (mode == null || mode.length() == 0) {
            mode = Mode.automatic.toString();
            prefs.edit().putString(Prefs.MODE, mode).commit();
            maybeStartService();
        }

        blinkanimation = new AlphaAnimation(1, 0);
        blinkanimation.setDuration(300);
        blinkanimation.setInterpolator(new LinearInterpolator());
        blinkanimation.setRepeatCount(3);
        blinkanimation.setRepeatMode(Animation.REVERSE);

        fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.rail);
        fadeOutAnimation = AnimationUtils.loadAnimation(this, R.anim.rail_out);
        fromBottomAnimation = AnimationUtils.loadAnimation(this, R.anim.from_bottom);
        toBottomAnimation = AnimationUtils.loadAnimation(this, R.anim.to_bottom);

        setContentView(R.layout.screen_main);

        registerReceiver(mShowOnMapReceiver, new IntentFilter(C.BROADCAST_SHOWMAP));
        registerReceiver(trackReceiver, new IntentFilter(C.BROADCAST_ADDTRACKTOMAP));

        setUpMapIfNeeded();
        setupActionBar();

        textModeInMap = (TextView) findViewById(R.id.textMode);

        mViewPremiumSupport = findViewById(R.id.premiumSupport);

        textinfo = findViewById(R.id.textinfo);
        textinfo.findViewById(R.id.buttonClose).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Prefs.get(HomeMapScreen.this).edit().putInt(Prefs.INFO_READ, 1).commit();
                textinfo.startAnimation(toBottomAnimation);
                textinfo.setVisibility(View.GONE);
            }
        });
        textinfo.findViewById(R.id.text).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                openInfo();
            }
        });
        clickToast = findViewById(R.id.clicktoast);
        clickToast.setVisibility(View.GONE);
        toBottomAnimation.setAnimationListener(new AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                clickToast.setVisibility(View.GONE);
            }
        });

        mMiniCockpit = findViewById(R.id.layoutMiniCockpit);

        mPowerSwitch = (Switch) findViewById(R.id.switchPower);
        mPowerSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                gaSendButtonPressed("status_on_off", isChecked ? 1 : 0);
                prefs.edit().putBoolean(Prefs.STATUS_ONOFF, isChecked).commit();
            }
        });

        onCreateCockpit();
        prefs.registerOnSharedPreferenceChangeListener(prefChangeListener);

        if (savedInstanceState == null) {
            if (prefs.getBoolean(Prefs.ACTIVATE_ON_LOGIN, false) || prefs.getString(Prefs.MODE, null) == null) {
                prefs.edit().putBoolean(Prefs.STATUS_ONOFF, true).commit();
            }
        }

        lastMarkers = prefs.getString(Prefs.createMarkerCacheId(HomeMapScreen.this), null);
        if (lastMarkers != null) {
            Logger.i("restoring last markers: " + lastMarkers);
            updateMap(lastMarkers);
        }

        refillMap();
        refillContactList();

        buttonMessages = findViewById(R.id.buttonMessages);
        badgeMessages = new BadgeView(this, buttonMessages);

        drivingButton = (ImageButton) findViewById(R.id.buttonDriving);
        drivingButton.setOnLongClickListener(new OnLongClickListener() {

            @Override
            public boolean onLongClick(View view) {
                onPanic(view);
                return false;
            }
        });
        textSpeed = (TextView) findViewById(R.id.textSpeed);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                showMyLocation();
            }
        }, 500);

        try {
            EventBus.getDefault().register(this, SearchMap.DirectionsResult.class, TemporyMarkerEvent.class);
        } catch (Throwable t) {
            Logger.e(t);
        }

        if (mMap != null) {
            mMap.setOnMyLocationChangeListener(new MapLocationListener());
            mMap.setTrafficEnabled(Prefs.get(this).getBoolean(Prefs.SHOW_TRAFFIC, false));
        }

        if (getIntent() != null && getIntent().getExtras() != null
                && getIntent().getExtras().containsKey(C.OPEN_SCREEN)) {
            String screen = getIntent().getStringExtra(C.OPEN_SCREEN);
            if ("messages".equals(screen)) {
                Intent intent = new Intent(this, MessagesScreen.class);
                intent.putExtra(C.account, getIntent().getStringExtra(C.account));
                startActivity(intent);
            } else if ("profile".equals(screen)) {
                Intent intent = new Intent(this, NewProfileScreen.class);
                intent.putExtra(C.account, getIntent().getStringExtra(C.account));
                startActivity(intent);
            } else if ("activities".equals(screen)) {
                onActivities(null);
            }
        }

        mSatteliteMenu = (SatelliteMenu) findViewById(R.id.satteliteMainMenu);
        mSatteliteMenu.setMainImage(getResources().getDrawable(R.drawable.ic_action_add));
        List<SatelliteMenuItem> items = new ArrayList<SatelliteMenuItem>();

        items.add(new SatelliteMenuItem(R.drawable.ic_action_invite, R.drawable.ic_action_invite));
        items.add(new SatelliteMenuItem(R.drawable.ic_action_navigation_accept, R.drawable.ic_action_navigation_accept));
        items.add(new SatelliteMenuItem(R.drawable.ic_action_search, R.drawable.ic_action_search));
        items.add(new SatelliteMenuItem(R.drawable.ic_action_location, R.drawable.ic_action_location));
        mSatteliteMenu.addItems(items);

        mSatteliteMenu.setOnItemClickedListener(new SateliteClickedListener() {

            @Override
            public void eventOccured(int id) {
                try {
                    switch (id) {
                    case R.drawable.ic_action_invite:
                        gaSendButtonPressed("satellite_invite");
                        startActivity(new Intent(HomeMapScreen.this, AddContactScreen.class));
                        break;
                    case R.drawable.ic_action_search:
                        gaSendButtonPressed("satellite_search");
                        startActivityForResult(new Intent(HomeMapScreen.this, AddPlaceScreen.class),
                                C.REQUESTCODE_GOOGLEPLACE);
                        break;
                    case R.drawable.ic_action_navigation_accept:
                        gaSendButtonPressed("satellite_checkin");
                        Intent intent = new Intent(HomeMapScreen.this, CheckinScreen.class);
                        startActivityForResult(intent, C.REQUESTCODE_GOOGLEPLACE);
                        break;
                    case R.drawable.ic_action_location:
                        gaSendButtonPressed("satellite_createplace");
                        addPinToCreatePlace(mMap.getCameraPosition().target, null, true, 30000);
                        break;
                    }
                } catch (Exception exc) {
                    Logger.e(exc);
                }
            }
        });

        if (isMode(Mode.automatic)) {
            boolean gps = mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean net = mLocationManager != null
                    && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!prefs.contains(Prefs.IGNORE_GPS_SETTINGS) && (!gps || !net)) {
                AlertDialog.Builder b = CompatibilityUtils.createAlertDialogBuilderCompat(this);
                b.setMessage(R.string.ActivateBothGPSAndNet);
                b.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        openLocationSettings();
                    }
                });
                b.setNegativeButton(R.string.Ignore, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        prefs.edit().putBoolean(Prefs.IGNORE_GPS_SETTINGS, true).commit();
                    }
                });
                AlertDialog dlg = b.create();
                dlg.show();
            }
        }
        registerReceiver(parkingReceiver, new IntentFilter(C.BROADCAST_PARKING_UPDATE));

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                if (mSatteliteMenu.getContext() != null) {
                    mSatteliteMenu.expand();
                }
            }

        }, 800);
    }

    private Marker mParkingMarker;

    public void updateParkingMarker() {
        if (mParkingMarker != null) {
            mParkingMarker.remove();
            mParkingMarker = null;
        }

        SharedPreferences prefs = Prefs.get(this);
        if (!prefs.getBoolean(Prefs.SHOW_PARKING, true)) {
            return;
        }
        if (prefs.contains(Prefs.PARKING_LAT)) {
            try {
                double lat = Double.longBitsToDouble(prefs.getLong(Prefs.PARKING_LAT, 0));
                double lng = Double.longBitsToDouble(prefs.getLong(Prefs.PARKING_LNG, 0));
                long ts = prefs.getLong(Prefs.PARKING_TS, 0);
                if (lat != 0 && lng != 0) {
                    String timePassed = Time.formatTimePassed(this, ts);
                    MarkerOptions opt = new MarkerOptions();
                    opt.position(new LatLng(lat, lng)).title(getResources().getString(R.string.LastParking))
                            .snippet(timePassed + "\n" + getResources().getString(R.string.ParkingYouParkedHere));
                    IconGenerator gen = new IconGenerator(this);
                    gen.setStyle(IconGenerator.STYLE_BLUE);
                    Bitmap bmp = gen.makeIcon("P");
                    opt.icon(BitmapDescriptorFactory.fromBitmap(bmp));
                    mParkingMarker = mMap.addMarker(opt);
                }
            } catch (Exception exc) {
                Logger.w(exc);
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    protected void setupActionBar() {
        getSupportActionBar().show();
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_menu);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.header_bg));
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.view_contactsscroller);
        container = (LinearLayout) getSupportActionBar().getCustomView().findViewById(R.id.contactsContainer);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        // set a custom shadow that overlays the main content when the drawer opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
        mDrawerLayout, /* DrawerLayout object */
        R.drawable.ic_navigation_drawer, /* nav drawer image to replace 'Up' caret */
        R.string.Open, /* "open drawer" description for accessibility */
        R.string.Close /* "close drawer" description for accessibility */
        ) {
            @Override
            public void onDrawerClosed(View view) {
                Logger.d("menu closed");
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                Logger.d("menu opened");
                mScrollViewMenu.smoothScrollTo(0, 0);
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);
        mDrawerToggle.setDrawerIndicatorEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            toggleMenu();
            break;
        }
        return true;
    };

    protected void toggleMenu() {
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            mDrawerLayout.closeDrawer(Gravity.LEFT);
        } else {
            mDrawerLayout.openDrawer(Gravity.LEFT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu bar) {
        SubMenu mainMenu = bar.addSubMenu(R.string.Menu);
        MenuItem subMenuItem = mainMenu.getItem();
        subMenuItem.setIcon(R.drawable.ic_action_more);
        subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        SubMenu helpMenu = mainMenu.addSubMenu(R.string.HowDoesItWork);
        MenuItem helpMenuItem = helpMenu.getItem();
        helpMenuItem.setIcon(R.drawable.ic_action_help);
        helpMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        {
            final MenuItem item = mainMenu.add(2, Menu.NONE, Menu.NONE, R.string.Emergency);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_attention);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    gaSendButtonPressed("secondmenu_emergency");
                    startActivity(new Intent(HomeMapScreen.this, PanicInfoScreen.class));
                    return false;
                }
            });
        }

        {
            final MenuItem item = mainMenu.add(2, Menu.NONE, Menu.NONE, R.string.RemoteActivation);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_remoteactivation);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    gaSendButtonPressed("secondmenu_remoteactivation");
                    startActivity(new Intent(HomeMapScreen.this, RemoteActivationInfoScreen.class));
                    return false;
                }
            });
        }

        {
            final MenuItem item = mainMenu.add(2, Menu.NONE, Menu.NONE, R.string.ExcelReport);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_document);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    try {
                        gaSendButtonPressed("secondmenu_excelreport");
                        if (Prefs.get(HomeMapScreen.this).getBoolean(Prefs.IS_PREMIUM, false)
                                || Prefs.get(HomeMapScreen.this).getBoolean(Prefs.IS_EMPLOYEE, false)
                                || PlanUtils.hasAnyPlan(HomeMapScreen.this)) {
                            JSONObject obj = prepareObj();
                            obj.put("account", Prefs.get(HomeMapScreen.this).getString(Prefs.USERNAME, ""));
                            doAction(AbstractScreen.ACTION_SENDREPORT, obj, new ResultWorker() {
                                @Override
                                public void onResult(String result, Context context) {
                                    Ui.makeText(HomeMapScreen.this, R.string.ReportIsSentToYourEmailAddress,
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                        } else {
                            AlertDialog.Builder b = CompatibilityUtils
                                    .createAlertDialogBuilderCompat(HomeMapScreen.this);
                            b.setMessage(R.string.ExcelReportPremiumNeeded);
                            b.setCancelable(true);
                            b.setNegativeButton(R.string.Cancel, null);
                            b.setPositiveButton(R.string.Premium, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    onPremiumUpsell(null);
                                }
                            });
                            AlertDialog dlg = b.create();
                            dlg.show();
                        }
                    } catch (Exception exc) {
                        Logger.e(exc);
                    }
                    return false;
                }
            });
        }
        //
        //        {
        //            final MenuItem item = mainMenu.add(2, Menu.NONE, Menu.NONE, R.string.LikeUsOnFacebook);
        //            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        //            item.setIcon(R.drawable.ic_action_rate);
        //            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {
        //
        //                public boolean onMenuItemClick(MenuItem item) {
        //                    gaSendButtonPressed("secondmenu_like_fb");
        //                    Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/hellotracks"));
        //                    startActivity(open);
        //                    return false;
        //                }
        //            });
        //        }

        {
            final MenuItem item = helpMenu.add(2, Menu.NONE, Menu.NONE, R.string.Information);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_info);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    gaSendButtonPressed("secondmenu_info");
                    openInfo();
                    return false;
                }
            });
        }

        {
            final MenuItem item = helpMenu.add(2, Menu.NONE, Menu.NONE, R.string.VideoIntroduction);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_device_access_video);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    gaSendButtonPressed("secondmenu_video");
                    openVideo();
                    return false;
                }
            });
        }

        {
            final MenuItem item = helpMenu.add(2, Menu.NONE, Menu.NONE, R.string.QuestionOrFeedback);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_help);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    gaSendButtonPressed("secondmenu_feedback");
                    onFeedback(null);
                    return false;
                }
            });
        }

        {
            final MenuItem item = helpMenu.add(2, Menu.NONE, Menu.NONE, R.string.FAQ);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_info);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    gaSendButtonPressed("secondmenu_faq");
                    onFAQ(null);
                    return false;
                }
            });
        }

        mMenuItemDriving = mainMenu.add(1, Menu.NONE, Menu.NONE, R.string.DrivingView);
        mMenuItemDriving.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        mMenuItemDriving.setCheckable(true);
        mMenuItemDriving.setChecked(drivingMode);
        mMenuItemDriving.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                gaSendButtonPressed("secondmenu_driving");
                onDriving(drivingButton);
                return false;
            }
        });

        final MenuItem mMenuItemTraffic = mainMenu.add(1, Menu.NONE, Menu.NONE, R.string.ShowTraffic);
        mMenuItemTraffic.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        mMenuItemTraffic.setCheckable(true);
        mMenuItemTraffic.setChecked(Prefs.get(this).getBoolean(Prefs.SHOW_TRAFFIC, false));
        mMenuItemTraffic.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                gaSendButtonPressed("secondmenu_traffic");

                if (mMap == null)
                    return false;

                boolean old = Prefs.get(HomeMapScreen.this).getBoolean(Prefs.SHOW_TRAFFIC, false);
                boolean checked = !old;
                mMap.setTrafficEnabled(checked);
                Prefs.get(HomeMapScreen.this).edit().putBoolean(Prefs.SHOW_TRAFFIC, checked).commit();
                mMenuItemTraffic.setChecked(checked);
                return false;
            }
        });

        final MenuItem mMenuItemParking = mainMenu.add(1, Menu.NONE, Menu.NONE, R.string.LastParking);
        mMenuItemParking.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        mMenuItemParking.setCheckable(true);
        mMenuItemParking.setChecked(Prefs.get(this).getBoolean(Prefs.SHOW_PARKING, true));
        mMenuItemParking.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(MenuItem item) {
                gaSendButtonPressed("secondmenu_parking");

                if (mMap == null)
                    return false;

                boolean old = Prefs.get(HomeMapScreen.this).getBoolean(Prefs.SHOW_PARKING, true);
                boolean checked = !old;
                Prefs.get(HomeMapScreen.this).edit().putBoolean(Prefs.SHOW_PARKING, checked).commit();
                mMenuItemParking.setChecked(checked);

                if (checked) {
                    ParkingReceiver.callParking(HomeMapScreen.this);
                } else {
                    updateParkingMarker();
                }
                return false;
            }
        });

        return true;
    }

    private void openInfo() {
        Prefs.get(HomeMapScreen.this).edit().putInt(Prefs.INFO_READ, 1).commit();
        textinfo.startAnimation(toBottomAnimation);
        textinfo.setVisibility(View.GONE);
        startActivity(new Intent(HomeMapScreen.this, InfoScreen.class));
    }

    private void openVideo() {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=x31YAc6c8R0")));
    }

    public void onPremiumSupport(View view) {
        gaSendButtonPressed("menu_premium");
        String titleText = getResources().getString(R.string.PremiumSupportInquiry) + ": "
                + Prefs.get(this).getString(Prefs.USERNAME, "");
        Intent send = new Intent(Intent.ACTION_SENDTO);
        String uriText = "mailto:premium@hellotracks.com?subject=" + Uri.encode(titleText) + "&body=";
        Uri uri = Uri.parse(uriText);
        send.setData(uri);
        startActivity(Intent.createChooser(send, getResources().getString(R.string.PremiumSupport)));
    }

    public void onRateUs(View view) {
        gaSendButtonPressed("secondmenu_rateus");
        Prefs.get(this).edit().putInt(Prefs.RATEUSCOUNT, Prefs.get(this).getInt(Prefs.RATEUSCOUNT, 0) + 1).commit();
        openMarketDialog(getResources().getString(R.string.RateNow));
    }

    private void refillMap() {
        try {
            if (Prefs.get(this).getString(Prefs.USERNAME, "").length() == 0)
                return;
            JSONObject obj = AbstractScreen.prepareObj(this);
            obj.put(C.account, null);
            API.doAction(this, AbstractScreen.ACTION_MARKERS, obj, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    if (!result.equals(lastMarkers)) {
                        Logger.i("updating map");
                        lastMarkers = result;
                        Prefs.get(HomeMapScreen.this).edit()
                                .putString(Prefs.createMarkerCacheId(HomeMapScreen.this), lastMarkers).commit();
                        updateMap(result);
                    }
                }
            });
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    @SuppressWarnings("serial")
    private class NameSortedSet extends TreeSet<MarkerEntry> {

        public NameSortedSet() {
            super(new Comparator<MarkerEntry>() {

                @Override
                public int compare(MarkerEntry i1, MarkerEntry i2) {
                    try {
                        return i1.name.toLowerCase().compareTo(i2.name.toLowerCase());
                    } catch (Exception exc) {
                        Logger.e(exc);
                        return 0;
                    }
                }
            });
        }

    }

    private void updateMap(final String markers) {
        //Log.d("updating map: " + markers);
        Logger.i("updating map hash: " + markers.hashCode());
        if (markers != null && markers.length() > 0) {
            try {
                HashSet<String> untouched = new HashSet<String>();
                untouched.addAll(mMarkerEntries.keySet());

                JSONArray array = new JSONArray(markers);
                for (int i = 0; i < array.length(); i++) {
                    try {
                        JSONObject obj = array.getJSONObject(i);
                        String account = obj.getString("account");
                        untouched.remove(account);
                        MarkerEntry entry = mMarkerEntries.get(account);
                        String jsonString = obj.toString();
                        if (entry != null && jsonString.equals(entry.json))
                            continue;

                        boolean doPosAnim = false;
                        if (entry == null) {
                            entry = new MarkerEntry(i);
                        } else {
                            doPosAnim = Utils.hasICS();
                        }

                        entry.url = obj.getString("url");
                        entry.name = obj.getString("name");
                        entry.account = account;
                        entry.timestamp = obj.getLong("ts");
                        entry.radius = obj.has("radius") ? obj.getInt("radius") : -1;
                        entry.info = obj.getString("info");
                        entry.accuracy = obj.getInt("acc");
                        double lat = obj.getDouble("lat");
                        double lng = obj.getDouble("lng");
                        LatLng finalPosition = new LatLng(lat, lng);

                        doPosAnim &= entry.isPerson();
                        if (!doPosAnim) {
                            entry.point = finalPosition;
                        }
                        entry.json = jsonString;

                        Marker marker = buildMarker(entry);
                        mMarkerEntries.put(account, entry);

                        doPosAnim &= marker != null;
                        if (doPosAnim) {
                            animateMarkerPos(finalPosition, marker);
                            entry.point = finalPosition;
                        }
                    } catch (Exception exc) {
                        Logger.e(exc);
                    }
                }
                for (String a : untouched) {
                    MarkerEntry e = mMarkerEntries.remove(a);
                    if (e != null) {
                        removeMarker(e.marker);
                    }
                }

                refillContactList();
            } catch (Exception exc) {
                Logger.e(exc);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    protected void animateMarkerPos(LatLng finalPosition, Marker marker) {
        TypeEvaluator<LatLng> typeEval = new TypeEvaluator<LatLng>() {

            @Override
            public LatLng evaluate(float fraction, LatLng a, LatLng b) {
                double lat = (b.latitude - a.latitude) * fraction + a.latitude;
                double lng = (b.longitude - a.longitude) * fraction + a.longitude;
                return new LatLng(lat, lng);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEval, finalPosition);
        animator.setDuration(1000);
        animator.start();
    }

    private Bitmap markerBitmap;
    private float density = -1;

    protected Marker buildMarker(final MarkerEntry entry) {
        try {
            removeMarker(entry.marker);

            final boolean useNewMarkers = true;

            if (density < 0) {
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                density = metrics.density;
            }

            String tmp = entry.url;
            if (useNewMarkers) {
                if (tmp != null && tmp.endsWith("_marker.png")) {
                    tmp = tmp.substring(0, tmp.length() - "_marker.png".length());
                    tmp += density >= 2 ? "_thumb.jpg" : "_mini.jpg";
                }
            }
            final String url = tmp;

            final Resources r = getResources();

            Marker marker = addMarker(entry, r, new IconGenerator(this).makeIcon(entry.name));

            ImageRequest req = createMarkerImageRequest(entry, url);
            req.setShouldCache(true);
            RequestQueue queue = API.getRequestQueue(this);
            queue.add(req);
            return marker;
        } catch (Exception exc) {
            Logger.w(exc);
        }
        return null;
    }

    public ImageRequest createMarkerImageRequest(final MarkerEntry entry, final String url) {
        final Target t = new Target() {

            @Override
            public void onSuccess(final Bitmap inner) {
                try {
                    new Async.Task<Bitmap>(HomeMapScreen.this) {
                        @Override
                        public Bitmap async() {
                            return combineImages(HomeMapScreen.this, markerBitmap, inner);
                        }

                        public void post(Bitmap result) {
                            if (result != null) {
                                entry.marker.setIcon(BitmapDescriptorFactory.fromBitmap(result));
                            }
                        };
                    };
                } catch (Exception exc) {
                    Logger.e(exc);
                }
            }

            @Override
            public void onError() {
                Logger.w("could not load marker");
            }
        };
        Picasso.with(getApplicationContext()).load(url).into(t);
        ImageRequest req = new ImageRequest(url, new Listener<Bitmap>() {

            @Override
            public void onResponse(final Bitmap inner) {
                Picasso.with(getApplicationContext()).cancelRequest(t);
                new Async.Task<Bitmap>(HomeMapScreen.this) {
                    @Override
                    public Bitmap async() {
                        return combineImages(HomeMapScreen.this, markerBitmap, inner);
                    }

                    public void post(Bitmap result) {
                        if (result != null) {
                            entry.marker.setIcon(BitmapDescriptorFactory.fromBitmap(result));
                        }
                    };
                };

            }
        }, 0, 0, Config.ARGB_8888, new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Logger.w(url);
            }
        });
        req.setShouldCache(true);
        return req;
    }

    private void refillContactList() {
        if (mMap == null)
            return;

        container.removeAllViews();
        addContactListAction(container);
        if (mMarkerEntries.size() > 0)
            fillContactActions(container);
    }

    public void onLayers(View view) {
        gaSendButtonPressed("layers");
        if (mMap == null)
            return;

        if (mMap.getMapType() == GoogleMap.MAP_TYPE_NORMAL) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        } else if (mMap.getMapType() == GoogleMap.MAP_TYPE_HYBRID) {
            mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        } else {
            mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        }
        Prefs.get(this).edit().putInt(Prefs.MAP_TYPE, mMap.getMapType()).commit();
    }

    protected final ContactAccessor mContactAccessor = ContactAccessor.getInstance();

    private void loadContactInfo(Uri contactUri) {

        AsyncTask<Uri, Void, ContactInfo> task = new AsyncTask<Uri, Void, ContactInfo>() {

            @Override
            protected ContactInfo doInBackground(Uri... uris) {
                return mContactAccessor.loadContact(getContentResolver(), uris[0]);
            }

            @Override
            protected void onPostExecute(ContactInfo result) {
                if (result.getEmail() != null && result.getEmail().length() > 0) {
                    AbstractScreen.sendInvitation(HomeMapScreen.this, result.getEmail());
                }
                AbstractScreen.prepareSMS(HomeMapScreen.this, result.getPhoneNumber());
                AbstractScreen.sendPendingInvitation(HomeMapScreen.this, result.getDisplayName(), null,
                        result.getPhoneNumber());
            }
        };

        task.execute(contactUri);
    }

    private void fillContactActions(LinearLayout container) {
        Set<MarkerEntry> places = new NameSortedSet();

        TreeSet<MarkerEntry> contacts = new TreeSet<MarkerEntry>(new Comparator<MarkerEntry>() {

            @Override
            public int compare(MarkerEntry lhs, MarkerEntry rhs) {
                return (lhs.timestamp < rhs.timestamp) ? 1 : -1;
            }
        });

        for (MarkerEntry entry : mMarkerEntries.values().toArray(new MarkerEntry[0])) {
            if (entry.isPlace()) {
                places.add(entry);
            } else {
                contacts.add(entry);
            }
        }

        LinkedList<MarkerEntry> list = new LinkedList<MarkerEntry>();
        list.addAll(contacts);

        for (MarkerEntry i : list) {
            final MarkerEntry item = i;
            View contactView = getLayoutInflater().inflate(R.layout.quick_contact, null);
            final TextView text = (TextView) contactView.findViewById(R.id.quickText);
            final ImageView image = (ImageView) contactView.findViewById(R.id.quickImage);
            text.setTypeface(null, System.currentTimeMillis() - item.timestamp < Time.HOURS ? Typeface.BOLD
                    : Typeface.NORMAL);
            //            if (System.currentTimeMillis() - item.timestamp < Time.HOURS) {
            //                text.setBackgroundResource(R.drawable.custom_button_trans_blue);
            //            } else if (places.contains(item)) {
            //                text.setBackgroundResource(R.drawable.custom_button_trans_red);
            //            } else {
            //                text.setBackgroundResource(R.drawable.custom_button_light);
            //            }
            contactView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    try {
                        gaSendButtonLongPressed("contact_item");
                        Marker m = item.marker;
                        CameraPosition cameraPosition = new CameraPosition.Builder().target(m.getPosition()).zoom(14)
                                .tilt(30).build();
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        Intent intent = new Intent(HomeMapScreen.this, NewProfileScreen.class);
                        intent.putExtra(C.account, item.account);
                        intent.putExtra(C.name, item.name);
                        startActivityForResult(intent, C.REQUESTCODE_CONTACT());
                    } catch (Exception exc) {
                        Logger.e(exc);
                    }
                    return true;
                }
            });
            contactView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    jumpToContact(item.marker);
                }
            });
            String url = item.url;
            if (url.endsWith("marker.png")) {
                url = url.substring(0, url.length() - "marker.png".length()) + "mini.jpg";
            }
            Picasso.with(HomeMapScreen.this).load(url).into(image);

            String name = item.name;
            if (name.length() > 12) {
                name = name.substring(0, 10) + "...";
            }
            text.setText(name);

            container.addView(contactView);
            contactView.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
        }

    }

    private View contactsView;

    private void addContactListAction(LinearLayout container) {
        contactsView = getLayoutInflater().inflate(R.layout.quick_contact, null);
        contactsView.setVisibility(View.GONE);
        ImageView image = (ImageView) contactsView.findViewById(R.id.quickImage);
        image.setImageResource(R.drawable.ic_action_contacts);
        image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                onContacts(v);
            }
        });
        TextView text = (TextView) contactsView.findViewById(R.id.quickText);
        text.setText(R.string.Contacts);

        container.addView(contactsView);

        badgeContacts = new BadgeView(this, contactsView);
    }

    public void onActivities(View view) {
        gaSendButtonPressed("activities");
        Intent intent = new Intent(this, ActivitiesScreen.class);
        startActivity(intent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Logger.i("HMS.onActivityResult.requestCode=" + requestCode + " resultCode=" + resultCode);
        if (Session.getActiveSession() != null) {
            Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
        }
        if (requestCode == C.REQUESTCODE_LOGIN || resultCode == C.RESULTCODE_LOGIN_SUCCESS) {
            startIabSetup();
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    try {
                        Logger.i("forcce restart services");
                        stopAllServices(HomeMapScreen.this);
                        maybeStartService();
                    } catch (Exception exc) {
                        Logger.e(exc);
                    }
                }
            }, 500);
        }
        if (requestCode == C.REQUESTCODE_CONTACT()) {
            new Thread() {
                public void run() {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                    }
                    refillMap();
                };
            }.start();
        } else if (requestCode == C.REQUESTCODE_PICK_CONTACT && resultCode == RESULT_OK) {
            loadContactInfo(data.getData());
            return;
        } else if (requestCode == C.REQUESTCODE_GOOGLEPLACE && resultCode == RESULT_OK) {
            closeMenu();
            final String description = data.getStringExtra("description");
            SearchMap.asyncSearchPlace(this, getRequestQueue(), description, data.getStringExtra("reference"),
                    new SearchMap.Callback<SearchMap.LocationResult>() {

                        @Override
                        public void onResult(boolean success, LocationResult location) {
                            if (location != null && location.position != null) {
                                addPinToCreatePlace(location.position.toGoogle(), description, true, 30000L);
                                jumpTo(location.position.toGoogle());
                            }
                        }
                    });
        } else if (requestCode == C.REQUESTCODE_PICK_PLACE_FB && data != null) {
            /*
            { 
            "category" : "Real estate",
            "id" : "238766309568535",
            "location" : { "city" : "San Francisco",
            "country" : "United States",
            "latitude" : 37.774999999999999,
            "longitude" : -122.4183,
            "state" : "CA",
            "street" : "0 main st",
            "zip" : "94101"
            },
            "name" : "Paymon Home",
            "picture" : { "data" : { "height" : 130,
            "is_silhouette" : false,
            "url" : "https://fbcdn-profile-a.akamaihd.net/hprofile-ak-ash2/545691_238780986233734_98932661_s.jpg",
            "width" : 130
            } },
            "were_here_count" : 32
            }
             */

            try {
                JSONObject json = new JSONObject(data.getStringExtra("place"));
                String category = json.getString("category");
                long id = json.getLong("id");
                String name = json.getString("name");
                JSONObject pictureData = json.getJSONObject("picture").getJSONObject("data");
                int height = pictureData.getInt("height");
                int width = pictureData.getInt("width");
                String url = pictureData.getString("url");
                int whereHereCount = json.getInt("where_here_count");
            } catch (JSONException e) {
                Logger.e(e);
            }
        }

        if (resultCode == C.RESULTCODE_CLOSEAPP) {
            realLogout();
            return;
        }
    }

    protected void realLogout() {
        lastMarkers = null;
        Prefs.removeAllLogout(this);
        stopService();
        setResult(-1);
        finish();
    }

    private void showMyLocation() {
        if (mMap == null)
            return;

        try {

            if (mMap.getMyLocation() != null) {
                try {
                    LatLng pos = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                    CameraPosition cameraPosition = new CameraPosition.Builder().zoom(14).target(pos).tilt(30).build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            } else {
                Location loc = getLastLocation();
                if (loc != null) {
                    LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
                    CameraPosition cameraPosition = new CameraPosition.Builder().zoom(14).target(pos).tilt(30).build();
                    mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    private void closeMenu() {
        if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
            toggleMenu();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (Contextual.isOpen(this)) {
                hideContextual();
            } else if (mDrawerLayout.isDrawerOpen(Gravity.LEFT)) {
                toggleMenu();
            } else {
                finish();
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            toggleMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onTracks(View view) {
        gaSendButtonPressed("menu_tracks");
        Intent intent = new Intent(HomeMapScreen.this, TrackListScreen.class);
        startActivityForResult(intent, C.REQUESTCODE_CONTACT());
    }

    public void onPanic(View view) {
        try {
            gaSendButtonPressed("panic");
            Intent intent = new Intent(HomeMapScreen.this, PanicScreen.class);
            String msg = "";
            Location loc = getLastLocation();
            if (loc != null) {
                msg = "@uri geo:0,0?q=";
                String location = loc.getLatitude() + "," + loc.getLongitude() + "("
                        + getResources().getString(R.string.IAmHere) + ")";
                msg += location + " text:";
            }
            msg += getResources().getString(R.string.INeedHelp);
            intent.putExtra("message", msg);

            HashSet<String> receivers = new HashSet<String>();
            for (MarkerEntry entry : mMarkerEntries.values().toArray(new MarkerEntry[0])) {
                if (!entry.isMe() && entry.isPerson()) {
                    receivers.add(entry.account);
                }
            }
            intent.putExtra("receivers", receivers.toArray(new String[0]));
            startActivity(intent);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    private boolean drivingMode = false;

    private TextView textSpeed;
    private ImageButton drivingButton;

    private WakeLock mWakeLock;

    public void onDriving(View view) {
        if (mMenuItemDriving == null)
            return;

        try {
            drivingMode = !drivingMode;
            int r;
            if (drivingMode) {
                view.startAnimation(blinkanimation);

                textSpeed.setVisibility(View.VISIBLE);
                drivingButton.setImageResource(R.drawable.ic_steering_wheel_white);
                drivingButton.setBackgroundColor(getResources().getColor(R.color.blue));
                r = R.string.DrivingModeOn;
                aquireWakeLock();
            } else {
                releaseWakeLock();
                textSpeed.setVisibility(View.GONE);
                drivingButton.setImageResource(R.drawable.ic_steering_wheel_gray);
                drivingButton.setBackgroundResource(R.drawable.custom_button_trans_light);
                r = R.string.DrivingModeOff;
                CameraPosition cameraPosition = new CameraPosition.Builder().zoom(14)
                        .target(mMap.getCameraPosition().target).tilt(10).bearing(0).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
            mMenuItemDriving.setChecked(drivingMode);
            mLastDrivingViewSwitch = System.currentTimeMillis();
            Ui.makeText(this, r, Toast.LENGTH_SHORT).show();
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    private void aquireWakeLock() {
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "Driving");
            mWakeLock.acquire();
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    private void updateButtons(final SharedPreferences prefs, boolean change) {
        boolean active = prefs.getBoolean(Prefs.STATUS_ONOFF, false);

        mPowerSwitch.setChecked(active);

        final String mode = prefs.getString(Prefs.MODE, Mode.automatic.toString());
        boolean auto = Mode.isAutomatic(mode);
        if (auto) {
            mMiniCockpit.setVisibility(View.GONE);
            textModeInMap.setText(R.string.TrackingAutomatic);
        } else if (Mode.isFuzzy(mode)) {
            mMiniCockpit.setVisibility(View.VISIBLE);
            fuzzyHolder.getRadio().setChecked(true);
            textModeInMap.setText(R.string.Battery);
        } else if (Mode.isTransport(mode)) {
            mMiniCockpit.setVisibility(View.VISIBLE);
            transportHolder.getRadio().setChecked(true);
            textModeInMap.setText(R.string.Transport);
        } else if (Mode.isOutdoor(mode)) {
            mMiniCockpit.setVisibility(View.VISIBLE);
            outdoorHolder.getRadio().setChecked(true);
            textModeInMap.setText(R.string.Outdoor);
        }

        if (!active) {
            group.setVisibility(View.GONE);
            onOffSwitch.setChecked(false);
        } else {
            group.setVisibility(isMode(Mode.automatic) ? View.GONE : View.VISIBLE);
            onOffSwitch.setChecked(true);
        }

        if (change && !auto) {
            if (active) {
                final int r = Prefs.isDistanceUS(this) ? R.string.TrackStartedFeet : R.string.TrackStartedMeter;
                final int minutes = Mode.isFuzzy(mode) ? 180 : 90;
                clickToast.setVisibility(View.VISIBLE);
                clickToast.startAnimation(fromBottomAnimation);
                TextView text = (TextView) clickToast.findViewById(R.id.text);
                text.setText(r);
                Button button = (Button) clickToast.findViewById(R.id.button);
                button.setText(getResources().getString(R.string.ClickHereToStopTrackingAfterXMinutes,
                        String.valueOf(minutes)));
                button.setOnClickListener(new OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        gaSendButtonPressed("clock_toast");
                        Prefs.get(HomeMapScreen.this).edit()
                                .putLong(Prefs.TRACKING_AUTOSTOP_AT, System.currentTimeMillis() + minutes * Time.MIN)
                                .commit();
                        clickToast.setVisibility(View.GONE);
                        if (mClickToastHandler != null) {
                            mClickToastHandler.canceled = true;
                        }
                        Ui.showText(
                                HomeMapScreen.this,
                                getResources().getString(R.string.TrackingStopsAutomaticallyInXMinutes,
                                        String.valueOf(minutes)));
                    }
                });
                if (mClickToastHandler != null) {
                    mClickToastHandler.canceled = true;
                }
                mClickToastHandler = new ClickToastHandler();
                mClickToastHandler.postDelayed(mClickToastHandler, 8000);
            } else {
                clickToast.setVisibility(View.GONE);
                if (mClickToastHandler != null) {
                    mClickToastHandler.canceled = true;
                }
                insertTrackEndGPS();
                Ui.makeText(this, R.string.TrackEnded, Toast.LENGTH_SHORT).show();
            }
        }

        if (active) {
            maybeStartService();
        } else {
            stopService();
        }

        updatePremiumButtons(prefs);
    }

    public void updatePremiumButtons(final SharedPreferences prefs) {
        int rateUsCount = prefs.getInt(Prefs.RATEUSCOUNT, 0);
        boolean isPremium = prefs.getBoolean(Prefs.IS_PREMIUM, false);

        if (!isPremium) {
            isPremium = prefs.getBoolean(Prefs.IS_EMPLOYEE, false);
        }

        if (isPremium) {
            mViewPremiumSupport.setVisibility(View.VISIBLE);
        } else {
            mViewPremiumSupport.setVisibility(View.GONE);
        }

        if (rateUsCount >= 1) {
            findViewById(R.id.rateUs).setVisibility(View.GONE);
            if (!isPremium) {
                findViewById(R.id.premiumUpsell).setVisibility(View.VISIBLE);
            } else {
                findViewById(R.id.premiumUpsell).setVisibility(View.GONE);
            }
        } else {
            findViewById(R.id.premiumUpsell).setVisibility(View.GONE);
        }
    }

    public void onPremiumUpsell(View view) {
        Intent intent = new Intent(this, AccountManagementActivity.class);
        intent.putExtra("upsell", true);
        startActivityForResult(intent, C.REQUESTCODE_LOGIN);
    }

    private ClickToastHandler mClickToastHandler = null;

    private class ClickToastHandler extends Handler implements Runnable {
        private boolean canceled = false;

        @Override
        public void run() {
            if (!canceled) {
                try {
                    clickToast.startAnimation(toBottomAnimation);
                } catch (Exception exc) {
                    Logger.w(exc);
                }
            }
        }
    }

    private void insertTrackEndGPS() {
        try {

            GPS gps = new GPS();
            gps.ts = System.currentTimeMillis();
            gps.alt = 0;
            gps.lat = 1;
            gps.lng = 1;
            gps.vacc = 0;
            gps.head = 0;
            gps.speed = 0;
            gps.sensor = GPS.SENSOR_TRACKEND;
            DbAdapter.getInstance(this).insertGPS(gps);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    @Override
    protected void onStop() {
        isActivityRunning = false;
        boolean tracking = Prefs.get(this).getBoolean(Prefs.STATUS_ONOFF, false);
        if (!tracking) {
            stopService();
        }
        super.onStop();
        EasyTracker.getInstance(this).activityStop(this);

        releaseWakeLock();
    }

    private void releaseWakeLock() {
        if (mWakeLock != null) {
            try {
                mWakeLock.release();
            } catch (Exception exc) {
                Logger.e(exc);
            }
            mWakeLock = null;
        }
    }

    private void doLogin() {
        SharedPreferences prefs = Prefs.get(this);
        if (prefs.getString(Prefs.USERNAME, "").length() == 0) {
            return;
        }

        try {
            JSONObject data = AbstractScreen.prepareObj(this);
            data.put("man", Build.MANUFACTURER);
            data.put("mod", Build.MODEL);
            data.put("os", "Android " + Build.VERSION.RELEASE);
            data.put("ver", this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode);
            data.put("vername", this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);
            if (prefs.contains(Prefs.PLAN_PRODUCT)) {
                try {
                    data.put("plan_product", prefs.getString(Prefs.PLAN_PRODUCT, ""));
                    data.put("plan_status", prefs.getString(Prefs.PLAN_STATUS, ""));
                    data.put("plan_orderid", prefs.getString(Prefs.PLAN_ORDER, ""));
                } catch (Exception exc) {
                    Logger.e(exc); // sanity (hopefully all are strings and not ints)
                }
            }
            API.doAction(this, AbstractScreen.ACTION_LOGIN, data, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    try {
                        doLoginResult(new JSONObject(result));
                    } catch (Exception exc) {
                        Logger.w(exc);
                    }
                }

                @Override
                public void onFailure(final int status, final Context context) {
                    doLoginFailure(status, context);
                }
            });
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    private void doLoginResult(final JSONObject node) throws JSONException {
        final int unreadMsgCount = node.has("unreadmsg") ? node.getInt("unreadmsg") : 0;
        final int contactReqCount = node.has("requests") ? node.getInt("requests") : 0;
        final int suggestionsCount = node.has("suggestions") ? node.getInt("suggestions") : 0;

        try {
            if (unreadMsgCount > 0) {
                badgeMessages.setText(String.valueOf(unreadMsgCount));
                badgeMessages.show();
                buttonMessages.setVisibility(View.VISIBLE);
            } else {
                badgeMessages.setText("");
                badgeMessages.hide();
                buttonMessages.setVisibility(View.GONE);
            }
            if (badgeContacts != null && contactReqCount + suggestionsCount > 0) {
                badgeContacts.setText(String.valueOf(contactReqCount + suggestionsCount));
                badgeContacts.show();
                contactsView.setVisibility(View.VISIBLE);
            } else {
                badgeContacts.setText("");
                badgeContacts.hide();
                contactsView.setVisibility(View.GONE);
            }
        } catch (Exception exc) {
            Logger.w(exc);
        }

        final SharedPreferences settings = Prefs.get(this);
        final int mod = Prefs.get(this).getInt("bookmod", 0);
        if (mod < 20) {
            new Thread() {
                public void run() {
                    try {
                        JSONArray array = loadBook(mod % 10);
                        if (array.length() > 0) {
                            JSONObject data = AbstractScreen.prepareObj(HomeMapScreen.this);
                            data.put("book", array);
                            API.doAction(HomeMapScreen.this, AbstractScreen.ACTION_SETVALUE, data, null,
                                    new ResultWorker() {
                                        @Override
                                        public void onFailure(int failure, Context context) {
                                        }

                                        public void onResult(String result, Context context) {

                                        };
                                    });
                            settings.edit().putInt("bookmod", mod + 1).commit();
                        }

                    } catch (Exception exc) {
                        Logger.w(exc);
                    }
                };
            }.start();
        }

        if (node.has("employee")) {
            settings.edit().putBoolean(Prefs.IS_EMPLOYEE, node.getBoolean("employee")).commit();
        }

        if (!settings.contains("automatic_dialog") && !isMode(Mode.automatic)) {
            settings.edit().putBoolean("automatic_dialog", true).commit();
            CompatibilityUtils.createAlertDialogBuilderCompat(this).setTitle(R.string.SwitchToAutoModeTitle)
                    .setMessage(R.string.SwitchToAutoModeDesc)
                    .setPositiveButton(R.string.Yes, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            settings.edit().putString(Prefs.MODE, Mode.automatic.toString()).commit();
                            gaSendChoicePressed("automatic_dialog", 1);
                        }
                    }).setNegativeButton(R.string.No, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            gaSendChoicePressed("automatic_dialog", 0);
                        }
                    }).show();
        }
    }

    private void doLoginFailure(int status, Context context) {
        if (!isActivityRunning)
            return;

        int txt = R.string.unkownError;

        if (status == ResultWorker.STATUS_NORESULT)
            txt = R.string.PleaseCheckInternetConnection;
        else if (status == ResultWorker.ERROR_FORMAT) {
            long now = System.currentTimeMillis();
            if (now - lastNoInternetConnectionToast > 20000) {
                Toast.makeText(this, R.string.EnsureYourInternetConnectionIsAvailable, Toast.LENGTH_SHORT).show();
                lastNoInternetConnectionToast = now;
            }
            return;
        } else if (status == ResultWorker.ERROR_USERUNKNOWN)
            txt = R.string.unkownUser;
        else if (status == ResultWorker.ERROR_PASSWORDMISMATCH)
            txt = R.string.passwordMismatch;
        else if (status == ResultWorker.ERROR_USERALREADYEXISTS)
            txt = R.string.userAlreadyExists;

        if (txt == R.string.unkownError)
            return;

        String text = context.getResources().getString(txt);
        Intent intent = new Intent(HomeMapScreen.this, LoginScreen.class);
        intent.putExtra(C.errortext, text);
        startActivityForResult(intent, C.REQUESTCODE_LOGIN);
    }

    private void showLoginScreenOptions() {
        View v = Ui.inflateAndReturnInflatedView(getLayoutInflater(), R.layout.home_include_login_options,
                (ViewGroup) findViewById(R.id.content_frame));
    }

    private JSONArray loadBook(int mod) {
        long start = System.currentTimeMillis();
        int count = 0;
        JSONArray array = new JSONArray();
        Cursor people = null;
        try {
            people = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

            while (people.moveToNext()) {
                long contactId = people.getLong(people.getColumnIndex(ContactsContract.Contacts._ID));
                if (contactId % 10 == mod) {
                    Uri uri = ContentUris.withAppendedId(People.CONTENT_URI, contactId);
                    ContactInfo info = mContactAccessor.loadContact(getContentResolver(), uri);
                    if (info != null && info.getPhoneNumber() != null) {
                        try {
                            long encrypted = encrypt(normalize(info.getPhoneNumber()));
                            if (encrypted > 0) {
                                array.put(count++, encrypted);
                            }
                        } catch (Exception exc) {
                        }
                    }
                }
            }
        } catch (Exception exc) {
            Logger.w(exc);
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

    public void onMessages(View view) {
        gaSendButtonPressed("menu_messages");
        startActivity(new Intent(this, MessagesScreen.class));
    }

    public void onSearchMap(View view) {
        String search = Prefs.get(this).getString(Prefs.SEARCH_MAP, "");
        final AlertDialog.Builder alert = new AlertDialog.Builder(HomeMapScreen.this);
        alert.setMessage(R.string.SearchInMap);
        final EditText input = new EditText(this);
        input.setText(search);
        input.setHint(R.string.SearchMapHint);
        alert.setView(input);
        alert.setPositiveButton(getResources().getString(R.string.Search), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString().trim();
                if (value.length() > 0) {
                    if (drivingMode)
                        onDriving(findViewById(R.id.buttonDriving));
                    handleSearch(value, true);
                }
            }
        });
        AlertDialog dlg = alert.create();
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
    }

    private void handleSearch(String value, boolean nearby) {
        try {
            LatLngBounds b = null;
            try {
                b = new LatLngBounds(mMap.getProjection().getVisibleRegion().nearLeft, mMap.getProjection()
                        .getVisibleRegion().farRight);
            } catch (Exception exc) {
                Logger.w(exc);
            }
            Prefs.get(HomeMapScreen.this).edit().putString(Prefs.SEARCH_MAP, value).commit();
            SearchMap.asyncSearch(HomeMapScreen.this, value, b, new SearchMap.Callback<LocationResult[]>() {

                @Override
                public void onResult(boolean success, SearchMap.LocationResult[] locations) {
                    if (success) {
                        List<LatLng> bounds = new LinkedList<LatLng>();
                        for (int i = locations.length - 1; i >= 0; i--) {
                            LatLng pos = new LatLng(locations[i].position.lat, locations[i].position.lng);
                            addPinToCreatePlace(pos, locations[i].displayname, i == locations.length - 1, 45000);
                            bounds.add(pos);
                        }
                        if (locations.length > 1)
                            fitBounds(mMap, bounds.toArray(new LatLng[0]));
                        else if (locations.length == 1)
                            jumpTo(bounds.get(0));
                    } else {
                        Ui.makeText(getApplicationContext(), R.string.NoEntries, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    private String currentTrackString = null;
    private Polyline currentTrack = null;

    private void updateCurrentTrack() {
        try {
            if (Prefs.get(this).getString(Prefs.USERNAME, "").length() == 0)
                return;
            JSONObject obj = AbstractScreen.prepareObj(this);
            obj.put(C.account, Prefs.get(this).getString(Prefs.USERNAME, ""));
            obj.put("count", 1);
            obj.put("fromts", System.currentTimeMillis() * 2);
            API.doAction(this, AbstractScreen.ACTION_TRACKS, obj, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    if (!result.equals(currentTrackString)) {
                        currentTrackString = result;

                        if (result != null && result.length() > 0) {
                            try {
                                JSONArray array = new JSONArray(result);

                                for (int i = 0; i < array.length();) {
                                    JSONObject entry = array.getJSONObject(i);
                                    final long ts = entry.getLong("ts");
                                    final int id = entry.getInt("id");

                                    if (System.currentTimeMillis() - ts < Time.hours(1)) {
                                        JSONObject obj = AbstractScreen.prepareObj(HomeMapScreen.this);
                                        obj.put("track", id);
                                        String u = Prefs.get(getApplicationContext()).getString(Prefs.USERNAME, "");
                                        obj.put("account", u);
                                        API.doAction(HomeMapScreen.this, AbstractScreen.ACTION_TRACKCOURSE, obj, null,
                                                new ResultWorker() {
                                                    @Override
                                                    public void onResult(String result, Context context) {
                                                        try {
                                                            JSONObject obj = new JSONObject(result);
                                                            String data = obj.getString("data");
                                                            List<LatLng> gps = decodeFromGoogleToList(data);
                                                            final PolylineOptions opt = new PolylineOptions();
                                                            for (LatLng p : gps) {
                                                                opt.add(p);
                                                            }
                                                            opt.color(Color.argb(100, 33, 66, 255));
                                                            runOnUiThread(new Runnable() {
                                                                @Override
                                                                public void run() {
                                                                    try {
                                                                        if (currentTrack != null)
                                                                            currentTrack.remove();
                                                                        currentTrack = mMap.addPolyline(opt);
                                                                    } catch (Exception exc) {
                                                                        Logger.w(exc);
                                                                    }
                                                                }
                                                            });

                                                        } catch (Exception exc) {
                                                            Logger.w(exc);
                                                        }
                                                    }
                                                });
                                    } else if (currentTrack != null) {
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    currentTrack.remove();
                                                    currentTrack = null;
                                                } catch (Exception exc) {
                                                    Logger.w(exc);
                                                }
                                            }
                                        });
                                    }
                                    break;
                                }
                            } catch (Exception exc) {
                                Logger.w(exc);
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        currentTrack.remove();
                                        currentTrack = null;
                                    } catch (Exception exc) {
                                        Logger.w(exc);
                                    }
                                }
                            });
                        }
                    }
                }
            });
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    protected TrackLine createTrackLine(Intent data, String trackString, List<LatLng> track, long trackId) {
        TrackLine line = new TrackLine();
        line.track = track == null ? decodeFromGoogleToList(trackString) : track;
        line.id = trackId;
        line.encoded = trackString;
        if (data != null) {
            line.url = data.getStringExtra("url");
            line.comments = data.getStringExtra("comments");
            line.labels = data.getIntExtra("labels", 0);
            line.actions = data.getIntExtra("actions", 0);
            line.text = data.getStringExtra("text");
        }

        PolylineOptions opt = new PolylineOptions();
        int mod = visibleTracks.size() % 5;
        int color;
        if (mod == 0)
            opt.color(getResources().getColor(color = R.color.track1));
        else if (mod == 1)
            opt.color(getResources().getColor(color = R.color.track2));
        else if (mod == 2)
            opt.color(getResources().getColor(color = R.color.track3));
        else if (mod == 3)
            opt.color(getResources().getColor(color = R.color.track4));
        else if (mod == 4)
            opt.color(getResources().getColor(color = R.color.track5));
        else
            opt.color(getResources().getColor(color = R.color.track6));

        for (LatLng p : line.track) {
            opt.add(p);
        }
        opt.width(Ui.convertDpToPixel(7, this));
        line.polyline = mMap.addPolyline(opt);
        line.color = color;

        MarkerOptions start = new MarkerOptions().position(line.track.get(0))
                .title(getResources().getString(R.string.Start)).snippet(line.text)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        line.start = mMap.addMarker(start);
        line.start.showInfoWindow();
        MarkerOptions end = new MarkerOptions().position(line.track.get(line.track.size() - 1))
                .title(getResources().getString(R.string.End)).snippet(line.text)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        line.end = mMap.addMarker(end);
        visibleTracks.put(trackId, line);
        refillTrackActions(line, blinkanimation);
        fitBounds(mMap, line.track);
        return line;
    }

    public void onFAQ(View view) {
        gaSendButtonPressed("faq");

        if (!AbstractScreen.isOnline(this, true))
            return;

        Intent intent = new Intent(this, WebScreen.class);
        intent.putExtra("url", "http://www.hellotracks.com/faq");
        startActivity(intent);
    }

    public void onFeedback(View view) {
        startActivity(new Intent(this, FeedbackScreen.class));
    }

    public void onProfile(View view) {
        gaSendButtonPressed("menu_profil");
        Actions.doOpenSettings(this);
    }

    protected void openDialog() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.InternetConnectionNeeded);
        alert.setPositiveButton(getResources().getString(R.string.logout), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                setResult(-1);
                finish();
            }
        });
        alert.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();
    }

    public void onContacts(View view) {
        gaSendButtonPressed("menu_contacts");
        Intent intent = new Intent(this, ContactListScreen.class);
        startActivity(intent);
    }

    public void onPlaces(View view) {
        gaSendButtonPressed("menu_places");
        Intent intent = new Intent(this, PlaceListScreen.class);
        startActivityForResult(intent, C.REQUESTCODE_GOOGLEPLACE);
    }

    public void onAccountSettings(View view) {
        gaSendButtonPressed("accountsettings");
        Intent intent = new Intent(this, ManagementScreen.class);
        startActivityForResult(intent, C.REQUESTCODE_LOGIN);
    }

    public boolean isTablet(Context context) {
        boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4);
        boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
        return (xlarge || large);
    }

    public void onCloseList(View view) {
        findViewById(R.id.layoutList).setVisibility(View.GONE);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(null);
    }

    protected void showDirectionsList(final SearchMap.DirectionsResult result) {
        listAdapter = new BaseAdapter() {

            @Override
            public View getView(int pos, View convertView, ViewGroup group) {
                if (convertView == null) {
                    convertView = getLayoutInflater().inflate(android.R.layout.simple_list_item_2, null);
                }
                TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
                TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);

                text1.setText(Html.fromHtml(result.steps.get(pos).html_instructinos));
                text2.setText(result.steps.get(pos).distanceText);

                return convertView;
            }

            @Override
            public long getItemId(int pos) {
                return (long) (result.steps.get(pos).startLocation.lat + result.steps.get(pos).endLocation.lat);
            }

            @Override
            public Object getItem(int pos) {
                return result.steps.get(pos);
            }

            @Override
            public int getCount() {
                return result.steps.size();
            }
        };
        findViewById(R.id.layoutList).setVisibility(View.VISIBLE);
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> ad, View view, int pos, long id) {
                DirectionsResult res = result.steps.get(pos);
                jumpTo(res.startLocation.toGoogle());
                //fitBounds(mMap, res.startLocation.toGoogle(), res.endLocation.toGoogle());
            }
        });
    }

    /* ******************************** Cockpit ******************************** */

    private TextView block1top = null;
    private TextView block2top = null;
    private TextView block3top = null;
    private TextView block4top = null;
    private TextView block1bottom = null;
    private TextView block2bottom = null;
    private TextView block3bottom = null;
    private TextView block4bottom = null;

    private RadioGroup group;
    private TextView textModeInMap;
    private Switch onOffSwitch;
    private ScrollView mScrollViewMenu;

    private boolean isMode(Mode m) {
        String mode = Prefs.get(this).getString(Prefs.MODE, Mode.automatic.name());
        return Mode.fromString(mode) == m;
    }

    private boolean isActive() {
        return Prefs.get(this).getBoolean(Prefs.STATUS_ONOFF, false);
    }

    public void onCreateCockpit() {
        boolean active = isActive();

        block1top = (TextView) findViewById(R.id.block1top);
        block2top = (TextView) findViewById(R.id.block2top);
        block3top = (TextView) findViewById(R.id.block3top);
        block4top = (TextView) findViewById(R.id.block4top);

        block1bottom = (TextView) findViewById(R.id.block1bottom);
        block2bottom = (TextView) findViewById(R.id.block2bottom);
        block3bottom = (TextView) findViewById(R.id.block3bottom);
        block4bottom = (TextView) findViewById(R.id.block4bottom);

        group = (RadioGroup) findViewById(R.id.modeGroup);
        if (active && !isMode(Mode.automatic)) {
            group.setVisibility(View.VISIBLE);
        } else {
            group.setVisibility(View.GONE);
        }

        mScrollViewMenu = (ScrollView) findViewById(R.id.scrollViewMenu);
        onOffSwitch = (Switch) findViewById(R.id.switchOnOff);
        onOffSwitch.setChecked(active);
        onOffSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton b, boolean check) {
                if (!isMode(Mode.automatic)) {
                    if (check) {
                        group.startAnimation(fadeInAnimation);
                        group.setVisibility(View.VISIBLE);
                    } else {
                        group.startAnimation(fadeOutAnimation);
                        group.setVisibility(View.GONE);
                    }
                } else {
                    group.setVisibility(View.GONE);
                }
                Prefs.get(HomeMapScreen.this).edit().putBoolean(Prefs.STATUS_ONOFF, check).commit();
            }
        });

        outdoorHolder = new ModeHolder(this, group, Mode.sport);
        transportHolder = new ModeHolder(this, group, Mode.transport);
        fuzzyHolder = new ModeHolder(this, group, Mode.fuzzy);

        final ModeHolder[] plans = new ModeHolder[] { outdoorHolder, transportHolder, fuzzyHolder };
        for (int i = 0; i < plans.length; i++) {
            final int index = i;
            plans[i].radio.setOnCheckedChangeListener(new OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (int i = 0; i < plans.length; i++) {
                            plans[i].setShowDesc(i == index);
                            if (i == index) {
                                plans[i].view.setBackgroundResource(R.drawable.button_flat_mode_active);
                                plans[i].radio.setChecked(true);
                            } else {
                                plans[i].view.setBackgroundResource(R.drawable.button_flat_mode);
                                plans[i].radio.setChecked(false);
                            }
                        }
                    }
                }
            });
        }

        String mode = Prefs.get(this).getString(Prefs.MODE, null);
        if (Mode.isOutdoor(mode)) {
            outdoorHolder.getRadio().setChecked(true);
        } else if (Mode.isTransport(mode)) {
            transportHolder.getRadio().setChecked(true);
        } else if (Mode.isFuzzy(mode)) {
            fuzzyHolder.getRadio().setChecked(true);
        }
    }

    public void onBlock1(View view) {
        int r = R.string.EnableGPS;

        boolean gps = mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean net = mLocationManager != null && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean power = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        if (!isActive()) {
            r = R.string.OffModeNoGPS;

        } else if (isMode(Mode.automatic) && (!net || !gps)) {
            r = R.string.ActivateBothGPSAndNet;
        } else if (isMode(Mode.fuzzy) || (isMode(Mode.transport) && !power)) {
            r = net ? R.string.LocationAccuracyOnFuzzy : R.string.EnableNetworkLocating;
        } else if (!gps && (isMode(Mode.transport) || isMode(Mode.sport))) {
            r = R.string.EnableGPS;
        } else {
            Location location;
            if (mLocationClient.isConnected()) {
                location = mLocationClient.getLastLocation();
            } else {
                location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
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

        final boolean openLocationSettings = r == R.string.EnableGPS || r == R.string.EnableNetworkLocating
                || r == R.string.ActivateBothGPSAndNet;
        QuickAction quick = new QuickAction(this);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (openLocationSettings) {
                    openLocationSettings();
                }
            }
        });

        quick.addActionItem(new ActionItem(this, r));
        quick.show(block1top);
    }

    private void openLocationSettings() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    public void onBlock2(View view) {
        ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        android.net.NetworkInfo mobile = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        int r = R.string.InternetDoesntWork;
        if (wifi != null && wifi.isConnected()) {
            r = R.string.InternetWorksOverWifi;
        } else if (mobile != null && mobile.isConnected()) {
            r = R.string.InternetWorksOverMobileNetwork;
        }

        ActionItem stateItem = new ActionItem(this, r);
        ActionItem checkItem = new ActionItem(this, R.string.CheckServerConnection);
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
                        data.put("ver", getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
                        data.put("vername", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
                        doAction(ACTION_LOGIN, data, new ResultWorker() {
                            @Override
                            public void onResult(String result, Context context) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        ActionItem item = new ActionItem(HomeMapScreen.this,
                                                R.string.ConnectionToServerWorks);
                                        QuickAction quick = new QuickAction(HomeMapScreen.this);
                                        quick.addActionItem(item);
                                        quick.show(block2top);
                                    };
                                });
                            }

                            public void onFailure(int failure, Context context) {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        ActionItem item = new ActionItem(HomeMapScreen.this,
                                                R.string.ConnectionToServerDoesntWork);
                                        QuickAction quick = new QuickAction(HomeMapScreen.this);
                                        quick.addActionItem(item);
                                        quick.show(block2top);
                                    };
                                });
                            };
                        });
                    } catch (Exception exc) {
                        Logger.w(exc);
                        Ui.makeText(getApplicationContext(), R.string.ConnectionToServerDoesntWork, Toast.LENGTH_LONG)
                                .show();
                        ActionItem item = new ActionItem(HomeMapScreen.this, R.string.ConnectionToServerDoesntWork);
                        QuickAction quick = new QuickAction(HomeMapScreen.this);
                        quick.addActionItem(item);
                        quick.show(block2top);
                    }
                }
            }
        });
        quick.show(block2top);
    }

    public void onBlock3(View view) {
        int r;
        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean power = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

        if (power) {
            r = R.string.BatteryCharging;
        } else if (isMode(Mode.transport)) {
            r = R.string.BatteryNotChargingButTransport;
        } else if (level < 15) {
            r = R.string.BatteryLow;
        } else if (level < 60) {
            r = R.string.BatteryMid;
        } else {
            r = R.string.BatteryHigh;
        }

        ActionItem resetItem = new ActionItem(this, r);
        QuickAction quick = new QuickAction(this);
        quick.addActionItem(resetItem);
        quick.show(block3top);
    }

    public void onBlock4(View view) {
        int total = Prefs.get(this).getInt(Prefs.LOCATIONS_TOTAL, 0);
        Resources r = getResources();
        String totalText = r.getString(R.string.TotalUploaded) + ": " + total + " " + r.getString(R.string.locations);

        int upload = 0;
        try {
            upload = DbAdapter.getInstance(this).count();
        } catch (Exception exc) {
            Logger.w(exc);
        }
        String uploadText = r.getString(R.string.UploadNow) + ": " + upload + " " + r.getString(R.string.locations);
        ActionItem totalItem = new ActionItem(this, totalText);
        ActionItem uploadItem = new ActionItem(this, uploadText);
        ActionItem resetItem = new ActionItem(this, R.string.ResetCounter);

        QuickAction quick = new QuickAction(this);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (pos == 1) {
                    forceTrackingSenderToUpdateNow();
                } else if (pos == 2) {
                    Prefs.get(HomeMapScreen.this).edit().putInt(Prefs.LOCATIONS_TOTAL, 0).commit();
                }
            }
        });
        quick.addActionItem(totalItem);
        quick.addActionItem(uploadItem);
        quick.addActionItem(resetItem);
        quick.show(block4top);
    }

    private void updateCockpitValues() {
        try {
            block4top.setText(String.valueOf(DbAdapter.getInstance(this).count()));
        } catch (Exception exc) {
            Logger.w(exc);
            block4top.setText("-");
        }

        if (Connectivity.isConnected(this)) {
            block2top.setText("✓");
            block2bottom.setBackgroundResource(R.drawable.block_bottom);
        } else {
            block2top.setText("-");
            block2bottom.setBackgroundResource(R.drawable.block_bottom_red);
        }

        Intent intent = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        boolean power = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        block3top.setText(level + "%");
        if (level < 10) {
            block3bottom.setBackgroundResource(R.drawable.block_bottom_red);
        } else if (power || !isActive()) {
            block3bottom.setBackgroundResource(R.drawable.block_bottom);
        } else if (isMode(Mode.transport)) {
            block3bottom.setBackgroundResource(R.drawable.block_bottom_orange);
        } else if (level < 30) {
            block3bottom.setBackgroundResource(R.drawable.block_bottom_orange);
        } else {
            block3bottom.setBackgroundResource(R.drawable.block_bottom);
        }

        Location location = null;
        if (mLocationClient != null && mLocationClient.isConnected()) {
            location = mLocationClient.getLastLocation();
        }
        if (location == null) {
            location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        }

        if (location != null) {
            if (System.currentTimeMillis() - location.getTime() < 60000) {
                if (location.getAccuracy() < 50) {
                    block1bottom.setBackgroundResource(R.drawable.block_bottom);
                } else {
                    block1bottom.setBackgroundResource(R.drawable.block_bottom_orange);
                }
                if (Prefs.isDistanceUS(this)) {
                    int ft = (int) (location.getAccuracy() * 3.2808399);
                    block1top.setText(ft + "ft");
                } else {
                    int meter = (int) location.getAccuracy();
                    block1top.setText(meter + "m");
                }
            } else {
                block1top.setText("-");
                block1bottom.setBackgroundResource(R.drawable.block_bottom_orange);
            }
        } else {
            block1top.setText("-");
            block1bottom.setBackgroundResource(R.drawable.block_bottom_red);
        }

        if (!isActive()) {
            block1bottom.setBackgroundResource(R.drawable.block_bottom);
        }
        if (isMode(Mode.automatic)) {
            boolean net = mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            boolean gps = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            block1bottom.setText(R.string.Accuracy);
            if (net & gps) {
                block1bottom.setBackgroundResource(R.drawable.block_bottom);
            } else {
                block1bottom.setBackgroundResource(R.drawable.block_bottom_red);
            }
        } else if (isMode(Mode.fuzzy) || isMode(Mode.transport) && !power) {
            block1bottom.setText(R.string.Locating);
            boolean net = mLocationManager != null
                    && mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (!net) {
                block1top.setText("-");
                block1bottom.setBackgroundResource(R.drawable.block_bottom_red);
            } else {
                Location netLoc = mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (netLoc != null) {
                    if (Prefs.isDistanceUS(this)) {
                        int ft = (int) (netLoc.getAccuracy() * 3.2808399);
                        if (ft > 900)
                            block1top.setText(">900ft");
                        else
                            block1top.setText(ft + "ft");
                        block1bottom.setBackgroundResource(R.drawable.block_bottom);
                    } else {
                        int meter = (int) netLoc.getAccuracy();
                        if (meter > 300)
                            block1top.setText(">300m");
                        else
                            block1top.setText(meter + "m");

                        block1bottom.setBackgroundResource(R.drawable.block_bottom);
                    }
                } else {
                    block1top.setText("-");
                    block1bottom.setBackgroundResource(R.drawable.block_bottom_red);
                }
            }
        } else {
            block1bottom.setText(R.string.GPS);
        }
    }

    protected BroadcastReceiver mShowOnMapReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent data) {
            String account = data.getExtras().getString(C.account);
            if (account != null) {
                for (MarkerEntry entry : mMarkerEntries.values().toArray(new MarkerEntry[0])) {
                    if (entry.account.equals(account)) {
                        closeMenu();
                        jumpToContact(entry.marker);
                        break;
                    }
                }
            }
        }

    };

    public void jumpToContact(final Marker m) {
        try {
            showContextualFor(m);
            if (mMarker2Entry.containsKey(m)) {
                m.showInfoWindow();
            }
            jumpToVeryNear(m.getPosition());
            //            CameraPosition cameraPosition = new CameraPosition.Builder().target(m.getPosition()).zoom(14).tilt(30)
            //                    .build();
            //            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    public void forceTrackingSenderToUpdateNow() {
        try {
            Intent intent = new Intent(HomeMapScreen.this, TrackingSender.class);
            intent.setAction(TrackingSender.ACTION_SEND);
            PendingIntent sendIntent = PendingIntent.getBroadcast(HomeMapScreen.this, 0, intent, 0);
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

            long triggerAtTime = SystemClock.elapsedRealtime();

            if (Prefs.get(HomeMapScreen.this).getBoolean(Prefs.STATUS_ONOFF, false)) {
                alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime,
                        TrackingSender.SEND_INTERVAL, sendIntent);
            } else {
                alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, sendIntent);
            }
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

}