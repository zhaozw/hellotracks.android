package com.hellotracks.account;

import org.json.JSONObject;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Prefs;
import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.base.C;
import com.hellotracks.base.WebScreen;
import com.hellotracks.network.RegisterScreen;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.Ui;

public class SignUpScreen extends RegisterScreen {

	private boolean business = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_login_signup);

		String username = PreferenceManager.getDefaultSharedPreferences(
				getApplicationContext()).getString(Prefs.USERNAME, "");
		if (username != null && username.length() > 0) {
			TextView userText = (TextView) findViewById(R.id.userText);
			userText.setText(username);
		}
		setupActionBar(R.string.Cancel);
	}

	public void onBack(View view) {
		Prefs.get(this).edit().putString(C.account, null)
				.putBoolean(Prefs.STATUS_ONOFF, false)
				.putString(Prefs.PASSWORD, "").commit();
		finish();
	}

	public void onBusinessUse(View view) {
		business = true;
	}

	public void onPrivateUse(View view) {
		business = false;
	}

	public void onSignUp(final View view) {
		try {
		    view.setEnabled(false);
		    new Handler().postDelayed(new Runnable() {
                
                @Override
                public void run() {
                    try {
                        view.setEnabled(true);
                    } catch(Exception exc) {
                        Log.e(exc);
                    }
                }
            }, 1500);
			String name = ((TextView) findViewById(R.id.nameText)).getText()
					.toString().trim().replaceAll("\n", "");

			String phone = ((TextView) findViewById(R.id.phoneText)).getText()
					.toString().trim().replaceAll("\n", "");

			if (name.length() <= 2) {
			    Ui.showText(this,
						getResources().getString(R.string.InvalidName));
				throw new Exception();
			}

			Ui.makeText(this, R.string.JustASecond, Toast.LENGTH_SHORT).show();
			
			JSONObject registerObj = new JSONObject();
			registerObj.put("accounttype", C.person);
			registerObj.put("name", name);
			if (phone.length() > 0)
				registerObj.put("phone", phone);
			LatLng ll = new LatLng(getLastLocation());
			if (ll.lat + ll.lng != 0) {
				registerObj.put("latitude", ll.lat);
				registerObj.put("longitude", ll.lng);
			}
			doRegister(registerObj, business);
		} catch (Exception exc) {
			Log.w(exc);
			view.setEnabled(true);
		}
	}

	public void onReadTerms(View view) {
		Intent intent = new Intent(this, WebScreen.class);
		intent.putExtra(C.url, C.URL_TERMS);
		startActivity(intent);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBack(null);
		}
		return super.onKeyDown(keyCode, event);
	}
}