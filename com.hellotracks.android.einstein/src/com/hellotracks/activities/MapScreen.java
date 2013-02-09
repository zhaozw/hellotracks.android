package com.hellotracks.activities;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapView;
import com.google.android.maps.Overlay;
import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.einstein.C;

public class MapScreen extends AbstractMapScreen {

	public void onCreate(final Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.screen_map);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);

		mapView = (MapView) findViewById(R.id.mapview);
		mapView.setBuiltInZoomControls(true);
		mapView.setSatellite(true);

		String track = getIntent().getExtras().getString("track");
		if (track != null && track.length() > 0) {
			this.track = decodeFromGoogleToList(track);
			TrackOverlay mapOverlay = new TrackOverlay(this.track, mapView);
			List<Overlay> listOfOverlays = mapView.getOverlays();
			listOfOverlays.clear();
			listOfOverlays.add(mapOverlay);
			mapView.invalidate();
		}

		String markers = getIntent().getExtras().getString("markers");
		if (markers != null && markers.length() > 0) {
			try {
				JSONArray array = new JSONArray(markers);
				points = new GeoPoint[array.length()];
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
					final GeoPoint point = new GeoPoint((int) (lat * 1000000),
							(int) (lng * 1000000));
					points[i] = point;
				}

				new OverlayTask(mapView.getContext()) {

					@Override
					public void onPostExecute(Void unused) {
						mapView.getOverlays().add(new MarkerOverlay());
						mapView.invalidate();
					}
				}.execute(urls);

				doShowAll();
			} catch (Exception exc) {
				Log.w(exc);
			}
		}

		mapController = mapView.getController();
		mapController.setZoom(10);
	}

}
