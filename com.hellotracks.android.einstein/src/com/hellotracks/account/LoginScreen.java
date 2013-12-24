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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import com.hellotracks.network.RegisterScreen;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.CompatibilityUtils;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;
import com.hellotracks.util.Utils;

import de.greenrobot.event.EventBus;

public class LoginScreen extends RegisterScreen {

    private SharedPreferences settings;
    private EditText userText;
    private EditText pwdText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_login_options);

        if (getIntent().getStringExtra(C.errortext) != null) {
            findViewById(R.id.textError).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textError)).setText(getIntent().getStringExtra(C.errortext));
        }

        userText = (EditText) findViewById(R.id.userText);
        pwdText = (EditText) findViewById(R.id.passwordText);

        EventBus.getDefault().register(this, LoginEvent.class);
    }
    
    @Override
    protected void onResume() {
        findViewById(R.id.loginExistingButton).setEnabled(true);
        findViewById(R.id.loginWithDeviceButton).setEnabled(true);
        findViewById(R.id.signupButton).setEnabled(true);
        super.onResume();
    }

    public void onEventMainThread(LoginEvent e) {
        setResult(C.RESULTCODE_LOGIN_SUCCESS, new Intent());
        finish();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    public void onForgotPassword(View view) {
        showDialog(DIALOG_FORGOTPASSWORD);
    }

    public void onLoginWithExisting(View view) {
        view.setEnabled(false);
        findViewById(R.id.loginWithDeviceButton).setEnabled(false);
        findViewById(R.id.signupButton).setEnabled(false);
        startActivityForResult(new Intent(this, LoginExistingScreen.class), C.REQUESTCODE_LOGIN);
    }

    public void onVideoIntroduction(View view) {
        gaSendButtonPressed("video_introduction");
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.youtube.com/watch?v=x31YAc6c8R0")));
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

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            AlertDialog dlg = new AlertDialog.Builder(this).setMessage(R.string.CloseAppQuestion)
                    .setPositiveButton(R.string.CloseApp, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            setResult(C.RESULTCODE_CLOSEAPP);
                            finish();
                        }
                    }).setNegativeButton(R.string.No, new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).setCancelable(true).create();
            dlg.setCanceledOnTouchOutside(true);
            dlg.show();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void onLoginDevice(final View view) {
        view.setEnabled(false);
        findViewById(R.id.loginExistingButton).setEnabled(false);
        findViewById(R.id.signupButton).setEnabled(false);
        Ui.makeText(this, R.string.JustASecond, Toast.LENGTH_SHORT).show();
        doLoginDevice(this, getLastLocation(), new Runnable() {
            @Override
            public void run() {
                try {
                    view.setEnabled(true);
                    findViewById(R.id.loginExistingButton).setEnabled(true); 
                    findViewById(R.id.signupButton).setEnabled(true);
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        });
    }

    public static void doLoginDevice(final Activity activity, final Location lastLocation, final Runnable doneListener) {
        try {
            if (!isOnline(activity, true)) {
                if (doneListener != null) {
                    doneListener.run();
                }
                return;
            }
            final String u = Utils.getDeviceAccountUsername(activity);
            final String p = Utils.getDeviceAccountPassword(activity);

            Prefs.get(activity).edit().putString(Prefs.USERNAME, u).putString(Prefs.PASSWORD, p).commit();

            JSONObject data = AbstractScreen.prepareObj(activity);
            data.put("man", Build.MANUFACTURER);
            data.put("mod", Build.MODEL);
            data.put("os", "Android " + Build.VERSION.RELEASE);
            data.put("ver", activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionCode);
            data.put("vername", activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName);
            Locale locale = Locale.getDefault();
            TimeZone timezone = TimeZone.getDefault();
            data.put("language", locale.getLanguage());
            data.put("country", locale.getCountry());
            data.put("timezone", timezone.getID());
            AbstractScreen.doAction(activity, AbstractScreen.ACTION_LOGIN, data, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    Prefs.get(activity).edit().putString(Prefs.USERNAME, u).putString(Prefs.PASSWORD, p)
                            .putBoolean(Prefs.STATUS_ONOFF, true).commit();
                    activity.setResult(C.RESULTCODE_LOGIN_SUCCESS, new Intent());
                    activity.finish();
                    EventBus.getDefault().post(new LoginEvent());
                }

                @Override
                public void onFailure(final int status, final Context context) {
                    Prefs.get(activity).edit().putString(Prefs.USERNAME, "").putString(Prefs.PASSWORD, "").commit();
                    
                    
                    final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                    alert.setMessage(R.string.PleaseEnterNameFirst);
                    final EditText input = new EditText(activity);
                    input.setHint(R.string.Name);
                    alert.setView(input);
                    alert.setCancelable(false);
                    alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString().trim();
                            if (value == null || value.trim().length() == 0) {
                                value = Build.MANUFACTURER.toUpperCase() + " " + Build.MODEL;
                            }
                            performRegister(activity, lastLocation, u, p, value);
                        }
                    });
                    alert.show();
                    if (doneListener != null) {
                        doneListener.run();
                    }
                }

                @Override
                public void onError() {
                    Ui.makeText(activity, R.string.InternetConnectionNeeded, Toast.LENGTH_SHORT).show();
                    Prefs.get(activity).edit().putString(Prefs.USERNAME, "").putString(Prefs.PASSWORD, "").commit();
                    if (doneListener != null) {
                        doneListener.run();
                    }
                    super.onError();
                }
            });

        } catch (Exception exc) {
            Log.w(exc);
            Ui.showText(activity, R.string.DoesNotWorkWithThisPhone);
        }
    }

    public static void performRegister(final Activity activity, final Location lastLocation, final String u,
            final String p, String value) {
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
            sendRegistration(activity, registerObj, u, p, false, true);
        } catch (Exception exc) {
            Log.w(exc);
            Ui.showText(activity, R.string.SomethingWentWrong);
        }
    }

    public void onSignup(View view) {
        startActivityForResult(new Intent(this, SignUpScreen.class), C.REQUESTCODE_LOGIN);
    }

}
