package com.hellotracks.einstein;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.GestureDetector.OnGestureListener;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.ItemizedOverlay;
import com.google.android.maps.MapView;
import com.google.android.maps.MyLocationOverlay;
import com.google.android.maps.Overlay;
import com.google.android.maps.OverlayItem;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingService.Mode;
import com.hellotracks.activities.AbstractMapScreen;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.RegisterPlaceScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.util.MapGestureDetectorOverlay;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;

public class LiveMapScreen extends AbstractMapScreen {

	class UpdateTimeTask extends TimerTask {

		public void run() {
			refillMap();
		}
	}

	private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			refillMap();
		}
	};

	private Timer timer;

	protected void onResume() {
		registerReceiver(mIntentReceiver,
				new IntentFilter(Prefs.TAB_MAP_INTENT));
		timer = new Timer();
		timer.schedule(new UpdateTimeTask(), 10000, 10000);
		if (isEnableMyLocation() && !myLocOverlay.isMyLocationEnabled()) {
			myLocOverlay.enableMyLocation();
			myLocOverlay.enableCompass();
			showMyLocation();
		}
		super.onResume();
	};

	private boolean isEnableMyLocation() {
		boolean active = Prefs.get(this).getBoolean(Prefs.STATUS_ONOFF, false);
		if (!active)
			return false;

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
		unregisterReceiver(mIntentReceiver);
		if (timer != null)
			timer.cancel();
		myLocOverlay.disableMyLocation();
		myLocOverlay.disableCompass();
		super.onPause();
	}

	private MyLocationOverlay myLocOverlay;
	private MapGestureDetectorOverlay gestureOverlay;
	private SitesOverlay sitesOverlay;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_livemap);

		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		TextView name = (TextView) findViewById(R.id.name);
		name.setTypeface(tf);

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setSatellite(true);
		mapController = mapView.getController();
		mapController.setZoom(10);

		myLocOverlay = new MyLocationOverlay(LiveMapScreen.this, mapView);
		gestureOverlay = new MapGestureDetectorOverlay(new OnGestureListener() {

			@Override
			public boolean onSingleTapUp(MotionEvent e) {
				if (quickView == null && sitesOverlay == null) {
					Toast.makeText(getApplicationContext(),
							R.string.PressLongToCreatePlace, Toast.LENGTH_SHORT)
							.show();
				}
				if (quickView != null) {
					mapView.removeView(quickView);
					quickView = null;
				}
				if (sitesOverlay != null) {
					mapView.getOverlays().remove(sitesOverlay);
					sitesOverlay = null;
					mapView.invalidate();
				}
				return false;
			}

			@Override
			public void onShowPress(MotionEvent e) {
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2,
					float distanceX, float distanceY) {
				return false;
			}

			@Override
			public void onLongPress(MotionEvent e) {
				if (quickView == null && sitesOverlay == null) {
					Toast.makeText(getApplicationContext(),
							R.string.DragMarkerToMoveLoc, Toast.LENGTH_SHORT)
							.show();
				}

				final GeoPoint loc = mapView.getProjection().fromPixels(
						(int) e.getX(), (int) e.getY());
				if (quickView != null) {
					mapView.removeView(quickView);
					quickView = null;
				}
				if (sitesOverlay != null) {
					mapView.getOverlays().remove(sitesOverlay);
				}

				sitesOverlay = new SitesOverlay(getResources().getDrawable(
						R.drawable.marker), loc);
				mapView.getOverlays().add(sitesOverlay);
				mapView.invalidate();

				ActionItem item1 = new ActionItem(mapView.getContext(),
						R.string.CreatePlace);
				final QuickAction quick = new QuickAction(mapView.getContext());
				quick.addActionItem(item1);
				quick.setOnActionItemClickListener(new OnActionItemClickListener() {

					@Override
					public void onItemClick(QuickAction source, int pos,
							int actionId) {
						if (quickView != null) {
							mapView.removeView(quickView);
							quickView = null;
						}
						if (sitesOverlay != null) {
							mapView.getOverlays().remove(sitesOverlay);
							mapView.invalidate();
						}
						Intent intent = new Intent(LiveMapScreen.this,
								RegisterPlaceScreen.class);
						intent.putExtra("lat",
								(double) loc.getLatitudeE6() / 1000000);
						intent.putExtra("lng",
								(double) loc.getLongitudeE6() / 1000000);
						startActivityForResult(intent, C.REQUESTCODE_CONTACT);
					}
				});
				quick.hideArrows();

				MapView.LayoutParams mapParams = new MapView.LayoutParams(
						ViewGroup.LayoutParams.WRAP_CONTENT,
						ViewGroup.LayoutParams.WRAP_CONTENT, loc, quick
								.getView().getWidth() / 2, 0,
						MapView.LayoutParams.BOTTOM_CENTER);
				mapView.addView(quickView = quick.getView(), mapParams);

			}

			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2,
					float velocityX, float velocityY) {
				return false;
			}

			@Override
			public boolean onDown(MotionEvent e) {
				return false;
			}
		});
		List<Overlay> overlays = mapView.getOverlays();
		overlays.clear();
		overlays.add(gestureOverlay);
		overlays.add(myLocOverlay);
		refillMap();
	}

	private String lastMarkers = null;

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

	private void updateMap(final String markers) {
		if (markers != null && markers.length() > 0) {
			try {
				JSONArray array = new JSONArray(markers);
				final String[] names = new String[array.length()];
				final String[] urls = new String[array.length()];
				final GeoPoint[] points = new GeoPoint[array.length()];
				final String[] accounts = new String[array.length()];
				final long[] timestamps = new long[array.length()];

				for (int i = 0; i < array.length(); i++) {
					JSONObject obj = array.getJSONObject(i);
					urls[i] = obj.getString("url");
					names[i] = obj.getString("name");
					accounts[i] = obj.getString("account");
					timestamps[i] = obj.getLong("ts");
					double lat = obj.getDouble("lat");
					double lng = obj.getDouble("lng");
					final GeoPoint point = new GeoPoint((int) (lat * 1000000),
							(int) (lng * 1000000));
					points[i] = point;
				}
				new OverlayTask(this) {

					@Override
					public void onPostExecute(Void unused) {
						LiveMapScreen.this.names = names;
						LiveMapScreen.this.urls = urls;
						LiveMapScreen.this.accounts = accounts;
						LiveMapScreen.this.timestamps = timestamps;
						LiveMapScreen.this.points = points;

						for (Overlay overlay : mapView.getOverlays().toArray(
								new Overlay[0])) {
							if (overlay instanceof MarkerOverlay) {
								mapView.getOverlays().remove(overlay);
							}
						}
						mapView.getOverlays().add(new MarkerOverlay());
						mapView.invalidate();

					}
				}.execute(urls);
			} catch (Exception exc) {
				Log.w(exc);
			}
		}
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

		}
	}

	private class SitesOverlay extends ItemizedOverlay<OverlayItem> {
		private List<OverlayItem> items = new ArrayList<OverlayItem>();
		private Drawable marker = null;
		private OverlayItem inDrag = null;
		private ImageView dragImage = null;
		private int xDragImageOffset = 0;
		private int yDragImageOffset = 0;
		private int xDragTouchOffset = 0;
		private int yDragTouchOffset = 0;

		public SitesOverlay(Drawable marker, GeoPoint geo) {
			super(marker);
			this.marker = marker;

			dragImage = (ImageView) findViewById(R.id.drag);
			xDragImageOffset = dragImage.getDrawable().getIntrinsicWidth() / 2;
			yDragImageOffset = dragImage.getDrawable().getIntrinsicHeight();

			items.add(new OverlayItem(geo, "", "")); // TODO
			populate();
		}

		@Override
		protected OverlayItem createItem(int i) {
			return (items.get(i));
		}

		@Override
		public void draw(Canvas canvas, MapView mapView, boolean shadow) {
			super.draw(canvas, mapView, shadow);

			boundCenterBottom(marker);
		}

		@Override
		public int size() {
			return (items.size());
		}

		@Override
		public boolean onTouchEvent(MotionEvent event, MapView mapView) {
			final int action = event.getAction();
			final int x = (int) event.getX();
			final int y = (int) event.getY();
			boolean result = false;

			if (action == MotionEvent.ACTION_DOWN) {
				for (OverlayItem item : items) {
					Point p = new Point(0, 0);

					mapView.getProjection().toPixels(item.getPoint(), p);

					if (hitTest(item, marker, x - p.x, y - p.y)) {
						result = true;
						inDrag = item;
						items.remove(inDrag);
						populate();

						xDragTouchOffset = 0;
						yDragTouchOffset = 0;

						setDragImagePosition(p.x, p.y);
						dragImage.setVisibility(View.VISIBLE);

						xDragTouchOffset = x - p.x;
						yDragTouchOffset = y - p.y;

						break;
					}
				}
			} else if (action == MotionEvent.ACTION_MOVE && inDrag != null) {
				setDragImagePosition(x, y);
				result = true;
			} else if (action == MotionEvent.ACTION_UP && inDrag != null) {
				dragImage.setVisibility(View.GONE);

				GeoPoint pt = mapView.getProjection().fromPixels(
						x - xDragTouchOffset, y - yDragTouchOffset);
				OverlayItem toDrop = new OverlayItem(pt, inDrag.getTitle(),
						inDrag.getSnippet());

				items.add(toDrop);
				populate();

				inDrag = null;
				result = true;
			}

			return (result || super.onTouchEvent(event, mapView));
		}

		private void setDragImagePosition(int x, int y) {
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) dragImage
					.getLayoutParams();

			lp.setMargins(x - xDragImageOffset - xDragTouchOffset, y
					- yDragImageOffset - yDragTouchOffset, 0, 0);
			dragImage.setLayoutParams(lp);
		}
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
							showMyLocation();
							break;
						case 1:
							doJumpTo();
							break;
						case 2:
							doShowAll();
							break;
						case 3:
							doMapType();
							break;
						}
					}
				});

		ActionItem item0 = new ActionItem(this, R.string.MyCurrentLocation);
		mQuickAction.addActionItem(item0);
		ActionItem item = new ActionItem(this, R.string.JumpTo);
		mQuickAction.addActionItem(item);
		ActionItem item2 = new ActionItem(this, R.string.ShowAll);
		mQuickAction.addActionItem(item2);
		ActionItem item3 = new ActionItem(this, R.string.MapType);
		mQuickAction.addActionItem(item3);
		mQuickAction.show(view);
	}

	private void showMyLocation() {
		myLocOverlay.runOnFirstFix(new Runnable() {

			@Override
			public void run() {
				mapView.getController().setZoom(14);
				mapView.getController().animateTo(myLocOverlay.getMyLocation());
			}

		});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (quickView != null) {
				mapView.removeView(quickView);
				quickView = null;
			} else
				finish();
			return true;
		}
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			onMenu(findViewById(R.id.button_down));
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}
}