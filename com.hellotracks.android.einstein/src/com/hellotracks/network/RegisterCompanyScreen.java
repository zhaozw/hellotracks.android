package com.hellotracks.network;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.base.WebScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.types.LatLng;

public class RegisterCompanyScreen extends RegisterScreen {

	private boolean force = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_register_company);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
		nameView.setText("@Business");

		try {
			force = getIntent().getExtras().getBoolean("force", false);
		} catch (Exception exc) {
		}
		if (force) {
			findViewById(R.id.button_back).setVisibility(View.GONE);
		}
	}

	public void onReadTerms(View view) {
		Intent intent = new Intent(this, WebScreen.class);
		intent.putExtra(C.url, C.URL_TERMS);
		startActivity(intent);
	}

	public void onCreate(View view) {
		try {
			final String name = ((TextView) findViewById(R.id.companyName))
					.getText().toString().trim().replaceAll("\n", "");

			if (name.length() == 0) {
				Toast.makeText(this,
						getResources().getString(R.string.InvalidCompanyName),
						Toast.LENGTH_LONG).show();
				throw new Exception();
			}

			final String login = ((TextView) findViewById(R.id.userText))
					.getText().toString().trim().replaceAll("\n", "");
			final String pwd = ((TextView) findViewById(R.id.passwordText))
					.getText().toString().trim().replaceAll("\n", "");
			if (!isValidLogin(login)) {
				Toast.makeText(this,
						getResources().getString(R.string.InvalidCompanyLogin),
						Toast.LENGTH_LONG).show();
				throw new Exception();
			}
			if (pwd.length() < 6 || pwd.contains(" ") || pwd.contains("\n")) {
				Toast.makeText(this,
						getResources().getString(R.string.invalidPassword),
						Toast.LENGTH_LONG).show();
				throw new Exception();
			}

			final String owner = Prefs.get(this).getString(Prefs.USERNAME, "");

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
									send(login, pwd, name, owner, lat, lng);
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
				send(login, pwd, name, owner, lat, lng);
			}
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

	private boolean isValidLogin(String login) {
		login = login.trim();
		return login.length() >= 6 && !login.contains(" ")
				&& !login.contains("\n");
	}

	private void send(String email, String pwd, String name, String owner,
			double latitude, double longitude) throws JSONException,
			UnsupportedEncodingException {
		JSONObject registerObj = new JSONObject();
		Locale locale = Locale.getDefault();
		TimeZone timezone = TimeZone.getDefault();
		registerObj.put("language", locale.getLanguage());
		registerObj.put("country", locale.getCountry());
		registerObj.put("timezone", timezone.getID());
		registerObj.put("accounttype", C.place);
		registerObj.put("placetype", "company");
		registerObj.put("name", name);
		registerObj.put("owner", owner);
		registerObj.put("extension", 800);
		registerObj.put("business", true);

		registerObj.put("username", email);
		registerObj.put("password", pwd);

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
						Toast.makeText(context, R.string.CompanyRegistrationOK,
								Toast.LENGTH_LONG).show();
						finish();
					}

				});
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (force && keyCode == KeyEvent.KEYCODE_BACK) {
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}

}
