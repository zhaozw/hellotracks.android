package com.hellotracks.account;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.util.ResultWorker;

public class AccountSettingsScreen extends AbstractScreen {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.screen_account);

		TextView nameView = (TextView) findViewById(R.id.name);
		Typeface tf = Typeface.createFromAsset(getAssets(), C.FortuneCity);
		nameView.setTypeface(tf);
	}

	@Override
	protected void onResume() {
		super.onResume();
		String username = Prefs.get(this).getString(Prefs.USERNAME, "");
		((TextView) findViewById(R.id.textUsername)).setText(username);
		TextView websiteView = (TextView) findViewById(R.id.textWebsiteInfo);
		TextView devInfoView = (TextView) findViewById(R.id.textDeviceInfo);
		try {
			String s = Secure
					.getString(getContentResolver(), Secure.ANDROID_ID);
			final String u = "+" + s.substring(4, 5) + s.substring(12, 16);
			if (username.equals(u)) {
				String deviceInfo = Build.MANUFACTURER.toUpperCase() + " "
						+ Build.MODEL;
				devInfoView.setText(deviceInfo);
				websiteView.setText(getResources().getString(
						R.string.WebsiteInfo, username,
						Prefs.get(this).getString(Prefs.PASSWORD, "")));
			} else {
				devInfoView.setText("");
				websiteView.setText(getResources().getString(
						R.string.WebsiteInfo, username, ""));
			}
		} catch (Exception exc) {
			Log.w(exc);
		}

	}

	public void onChangeUser(View view) {
		Intent intent = new Intent(this, ChangeUserScreen.class);
		startActivity(intent);
		finish();
	}

	public void onDeleteAccount(View view) {
		final AlertDialog.Builder alert1 = new AlertDialog.Builder(this);
		alert1.setMessage(R.string.ReallyDeleteAccount);
		alert1.setPositiveButton(R.string.Yes,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						final AlertDialog.Builder alert = new AlertDialog.Builder(
								AccountSettingsScreen.this);
						alert.setMessage(R.string.DeleteAccount);
						final EditText input = new EditText(
								AccountSettingsScreen.this);
						input.setHint(R.string.PleaseGiveUsFeedbackWhyDelete);
						alert.setView(input);
						alert.setPositiveButton(
								getResources()
										.getString(R.string.DeleteAccount),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										String value = input.getText()
												.toString().trim();
										sendDeactivate(value);
									}
								});
						alert.setNegativeButton(
								getResources().getString(R.string.Cancel),
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int whichButton) {
										dialog.cancel();
									}
								});
						alert.show();
					}
				});
		alert1.setNegativeButton(getResources().getString(R.string.Cancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						dialog.cancel();
					}
				});
		alert1.show();
	}

	private void sendDeactivate(String value) {
		try {
			JSONObject obj = prepareObj();
			obj.put("msg", value);

			doAction(AbstractScreen.ACTION_DEACTIVATE, obj, getResources()
					.getString(R.string.DeleteAccount), new ResultWorker() {

				@Override
				public void onResult(final String result, Context context) {
					Prefs.get(AccountSettingsScreen.this).edit()
							.putString(C.account, null)
							.putBoolean(Prefs.STATUS_ONOFF, false)
							.putString(Prefs.USERNAME, "")
							.putString(Prefs.PASSWORD, "").commit();
					stopService(new Intent(AccountSettingsScreen.this,
					        C.trackingServiceClass));
					Prefs.get(AccountSettingsScreen.this)
							.edit()
							.putLong(Prefs.LAST_LOGOUT,
									System.currentTimeMillis()).commit();
					finish();
				}

			});

		} catch (Exception exc) {
			Log.w(exc);
		}
	}
}
