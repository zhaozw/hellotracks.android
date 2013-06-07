package com.hellotracks.einstein;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.RegisterScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.types.LatLng;

public class RegisterMemberScreen extends RegisterScreen {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_register_member);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
	}

	public void onCreate(View view) {
		final String owner = getIntent().getStringExtra(C.owner);

		try {
			final String name = ((TextView) findViewById(R.id.memberName))
					.getText().toString().trim().replaceAll("\n", "");

			if (name.length() == 0) {
				Toast.makeText(this,
						getResources().getString(R.string.InvalidMemberName),
						Toast.LENGTH_LONG).show();
				throw new Exception();
			}

			LatLng ll = new LatLng(getLastLocation());
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
									send(name, owner, lat, lng);
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
				send(name, owner, lat, lng);
			}
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	private void send(String name, String owner, double latitude,
			double longitude) throws Exception {

		JSONObject registerObj = new JSONObject();

		final String login = ((TextView) findViewById(R.id.userText)).getText()
				.toString().trim().replaceAll("\n", "");
		final String pwd = ((TextView) findViewById(R.id.passwordText))
				.getText().toString().trim().replaceAll("\n", "");

		if (login.length() < 6 || login.contains(" ") || login.contains("\n")) {
			Toast.makeText(this,
					getResources().getString(R.string.InvalidUsername),
					Toast.LENGTH_LONG).show();
			throw new Exception();
		}
		
		if (pwd.length() < 6 || pwd.contains(" ") || pwd.contains("\n")) {
			Toast.makeText(this,
					getResources().getString(R.string.invalidPassword),
					Toast.LENGTH_LONG).show();
			throw new Exception();
		}
		registerObj.put("username", login);
		registerObj.put("password", pwd);

		Locale locale = Locale.getDefault();
		TimeZone timezone = TimeZone.getDefault();
		registerObj.put("language", locale.getLanguage());
		registerObj.put("country", locale.getCountry());
		registerObj.put("timezone", timezone.getID());
		registerObj.put("accounttype", "person");
		registerObj.put("name", name);
		registerObj.put("owner", owner);
		registerObj.put("business", true);
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
						finish();
					}

				});
	}

	public static boolean isValidUsername(String username) {
		if (username == null || username.length() < 6 || username.contains(" "))
			return false;
		return true;
	}

}
