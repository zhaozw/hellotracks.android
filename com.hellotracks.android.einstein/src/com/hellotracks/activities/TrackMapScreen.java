package com.hellotracks.activities;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.einstein.C;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class TrackMapScreen extends AbstractMapScreen {
	
	List<LatLng> track;

	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.screen_map);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
		setUpMapIfNeeded();
		final Marker startMarker;
		final Marker endMarker;
		
		String trackString = getIntent().getExtras().getString("track");
		if (trackString != null && trackString.length() > 0) {
			this.track = decodeFromGoogleToList(trackString);

			PolylineOptions opt = new PolylineOptions();
			opt.color(Color.argb(200, 33, 66, 255));
			for (LatLng p : this.track) {
				opt.add(p);
			}
			mMap.addPolyline(opt);

			MarkerOptions start = new MarkerOptions().position(track.get(0))
					.title(getResources().getString(R.string.Start));
			startMarker = mMap.addMarker(start);
			startMarker.showInfoWindow();
			MarkerOptions end = new MarkerOptions().position(
					this.track.get(track.size() - 1)).title(
					getResources().getString(R.string.End));
			endMarker = mMap.addMarker(end);
		} else {
			startMarker = null;
			endMarker = null;
		}
		mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			@Override
			public void onInfoWindowClick(Marker marker) {
				try {
					CameraPosition cameraPosition = new CameraPosition.Builder()
							.target(marker.getPosition()).zoom(16).tilt(30)
							.build();
					mMap.animateCamera(CameraUpdateFactory
							.newCameraPosition(cameraPosition));
				} catch (Exception exc) {
				}
			}
		});

		String markers = getIntent().getExtras().getString("markers");
		if (markers != null && markers.length() > 0) {
			try {
				JSONArray array = new JSONArray(markers);
				points = new LatLng[array.length()];
				names = new String[array.length()];
				urls = new String[array.length()];
				accounts = new String[array.length()];
				timestamps = new long[array.length()];
				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					urls[i] = obj.getString("url");
					names[i] = obj.getString("name");
					accounts[i] = obj.getString("account");

					timestamps[i] = obj.getLong("ts");
					double lat = obj.getDouble("lat");
					double lng = obj.getDouble("lng");
					points[i] = new LatLng(lat, lng);
				}

				buildMarkers();
			} catch (Exception exc) {
				Log.w(exc);
			}
		}

		if (track != null && track.size() > 0) {
			fitBounds(mMap, track);
		} else if (points.length > 0) {
			fitBounds(mMap, points);
		} else {
			doShowAll();
		}
	}

	private void startAnimation() {
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
			stepAnimation(0, step, zoom, radius);
		}
	}

	private void stepAnimation(final int i, final int step, final float zoom, final int radius) {
		final LatLng point = track.get(i);
		
		CircleOptions opt = new CircleOptions();
		opt.center(point).strokeColor(Color.argb(255, 33, 66, 200)).radius(radius).fillColor(Color.argb(255, 33, 66, 200));
		final Circle circle = mMap.addCircle(opt);
		circle.setZIndex(100);
		final Handler bounceHandler = new Handler();
		bounceHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				circle.remove();
			}
		}, 500);
		
		
		Location startingLocation = new Location("starting point");
		startingLocation.setLatitude(mMap.getCameraPosition().target.latitude);
		startingLocation
				.setLongitude(mMap.getCameraPosition().target.longitude);

		// Get the target location
		Location endingLocation = new Location("ending point");
		endingLocation.setLatitude(point.latitude);
		endingLocation.setLongitude(point.longitude);

		// Find the Bearing from current location to next location
		float targetBearing = startingLocation.bearingTo(endingLocation);
		

		mMap.animateCamera(CameraUpdateFactory
				.newCameraPosition(new CameraPosition.Builder().target(point)
						.zoom(zoom).tilt(60).bearing(targetBearing).build()), 500,
				new CancelableCallback() {

					@Override
					public void onFinish() {
						if (i + step < track.size())
							stepAnimation(i + step, step, zoom, radius);
					}

					@Override
					public void onCancel() {

					}
				});
	}

	public void onMenu(View view) {
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						switch (pos) {
						case 0:
							jumpTo(track.get(0));
							break;
						case 1:
							jumpTo(track.get(track.size() - 1));
							break;
						case 2:
							doShowAll();
							break;
						case 3:
							doMapType();
							break;
						case 4:
							onBack(null);
							break;
						}
					}
				});

		ActionItem item0 = new ActionItem(this, R.string.Start);
		mQuickAction.addActionItem(item0);
		ActionItem item1 = new ActionItem(this, R.string.End);
		mQuickAction.addActionItem(item1);
		ActionItem item2 = new ActionItem(this, R.string.ShowAll);
		mQuickAction.addActionItem(item2);
		ActionItem item3 = new ActionItem(this, R.string.MapType);
		mQuickAction.addActionItem(item3);
		ActionItem item4 = new ActionItem(this, R.string.CloseMap);
		mQuickAction.addActionItem(item4);
		mQuickAction.show(view);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_start:
			jumpTo(track.get(0));
			break;
		case R.id.menu_end:
			jumpTo(track.get(track.size() - 1));
			break;
		case R.id.menu_showall:
			doShowAll();
			break;
		case R.id.menu_close:
			finish();
			break;
		case R.id.menu_maptype:
			doMapType();
			break;
		}
		return true;
	}

	protected void doMapType() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.MapType);
		String[] items = new String[] {
				getResources().getString(R.string.Satellite),
				getResources().getString(R.string.Map),
				getResources().getString(R.string.Terrain) };
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				int type = GoogleMap.MAP_TYPE_HYBRID;
				switch (item) {
				case 0:
					type = GoogleMap.MAP_TYPE_HYBRID;
					break;
				case 1:
					type = GoogleMap.MAP_TYPE_NORMAL;
					break;
				case 2:
					type = GoogleMap.MAP_TYPE_TERRAIN;
					break;
				}
				mMap.setMapType(type);
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	protected void doJumpTo() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.JumpTo);
		if (names.length > 0) {
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					CameraPosition cameraPosition = new CameraPosition.Builder()
							.target(points[item]).zoom(14).tilt(30).build();
					mMap.animateCamera(CameraUpdateFactory
							.newCameraPosition(cameraPosition));
				}
			});
			AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(true);
			dialog.show();
		} else if (track != null && track.size() > 1) {
			String[] items = new String[] {
					getResources().getString(R.string.Start),
					getResources().getString(R.string.End) };
			builder.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					LatLng pos;
					if (item == 0) {
						pos = track.get(0);
					} else {
						pos = track.get(track.size() - 1);
					}
					CameraPosition cameraPosition = new CameraPosition.Builder()
							.target(pos).zoom(14).tilt(30).build();
					mMap.animateCamera(CameraUpdateFactory
							.newCameraPosition(cameraPosition));
				}
			});
			AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(true);
			dialog.show();
		}
	}

}
