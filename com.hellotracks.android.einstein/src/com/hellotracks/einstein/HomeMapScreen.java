package com.hellotracks.einstein;

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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
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
import android.provider.Contacts.People;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.Html;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.actionbarsherlock.view.SubMenu;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.hellotracks.Log;
import com.hellotracks.Mode;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.activities.AbstractMapScreen;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.ChangeUserScreen;
import com.hellotracks.activities.TrackListScreen;
import com.hellotracks.activities.WebScreen;
import com.hellotracks.c2dm.C2DMReceiver;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.types.GPS;
import com.hellotracks.ui.SettingsActivity;
import com.hellotracks.util.Async;
import com.hellotracks.util.BadgeView;
import com.hellotracks.util.ContactAccessor;
import com.hellotracks.util.ContactInfo;
import com.hellotracks.util.ImageCache;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.SearchMap.DirectionsResult;
import com.hellotracks.util.SearchMap.LocationResult;
import com.hellotracks.util.Time;
import com.hellotracks.util.Ui;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;
import com.jeremyfeinstein.slidingmenu.lib.SlidingMenu;
import com.squareup.picasso.Picasso;

import de.greenrobot.event.EventBus;

public class HomeMapScreen extends AbstractMapScreen {

    private Timer timer;
    private boolean isActivityRunning = false;
    private Animation blinkanimation;

    private String lastMarkers = null;
    private BadgeView badgeMessages;
    private BadgeView badgeContacts;
    private View buttonMessages;

    private long lastNoInternetConnectionToast = 0;
    private long mLastDrivingViewSwitch = 0;

    private MenuItem mMenuItemDriving;
    private MenuItem mMenuItemCloseApp;

    private SharedPreferences.OnSharedPreferenceChangeListener prefChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

        @Override
        public void onSharedPreferenceChanged(final SharedPreferences prefs, final String key) {
            if (Prefs.STATUS_ONOFF.equals(key) || Prefs.MODE.equals(key)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateButtons(prefs, Prefs.STATUS_ONOFF.equals(key));
                    }
                });
            }
        }
    };

    private class UpdateTimeTask extends TimerTask {

        int count = 0;

        public void run() {
            updateUnsetWaypoints();
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    updateCockpitValues();
                }
            });

            if (Prefs.get(HomeMapScreen.this).getString(Prefs.USERNAME, "").length() == 0) {
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
                break;
            }
            count++;

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
                                        .target(new LatLng(loc.getLatitude(), loc.getLongitude())).zoom(18).tilt(70)
                                        .bearing(mLastBearing).build();
                                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                                if (Prefs.isDistanceUS(HomeMapScreen.this)) {
                                    textSpeed.setText((int) (loc.getSpeed() * 2.23694) + " mph");
                                } else {
                                    textSpeed.setText((int) (loc.getSpeed() * 3.6) + " km/h");
                                }
                            }
                        } catch (Exception exc) {
                            Log.w(exc);
                        }
                    }
                });
            }
        }
    }

    private float mLastBearing = 0;

    protected void onStart() {
        super.onStart();
        isActivityRunning = true;
        FlurryAgent.onStartSession(this, "3TJ7YYSYK4C4HB983H27");
        registerReceiver(trackReceiver, new IntentFilter(C.BROADCAST_ADDTRACKTOMAP));
    };

    private BaseAdapter listAdapter;

    public void onEvent(final SearchMap.DirectionsResult result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<LatLng> track = new LinkedList<LatLng>();
                for (DirectionsResult step : result.steps) {
                    track.addAll(decodeFromGoogleToList(step.track));
                }
                TrackLine line = createTrackLine(null, result.track, track, -System.currentTimeMillis());
                line.result = result;
                showDirectionsList(result);
                showDirectionsInfo(result);
            }
        });
    }

    protected void onResume() {
        AbstractScreen.isOnline(this, true);

        String username = Prefs.get(this).getString(Prefs.USERNAME, "");
        if (username.length() > 0) {
            updateButtons(Prefs.get(this), false);
        } else {
            AlertDialog.Builder b = new AlertDialog.Builder(this).setMessage(R.string.NotLoggedInText)
                    .setPositiveButton(R.string.Start, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            final AlertDialog.Builder alert = new AlertDialog.Builder(HomeMapScreen.this);
                            alert.setMessage(R.string.PleaseEnterNameFirst);
                            final EditText input = new EditText(HomeMapScreen.this);
                            input.setHint(R.string.Name);
                            alert.setView(input);
                            alert.setPositiveButton(getResources().getString(R.string.OK),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            String name = input.getText().toString().trim();
                                            ChangeUserScreen.doLoginDevice(HomeMapScreen.this, getLastLocation(),
                                                    false, name);
                                        }
                                    });
                            AlertDialog dlg = alert.create();
                            dlg.setCanceledOnTouchOutside(true);
                            dlg.show();

                        }
                    });
            if (Prefs.get(this).getLong(Prefs.LAST_LOGOUT, 0) > 0) {
                b.setNegativeButton(R.string.CloseApp, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {
                        realLogout();
                    }
                });
            }
            b.setCancelable(false).create().show();
        }

        timer = new Timer();
        timer.schedule(new UpdateTimeTask(), 4000, 5000);

        super.onResume();
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
        Prefs.get(this).unregisterOnSharedPreferenceChangeListener(prefChangeListener);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    public void activate() {
        Log.i("activating");
        Prefs.get(this).edit().putBoolean(Prefs.STATUS_ONOFF, true).commit();
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
                }
            }
        }

    };

    private SlidingMenu mSlidingMenu;
    private Switch mPowerSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        blinkanimation = new AlphaAnimation(1, 0);
        blinkanimation.setDuration(300);
        blinkanimation.setInterpolator(new LinearInterpolator());
        blinkanimation.setRepeatCount(3);
        blinkanimation.setRepeatMode(Animation.REVERSE);

        C2DMReceiver.refreshAppC2DMRegistrationState(getApplicationContext());
        
       

        setContentView(R.layout.screen_homemap);

        setUpMapIfNeeded();
        setupActionBar();

        mSlidingMenu = new SlidingMenu(this);
        mSlidingMenu.setTouchModeAbove(SlidingMenu.TOUCHMODE_MARGIN);
        mSlidingMenu.setFadeDegree(0.35f);
        mSlidingMenu.setMode(SlidingMenu.LEFT);
        mSlidingMenu.attachToActivity(this, SlidingMenu.SLIDING_CONTENT);
        mSlidingMenu.setShadowWidthRes(R.dimen.shadow_width);
        mSlidingMenu.setMenu(R.layout.screen_sidemenu);
        int screenWidth = Ui.getScreenWidth(this);
        int slideWitdh = Ui.convertDpToPixel(200, this);
        Log.w("screen=" + screenWidth);
        Log.w("slide=" + slideWitdh);
        mSlidingMenu.setBehindWidth(getResources().getDimensionPixelSize(R.dimen.slidingmenu_width));

        mPowerSwitch = (Switch) findViewById(R.id.switchPower);
        mPowerSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                FlurryAgent.logEvent("OnOff");
                Prefs.get(HomeMapScreen.this).edit().putBoolean(Prefs.STATUS_ONOFF, isChecked).commit();
            }
        });

        Prefs.get(this).registerOnSharedPreferenceChangeListener(prefChangeListener);

        if (Prefs.get(this).getBoolean(Prefs.ACTIVATE_ON_LOGIN, false)
                || Prefs.get(this).getString(Prefs.MODE, null) == null) {
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
                    Prefs.get(HomeMapScreen.this).edit().putString(Prefs.MODE, m).commit();
                    maybeStartService();
                }
            });
        }

        lastMarkers = Prefs.get(this).getString(createMarkerCacheId(), null);
        if (lastMarkers != null) {
            updateMap(lastMarkers);
        }

        refillMap();
        refillContactList();

        buttonMessages = findViewById(R.id.buttonMessages);
        badgeMessages = new BadgeView(this, buttonMessages);

        drivingButton = (ImageButton) findViewById(R.id.buttonDriving);
        textSpeed = (TextView) findViewById(R.id.textSpeed);

        showMyLocation();

        try {
            EventBus.getDefault().register(this, SearchMap.DirectionsResult.class);
        } catch (Throwable t) {
        }

        onCreateCockpit();
    }

    private LinearLayout container;

    protected void setupActionBar() {
        getSupportActionBar().show();
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setLogo(R.drawable.ic_menu);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.header_bg));
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setCustomView(R.layout.view_contactsscroller);
        container = (LinearLayout) getSupportActionBar().getCustomView().findViewById(R.id.contactsContainer);
    }

    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            toggleMenu();
            break;
        }
        return true;
    };

    protected void toggleMenu() {
        mSlidingMenu.toggle(true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu bar) {
        boolean large = Ui.convertPixelsToDp(Ui.getScreenWidth(this), this) > 400;

        MenuItem searchItem = null;

        if (large) {
            searchItem = bar.add(1, Menu.NONE, Menu.NONE, R.string.Search);
        }

        SubMenu mainMenu = bar.addSubMenu(R.string.Menu);
        MenuItem subMenuItem = mainMenu.getItem();
        subMenuItem.setIcon(R.drawable.ic_action_more);
        subMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        if (!large) {
            searchItem = mainMenu.add(1, Menu.NONE, Menu.NONE, R.string.Search);
        }

        SubMenu advancedMenu = mainMenu.addSubMenu(R.string.Tools);
        advancedMenu.setIcon(R.drawable.ic_action_settings);

        searchItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        searchItem.setIcon(R.drawable.ic_action_search);
        searchItem.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                onSearchMap(null);
                return false;
            }
        });

        MenuItem advancedMenuItem = advancedMenu.getItem();
        advancedMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        SubMenu helpMenu = mainMenu.addSubMenu(R.string.HelpAndFAQ);
        MenuItem helpMenuItem = helpMenu.getItem();
        helpMenuItem.setIcon(R.drawable.ic_action_help);
        helpMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        {
            final MenuItem item = advancedMenu.add(2, Menu.NONE, Menu.NONE, R.string.Emergency);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_attention);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    startActivity(new Intent(HomeMapScreen.this, PanicInfoScreen.class));
                    return false;
                }
            });
        }

        {
            final MenuItem item = advancedMenu.add(2, Menu.NONE, Menu.NONE, R.string.RemoteActivation);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_remoteactivation);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    startActivity(new Intent(HomeMapScreen.this, RemoteActivationInfoScreen.class));
                    return false;
                }
            });
        }

        {
            final MenuItem item = advancedMenu.add(2, Menu.NONE, Menu.NONE, R.string.PublicUrl);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_world);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    startActivity(new Intent(HomeMapScreen.this, PublicUrlInfoScreen.class));
                    return false;
                }
            });
        }

        {
            final MenuItem item = advancedMenu.add(2, Menu.NONE, Menu.NONE, R.string.LikeUsOnFacebook);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_rate);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    Intent open = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.facebook.com/hellotracks"));
                    startActivity(open);
                    return false;
                }
            });
        }

        {
            final MenuItem item = helpMenu.add(2, Menu.NONE, Menu.NONE, R.string.FAQ);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_info);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    onFAQ(null);
                    return false;
                }
            });
        }
        {
            final MenuItem item = helpMenu.add(2, Menu.NONE, Menu.NONE, R.string.QuestionOrFeedback);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_help);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                    onFeedback(null);
                    return false;
                }
            });
        }

        mMenuItemDriving = mainMenu.add(1, Menu.NONE, Menu.NONE, R.string.DrivingView);
        mMenuItemDriving.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        mMenuItemDriving.setCheckable(true);
        mMenuItemDriving.setChecked(drivingMode);
        mMenuItemDriving.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                onDriving(drivingButton);
                return false;
            }
        });

        boolean active = Prefs.get(this).getBoolean(Prefs.STATUS_ONOFF, false);
        mMenuItemCloseApp = mainMenu.add(1, Menu.NONE, Menu.NONE, active ? R.string.CloseButKeepRunning
                : R.string.CloseAndStopTracking);
        mMenuItemCloseApp.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        mMenuItemCloseApp.setOnMenuItemClickListener(new OnMenuItemClickListener() {

            public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
                finish();
                return false;
            }
        });

        return true;
    }

    private String createMarkerCacheId() {
        String cacheId = "cache_markers_" + Prefs.get(this).getString(Prefs.USERNAME, "");
        return cacheId;
    }

    private void refillMap() {
        try {
            JSONObject obj = AbstractScreen.prepareObj(this);
            obj.put(C.account, null);
            AbstractScreen.doAction(this, AbstractScreen.ACTION_MARKERS, obj, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    if (!result.equals(lastMarkers)) {
                        lastMarkers = result;
                        Prefs.get(HomeMapScreen.this).edit().putString(createMarkerCacheId(), lastMarkers).commit();
                        updateMap(result);
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
                        return names[i1].toLowerCase().compareTo(names[i2].toLowerCase());
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

                new Async.Task<Void>(this) {

                    @Override
                    public Void async() {
                        String hash[] = new String[urls.length];
                        for (int i = 0; i < hash.length; i++) {
                            String url = urls[i];
                            hash[i] = ImageCache.getInstance().getHash(url);
                            ImageCache.getInstance().loadSync(url, hash[i], HomeMapScreen.this);
                        }
                        return (null);
                    }

                    @Override
                    public void post(Void result) {
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

                };
            } catch (Exception exc) {
                Log.w(exc);
            }
        }
    }

    private void refillContactList() {
        if (mMap == null)
            return;

        container.removeAllViews();
        //addContactListAction(container);
        if (accounts != null && accounts.length > 0)
            fillContactActions(container);
    }

    public void onLayers(View view) {
        FlurryAgent.logEvent("Layers");
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

    protected void openSearchDialog() {
        FlurryAgent.logEvent("Search");
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.EnterSearch);
        final EditText input = new EditText(this);
        input.setHint(R.string.NameOrPlace);
        alert.setView(input);
        alert.setPositiveButton(getResources().getString(R.string.Search), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString().trim();
                if (value.length() >= 2) {
                    Intent intent = new Intent(HomeMapScreen.this, NetworkScreen.class);
                    intent.putExtra(C.type, C.search);
                    intent.putExtra(C.search, value);
                    intent.putExtra(C.action, AbstractScreen.ACTION_SEARCH);
                    startActivity(intent);
                }
            }
        });
        alert.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();
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
            } else if (ts > System.currentTimeMillis() - Time.D / 2) {
                today.add(i);
                if (ts > System.currentTimeMillis() - Time.MIN * 40)
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
            View contactView = getLayoutInflater().inflate(R.layout.quick_contact, null);
            final TextView text = (TextView) contactView.findViewById(R.id.quickText);
            final ImageView image = (ImageView) contactView.findViewById(R.id.quickImage);
            if (today.contains(item)) {
                text.setBackgroundResource(R.drawable.custom_button_trans_blue);
            } else if (places.contains(item)) {
                text.setBackgroundResource(R.drawable.custom_button_trans_red);
            } else {
                text.setBackgroundResource(R.drawable.custom_button_light);
            }
            contactView.setOnLongClickListener(new View.OnLongClickListener() {

                @Override
                public boolean onLongClick(View v) {
                    try {
                        FlurryAgent.logEvent("ProfileLongClick");
                        Marker m = getMarker(item);
                        CameraPosition cameraPosition = new CameraPosition.Builder().target(m.getPosition()).zoom(14)
                                .tilt(30).build();
                        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                        Intent intent = new Intent(HomeMapScreen.this, NewProfileScreen.class);
                        intent.putExtra(C.account, accounts[item]);
                        intent.putExtra(C.name, names[item]);
                        startActivityForResult(intent, C.REQUESTCODE_CONTACT);
                    } catch (Exception exc) {
                    }
                    return true;
                }
            });
            contactView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    try {
                        Marker m = getMarker(item);
                        m.showInfoWindow();
                        CameraPosition cameraPosition = new CameraPosition.Builder().target(m.getPosition()).zoom(14)
                                .tilt(30).build();
                        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                    } catch (Exception exc) {
                    }
                }
            });
            String url = urls[i];
            if (url.endsWith("marker.png")) {
                url = url.substring(0, url.length() - "marker.png".length()) + "mini.jpg";
            }
            Picasso.with(HomeMapScreen.this).load(url).into(image);

            String name = names[i];
            if (name.length() > 12) {
                name = name.substring(0, 10) + "...";
            }
            text.setText(name);

            container.addView(contactView);
        }
    }

    private void addContactListAction(LinearLayout container) {
        View contactsView = getLayoutInflater().inflate(R.layout.quick_contact, null);
        ImageView image = (ImageView) contactsView.findViewById(R.id.quickImage);
        image.setImageResource(R.drawable.ic_action_contacts);
        image.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeMapScreen.this, NetworkScreen.class);
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
        } else if (requestCode == C.REQUESTCODE_PICK_CONTACT && resultCode == RESULT_OK) {
            loadContactInfo(data.getData());
            return;
        }

        if (resultCode < 0) {
            realLogout();
            return;
        }
    }

    protected void realLogout() {
        Prefs.get(this).edit().putString(C.account, null).putBoolean(Prefs.STATUS_ONOFF, false)
                .putString(Prefs.PASSWORD, "").commit();
        stopService(new Intent(this, C.trackingServiceClass));
        setResult(-1);
        finish();
    }

    public void onMenu(View view) {
        FlurryAgent.logEvent("Menu");
        startActivityForResult(new Intent(HomeMapScreen.this, MenuScreen.class), C.REQUESTCODE_CONTACT);
    }

    private void showMyLocation() {
        if (mMap == null)
            return;

        if (mMap.isMyLocationEnabled() && mMap.getMyLocation() != null) {
            try {
                LatLng pos = new LatLng(mMap.getMyLocation().getLatitude(), mMap.getMyLocation().getLongitude());
                CameraPosition cameraPosition = new CameraPosition.Builder().zoom(14).target(pos).tilt(30).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            } catch (Exception exc) {
                Log.w(exc);
            }
        } else {
            Location loc = getLastLocation();
            if (loc != null) {
                LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
                CameraPosition cameraPosition = new CameraPosition.Builder().zoom(14).target(pos).tilt(30).build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mSlidingMenu.isMenuShowing()) {
                toggleMenu();
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
            toggleMenu();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onTracks(View view) {
        FlurryAgent.logEvent("Tracks");
        Intent intent = new Intent(HomeMapScreen.this, TrackListScreen.class);
        startActivityForResult(intent, C.REQUESTCODE_CONTACT);
    }

    public void onPanic(View view) {
        try {
            FlurryAgent.logEvent("Panic");
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

    private boolean drivingMode = false;

    private TextView textSpeed;
    private ImageButton drivingButton;

    public void onDriving(View view) {
        drivingMode = !drivingMode;
        int r;
        if (drivingMode) {
            view.startAnimation(blinkanimation);

            textSpeed.setVisibility(View.VISIBLE);
            drivingButton.setImageResource(R.drawable.ic_action_driving_white);
            drivingButton.setBackgroundColor(getResources().getColor(R.color.blue));
            r = R.string.DrivingModeOn;
        } else {
            textSpeed.setVisibility(View.GONE);
            drivingButton.setImageResource(R.drawable.ic_action_driving_gray);
            drivingButton.setBackgroundResource(R.drawable.custom_button_trans_light);
            r = R.string.DrivingModeOff;
            CameraPosition cameraPosition = new CameraPosition.Builder().zoom(14)
                    .target(mMap.getCameraPosition().target).tilt(10).bearing(0).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        mMenuItemDriving.setChecked(drivingMode);
        mLastDrivingViewSwitch = System.currentTimeMillis();
        Toast.makeText(this, r, Toast.LENGTH_SHORT).show();
    }

    private void updateButtons(final SharedPreferences prefs, boolean change) {

        boolean active = prefs.getBoolean(Prefs.STATUS_ONOFF, false);

        if (mMenuItemCloseApp != null)
            mMenuItemCloseApp.setTitle(active ? R.string.CloseButKeepRunning : R.string.CloseAndStopTracking);

        mPowerSwitch.setChecked(active);

        String mode = prefs.getString(Prefs.MODE, Mode.sport.name());
        if (Mode.isFuzzy(mode)) {
            roughBtn.setChecked(true);
        } else if (Mode.isTransport(mode)) {
            transportBtn.setChecked(true);
        } else if (Mode.isOutdoor(mode)) {
            outdoorBtn.setChecked(true);
        }

        if (!active) {
            modeText.setVisibility(View.GONE);
            group.setVisibility(View.GONE);
            onOffSwitch.setChecked(false);
        } else {
            modeText.setVisibility(View.VISIBLE);
            group.setVisibility(View.VISIBLE);
            onOffSwitch.setChecked(true);
        }

        if (change && !Mode.isFuzzy(mode)) {
            if (active) {
                int r = Prefs.isDistanceUS(this) ? R.string.TrackStartedFeet : R.string.TrackStartedMeter;
                Toast.makeText(this, r, Toast.LENGTH_LONG).show();
            } else {
                insertTrackEndGPS();
                Toast.makeText(this, R.string.TrackEnded, Toast.LENGTH_SHORT).show();
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
        stopService(new Intent(this, C.trackingServiceClass));
    }

    private void maybeStartService() {
        if (!isMyServiceRunning()) {
            Log.i("service not running -> start it");
            Intent serviceIntent = new Intent(this, C.trackingServiceClass);
            startService(serviceIntent);
        }
    }

    @Override
    protected void onStop() {
        isActivityRunning = false;
        FlurryAgent.onEndSession(this);
        boolean tracking = Prefs.get(this).getBoolean(Prefs.STATUS_ONOFF, false);
        if (!tracking) {
            stopService();
        }
        super.onStop();
    }

    private boolean isMyServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (C.trackingServiceClass.getCanonicalName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void doLogin() {
        if (Prefs.get(this).getString(Prefs.USERNAME, "").length() == 0) {
            return;
        }

        try {
            JSONObject data = AbstractScreen.prepareObj(this);
            data.put("man", Build.MANUFACTURER);
            data.put("mod", Build.MODEL);
            data.put("os", "Android " + Build.VERSION.RELEASE);
            data.put("ver", this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionCode);
            data.put("vername", this.getPackageManager().getPackageInfo(this.getPackageName(), 0).versionName);

            AbstractScreen.doAction(this, AbstractScreen.ACTION_LOGIN, data, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    try {
                        doLoginResult(new JSONObject(result));
                    } catch (Exception exc) {
                        Log.w(exc);
                    }
                }

                @Override
                public void onFailure(final int status, final Context context) {
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
        final int unreadMsgCount = node.has("unreadmsg") ? node.getInt("unreadmsg") : 0;
        final int contactReqCount = node.has("requests") ? node.getInt("requests") : 0;
        final int suggestionsCount = node.has("suggestions") ? node.getInt("suggestions") : 0;

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
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
                    } else {
                        badgeContacts.setText("");
                        badgeContacts.hide();
                    }
                } catch (Exception exc) {
                    Log.w(exc);
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
                            JSONObject data = AbstractScreen.prepareObj(HomeMapScreen.this);
                            data.put("book", array);
                            AbstractScreen.doAction(HomeMapScreen.this, AbstractScreen.ACTION_SETVALUE, data, null,
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
                        Log.w(exc);
                    }
                };
            }.start();
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
            if (now - lastNoInternetConnectionToast > 10000) {
                Toast.makeText(this, R.string.EnsureYourInternetConnectionIsAvailable, Toast.LENGTH_LONG).show();
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
        Intent intent = new Intent(HomeMapScreen.this, ChangeUserScreen.class);
        intent.putExtra(C.errortext, text);
        startActivity(intent);
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

    public void onMessages(View view) {
        FlurryAgent.logEvent("Messages");
        startActivity(new Intent(this, ConversationsScreen.class));
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
                Log.w(exc);
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
                        Toast.makeText(getApplicationContext(), R.string.NoEntries, Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    private String currentTrackString = null;
    private Polyline currentTrack = null;

    private void updateCurrentTrack() {
        try {
            JSONObject obj = AbstractScreen.prepareObj(this);
            obj.put(C.account, Prefs.get(this).getString(Prefs.USERNAME, ""));
            obj.put("count", 1);
            obj.put("fromts", System.currentTimeMillis() * 2);
            AbstractScreen.doAction(this, AbstractScreen.ACTION_TRACKS, obj, null, new ResultWorker() {

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
                                        AbstractScreen.doAction(HomeMapScreen.this, AbstractScreen.ACTION_TRACKCOURSE,
                                                obj, null, new ResultWorker() {
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
                                                                        Log.w(exc);
                                                                    }
                                                                }
                                                            });

                                                        } catch (Exception exc) {
                                                            Log.w(exc);
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
                                                    Log.w(exc);
                                                }
                                            }
                                        });
                                    }
                                    break;
                                }
                            } catch (Exception exc) {
                                Log.w(exc);
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        currentTrack.remove();
                                        currentTrack = null;
                                    } catch (Exception exc) {
                                        Log.w(exc);
                                    }
                                }
                            });
                        }
                    }
                }
            });
        } catch (Exception exc) {
            Log.w(exc);
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
        line.polyline = mMap.addPolyline(opt);
        line.color = color;

        MarkerOptions start = new MarkerOptions().position(line.track.get(0))
                .title(getResources().getString(R.string.Start)).snippet(line.text);
        line.start = mMap.addMarker(start);
        line.start.showInfoWindow();
        MarkerOptions end = new MarkerOptions().position(line.track.get(line.track.size() - 1))
                .title(getResources().getString(R.string.End)).snippet(line.text);
        line.end = mMap.addMarker(end);
        visibleTracks.put(trackId, line);
        refillTrackActions(line, blinkanimation);
        fitBounds(mMap, line.track);
        return line;
    }

    public void onFAQ(View view) {
        FlurryAgent.logEvent("MainMenu-FAQ");

        if (!AbstractScreen.isOnline(this, true))
            return;

        Intent intent = new Intent(this, WebScreen.class);
        intent.putExtra("url", "http://www.hellotracks.com/faq");
        startActivity(intent);
    }

    public void onFeedback(View view) {
        startActivity(new Intent(this, ContactScreen.class));
    }

    public void onHelp(final View view) {
        FlurryAgent.logEvent("MainMenu-Help");

        if (!AbstractScreen.isOnline(this, true))
            return;

        ActionItem infoItem = new ActionItem(this, R.string.Information);
        ActionItem faqItem = new ActionItem(this, R.string.FAQ);
        ActionItem questionFeedbackItem = new ActionItem(this, R.string.QuestionOrFeedback);
        QuickAction quick = new QuickAction(this);
        quick.addActionItem(infoItem);
        quick.addActionItem(faqItem);
        quick.addActionItem(questionFeedbackItem);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                switch (pos) {
                case 0:
                    startActivity(new Intent(HomeMapScreen.this, HelpScreen.class));
                    break;
                case 1:
                    onFAQ(view);
                    break;
                case 2:
                    startActivity(new Intent(HomeMapScreen.this, ContactScreen.class));
                    break;
                }
            }
        });
        quick.show(view);
    }

    public void onProfile(View view) {
        if (AbstractScreen.isOnline(this, true)) {
            startActivityForResult(new Intent(this, ProfileSettingsScreen.class), C.REQUESTCODE_CONTACT);
        }
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

    public void onNetwork(View view) {
        FlurryAgent.logEvent("MainMenu-Network");
        Intent intent = new Intent(this, NetworkScreen.class);
        startActivity(intent);
    }

    public void onAccountSettings(View view) {
        FlurryAgent.logEvent("MainMenu-Account");
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
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

    private RadioButton roughBtn;
    private RadioButton transportBtn;
    private RadioButton outdoorBtn;
    private RadioButton offBtn;
    private RadioGroup group;
    private TextView modeText;
    private Switch onOffSwitch;

    private static final int MODE_OFF = R.id.offButton;
    private static final int MODE_FUZZY = R.id.roughLocatingButton;
    private static final int MODE_TRANSPORT = R.id.transportButton;
    private static final int MODE_OUTDOOR = R.id.outdoorButton;

    private boolean isModeTransport() {
        String mode = Prefs.get(this).getString(Prefs.MODE, Mode.sport.name());
        return Mode.isTransport(mode);
    }

    private boolean isModeFuzzy() {
        String mode = Prefs.get(this).getString(Prefs.MODE, Mode.sport.name());
        return Mode.isFuzzy(mode);
    }

    private boolean isModeOutdoor() {
        String mode = Prefs.get(this).getString(Prefs.MODE, Mode.sport.name());
        return Mode.isOutdoor(mode);
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
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int id) {
                onModeChanged(id);
            }

        });

        modeText = (TextView) findViewById(R.id.modeText);

        if (active) {
            modeText.setVisibility(View.VISIBLE);
            group.setVisibility(View.VISIBLE);
        } else {
            modeText.setVisibility(View.GONE);
            group.setVisibility(View.GONE);
        }

        onOffSwitch = (Switch) findViewById(R.id.switchOnOff);
        onOffSwitch.setChecked(active);
        onOffSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton b, boolean check) {
                if (check) {
                    group.setVisibility(View.VISIBLE);
                    modeText.setVisibility(View.VISIBLE);
                } else {
                    group.setVisibility(View.GONE);
                    modeText.setVisibility(View.GONE);
                }
                Prefs.get(HomeMapScreen.this).edit().putBoolean(Prefs.STATUS_ONOFF, check).commit();
            }
        });

        roughBtn = (RadioButton) findViewById(R.id.roughLocatingButton);
        transportBtn = (RadioButton) findViewById(R.id.transportButton);
        outdoorBtn = (RadioButton) findViewById(R.id.outdoorButton);
        roughBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    modeText.setText(R.string.FuzzyShortDesc);
            }
        });

        transportBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    modeText.setText(R.string.TransportShortDesc);
            }
        });

        outdoorBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    modeText.setText(R.string.OutdoorShortDesc);
            }
        });
        String mode = Prefs.get(this).getString(Prefs.MODE, null);
        if (Mode.isOutdoor(mode)) {
            outdoorBtn.setChecked(true);
        } else if (Mode.isTransport(mode)) {
            transportBtn.setChecked(true);
        } else if (Mode.isFuzzy(mode)) {
            roughBtn.setChecked(true);
        }

    }

    private void onModeChanged(int i) {
        final String newMode;

        switch (i) {
        case MODE_OFF:
            Prefs.get(this).edit().putBoolean(Prefs.STATUS_ONOFF, false).commit();
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
        Prefs.get(this).edit().putString(Prefs.MODE, newMode).putBoolean(Prefs.STATUS_ONOFF, true).commit();
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
        } else if (isModeFuzzy() || (isModeTransport() && !power)) {
            r = net ? R.string.LocationAccuracyOnFuzzy : R.string.EnableNetworkLocating;
        } else if (!gps && (isModeTransport() || isModeOutdoor())) {
            r = R.string.EnableGPS;
        } else {
            ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (wifi.isConnected()) {
                r = R.string.GPSIsNotNeededWhileWifiConnected;
            } else {
                Location location = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
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

        final boolean openLocationSettings = r == R.string.EnableGPS || r == R.string.EnableNetworkLocating;
        QuickAction quick = new QuickAction(this);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (openLocationSettings) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            }
        });

        quick.addActionItem(new ActionItem(this, r));
        quick.show(block1top);
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
                        Log.w(exc);
                        Toast.makeText(getApplicationContext(), R.string.ConnectionToServerDoesntWork,
                                Toast.LENGTH_LONG).show();
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
        QuickAction quick = new QuickAction(this);
        quick.addActionItem(resetItem);
        quick.show(block3top);
    }

    private void updateCockpitValues() {
        DbAdapter dbAdapter = null;
        try {
            dbAdapter = new DbAdapter(getApplicationContext());
            dbAdapter.open();
            block4top.setText(String.valueOf(dbAdapter.count()));
        } catch (Exception exc) {
            Log.w(exc);
            block4top.setText("-");
        } finally {
            try {
                dbAdapter.close();
            } catch (Exception exc) {
            }
        }

        ConnectivityManager connec = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        android.net.NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        android.net.NetworkInfo mobile = connec.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (wifi.isConnected()) {
            block2top.setText("Wi-Fi");
            block2bottom.setBackgroundResource(R.drawable.block_bottom);
        } else if (mobile != null && mobile.isConnected()) {
            block2top.setText("3G");
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
        } else if (isModeTransport()) {
            block3bottom.setBackgroundResource(R.drawable.block_bottom_orange);
        } else if (level < 30) {
            block3bottom.setBackgroundResource(R.drawable.block_bottom_orange);
        } else {
            block3bottom.setBackgroundResource(R.drawable.block_bottom);
        }

        Location gpsLoc = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (gpsLoc != null) {
            if (System.currentTimeMillis() - gpsLoc.getTime() < 60000) {
                if (gpsLoc.getAccuracy() < 50) {
                    block1bottom.setBackgroundResource(R.drawable.block_bottom);
                } else {
                    block1bottom.setBackgroundResource(R.drawable.block_bottom_orange);
                }
                if (Prefs.isDistanceUS(this)) {
                    int ft = (int) (gpsLoc.getAccuracy() * 3.2808399);
                    block1top.setText(ft + "ft");
                } else {
                    int meter = (int) gpsLoc.getAccuracy();
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
        if (isModeFuzzy() || isModeTransport() && !power) {
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

}