package com.hellotracks.activities;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.types.LatLng;

public class RegisterPlaceScreen extends RegisterScreen {

	private SeekBar extension;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_register_place);
		
		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), "FortuneCity.ttf");
		nameView.setTypeface(tf);

		final TextView extensionLabel = (TextView) findViewById(R.id.radiusLabel);

		extension = (SeekBar) findViewById(R.id.radius);
		int p = extension.getProgress();
		extensionLabel.setText(fromProgressToText(p));
		extension
				.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

					@Override
					public void onStopTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar seekBar) {
					}

					@Override
					public void onProgressChanged(SeekBar seekBar, int p,
							boolean fromUser) {
						extensionLabel.setText(fromProgressToText(p));
					}
				});
	}

	public static String fromProgressToText(int p) {
		int meter = fromProgressToMeter(p);
		int feet = (int) (3.2808399 * (double) meter);
		return meter + " m / " + feet + " feet";
	}

	public static int fromMeterToProgress(int x) {
		return (int) Math.sqrt(x -100);
	}
	
	public static int fromProgressToMeter(int p) {
		return p * p + 100;
	}

	public void onCreate(View view) {
		try {
			final String name = ((TextView) findViewById(R.id.placeName))
					.getText().toString().trim().replaceAll("\n", "");

			if (name.length() == 0) {
				Toast.makeText(this,
						getResources().getString(R.string.InvalidPlaceName),
						Toast.LENGTH_LONG).show();
				throw new Exception();
			}

			final String owner = Prefs.get(this).getString(Prefs.USERNAME, "");
			final int radiusMeter = fromProgressToMeter(extension.getProgress());

			LatLng ll = getLastLocation();
			final double lat = ll.lat;
			final double lng = ll.lng;
			if (lat + lng == 0) {
				AlertDialog.Builder alert = new AlertDialog.Builder(this);
				alert.setMessage(getResources().getString(
						R.string.CurrentLocationUnavailable));
				alert.setPositiveButton(
						getResources().getString(R.string.CreateAnyhow),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								try {
									send(name, owner, radiusMeter, lat,
											lng);
								} catch (Exception exc) {
									Log.w(exc);
								}
							}
						});
				alert.setNegativeButton(
						getResources().getString(R.string.Cancel),
						new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
							}
						});
				alert.show();
			} else {
				send(name, owner, radiusMeter, lat, lng);
			}
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	private void send(String name, String owner,
			int radiusMeter, double latitude, double longitude) throws JSONException,
			UnsupportedEncodingException {
		JSONObject registerObj = new JSONObject();
		Locale locale = Locale.getDefault();
		TimeZone timezone = TimeZone.getDefault();
		registerObj.put("language", locale.getLanguage());
		registerObj.put("country", locale.getCountry());
		registerObj.put("timezone", timezone.getID());
		registerObj.put("accounttype", "place");
		registerObj.put("name", name);
		registerObj.put("owner", owner);
		registerObj.put("extension", radiusMeter * 2);
		if (latitude + longitude != 0) {
			registerObj.put("latitude", latitude);
			registerObj.put("longitude", longitude);
		}
		String msg = getResources().getString(R.string.registering) + " "
				+ name + "...";
		doAction(AbstractScreen.ACTION_REGISTER, registerObj, msg,
				new ResultWorker() {

					@Override
					public void onResult(String result, Context context) {
						Toast.makeText(
								RegisterPlaceScreen.this,
								getResources().getString(
										R.string.placeRegisteredSuccessfully),
								Toast.LENGTH_LONG).show();
						finish();
					}

				});
	}

}
