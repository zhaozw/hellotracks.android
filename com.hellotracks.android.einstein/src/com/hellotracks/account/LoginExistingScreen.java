package com.hellotracks.account;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
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
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;
import com.hellotracks.util.Utils;

import de.greenrobot.event.EventBus;

public class LoginExistingScreen extends RegisterScreen {

    private SharedPreferences settings;
    private EditText userText;
    private EditText pwdText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_login_existing);

        if (getIntent().getStringExtra(C.errortext) != null) {
            findViewById(R.id.textError).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textError)).setText(getIntent().getStringExtra(C.errortext));
        }

        userText = (EditText) findViewById(R.id.userText);
        pwdText = (EditText) findViewById(R.id.passwordText);

        EventBus.getDefault().register(this, LoginEvent.class);
    }

    public void onEvent(LoginEvent e) {
        finish();
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
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
        
        if (user.length() < 4 || pwd.length() < 6) {
            Ui.showText(this, getResources().getString(R.string.enterUsernameAndPasswordToLogin));
            return;
        }
        
        final int logins = settings.getInt(Prefs.LOGINS, 0);

        try {
            if (!isOnline(this, true)) {
                return;
            }
            
            view.setEnabled(false);
            Ui.makeText(this, R.string.JustASecond, Toast.LENGTH_SHORT).show();

            settings.edit().putString(Prefs.USERNAME, user).putString(Prefs.PASSWORD, pwd)
                    .putInt(Prefs.LOGINS, logins + 1).commit();

            JSONObject data = AbstractScreen.prepareObj(this);
            data.put("man", Build.MANUFACTURER);
            data.put("mod", Build.MODEL);
            data.put("os", "Android " + Build.VERSION.RELEASE);
            data.put("ver", getPackageManager().getPackageInfo(getPackageName(), 0).versionCode);
            data.put("vername", getPackageManager().getPackageInfo(getPackageName(), 0).versionName);

            AbstractScreen.doAction(this, AbstractScreen.ACTION_LOGIN, data, null, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    view.setEnabled(true);
                    settings.edit().putBoolean(Prefs.STATUS_ONOFF, true).commit();
                    EventBus.getDefault().post(new LoginEvent());
                    finish();
                }

                @Override
                public void onFailure(final int status, final Context context) {
                    view.setEnabled(true);
                    settings.edit().putString(Prefs.USERNAME, "").putString(Prefs.PASSWORD, "").commit();
                    
                    int txt = R.string.unkownError;
                    if (status == ResultWorker.STATUS_NORESULT)
                        txt = R.string.PleaseCheckInternetConnection;
                    else if (status == ResultWorker.ERROR_USERUNKNOWN)
                        txt = R.string.unkownUser;
                    else if (status == ResultWorker.ERROR_PASSWORDMISMATCH)
                        txt = R.string.passwordMismatch;
                    
                    Ui.makeText(context, txt, Toast.LENGTH_LONG).show();
                }

                @Override
                public void onError() {
                    view.setEnabled(true);
                    settings.edit().putString(Prefs.USERNAME, "").putString(Prefs.PASSWORD, "").commit();
                    super.onError();
                }
            });

        } catch (Exception exc) {
            Log.w(exc);
            Ui.showText(this, R.string.DoesNotWorkWithThisPhone);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            onBack(null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

}
