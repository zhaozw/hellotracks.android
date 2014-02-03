package com.hellotracks.map;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;

import com.google.analytics.tracking.android.Log;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.maps.model.Marker;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingSender;
import com.hellotracks.account.ManagementScreen;
import com.hellotracks.api.API;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.ActivitiesScreen;
import com.hellotracks.base.C;
import com.hellotracks.messaging.MessagesScreen;
import com.hellotracks.places.GeofenceRequester;
import com.hellotracks.places.SimpleGeofence;
import com.hellotracks.places.SimpleGeofenceStore;
import com.hellotracks.profile.NewProfileScreen;
import com.hellotracks.profile.PlaceSettingsScreen;
import com.hellotracks.profile.ProfileSettingsScreen;
import com.hellotracks.util.MediaUtils;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.Ui;

import de.greenrobot.event.EventBus;

public class Actions {

    public static void doOnDirections(final Activity context, Location lastLocation, final double lat, final double lng) {
        com.hellotracks.types.LatLng origin = new com.hellotracks.types.LatLng(lastLocation);
        com.hellotracks.types.LatLng destination = new com.hellotracks.types.LatLng(lat, lng);

        if (origin.lat + origin.lng == 0) {
            Ui.makeText(context, R.string.NoGPSSignal, Toast.LENGTH_LONG).show();
            return;
        }

        SearchMap.asyncGetDirections(context, origin, destination,
                new SearchMap.Callback<SearchMap.DirectionsResult>() {

                    @Override
                    public void onResult(boolean success, SearchMap.DirectionsResult result) {
                        if (success) {
                            EventBus.getDefault().post(result);
                        } else {
                            Ui.makeText(context, R.string.NoEntries, Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public static void doOnCall(Activity activity, String phone) {
        if (phone != null && phone.trim().length() > 0) {
            try {
                String uri = "tel:" + phone.trim();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse(uri));
                activity.startActivity(intent);
            } catch (ActivityNotFoundException exc) {
                Toast.makeText(activity, R.string.NotAvailable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void doShowConversation(Activity activity, String account, String name) {
        Intent intent = new Intent(activity, MessagesScreen.class);
        if (account != null) {
            intent.putExtra(C.account, account);
            intent.putExtra(C.name, name);
        }
        activity.startActivity(intent);
    }

    public static void doShowActivities(Activity context, String account) {
        Intent i = new Intent(context, ActivitiesScreen.class);
        i.putExtra(C.account, account);
        context.startActivity(i);
    }

    public static void doOnUpdateLocation(final AbstractScreen activity, final String account) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setMessage(R.string.UpdateLocationMsg);
        builder.setNegativeButton(R.string.Cancel, null);
        builder.setPositiveButton(R.string.UpdateLocation, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.sendMessage(account, C.GCM_CMD_STARTTRACKINGSERVICE, null);
                Ui.showText(activity, R.string.MayTakeSomeMinutes);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public static void doCreateNewPlace(final AbstractScreen screen, final String name, final double latitude,
            final double longitude, final boolean finishOnResult) {
        if (screen.isOnline(true)) {
            final AlertDialog.Builder alert = new AlertDialog.Builder(screen);
            alert.setTitle(R.string.CreatePlace);
            final EditText input = new EditText(screen);
            input.setHint(R.string.PlaceName);
            if (!name.equals(screen.getResources().getString(R.string.CreateNewPlace)))
                input.setText(name);
            alert.setView(input);
            alert.setPositiveButton(screen.getResources().getString(R.string.Create),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString().trim();
                            registerPlace(screen, value, latitude, longitude, finishOnResult, false, false, false);
                        }
                    });

            AlertDialog dlg = alert.create();
            dlg.setCanceledOnTouchOutside(true);
            dlg.show();

        }
    }

    public static void registerPlace(final AbstractScreen screen, final String name, final double latitude,
            final double longitude, final boolean finishOnResult, final boolean forNetwork,
            final boolean notifyMeOnCheckIns, final boolean autoCheckIn) {
        try {
            String owner = Prefs.get(screen).getString(Prefs.USERNAME, "");
            final int radiusMeter = 100;

            JSONObject registerObj = new JSONObject();
            Locale locale = Locale.getDefault();
            TimeZone timezone = TimeZone.getDefault();
            registerObj.put("language", locale.getLanguage());
            registerObj.put("country", locale.getCountry());
            registerObj.put("timezone", timezone.getID());
            registerObj.put("accounttype", C.place);
            registerObj.put("name", name);
            registerObj.put("owner", owner);
            registerObj.put("extension", radiusMeter * 2);
            if (forNetwork)
                registerObj.put("visibility", "network");
            if (latitude + longitude != 0) {
                registerObj.put("latitude", latitude);
                registerObj.put("longitude", longitude);
            }
            if (notifyMeOnCheckIns) {
                registerObj.put("notify_checkin", true);
            }
            String msg = screen.getResources().getString(R.string.registering) + " " + name + "...";
            screen.doAction(AbstractScreen.ACTION_REGISTER, registerObj, msg, new ResultWorker() {

                @Override
                public void onResult(String result, Context context) {
                    Ui.makeText(screen, screen.getResources().getString(R.string.placeRegisteredSuccessfully),
                            Toast.LENGTH_LONG).show();
                    try {
                        JSONObject obj = new JSONObject(result);
                        String account = obj.getString("account");
                        if (autoCheckIn) {
                            doAddGeofence(screen, latitude, longitude, radiusMeter, account, name);
                        }
                        //                        Uri uri = Uri.parse("file:///android_asset/building.png");
                        //                        MediaUtils.post(screen, account, Prefs.CONNECTOR_BASE_URL + "uploadprofileimage",
                        //                                MediaUtils.getPath(screen, uri));
                    } catch (Exception exc) {
                        Logger.e(exc);
                    }

                    if (finishOnResult)
                        screen.finish();
                }

            });
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public static void doOpenProfile(AbstractScreen screen, String account, String name) {
        Intent intent = new Intent(screen, NewProfileScreen.class);
        intent.putExtra(C.account, account);
        intent.putExtra(C.name, name);
        screen.startActivityForResult(intent, C.REQUESTCODE_CONTACT());
    }

    public static void doShareLocation(Activity activity, Marker m, boolean useLiveMap) {
        try {
            String coord = m.getPosition().latitude + "," + m.getPosition().longitude;
            String link = "http://maps.google.com/maps?ll=" + coord;

            if (useLiveMap) {
                String user = Prefs.get(activity).getString(Prefs.USERNAME, "");
                String pwd = TrackingSender.md5("0", Prefs.get(activity).getString(Prefs.PASSWORD, ""));
                StringBuilder sb = new StringBuilder();
                sb.append("http://hellotracks.com/live.html?usr=").append(URLEncoder.encode(user, "UTF-8"))
                        .append("&pwd=").append(URLEncoder.encode(pwd, "UTF-8")).append("&tok=0");
                link = sb.toString();
            }

            Intent sendIntent = new Intent();
            sendIntent.setAction(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_TEXT, m.getTitle() + " " + link);
            sendIntent.setType("text/plain");
            activity.startActivity(sendIntent);
        } catch (UnsupportedEncodingException exc) {
            Logger.e(exc);
        }
    }

    public static void doOnPlaceEdit(Activity activity, String profileString) {
        Intent intent = new Intent(activity, PlaceSettingsScreen.class);
        intent.putExtra(C.profilestring, profileString);
        activity.startActivityForResult(intent, C.REQUESTCODE_CONTACT());
    }

    public static void doOnPersonEdit(Activity activity, String profileString) {
        Intent intent = new Intent(activity, ProfileSettingsScreen.class);
        intent.putExtra(C.profilestring, profileString);
        activity.startActivityForResult(intent, C.REQUESTCODE_CONTACT());
    }

    public static void doAddGeofence(final AbstractScreen screen, final double latitude, final double longitude,
            final int radiusMeter, String account, String name) {
        try {
            GeofenceRequester req = new GeofenceRequester(screen);
            ArrayList<Geofence> list = new ArrayList<Geofence>();
            SimpleGeofence geofence = new SimpleGeofence(account, latitude, longitude, radiusMeter,
                    Geofence.NEVER_EXPIRE, Geofence.GEOFENCE_TRANSITION_ENTER, name);
            list.add(geofence.toGeofence());
            req.addGeofences(list);
            new SimpleGeofenceStore(screen).setGeofence(account, geofence);
        } catch (Exception exc) {
            Logger.e(exc);
        }
    }

    public static void doSendMessage(Context context, String message, String[] receivers, final ResultWorker worker) {
        try {
            JSONObject obj = AbstractScreen.prepareObj(context);
            obj.put("msg", message);
            JSONArray array = new JSONArray();
            for (String r : receivers) {
                array.put(r);
            }
            obj.put("receivers", array);
            API.doAction(context, AbstractScreen.ACTION_SENDMSG, obj, null, worker);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public static void doCheckIn(Context context, String userText, String place, long ts, ResultWorker worker) {
        try {
            JSONObject obj = AbstractScreen.prepareObj(context);
            obj.put("text", userText != null ? userText : userText);
            obj.put("place", place);
            obj.put("ts", ts);
            API.doAction(context, AbstractScreen.ACTION_CHECKIN, obj, null, worker);
        } catch (Exception exc) {
            Logger.w(exc);
        }
    }

    public static void doOpenSettings(AbstractScreen screen) {
        Intent intent = new Intent(screen, ManagementScreen.class);
        intent.putExtra("profile", true);
        screen.startActivityForResult(intent, C.REQUESTCODE_CONTACT());
    }
}
