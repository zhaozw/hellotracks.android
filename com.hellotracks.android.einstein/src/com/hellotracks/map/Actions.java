package com.hellotracks.map;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingSender;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.ActivitiesScreen;
import com.hellotracks.base.C;
import com.hellotracks.messaging.MessagesScreen;
import com.hellotracks.network.NewPlaceScreen;
import com.hellotracks.profile.NewProfileScreen;
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
                            registerPlace(screen, value, latitude, longitude, finishOnResult, false);
                        }
                    });

            AlertDialog dlg = alert.create();
            dlg.setCanceledOnTouchOutside(true);
            dlg.show();

        }
    }

    public static void registerPlace(final AbstractScreen screen, String name, double latitude, double longitude,
            final boolean finishOnResult, final boolean forNetwork) {
        try {
            String owner = Prefs.get(screen).getString(Prefs.USERNAME, "");
            int radiusMeter = 400;

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
            String msg = screen.getResources().getString(R.string.registering) + " " + name + "...";
            screen.doAction(AbstractScreen.ACTION_REGISTER, registerObj, msg, new ResultWorker() {

                @Override
                public void onResult(String result, Context context) {
                    Ui.makeText(screen, screen.getResources().getString(R.string.placeRegisteredSuccessfully),
                            Toast.LENGTH_LONG).show();
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

        String coord = m.getPosition().latitude + "," + m.getPosition().longitude;
        String link = "http://maps.google.com/maps?ll=" + coord;

        if (useLiveMap) {
            String user = Prefs.get(activity).getString(Prefs.USERNAME, "");
            String pwd = TrackingSender.md5("0", Prefs.get(activity).getString(Prefs.PASSWORD, ""));
            StringBuilder sb = new StringBuilder();
            sb.append("http://hellotracks.com/live.html?usr=").append(user).append("&pwd=").append(pwd)
                    .append("&tok=0");
            link = sb.toString();
        }

        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, m.getTitle() + " " + link);
        sendIntent.setType("text/plain");
        activity.startActivity(sendIntent);
    }
}
