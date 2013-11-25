package com.hellotracks.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.text.SpannableString;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.animation.Animation;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.Session.StatusCallback;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.FacebookDialog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.maps.GeoPoint;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.db.DbAdapter;
import com.hellotracks.network.NewPlaceScreen;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.tracks.TrackInfoScreen;
import com.hellotracks.types.GPS;
import com.hellotracks.util.FlurryAgent;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.StaticMap;
import com.hellotracks.util.Time;
import com.hellotracks.util.Ui;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;
import com.squareup.picasso.Picasso;

public abstract class AbstractMapScreen extends AbstractScreen {

    protected GoogleMap mMap;
    protected UiLifecycleHelper uiHelper;

    protected HashMap<String, MarkerEntry> mMarkerEntries = new HashMap<String, MarkerEntry>();
    protected HashMap<Marker, MarkerEntry> mMarker2Entry = new HashMap<Marker, MarkerEntry>();
    protected HashMap<Marker, Circle> mMarker2Circle = new HashMap<Marker, Circle>();

    protected TreeMap<Long, TrackLine> visibleTracks = new TreeMap<Long, TrackLine>();
    private HashMap<String, Marker> mUnsetWaypoints = new HashMap<String, Marker>();

    public class TrackLine {
        public Marker start;
        public Marker end;
        public Polyline polyline;
        public List<LatLng> track = null;
        public String url;
        public long id;
        public String comments;
        public int actions;
        public int labels;
        public String text = " ";
        public View view;
        public String encoded;
        public SearchMap.DirectionsResult result = null;
        public int color;

        public TrackLine() {
        }

        public void remove() {
            start.remove();
            end.remove();
            polyline.remove();
            visibleTracks.remove(id);
        }
    }

    private BitmapDescriptor redDot;

    static Bitmap makeSrc(int w, int h) {
        Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bm);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setColor(0xFF66AAFF);
        c.drawOval(new RectF(0, 0, w, h), p);
        return bm;
    }

    protected void updateUnsetWaypoints() {
        if (mMap == null)
            return;

        try {
            if (redDot == null) {
                int size = (int) getResources().getDimension(R.dimen.unsetDotSize);
                redDot = BitmapDescriptorFactory.fromBitmap(makeSrc(size, size));
            }
            final GPS[] points = DbAdapter.getInstance(this).selectGPS(500);
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    HashSet<String> positions = new HashSet<String>();
                    for (GPS gps : points) {
                        LatLng pos = new LatLng(gps.lat, gps.lng);
                        positions.add(pos.toString());
                        if (!mUnsetWaypoints.containsKey(pos.toString())) {
                            MarkerOptions opt = new MarkerOptions();
                            opt.position(pos);
                            opt.anchor(0.5f, 0.5f);
                            opt.title(getResources().getString(R.string.UnsentWaypointAt,
                                    Time.FORMAT_TIME.format(gps.ts)));
                            opt.snippet(getResources().getString(R.string.UnsentWaypointDesc));
                            opt.icon(redDot);
                            mUnsetWaypoints.put(pos.toString(), mMap.addMarker(opt));
                        }
                    }
                    for (String pos : mUnsetWaypoints.keySet().toArray(new String[0])) {
                        if (!positions.contains(pos)) {
                            mUnsetWaypoints.get(pos).remove();
                            mUnsetWaypoints.remove(pos);
                        }
                    }
                }

            });
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    protected void putMarker(Marker marker, MarkerEntry entry, Circle... circle) {
        entry.marker = marker;
        mMarker2Entry.put(marker, entry);
        if (circle.length > 0) {
            mMarker2Circle.put(marker, circle[0]);
        }
    }

    protected void removeMarker(Marker marker) {
        if (marker != null) {
            MarkerEntry entry = mMarker2Entry.remove(marker);
            if (entry != null)
                entry.marker = null;
            Circle c = mMarker2Circle.get(marker);
            if (c != null) {
                c.remove();
                mMarker2Circle.remove(marker);
            }
            marker.remove();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        uiHelper.onActivityResult(requestCode, resultCode, data, new FacebookDialog.Callback() {
            @Override
            public void onError(FacebookDialog.PendingCall pendingCall, Exception error, Bundle data) {
                Log.e(String.format("Error: %s", error.toString()));
            }

            @Override
            public void onComplete(FacebookDialog.PendingCall pendingCall, Bundle data) {
                Log.i("Success!");
            }
        });

        if (requestCode == C.REQCODE_GOOGLEPLAYSERVICES) {
            setUpMapIfNeeded();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        uiHelper.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        uiHelper.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        uiHelper.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        uiHelper.onDestroy();
    }

    protected void setUpMapIfNeeded() {
        if (mMap == null) {
            int code = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
            if (code == ConnectionResult.SUCCESS) {
                try {
                    MapsInitializer.initialize(this);
                    mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
                    if (mMap != null) {
                        setUpMap();
                    } else {
                        GooglePlayServicesUtil.getErrorDialog(ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED, this,
                                C.REQCODE_GOOGLEPLAYSERVICES).show();
                    }
                } catch (GooglePlayServicesNotAvailableException e) {
                    GooglePlayServicesUtil.getErrorDialog(e.errorCode, this, C.REQCODE_GOOGLEPLAYSERVICES).show();
                    return;
                }
            } else {
                GooglePlayServicesUtil.getErrorDialog(code, this, C.REQCODE_GOOGLEPLAYSERVICES).show();
            }
        }
    }

    private List<Marker> tempCreateNewPlaceMarker = new LinkedList<Marker>();

    private void setUpMap() {
        //mMap.setTrafficEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.setMyLocationEnabled(true);
        mMap.setInfoWindowAdapter(new PersonInfoWindowAdapter());
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setMapType(Prefs.get(this).getInt(Prefs.MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL));
        mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {
                MarkerEntry e = mMarker2Entry.get(marker);
                if (e != null) {
                    Intent intent = new Intent(AbstractMapScreen.this, NewProfileScreen.class);
                    intent.putExtra(C.account, e.account);
                    intent.putExtra(C.name, e.name);
                    startActivityForResult(intent, C.REQUESTCODE_CONTACT);
                    return;
                }

                for (TrackLine line : visibleTracks.values().toArray(new TrackLine[0])) {
                    long trackLat1 = Math.round(line.track.get(0).latitude * 100000);
                    long trackLat2 = Math.round(line.track.get(line.track.size() - 1).latitude * 100000);
                    long markerLat = Math.round(marker.getPosition().latitude * 100000);
                    if (trackLat1 == markerLat || trackLat2 == markerLat) {
                        showTrackOptions(line);
                        return;
                    }
                }

                Intent intent = new Intent(AbstractMapScreen.this, NewPlaceScreen.class);
                intent.putExtra("lat", marker.getPosition().latitude);
                intent.putExtra("lng", marker.getPosition().longitude);
                intent.putExtra("name", marker.getTitle());
                AbstractMapScreen.this.startActivityForResult(intent, C.REQUESTCODE_CONTACT);
            }
        });
        mMap.setOnMarkerDragListener(new OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker marker) {
                AbstractScreen.isOnline(AbstractMapScreen.this, true);
            }

            @Override
            public void onMarkerDragEnd(Marker marker) {
                Circle c = mMarker2Circle.get(marker);
                if (c != null) {
                    c.setCenter(marker.getPosition());
                }
                MarkerEntry e = mMarker2Entry.get(marker);
                if (e != null) {
                    try {
                        JSONObject obj = AbstractScreen.prepareObj(AbstractMapScreen.this);
                        obj.put(C.account, e.account);
                        JSONObject loc = new JSONObject();
                        final double lat = marker.getPosition().latitude;
                        final double lng = marker.getPosition().longitude;
                        loc.put("lat", lat);
                        loc.put("lng", lng);
                        obj.put("location", loc);
                        AbstractScreen.doAction(AbstractMapScreen.this, AbstractScreen.ACTION_EDITPROFILE, obj, null,
                                new ResultWorker());
                    } catch (Exception exc) {
                        Log.w(exc);
                    }
                }
            }

            @Override
            public void onMarkerDrag(Marker marker) {
                Circle c = mMarker2Circle.get(marker);
                if (c != null) {
                    c.setCenter(marker.getPosition());
                }
            }
        });

        mMap.setOnMapLongClickListener(new OnMapLongClickListener() {

            @Override
            public void onMapLongClick(final LatLng point) {
                addPinToCreatePlace(point, null, true, 8000);
            }
        });
    }

    public static Bitmap combineImages(Activity activity, Bitmap marker, Bitmap img) {
        int pad = 10;
        int size = Ui.convertDpToPixel(50 - (2 * pad), activity);
        float padding = Ui.convertDpToPixel(pad, activity);
        img = Bitmap.createScaledBitmap(img, size, size, false);

        Bitmap cs = Bitmap.createBitmap(marker.getWidth(), marker.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(cs);
        canvas.drawBitmap(marker, 0, 0, null);
        canvas.drawBitmap(img, padding, padding / 4, null);
        return cs;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uiHelper = new UiLifecycleHelper(this, new StatusCallback() {

            @Override
            public void call(Session session, SessionState state, Exception exception) {
                Log.i(session.toString());
            }
        });
        uiHelper.onCreate(savedInstanceState);
    }

    public static Bitmap getResizedBitmap(Bitmap bm, int newHeight, int newWidth) {
        if (bm == null)
            return bm;
        int width = bm.getWidth();
        int height = bm.getHeight();
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
        return resizedBitmap;
    }

    public void onBack(View view) {
        finish();
    }

    protected void doShowAll() {
        if (mMarkerEntries.size() > 0) {
            LatLng[] points = new LatLng[mMarkerEntries.size()];
            int i = 0;
            for (MarkerEntry e : mMarkerEntries.values().toArray(new MarkerEntry[0])) {
                points[i++] = e.point;
            }

            if (points.length > 1) {
                fitBounds(mMap, points);
            } else if (points.length == 1) {
                CameraPosition cameraPosition = new CameraPosition.Builder().target(points[0]).zoom(14).tilt(30)
                        .build();
                mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        }
    }

    public static List<LatLng> decodeFromGoogleToList(String encodedPolyline) {
        List<LatLng> list = new LinkedList<LatLng>();

        int len = encodedPolyline.length();
        int index = 0;
        int lat = 0;
        int lng = 0;

        // Decode polyline according to Google's polyline decoder utility.
        while (index < len) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = (((result & 1) != 0) ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encodedPolyline.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = (((result & 1) != 0) ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((lat * 10d) / 1E6d, (lng * 10d) / 1E6d);
            list.add(p);
        }

        return list;
    }

    public static void fitBounds(final GoogleMap map, final LatLng... bounds) {

        double minLat = Integer.MAX_VALUE;
        double minLong = Integer.MAX_VALUE;
        double maxLat = Integer.MIN_VALUE;
        double maxLong = Integer.MIN_VALUE;

        for (LatLng point : bounds) {
            minLat = Math.min(point.latitude, minLat);
            minLong = Math.min(point.longitude, minLong);
            maxLat = Math.max(point.latitude, maxLat);
            maxLong = Math.max(point.longitude, maxLong);
        }

        LatLngBounds b = new LatLngBounds(new LatLng(minLat, minLong), new LatLng(maxLat, maxLong));
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 400, 400, 20));
    }

    public static void fitBounds(GoogleMap map, List<LatLng> bounds) {
        fitBounds(map, bounds.toArray(new LatLng[0]));
    }

    public static LatLng toLatLng(GeoPoint p) {
        return new LatLng(p.getLatitudeE6() / 1E6f, p.getLongitudeE6() / 1E6f);
    }

    class DetaultInfoWindowAdapter implements InfoWindowAdapter {

        private final View mContents;

        DetaultInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.marker_info_default, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            render(marker, mContents);
            return mContents;
        }

        private void render(Marker marker, final View view) {
            String title = marker.getTitle();
            TextView titleUi = ((TextView) view.findViewById(R.id.title));
            if (title != null) {
                SpannableString titleText = new SpannableString(title);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            String snippet = marker.getSnippet();
            TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
            if (snippet != null && snippet.length() > 12) {
                SpannableString snippetText = new SpannableString(snippet);
                snippetUi.setText(snippetText);
            } else {
                snippetUi.setText("");
            }
        }
    }

    class PersonInfoWindowAdapter implements InfoWindowAdapter {

        private final View mWindow;

        PersonInfoWindowAdapter() {
            mWindow = getLayoutInflater().inflate(R.layout.marker_info_person, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            render(marker, mWindow);
            return mWindow;
        }

        private void render(Marker marker, final View view) {
            String title = marker.getTitle();
            TextView titleUi = ((TextView) view.findViewById(R.id.title));
            if (title != null) {
                SpannableString titleText = new SpannableString(title);
                titleUi.setText(titleText);
            } else {
                titleUi.setText("");
            }

            String snippet = marker.getSnippet();
            TextView snippetUi = ((TextView) view.findViewById(R.id.snippet));
            if (snippet != null && snippet.length() > 12) {
                SpannableString snippetText = new SpannableString(snippet);
                snippetUi.setText(snippetText);
            } else {
                snippetUi.setText("");
            }
        }

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }
    }

    protected void startAnimation(List<LatLng> track) {
        if (track != null) {
            int step = 15;
            int zoom = 14;
            int radius = 50;
            if (track.size() < 100) {
                step = 2;
                zoom = 18;
                radius = 15;
            } else if (track.size() < 500) {
                step = 5;
                zoom = 16;
                radius = 35;
            }
            stepAnimation(track, 0, step, zoom, radius);
        }
    }

    private void stepAnimation(final List<LatLng> track, final int i, final int step, final float zoom, final int radius) {
        final LatLng point = track.get(i);

        CircleOptions opt = new CircleOptions();
        opt.center(point).strokeColor(Color.argb(255, 33, 66, 200)).radius(radius)
                .fillColor(Color.argb(255, 33, 66, 200));
        final Circle circle = mMap.addCircle(opt);
        circle.setZIndex(100);
        final Handler bounceHandler = new Handler();
        bounceHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                circle.remove();
            }
        }, 1000);

        Location startingLocation = new Location("starting point");
        startingLocation.setLatitude(mMap.getCameraPosition().target.latitude);
        startingLocation.setLongitude(mMap.getCameraPosition().target.longitude);

        // Get the target location
        Location endingLocation = new Location("ending point");
        endingLocation.setLatitude(point.latitude);
        endingLocation.setLongitude(point.longitude);

        // Find the Bearing from current location to next location
        float targetBearing = startingLocation.bearingTo(endingLocation);

        mMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(point).zoom(zoom).tilt(60)
                        .bearing(targetBearing).build()), 800, new CancelableCallback() {

                    @Override
                    public void onFinish() {
                        if (i + step < track.size())
                            stepAnimation(track, i + step, step, zoom, radius);
                    }

                    @Override
                    public void onCancel() {

                    }
                });
    }

    protected void jumpTo(LatLng pos) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(pos).zoom(14)
                .tilt(30).build()));
    }

    protected void jumpToVeryNear(LatLng pos) {
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(new CameraPosition.Builder().target(pos).zoom(16)
                .tilt(90).build()));
    }

    protected void showTrackOptions(final TrackLine line) {
        ActionItem start = new ActionItem(this, R.string.JumpToStart);
        start.setIcon(getResources().getDrawable(R.drawable.ic_action_track_start));
        ActionItem end = new ActionItem(this, R.string.JumpToEnd);
        end.setIcon(getResources().getDrawable(R.drawable.ic_action_b));
        ActionItem startAnimation = new ActionItem(this, R.string.StartAnimation);
        startAnimation.setIcon(getResources().getDrawable(R.drawable.ic_action_play));
        ActionItem infoItem = new ActionItem(this, R.string.TrackInfoAndTools);
        infoItem.setIcon(getResources().getDrawable(R.drawable.ic_action_info));
        ActionItem removeItem = new ActionItem(this, R.string.RemoveFromMap);
        removeItem.setIcon(getResources().getDrawable(R.drawable.ic_action_close));
        ActionItem facebookShareDialogItem = new ActionItem(this, R.string.ShareWithFacebook);
        facebookShareDialogItem.setIcon(getResources().getDrawable(R.drawable.ic_facebook_white));
        final QuickAction quick = new QuickAction(this);
        quick.addActionItem(start);
        quick.addActionItem(end);
        quick.addActionItem(startAnimation);
        quick.addActionItem(infoItem);
        quick.addActionItem(removeItem);
        if (FacebookDialog.canPresentShareDialog(getApplicationContext(),
                FacebookDialog.ShareDialogFeature.SHARE_DIALOG) && line.id > 0) {
            quick.addActionItem(facebookShareDialogItem);
        }
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                switch (pos) {
                case 0:
                    FlurryAgent.logEvent("TrackAction-Start");
                    jumpToVeryNear(line.start.getPosition());
                    break;
                case 1:
                    FlurryAgent.logEvent("TrackAction-End");
                    jumpToVeryNear(line.end.getPosition());
                    break;
                case 2:
                    FlurryAgent.logEvent("TrackAction-Animation");
                    startAnimation(line.track);
                    break;
                case 3:
                    FlurryAgent.logEvent("TrackAction-Tools");
                    if (line.id > 0) {
                        Intent intent = new Intent(AbstractMapScreen.this, TrackInfoScreen.class);
                        intent.putExtra("track", line.id);
                        intent.putExtra("comments", line.comments);
                        intent.putExtra("actions", line.actions);
                        intent.putExtra("labels", line.labels);
                        intent.putExtra("url", line.url);
                        intent.putExtra("text", line.text);
                        startActivityForResult(intent, 0);
                    } else if (line.result != null) {
                        showDirectionsList(line.result);
                        showDirectionsInfo(line.result);
                    }
                    break;
                case 4:
                    FlurryAgent.logEvent("TrackAction-Remove");
                    line.remove();
                    refillTrackActions(null, null);
                    break;
                case 5:
                    doShareWithFacebookDialog(line);
                    break;
                }
            }
        });
        quick.show(line.view);
    }

    private void doShareWithFacebookDialog(TrackLine line) {
        try {
            int begin = line.url.indexOf("size=");
            int end = line.url.indexOf("&", begin);
            String url = line.url.substring(0, begin) + "size=640x640" + line.url.substring(end);
            url = url.replace("weight:4", "weight:15");
            FacebookDialog shareDialog = new FacebookDialog.ShareDialogBuilder(this)
                    .setLink("https://play.google.com/store/apps/details?id=com.hellotracks").setPicture(url).build();
            uiHelper.trackPendingDialogCall(shareDialog.present());
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    abstract protected void showDirectionsList(final SearchMap.DirectionsResult result);

    protected void refillTrackActions(final TrackLine animate, final Animation animation) {
        LinearLayout container = (LinearLayout) findViewById(R.id.tracksActionsContainer);
        container.removeAllViews();
        if (visibleTracks.size() > 0) {
            for (final TrackLine line : visibleTracks.values().toArray(new TrackLine[0])) {
                View v = getLayoutInflater().inflate(R.layout.quick_track, null);
                v.setBackgroundColor(getResources().getColor(line.color));

                line.view = v;

                if (animation != null && animate == line) {
                    v.startAnimation(animation);
                }

                final ImageButton image = (ImageButton) v.findViewById(R.id.quickImage);

                final String url = line.url != null ? line.url : StaticMap.Google.createMap(100, line.encoded)
                        .toString();

                Picasso.with(this).load(url).into(image);

                image.setOnLongClickListener(new OnLongClickListener() {

                    @Override
                    public boolean onLongClick(View v) {
                        showTrackOptions(line);
                        return true;
                    }
                });

                image.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        fitBounds(mMap, line.track);

                        Handler h = new Handler();
                        h.postDelayed(new Runnable() {

                            @Override
                            public void run() {
                                showTrackOptions(line);
                            }

                        }, 700);
                    }
                });
                container.addView(v);
            }
        }
    }

    protected void addPinToCreatePlace(final LatLng point, final String name, boolean removeOthers, final long millis) {
        if (removeOthers) {
            for (Marker m : tempCreateNewPlaceMarker) {
                m.remove();
            }
            tempCreateNewPlaceMarker.clear();
        }
        String title = name != null ? name : getResources().getString(R.string.CreateNewPlace);
        MarkerOptions opt = new MarkerOptions();
        opt.position(point).title(title).snippet(getResources().getString(R.string.ClickForMore)).draggable(true);
        final Marker thisMarker = mMap.addMarker(opt);
        tempCreateNewPlaceMarker.add(thisMarker);
        thisMarker.showInfoWindow();

        final long start = SystemClock.uptimeMillis();
        Projection proj = mMap.getProjection();
        Point startPoint = proj.toScreenLocation(point);
        startPoint.offset(0, -100);
        final LatLng startLatLng = proj.fromScreenLocation(startPoint);
        final long duration = 1500;

        final Interpolator interpolator = new BounceInterpolator();
        final Handler bounceHandler = new Handler();
        bounceHandler.post(new Runnable() {
            @Override
            public void run() {
                try {
                    if (point == null || startLatLng == null)
                        return;
                    long elapsed = SystemClock.uptimeMillis() - start;
                    float t = interpolator.getInterpolation((float) elapsed / duration);
                    if (t < 1) {
                        double lng = t * point.longitude + (1 - t) * startLatLng.longitude;
                        double lat = t * point.latitude + (1 - t) * startLatLng.latitude;
                        thisMarker.setPosition(new LatLng(lat, lng));
                    }
                    if (elapsed < millis) {
                        bounceHandler.postDelayed(this, 16);
                    } else {
                        thisMarker.remove();
                        tempCreateNewPlaceMarker.remove(thisMarker);
                    }
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        });
    }

    protected void showDirectionsInfo(final SearchMap.DirectionsResult result) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        StringBuilder sb = new StringBuilder();
        Resources r = getResources();
        sb.append(r.getString(R.string.Start)).append(": ").append(result.startAddress).append("\n\n")
                .append(r.getString(R.string.End)).append(": ").append(result.endAddress).append("\n\n")
                .append(r.getString(R.string.Distance)).append(": ").append(result.distanceText).append("\n\n")
                .append(r.getString(R.string.Duration)).append(": ").append(result.durationText);
        builder.setMessage(sb.toString());
        AlertDialog dlg = builder.create();
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
    }

    public void addMarker(final MarkerEntry entry, final Resources r, Bitmap bmp) {
        String infoText = entry.info;
        int s1 = entry.info.indexOf(",");
        int s2 = entry.info.indexOf(",", s1 + 1);
        int s3 = entry.info.indexOf(",", s2 + 1);
        if (s2 > 0 && s2 < entry.info.length()) {
            infoText = entry.info.substring(0, s1);
            infoText += "\n";
            infoText += entry.info.substring(s1 + 2, s2);
            infoText += "\n";
        }
        String timeText = "";
        if (entry.accuracy > 0) {
            if (!Prefs.isDistanceUS(AbstractMapScreen.this)) {
                timeText = getResources().getString(R.string.Within) + " " + entry.accuracy + "m\n";
            } else {
                timeText = getResources().getString(R.string.Within) + " " + (int) (3.28084 * entry.accuracy) + "ft\n";
            }
        }
        timeText += Time.formatTimePassed(AbstractMapScreen.this, entry.timestamp);

        Circle circle = null;
        if (entry.radius > 0) {
            CircleOptions circleOptions = new CircleOptions().center(entry.point).radius(entry.radius).strokeWidth(2)
                    .strokeColor(Color.argb(200, 102, 51, 51)).fillColor(Color.argb(35, 102, 51, 51));
            circle = mMap.addCircle(circleOptions);
        }

        MarkerOptions opt = new MarkerOptions();
        opt.position(entry.point).title(entry.name).snippet(infoText + "\n" + timeText);
        if (bmp == null) {
            opt.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
        } else {
            opt.icon(BitmapDescriptorFactory.fromBitmap(bmp));

            //            int w = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 42, r.getDisplayMetrics());
            //            int h = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 72, r.getDisplayMetrics());
            //            //Bitmap resized = getResizedBitmap(fancyImage, h, w);
            //            //fancyImage.recycle();
            //            opt.icon(BitmapDescriptorFactory.fromBitmap(bmp));
            opt.anchor(0.5f, 1f);
        }

        if (entry.index == 0 || entry.radius > 0) {
            opt.draggable(true);
        }

        Marker marker = mMap.addMarker(opt);

        if (circle != null)
            putMarker(marker, entry, circle);
        else
            putMarker(marker, entry);
    }
}
