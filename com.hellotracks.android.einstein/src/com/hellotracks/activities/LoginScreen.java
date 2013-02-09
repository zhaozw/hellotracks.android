package com.hellotracks.activities;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.einstein.C;
import com.hellotracks.einstein.HomeMapScreen;

public class LoginScreen extends AbstractScreen {

	private SharedPreferences settings;
	private EditText userText;
	private EditText pwdText;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		settings = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_login);

		userText = (EditText) findViewById(R.id.userText);
		pwdText = (EditText) findViewById(R.id.passwordText);
	}

	public void onBack(View view) {
		Prefs.get(this).edit().putString(C.account, null)
				.putBoolean(Prefs.STATUS_ONOFF, false)
				.putString(Prefs.PASSWORD, "").commit();
		setResult(-1);
		startActivity(new Intent(this, WelcomeScreen.class));
		finish();
	}

	@Override
	protected void onResume() {

		String username = settings.getString(Prefs.USERNAME, "");
		String password = settings.getString(Prefs.PASSWORD, "");
		boolean autologin = settings.getBoolean(Prefs.AUTOLOGIN, true);
		boolean tracking = settings.getBoolean(Prefs.STATUS_ONOFF, false);
		if ((tracking || autologin) && username.length() > 0
				&& password.length() > 0) {
			long lastlogout = settings.getLong(Prefs.LASTLOG,
					System.currentTimeMillis());
			if (Math.abs(lastlogout - System.currentTimeMillis()) > 5000) {
				startActivity(new Intent(this, HomeMapScreen.class));
				finish();
			}
		}

		super.onResume();

		userText.setText(username);
		pwdText.setText(password);
	}

	public void onForgotPassword(View view) {
		showDialog(DIALOG_FORGOTPASSWORD);
	}

	final int DIALOG_FORGOTPASSWORD = 1;

	@Override
	protected Dialog onCreateDialog(int id, Bundle bundle) {
		if (id == DIALOG_FORGOTPASSWORD) {
			final AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle(R.string.ForgotPassword);
			alert.setMessage(R.string.EnterEmailToReceivePassword);
			final EditText input = new EditText(this);
			input.setHint(R.string.Email);
			input.setText(Prefs.get(this).getString(Prefs.USERNAME, ""));
			alert.setView(input);
			alert.setPositiveButton(getResources().getString(R.string.OK),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							String value = input.getText().toString().trim();
							if (value.length() > 0) {
								try {
									JSONObject obj = prepareObj();
									obj.put(C.usr, value);
									doAction(ACTION_REQUESTPASSWORD,
											obj, null);
								} catch (Exception e) {
								}
							}

						}
					});
			alert.setNegativeButton(getResources().getString(R.string.Cancel),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog,
								int whichButton) {
							dialog.cancel();
						}
					});
			return alert.create();
		}
		return super.onCreateDialog(id, bundle);
	}

	public void doLogin(final View view) {
		final String user = userText.getText().toString().trim();
		final String pwd = pwdText.getText().toString().trim();
		final int logins = settings.getInt(Prefs.LOGINS, 0);
		settings.edit().putString(Prefs.USERNAME, user)
				.putString(Prefs.PASSWORD, pwd)
				.putInt(Prefs.LOGINS, logins + 1).commit();

		if (user.length() > 0 && pwd.length() > 0) {
			startNow(user, pwd);
		} else {
			Toast.makeText(
					this,
					getResources().getString(
							R.string.enterUsernameAndPasswordToLogin),
					Toast.LENGTH_LONG).show();
		}
	}

	private void startNow(String user, String pwd) {
		startActivity(new Intent(this, HomeMapScreen.class));
		finish();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			onBack(null);
		}
		return super.onKeyDown(keyCode, event);
	}

}
