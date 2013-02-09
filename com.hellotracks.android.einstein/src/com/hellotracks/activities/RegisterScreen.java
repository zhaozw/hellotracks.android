package com.hellotracks.activities;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingService.Mode;
import com.hellotracks.einstein.HomeMapScreen;
import com.hellotracks.einstein.RegisterCompanyScreen;
import com.hellotracks.model.ResultWorker;

public abstract class RegisterScreen extends AbstractScreen {
	
	public static boolean isEmailAddress(String username) {
		try {
			if (username.contains(" "))
				return false;
			String[] both = username.split("@");
			if (both.length != 2)
				return false;
			if (both[0].length() == 0)
				return false;
			String[] right = both[1].split("\\.");
			if (right.length < 2)
				return false;
			if (right[0].length() <= 1 || right[1].length() <= 1)
				return false;
			return true;
		} catch (Exception exc) {
			return false;
		}
	}

	public void onBack(View view) {
		finish();
	}

	public void doRegister(final JSONObject registerObj,
			final boolean createCompany) throws Exception {
		try {
			final String email = ((TextView) findViewById(R.id.userText))
					.getText().toString().trim().replaceAll("\n", "");
			final String pwd = ((TextView) findViewById(R.id.passwordText))
					.getText().toString().trim().replaceAll("\n", "");
			if (!isEmailAddress(email)) {
				Toast.makeText(this,
						getResources().getString(R.string.invalidEmail),
						Toast.LENGTH_LONG).show();
				throw new Exception();
			}

			if (pwd.length() < 6 || pwd.contains(" ") || pwd.contains("\n")) {
				Toast.makeText(this,
						getResources().getString(R.string.invalidPassword),
						Toast.LENGTH_LONG).show();
				throw new Exception();
			}
			registerObj.put("username", email);
			registerObj.put("password", pwd);

			Locale locale = Locale.getDefault();
			TimeZone timezone = TimeZone.getDefault();
			registerObj.put("language", locale.getLanguage());
			registerObj.put("country", locale.getCountry());
			registerObj.put("timezone", timezone.getID());

			String confirmMsg = getResources().getString(
					R.string.PleaseConfirmEmail, email);
			AlertDialog.Builder ab = new AlertDialog.Builder(this);
			ab.setMessage(confirmMsg);
			ab.setPositiveButton(R.string.Confirm,
					new DialogInterface.OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							sendRegistration(registerObj, email, pwd,
									createCompany);
						}

					}).setNegativeButton(R.string.ChangeEmail, null);
			ab.show();
		} catch (Exception exc) {
			Log.w(exc);
			throw exc;
		}
	}

	public void sendRegistration(final JSONObject registerObj,
			final String email, final String pwd, final boolean createCompany) {
		try {
			String msg = getResources().getString(R.string.registering) + " "
					+ email + "...";
			doAction(AbstractScreen.ACTION_REGISTER, registerObj, msg,
					new ResultWorker() {

						@Override
						public void onResult(String result, Context context) {
							Toast.makeText(
									RegisterScreen.this,
									getResources()
											.getString(
													R.string.userRegisteredSuccessfully),
									Toast.LENGTH_LONG).show();
							SharedPreferences sprefs = PreferenceManager
									.getDefaultSharedPreferences(getApplicationContext());
							sprefs.edit()
									.putString(Prefs.USERNAME, email)
									.putString(Prefs.PASSWORD, pwd)
									.putString(Prefs.MODE,
											Mode.sport.toString())
									.putBoolean(Prefs.STATUS_ONOFF, false)
									.commit();
							finish();
							startActivity(new Intent(getApplicationContext(),
									HomeMapScreen.class));
							if (createCompany) {
								Intent intent = new Intent(
										getApplicationContext(),
										RegisterCompanyScreen.class);
								intent.putExtra("force", true);
								startActivity(intent);
							}
						}

					});
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

}