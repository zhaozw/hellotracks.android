package com.hellotracks.account;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.map.HomeMapScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.network.RegisterScreen;
import com.hellotracks.types.LatLng;

public class ChangeUserScreen extends RegisterScreen {

    private SharedPreferences settings;
    private EditText userText;
    private EditText pwdText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_changeuser);

        if (getIntent().getStringExtra(C.errortext) != null) {
            ((TextView) findViewById(R.id.textError)).setText(getIntent().getStringExtra(C.errortext));
        } else {
            findViewById(R.id.textError).setVisibility(View.GONE);
        }

        userText = (EditText) findViewById(R.id.userText);
        pwdText = (EditText) findViewById(R.id.passwordText);
        
        setupActionBar(R.string.AccountSettings);
    }

    public void onBack(View view) {
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String username = settings.getString(Prefs.USERNAME, "");
        String password = settings.getString(Prefs.PASSWORD, "");
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
            alert.setPositiveButton(getResources().getString(R.string.OK), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString().trim();
                    if (value.length() > 0) {
                        try {
                            JSONObject obj = prepareObj();
                            obj.put(C.usr, value);
                            doAction(ACTION_REQUESTPASSWORD, obj, null);
                        } catch (Exception e) {
                        }
                    }

                }
            });
            alert.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    dialog.cancel();
                }
            });
            return alert.create();
        }
        return super.onCreateDialog(id, bundle);
    }

    public void onLogin(final View view) {
        final String user = userText.getText().toString().trim();
        final String pwd = pwdText.getText().toString().trim();
        final int logins = settings.getInt(Prefs.LOGINS, 0);
        settings.edit().putString(Prefs.USERNAME, user).putString(Prefs.PASSWORD, pwd).putInt(Prefs.LOGINS, logins + 1)
                .commit();

        if (user.length() > 0 && pwd.length() > 0) {
            finish();
        } else {
            Toast.makeText(this, getResources().getString(R.string.enterUsernameAndPasswordToLogin), Toast.LENGTH_LONG)
                    .show();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onLoginDevice(View view) {
        doLoginDevice(this, getLastLocation(), true, "");
    }

    public static void doLoginDevice(final Activity activity, final Location lastLocation,
            final boolean finishActivity, final String name) {
        try {
            if (!isOnline(activity, true)) {
                return;
            }
            String s = getUniqueString(activity);
            final String u = "+" + s.substring(4, 5) + s.substring(12, 16);
            final String p = s.substring(1, 4) + s.substring(8, 11);

            Prefs.get(activity).edit().putString(Prefs.USERNAME, u).putString(Prefs.PASSWORD, p).commit();

            JSONObject data = AbstractScreen.prepareObj(activity);
            data.put("man", Build.MANUFACTURER);
            data.put("mod", Build.MODEL);
            data.put("os", "Android " + Build.VERSION.RELEASE);
            data.put("ver", activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode);
            data.put("vername", activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName);

            AbstractScreen.doAction(activity, AbstractScreen.ACTION_LOGIN, data, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    if (finishActivity) {
                        Prefs.get(activity).edit().putBoolean(Prefs.STATUS_ONOFF, true).commit();
                        activity.finish();
                    }
                }

                @Override
                public void onFailure(final int status, final Context context) {
                    String value;
                    if (name == null || name.trim().length() == 0) {
                        value = Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL;
                    } else {
                        value = name;
                    }
                    try {
                        JSONObject registerObj = new JSONObject();
                        registerObj.put("accounttype", C.person);
                        registerObj.put("name", value);
                        registerObj.put("username", u);
                        registerObj.put("password", p);

                        Locale locale = Locale.getDefault();
                        if (locale != null) {
                            registerObj.put("language", locale.getLanguage());
                            registerObj.put("country", locale.getCountry());
                        }
                        TimeZone timezone = TimeZone.getDefault();
                        if (timezone != null)
                            registerObj.put("timezone", timezone.getID());
                        if (lastLocation != null) {
                            LatLng ll = new LatLng(lastLocation);
                            if (ll.lat + ll.lng != 0) {
                                registerObj.put("latitude", ll.lat);
                                registerObj.put("longitude", ll.lng);
                            }
                        }
                        sendRegistration(activity, registerObj, u, p, false, finishActivity);
                    } catch (Exception exc) {
                        Log.w(exc);
                        Toast.makeText(activity, R.string.SomethingWentWrong, Toast.LENGTH_LONG).show();
                    }
                }
            });

        } catch (Exception exc) {
            Log.w(exc);
            Toast.makeText(activity, R.string.DoesNotWorkWithThisPhone, Toast.LENGTH_LONG).show();
        }
    }

    private static String getUniqueString(final Activity activity) {
        String s;
        try {
            s = Secure.getString(activity.getContentResolver(), Secure.ANDROID_ID);
            if (s == null)
                throw new Exception();
        } catch (Exception e1) {
            try {
                TelephonyManager tManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
                s = tManager.getDeviceId();
                if (s == null)
                    throw new Exception();
            } catch (Exception e3) {
                s = System.currentTimeMillis() + "a" + System.currentTimeMillis();
            }
        }
        if (s.length() < 16) {
            s += "abcdefghijklmnopqrstuvwxyz";
        }
        return s;
    }

    public void onNewUser(View view) {
        startActivity(new Intent(this, SignUpScreen.class));
    }

}
