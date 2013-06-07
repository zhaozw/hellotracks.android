package com.hellotracks.activities;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.Mode;
import com.hellotracks.Prefs;
import com.hellotracks.R;
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
							sendRegistration(RegisterScreen.this, registerObj,
									email, pwd, createCompany, true);
						}

					}).setNegativeButton(R.string.ChangeEmail, null);
			ab.show();
		} catch (Exception exc) {
			Log.w(exc);
			throw exc;
		}
	}

	public static void sendRegistration(final Activity activity,
			final JSONObject registerObj, final String email, final String pwd,
			final boolean createCompany, final boolean finishActivity) {
		try {
			String msg = activity.getResources()
					.getString(R.string.registering) + " " + email + "...";
			doAction(activity, AbstractScreen.ACTION_REGISTER, registerObj,
					msg, new ResultWorker() {

						@Override
						public void onResult(String result, Context context) {
							Toast.makeText(
									context,
									context.getResources()
											.getString(
													R.string.userRegisteredSuccessfully),
									Toast.LENGTH_LONG).show();
							SharedPreferences sprefs = Prefs.get(context);
							sprefs.edit()
									.putString(Prefs.USERNAME, email)
									.putString(Prefs.PASSWORD, pwd)
									.putString(Prefs.MODE,
											Mode.sport.toString())
									.putBoolean(Prefs.STATUS_ONOFF, true)
									.commit();
							if (finishActivity)
								activity.finish();
							// startActivity(new
							// Intent(getApplicationContext(),HomeMapScreen.class));
							if (createCompany) {
								Intent intent = new Intent(context,
										RegisterCompanyScreen.class);
								intent.putExtra("force", true);
								context.startActivity(intent);
							}
						}

					});
		} catch (Exception exc) {
			Log.w(exc);
		}
	}

}