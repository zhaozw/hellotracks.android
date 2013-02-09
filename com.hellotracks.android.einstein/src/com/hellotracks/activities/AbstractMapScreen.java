package com.hellotracks.activities;

import java.util.LinkedList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.google.android.maps.OverlayItem;
import com.hellotracks.R;
import com.hellotracks.einstein.C;
import com.hellotracks.einstein.ProfileScreen;
import com.hellotracks.util.ImageCache;
import com.hellotracks.util.Time;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public abstract class AbstractMapScreen extends MapActivity {

	protected MapController mapController;
	protected MapView mapView;

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

	public class MarkerOverlay extends ItemizedOverlay<CustomItem> {

		public MarkerOverlay() {
			super(null);
			populate();
		}

		@Override
		protected boolean onTap(final int index) {
			onTabMarker(index);
			return true;
		}

		@Override
		protected CustomItem createItem(int i) {
			Bitmap image = ImageCache.getInstance().loadFromCache(urls[i]);			
			Resources r = getResources();
			if (image == null) {
				return new CustomItem(points[i], names[i], accounts[i],
						r.getDrawable(R.drawable.marker));
			}
			int w = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, 48, r.getDisplayMetrics());
			int h = (int) TypedValue.applyDimension(
					TypedValue.COMPLEX_UNIT_DIP, 55, r.getDisplayMetrics());
			
			image = getResizedBitmap(image, h, w);
			return new CustomItem(points[i], names[i], accounts[i],
					new BitmapDrawable(image));
		}

		@Override
		public int size() {
			return points != null ? points.length : 0;
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

	public static class TrackOverlay extends com.google.android.maps.Overlay {
		private List<GeoPoint> mPoints;

		public TrackOverlay(List<GeoPoint> points, MapView mv) {
			this.mPoints = points;
			if (mPoints.size() > 0) {
				fitBounds(mv, points);
			}
		}

		@Override
		public boolean draw(Canvas canvas, MapView mv, boolean shadow, long when) {
			super.draw(canvas, mv, shadow);
			drawPath(mv, canvas);
			return true;
		}

		public void drawPath(MapView mv, Canvas canvas) {
			int x1 = -1, y1 = -1, x2 = -1, y2 = -1;
			Paint paint = new Paint();
			paint.setARGB(40, 100, 100, 255);
			paint.setStyle(Paint.Style.STROKE);
			paint.setAntiAlias(true);
			paint.setAlpha(165);
			paint.setStrokeWidth(9);
			for (int i = 0; i < mPoints.size(); i++) {
				Point point = new Point();
				mv.getProjection().toPixels(mPoints.get(i), point);
				x2 = point.x;
				y2 = point.y;
				if (i > 0) {
					canvas.drawLine(x1, y1, x2, y2, paint);
				}
				x1 = x2;
				y1 = y2;
			}
		}
	}

	protected GeoPoint[] points = new GeoPoint[0];
	protected int[] radius = new int[0];
	protected int[] accuracies = new int[0];
	protected String[] names = new String[0];
	protected String[] accounts = new String[0];
	protected long[] timestamps = new long[0];
	protected String[] urls = new String[0];
	protected String[] infos = new String[0];
	protected List<GeoPoint> track = null;

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_map, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_jumpto:
			doJumpTo();
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

	public void onMenu(View view) {
		QuickAction mQuickAction = new QuickAction(this);
		mQuickAction
				.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						switch (pos) {
						case 0:
							doJumpTo();
							break;
						case 1:
							doShowAll();
							break;
						case 2:
							doMapType();
							break;
						case 3:
							onBack(null);
							break;
						}
					}
				});

		ActionItem item = new ActionItem(this, R.string.JumpTo);
		mQuickAction.addActionItem(item);
		ActionItem item2 = new ActionItem(this, R.string.ShowAll);
		mQuickAction.addActionItem(item2);
		ActionItem item3 = new ActionItem(this, R.string.MapType);
		mQuickAction.addActionItem(item3);
		ActionItem item4 = new ActionItem(this, R.string.CloseMap);
		mQuickAction.addActionItem(item4);
		mQuickAction.show(view);
	}

	public void onBack(View view) {
		finish();
	}

	protected void doMapType() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.MapType);
		String[] items = new String[] {
				getResources().getString(R.string.Satellite),
				getResources().getString(R.string.Map) };
		builder.setItems(items, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				mapView.setSatellite(item == 0);
			}
		});
		AlertDialog dialog = builder.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}

	protected void doShowAll() {
		if (points.length > 0) {
			if (points.length > 1) {
				zoomInBounds(mapView, points);
			} else if (points.length == 1) {
				mapView.getController().animateTo(points[0]);
				mapView.getController().setZoom(14);
			}
		} else if (track != null && track.size() > 1) {
			fitBounds(mapView, track);
		}
	}

	protected void doJumpTo() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.JumpTo);
		if (names.length > 0) {
			builder.setItems(names, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int item) {
					mapView.getController().animateTo(points[item]);
					mapView.getController().setZoom(14);
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
					if (item == 0) {
						mapView.getController().animateTo(track.get(0));
						mapView.getController().setZoom(14);
					} else {
						mapView.getController().animateTo(
								track.get(track.size() - 1));
						mapView.getController().setZoom(14);
					}
				}
			});
			AlertDialog dialog = builder.create();
			dialog.setCanceledOnTouchOutside(true);
			dialog.show();
		}
	}

	@Override
	protected boolean isRouteDisplayed() {
		return false;
	}

	protected void onTabMarker(final int index) {
		try {
			if (quickView != null) {
				mapView.removeView(quickView);
				quickView = null;
			}

			mapView.getController().animateTo(points[index]);

			quickView = getLayoutInflater()
					.inflate(R.layout.quick_bubble, null);
			TextView title = (TextView) quickView.findViewById(R.id.title);
			title.setText(names[index]);

			String infoText = infos[index];
			int s1 = infos[index].indexOf(",");
			int s2 = infos[index].indexOf(",", s1 + 1);
			int s3 = infos[index].indexOf(",", s2 + 1);
			if (s2 > 0 && s2 < infos[index].length()) {
				infoText = infos[index].substring(0, s1);
				infoText += "\n";
				infoText += infos[index].substring(s1 + 2, s2);
				infoText += "\n";
				if (s3 > 0)
					infoText += infos[index].substring(s2 + 2, s3);
				else
					infoText += infos[index].substring(s2 + 2);
			}
			TextView info = (TextView) quickView.findViewById(R.id.info);
			info.setText(infoText);

			TextView timeAndAccuracy = (TextView) quickView
					.findViewById(R.id.timeAndAccuracy);

			String timeText = "";
			if (accuracies[index] > 0) {
				// TODO from meter to feet
				timeText = getResources().getString(R.string.Within) + " "
						+ accuracies[index] + "m\n";
			}
			timeText += Time.formatTimePassed(AbstractMapScreen.this,
					timestamps[index]);
			timeAndAccuracy.setText(timeText);
			quickView.setOnClickListener(new View.OnClickListener() {

				@Override
				public void onClick(View v) {
					if (quickView != null) {
						mapView.removeView(quickView);
						quickView = null;
					}
					Intent intent = new Intent(AbstractMapScreen.this,
							ProfileScreen.class);
					intent.putExtra(C.account, accounts[index]);
					intent.putExtra(C.name, names[index]);
					startActivityForResult(intent, C.REQUESTCODE_CONTACT);
				}
			});
			MapView.LayoutParams mapParams = new MapView.LayoutParams(
					ViewGroup.LayoutParams.WRAP_CONTENT,
					ViewGroup.LayoutParams.WRAP_CONTENT, points[index], 0, 0,
					MapView.LayoutParams.BOTTOM | MapView.LayoutParams.LEFT);
			mapView.addView(quickView, mapParams);
		} catch (Exception exc) {
		}
	}

	public static List<GeoPoint> decodeFromGoogleToList(String encodedPolyline) {
		List<GeoPoint> list = new LinkedList<GeoPoint>();

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

			GeoPoint p = new GeoPoint(lat * 10, lng * 10);
			list.add(p);
		}

		return list;
	}

	public static void zoomInBounds(final MapView mapView,
			final GeoPoint... bounds) {

		int minLat = Integer.MAX_VALUE;
		int minLong = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int maxLong = Integer.MIN_VALUE;

		for (GeoPoint point : bounds) {
			minLat = Math.min(point.getLatitudeE6(), minLat);
			minLong = Math.min(point.getLongitudeE6(), minLong);
			maxLat = Math.max(point.getLatitudeE6(), maxLat);
			maxLong = Math.max(point.getLongitudeE6(), maxLong);
		}

		mapView.getController().zoomToSpan((maxLat - minLat),
				(maxLong - minLong));
		mapView.getController().animateTo(
				new GeoPoint((maxLat + minLat) / 2, (maxLong + minLong) / 2));
	}

	public static void fitBounds(MapView mv, List<GeoPoint> mPoints) {
		int moveToLat = (mPoints.get(0).getLatitudeE6() + (mPoints.get(
				mPoints.size() - 1).getLatitudeE6() - mPoints.get(0)
				.getLatitudeE6()) / 2);
		int moveToLong = (mPoints.get(0).getLongitudeE6() + (mPoints.get(
				mPoints.size() - 1).getLongitudeE6() - mPoints.get(0)
				.getLongitudeE6()) / 2);
		GeoPoint moveTo = new GeoPoint(moveToLat, moveToLong);

		MapController mapController = mv.getController();
		mapController.animateTo(moveTo);
		zoomInBounds(mv, mPoints.toArray(new GeoPoint[0]));
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
