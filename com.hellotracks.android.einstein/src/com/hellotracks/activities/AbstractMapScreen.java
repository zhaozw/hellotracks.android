package com.hellotracks.activities;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.FragmentActivity;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.BounceInterpolator;
import android.view.animation.Interpolator;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.maps.GeoPoint;
import com.google.android.maps.OverlayItem;
import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.einstein.C;
import com.hellotracks.einstein.ProfileScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.ImageCache;
import com.hellotracks.util.Time;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public abstract class AbstractMapScreen extends FragmentActivity {

	protected GoogleMap mMap;

	private HashMap<Marker, Integer> mMarker2Index = new HashMap<Marker, Integer>();
	private HashMap<Integer, Marker> mIndex2Marker = new HashMap<Integer, Marker>();
	private HashMap<Marker, Circle> mMarker2Circle = new HashMap<Marker, Circle>();

	protected void putMarker(Marker marker, int index, Circle... circle) {
		mMarker2Index.put(marker, index);
		mIndex2Marker.put(index, marker);
		if (circle.length > 0) {
			mMarker2Circle.put(marker, circle[0]);
		}
	}

	protected Marker getMarker(int index) {
		return mIndex2Marker.get(index);
	}

	protected Integer getIndex(Marker marker) {
		return mMarker2Index.get(marker);
	}

	protected void removeMarker(Marker marker) {
		if (marker != null) {
			Integer index = getIndex(marker);
			mMarker2Index.remove(marker);
			if (index != null)
				mIndex2Marker.remove(index);
			Circle c = mMarker2Circle.get(marker);
			if (c != null) {
				c.remove();
				mMarker2Circle.remove(marker);
			}
			marker.remove();
		}
	}

	protected void setUpMapIfNeeded() {
		if (mMap == null) {
			mMap = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map)).getMap();
			if (mMap != null) {
				setUpMap();
			}
		}
	}

	private Marker tempCreateNewPlaceMarker = null;

	private void setUpMap() {
		mMap.setTrafficEnabled(true);
		mMap.getUiSettings().setZoomControlsEnabled(false);
		mMap.getUiSettings().setScrollGesturesEnabled(true);
		mMap.setMyLocationEnabled(true);

		mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
		mMap.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

			@Override
			public void onInfoWindowClick(Marker marker) {
				Integer i = getIndex(marker);
				if (i != null) {
					Intent intent = new Intent(AbstractMapScreen.this,
							ProfileScreen.class);
					intent.putExtra(C.account, accounts[i]);
					intent.putExtra(C.name, names[i]);
					startActivityForResult(intent, C.REQUESTCODE_CONTACT);
				} else {
					Intent intent = new Intent(AbstractMapScreen.this,
							RegisterPlaceScreen.class);
					intent.putExtra("lat", marker.getPosition().latitude);
					intent.putExtra("lng", marker.getPosition().longitude);
					AbstractMapScreen.this.startActivityForResult(intent,
							C.REQUESTCODE_CONTACT);
				}
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
				Integer i = getIndex(marker);
				if (i != null) {
					try {
						JSONObject obj = AbstractScreen
								.prepareObj(AbstractMapScreen.this);
						obj.put(C.account, accounts[i]);
						JSONObject loc = new JSONObject();
						final double lat = marker.getPosition().latitude;
						final double lng = marker.getPosition().longitude;
						loc.put("lat", lat);
						loc.put("lng", lng);
						obj.put("location", loc);
						AbstractScreen.doAction(AbstractMapScreen.this,
								AbstractScreen.ACTION_EDITPROFILE, obj, null,
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
				if (tempCreateNewPlaceMarker != null) {
					tempCreateNewPlaceMarker.remove();
					tempCreateNewPlaceMarker = null;
				}
				MarkerOptions opt = new MarkerOptions();
				opt.position(point)
						.title(getResources()
								.getString(R.string.CreateNewPlace))
						.snippet(
								getResources()
										.getString(R.string.ClickToCreate))
						.draggable(true);
				tempCreateNewPlaceMarker = mMap.addMarker(opt);
				tempCreateNewPlaceMarker.showInfoWindow();

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
						long elapsed = SystemClock.uptimeMillis() - start;
						float t = interpolator.getInterpolation((float) elapsed
								/ duration);
						if (t < 1) {
							double lng = t * point.longitude + (1 - t)
									* startLatLng.longitude;
							double lat = t * point.latitude + (1 - t)
									* startLatLng.latitude;
							tempCreateNewPlaceMarker.setPosition(new LatLng(
									lat, lng));
						}
						if (elapsed < 7000) {
							bounceHandler.postDelayed(this, 16);
						} else {
							if (tempCreateNewPlaceMarker != null) {
								tempCreateNewPlaceMarker.remove();
								tempCreateNewPlaceMarker = null;
							}
						}
					}
				});
			}
		});
	}

	public class CustomItem extends OverlayItem {
		Drawable marker = null;
		String account = null;

		CustomItem(GeoPoint pt, String name, String account, Drawable marker) {
			super(pt, name, "");
			this.account = account;
			this.marker = marker;
		}

		public String getAccount() {
			return account;
		}

		@Override
		public Drawable getMarker(int stateBitset) {
			setState(marker, stateBitset);

			marker.setBounds(-marker.getIntrinsicWidth() / 2,
					-marker.getIntrinsicHeight(),
					marker.getIntrinsicWidth() / 2, 0);
			return marker;
		}
	}

	protected View quickView = null;

	protected void buildMarkers() {
		for (Marker m : mMarker2Index.keySet().toArray(new Marker[0])) {
			removeMarker(m);
		}

		for (int i = 0; i < points.length; i++) {
			String infoText = infos[i];
			int s1 = infos[i].indexOf(",");
			int s2 = infos[i].indexOf(",", s1 + 1);
			int s3 = infos[i].indexOf(",", s2 + 1);
			if (s2 > 0 && s2 < infos[i].length()) {
				infoText = infos[i].substring(0, s1);
				infoText += "\n";
				infoText += infos[i].substring(s1 + 2, s2);
				infoText += "\n";
				if (s3 > 0)
					infoText += infos[i].substring(s2 + 2, s3);
				else
					infoText += infos[i].substring(s2 + 2);
			}
			String timeText = "";
			if (accuracies[i] > 0) {
				// TODO from meter to feet
				timeText = getResources().getString(R.string.Within) + " "
						+ accuracies[i] + "m\n";
			}
			timeText += Time.formatTimePassed(AbstractMapScreen.this,
					timestamps[i]);

			Bitmap image = ImageCache.getInstance().loadFromCache(urls[i]);
			Resources r = getResources();
			MarkerOptions opt = new MarkerOptions();
			opt.position(points[i]).title(names[i]).snippet(infoText);
			if (image == null) {
				opt.icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
			} else {
				int w = (int) TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());
				int h = (int) TypedValue.applyDimension(
						TypedValue.COMPLEX_UNIT_DIP, 55, r.getDisplayMetrics());
				image = getResizedBitmap(image, h, w);
				opt.icon(BitmapDescriptorFactory.fromBitmap(image));
			}

			if (i == 0 || radius[i] > 0) {
				opt.draggable(true);
			}

			Circle circle = null;
			if (radius[i] > 0) {
				CircleOptions circleOptions = new CircleOptions()
						.center(points[i]).radius(radius[i]).strokeWidth(3)
						.strokeColor(Color.argb(200, 102, 51, 51))
						.fillColor(Color.argb(35, 102, 51, 51));
				circle = mMap.addCircle(circleOptions);
			}

			Marker marker = mMap.addMarker(opt);

			if (circle != null)
				putMarker(marker, i, circle);
			else
				putMarker(marker, i);
		}
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
		Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height,
				matrix, false);
		return resizedBitmap;
	}

	protected LatLng[] points = new LatLng[0];
	protected int[] radius = new int[0];
	protected int[] accuracies = new int[0];
	protected String[] names = new String[0];
	protected String[] accounts = new String[0];
	protected long[] timestamps = new long[0];
	protected String[] urls = new String[0];
	protected String[] infos = new String[0];
	protected List<LatLng> track = null;
	
	public void onBack(View view) {
		finish();
	}

	protected void doShowAll() {
		if (points.length > 0) {
			if (points.length > 1) {
				fitBounds(mMap, points);
			} else if (points.length == 1) {
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(points[0]).zoom(14).tilt(30).build();
				mMap.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
			}
		} else if (track != null && track.size() > 1) {
			fitBounds(mMap, track);
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

		LatLngBounds b = new LatLngBounds(new LatLng(minLat, minLong),
				new LatLng(maxLat, maxLong));
		map.animateCamera(CameraUpdateFactory.newLatLngBounds(b, 400, 400, 20));
	}

	public static void fitBounds(GoogleMap map, List<LatLng> bounds) {
		fitBounds(map, bounds.toArray(new LatLng[0]));
	}

	public static LatLng toLatLng(GeoPoint p) {
		return new LatLng(p.getLatitudeE6() / 1E6f, p.getLongitudeE6() / 1E6f);
	}

	public static abstract class OverlayTask extends
			AsyncTask<String, Void, Void> {

		private Context context;

		public OverlayTask(Context context) {
			this.context = context;
		}

		@Override
		public void onPreExecute() {

		}

		@Override
		public Void doInBackground(String... url) {
			String hash[] = new String[url.length];
			for (int i = 0; i < hash.length; i++) {
				hash[i] = ImageCache.getInstance().getHash(url[i]);
				ImageCache.getInstance().loadSync(url[i], hash[i], context);
			}
			return (null);
		}

		@Override
		abstract public void onPostExecute(Void unused);
	}
}
