package com.hellotracks.account;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.C;
import com.hellotracks.db.Closer;
import com.hellotracks.network.RegisterScreen;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.Ui;
import com.hellotracks.util.Utils;

import de.greenrobot.event.EventBus;

public class LoginScreen extends RegisterScreen {

    private SharedPreferences settings;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH) @Override
    protected void onCreate(Bundle savedInstanceState) {
        settings = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_login_or_signup);
        
        
        TextView slogan = (TextView) findViewById(R.id.textSlogan);
        Typeface laBelle = Typeface.createFromAsset(getAssets(), C.LaBelle);
        slogan.setTypeface(laBelle);
        
        TextView ht = (TextView) findViewById(R.id.textTitle);
        Typeface fortCity = Typeface.createFromAsset(getAssets(), C.FortuneCity);
        ht.setTypeface(fortCity);

        if (getIntent().getStringExtra(C.errortext) != null) {
            findViewById(R.id.textError).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.textError)).setText(getIntent().getStringExtra(C.errortext));
        }

        String username = PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(
                Prefs.USERNAME, "");
        TextView userText = (TextView) findViewById(R.id.userText);
        TextView nameText = (TextView) findViewById(R.id.nameText);
        if (username != null && username.length() > 0) {
            userText.setText(username);
        } else {
            Account[] accounts = AccountManager.get(this).getAccountsByType("com.google");
            for (Account a : accounts) {
                userText.setText(a.name);
                break;
            }

            if (Utils.hasICS()) {
                Cursor c = null;
                try {
                    c = getContentResolver().query(ContactsContract.Profile.CONTENT_URI, null, null, null, null);
                    int count = c.getCount();
                    String[] columnNames = c.getColumnNames();
                    c.moveToFirst();
                    int position = c.getPosition();
                    if (count == 1 && position == 0) {
                        for (int j = 0; j < columnNames.length; j++) {
                            String columnName = columnNames[j];
                            if ("display_name".equals(columnName)) {
                                String columnValue = c.getString(c.getColumnIndex(columnName));
                                nameText.setText(columnValue);
                            }
                        }
                    }
                    findViewById(R.id.passwordText).requestFocus();
                } catch (Exception exc) {
                    Logger.e(exc);
                } finally {
                    Closer.close(c);
                }
            }
        }

        EventBus.getDefault().register(this, LoginEvent.class);
    }

    @Override
    protected void onResume() {
        findViewById(R.id.loginExistingButton).setEnabled(true);
        findViewById(R.id.signUpButton).setEnabled(true);
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
        gaSendButtonPressed("forgot_password");
        showDialog(DIALOG_FORGOTPASSWORD);
    }

    public void onLoginWithExisting(View view) {
        gaSendButtonPressed("login_exisiting");
        view.setEnabled(false);
        findViewById(R.id.signUpButton).setEnabled(false);
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
            sendRegistration(activity, registerObj, u, p, false, true, null);
        } catch (Exception exc) {
            Logger.w(exc);
            Ui.showText(activity, R.string.SomethingWentWrong);
        }
    }

}
