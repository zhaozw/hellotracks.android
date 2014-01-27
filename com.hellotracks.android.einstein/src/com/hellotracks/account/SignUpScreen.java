package com.hellotracks.account;

import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.TargetApi;
import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.C;
import com.hellotracks.base.WebScreen;
import com.hellotracks.db.Closer;
import com.hellotracks.network.RegisterScreen;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.Ui;
import com.hellotracks.util.Utils;

public class SignUpScreen extends RegisterScreen {

    private boolean business = false;

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.screen_login_signup);

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
                } catch (Exception exc) {
                    Logger.e(exc);
                } finally {
                    Closer.close(c);
                }
            }
        }
        setupActionBar(R.string.Back);
    }

    public void onBack(View view) {
        Prefs.get(this).edit().putString(C.account, null).putBoolean(Prefs.STATUS_ONOFF, false)
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
                    } catch (Exception exc) {
                        Logger.e(exc);
                    }
                }
            }, 1500);
            String name = ((TextView) findViewById(R.id.nameText)).getText().toString().trim().replaceAll("\n", "");

            String phone = ((TextView) findViewById(R.id.phoneText)).getText().toString().trim().replaceAll("\n", "");

            if (name.length() <= 2) {
                Ui.showText(this, getResources().getString(R.string.InvalidName));
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
            Logger.w(exc);
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