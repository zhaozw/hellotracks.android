package com.hellotracks.c2dm;

import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.hellotracks.Logger;
import com.hellotracks.Mode;
import com.hellotracks.Prefs;
import com.hellotracks.api.API;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.util.Time;

public class C2DMReceiver extends C2DMBaseReceiver {
    public static final String SENDER = "152563520904";

    public C2DMReceiver() {
        super(SENDER);
    }

    @Override
    public void onRegistered(Context context, String registration) {
        Logger.d("onRegistered");
        try {
            JSONObject obj = AbstractScreen.prepareObj(context);
            obj.put("gcm_registration", registration);

            API.doAction(context, AbstractScreen.ACTION_SETVALUE, obj, null, null);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    @Override
    public void onUnregistered(Context context) {
        Logger.d("onUnregistered");
    }

    @Override
    public void onError(Context context, String errorId) {
    }

    @Override
    public void onMessage(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            String msg = (String) extras.get("msg");
            String title = (String) extras.get("title");
            String account = (String) extras.get("account");
            String uri = null;
            int type;
            try {
                if (msg != null && msg.startsWith("@!")) {
                    if (C.GCM_CMD_STARTTRACKINGSERVICE.equals(msg)) {
                        boolean tracking = Prefs.get(context).getBoolean(Prefs.STATUS_ONOFF, false);
                        if (!tracking) {
                            Prefs.get(context).edit().putBoolean(Prefs.STATUS_ONOFF, true)
                                    .putLong(Prefs.TRACKING_AUTOSTOP_AT, System.currentTimeMillis() + 20 * Time.MIN)
                                    .putString(Prefs.SEND_LOCATION_TO, account).commit();
                        } else {
                            Prefs.get(context).edit().putString(Prefs.MODE, Mode.automatic.toString())
                                    .putString(Prefs.SEND_LOCATION_TO, account).commit();
                        }
                        AbstractScreen.maybeStartService(this);
                    } else if (C.GCM_CMD_STOPTRACKINGSERVICE.equals(msg)) {
                        Prefs.get(context).edit().putBoolean(Prefs.STATUS_ONOFF, false).commit();
                        AbstractScreen.stopAllServices(this);
                    } else if (C.GCM_CMD_STARTTRANSPORT.equals(msg)) {
                        Prefs.get(context).edit().putString(Prefs.MODE, Mode.transport.name())
                                .putBoolean(Prefs.STATUS_ONOFF, true).commit();
                        AbstractScreen.maybeStartService(this);
                    } else if (C.GCM_CMD_STARTOUTDOOR.equals(msg)) {
                        Prefs.get(context).edit().putString(Prefs.MODE, Mode.sport.name())
                                .putBoolean(Prefs.STATUS_ONOFF, true).commit();
                        AbstractScreen.maybeStartService(this);
                    } else if (msg.startsWith(C.GCM_CMD_PLAYSTORE)) {
                        String txt = msg.substring(C.GCM_CMD_PLAYSTORE.length());
                        LauncherUtils.generatePlayStoreNotification(context, txt.trim());
                    } else if (msg.startsWith(C.GCM_CMD_URI)) {
                        int space = msg.indexOf("text:");
                        if (space < 0)
                            space = msg.length();
                        uri = msg.substring(5, space);
                        String txt = msg.substring(space + 5);
                        LauncherUtils.generateUriNotification(context, title, uri.trim(), txt.trim());
                    } else if (msg.startsWith(C.GCM_CMD_DIRECTMSG)) {
                        int idx = msg.indexOf("text:");
                        if (idx > 0) {
                            String text = msg.substring(idx + 5);
                            LauncherUtils.sendNotification(context, account, text);
                        }        
                    }
                    return;
                }

                if (msg.startsWith("@uri")) {
                    int space = msg.indexOf("text:");
                    if (space < 0)
                        space = msg.length();
                    uri = msg.substring(4, space);
                    String txt = msg.substring(space + 5);

                    msg = txt.trim();
                }

                type = Integer.parseInt((String) extras.get("type"));
            } catch (Exception exc) {
                type = 0;
            }

            Intent i = new Intent();
            i.putExtra("msg", msg);
            i.putExtra("title", title);
            i.putExtra("type", type);
            if (account != null)
                i.putExtra("account", account);
            i.setAction(Prefs.PUSH_INTENT);
            context.sendBroadcast(i);

            LauncherUtils.generateNotification(context, msg, title, account, type, intent, uri);
        }
    }

    /**
     * Register or unregister based on phone sync settings. Called on each performSync by the SyncAdapter.
     */
    public static void refreshAppC2DMRegistrationState(Context context) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String username = settings.getString(Prefs.USERNAME, null);
        String password = settings.getString(Prefs.PASSWORD, null);
        if (username != null && password != null) {
            Logger.i("register gcm");
            C2DMessaging.register(context, SENDER);
        } else {
            Logger.i("unregister gcm");
            C2DMessaging.unregister(context);
        }

    }
}