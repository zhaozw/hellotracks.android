package com.hellotracks.base;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.hellotracks.BestTrackingService;
import com.hellotracks.Log;
import com.hellotracks.Mode;
import com.hellotracks.NewTrackingService;
import com.hellotracks.OldTrackingService;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.TrackingSender;
import com.hellotracks.api.StringRequest;
import com.hellotracks.tracks.TrackListScreen;
import com.hellotracks.util.ContactAccessor;
import com.hellotracks.util.ContactInfo;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.Ui;

public abstract class AbstractScreen extends SherlockFragmentActivity implements IActions {

    public static final String TITLE = "title";
    public static final String URL = "url";
    public static final String URL2 = "url2";
    public static final String INFO = "info";
    public static final String NAME = "title";
    public static final String MESSAGE = "msg";
    public static final String ACCOUNT = "account";
    public static final String ID = "id";
    public static final String TRACK = "track";

    public static final String DATA = "data";
    public static final String TYPE = "type";

    public static final int DIALOG_SHAREMESSAGE = 101;
    public static final int DIALOG_SENDMESSAGE = 102;

    public static final String FIELD_VERSION = "version";
    public static final String FIELD_DATA = "data";
    public static final int CURRENT_VERSION = 1;

    public static final int MAY_SHOWTRACKS = 1 << 1;
    public static final int MAY_SHOWMAP = 1 << 2;
    public static final int MAY_SENDMSG = 1 << 3;
    public static final int MAY_EDIT = 1 << 4;
    public static final int MAY_VIEW = 1 << 5;
    public static final int MAY_DELETE = 1 << 6;
    public static final int IS_LINKED = 1 << 7;
    public static final int MAY_SHARE = 1 << 8;
    public static final int IS_PUBLIC = 1 << 9;

    public static final int TYPE_INVITATION = 1 << 2;
    public static final int TYPE_RECOMMENDATION = 1 << 3;
    public static final int TYPE_EXTERNAL = 1 << 4;

    protected final ContactAccessor mContactAccessor = ContactAccessor.getInstance();

    protected void doAction(String action, JSONObject data, final ResultWorker runnable) throws JSONException,
            UnsupportedEncodingException {
        doAction(action, data, null, runnable);
    }

    protected void doAction(String action, JSONObject data, String message, final ResultWorker runnable)
            throws JSONException, UnsupportedEncodingException {
        doAction(AbstractScreen.this, action, data, message, runnable);
    }

    protected static HashMap<Context, RequestQueue> queues = new HashMap<Context, RequestQueue>();
    
    
    public RequestQueue getRequestQueue() {
        RequestQueue queue = queues.get(this);
        if (queue == null) {
            queue = Volley.newRequestQueue(this);
            queues.put(this, queue);
        }
        return queue;
    }

    public static void doAction(final Context context, String action, JSONObject data, String message,
            final ResultWorker runnable) throws JSONException, UnsupportedEncodingException {
        RequestQueue queue = queues.get(context);
        if (queue == null) {
            queue = Volley.newRequestQueue(context);
            queues.put(context, queue);
        }

        final ProgressDialog dialog;
        if (message != null) {
            dialog = ProgressDialog.show(context, "", context.getResources().getString(R.string.JustASecond), true,
                    true);
            dialog.show();
        } else {
            dialog = null;
        }

        JSONObject body = new JSONObject();
        body.put(FIELD_VERSION, CURRENT_VERSION);
        body.put(FIELD_DATA, data);

        String url = Prefs.CONNECTOR_BASE_URL + action;

        Response.ErrorListener errListener = new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                try {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    if (runnable != null) {
                        runnable.onError();
                    }
                } catch (Exception exc) {
                    Log.e(exc);
                }
            }
        };

        StringRequest req = new StringRequest(url, body, new Response.Listener<String>() {

            @Override
            public void onResponse(String string) {
                try {
                    if (dialog != null) {
                        dialog.dismiss();
                    }
                    if (runnable == null)
                        return;

                    if (string.length() == 0) {
                        runnable.onError();
                    } else {
                        try {
                            Log.d(string);
                            JSONObject response = new JSONObject(string);
                            int status = ResultWorker.STATUS_OK;
                            if (response.has("status"))
                                status = response.getInt("status");
                            try {
                                if (status == ResultWorker.STATUS_OK) {
                                    runnable.onResult(string, context);
                                } else {
                                    runnable.onFailure(status, context);
                                }
                            } catch (Exception exc) {
                                Log.e(exc);
                            }
                        } catch (Exception exc) {
                            try {
                                new JSONArray(string);
                                runnable.onResult(string, context);
                            } catch (Exception exc2) {
                                Log.e(exc2);
                            }
                        }
                    }
                } catch (Exception exc) {
                    Log.e(exc);
                    Ui.makeText(context, R.string.SomethingWentWrong, Toast.LENGTH_LONG).show();
                }
            }
        }, errListener);
        req.setShouldCache(false);
        queue.add(req);
    }

    public boolean isOnline(boolean alert) {
        return isOnline(this, alert);
    }

    protected EasyTracker mEasyTracker;

    @Override
    protected void onStart() {
        super.onStart();
        mEasyTracker = EasyTracker.getInstance(this);
        mEasyTracker.activityStart(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mEasyTracker.activityStop(this);
    }

    public static boolean isOnline(Context context, boolean alert) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            boolean conn = cm.getActiveNetworkInfo().isConnected();
            if (!conn) {
                Ui.makeText(context, context.getResources().getString(R.string.InternetConnectionNeeded),
                        Toast.LENGTH_LONG).show();
            }
            return conn;
        } catch (Exception exc) {
            Ui.makeText(context, context.getResources().getString(R.string.InternetConnectionNeeded), Toast.LENGTH_LONG)
                    .show();
            return false;
        }
    }

    public void sendMessage(String receiver, String value, ResultWorker resultWorker) {
        try {
            JSONObject obj = prepareObj();
            obj.put("msg", value);
            obj.put("receiver", receiver);
            doAction(AbstractScreen.ACTION_SENDMSG, obj, getResources().getString(R.string.SendNow), resultWorker);

        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void sendMessage(String[] receivers, String value, ResultWorker resultWorker) {
        try {
            JSONObject obj = prepareObj();
            obj.put("msg", value);
            JSONArray array = new JSONArray();
            for (String r : receivers) {
                array.put(r);
            }
            obj.put("receivers", array);
            doAction(AbstractScreen.ACTION_SENDMSG, obj, getResources().getString(R.string.SendNow), resultWorker);

        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    protected void showTracks(final String account, final String name) {
        if (!isOnline(true))
            return;

        try {
            JSONObject obj = prepareObj();
            obj.put("account", account);
            doAction(ACTION_TRACKS, obj, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    Intent intent = new Intent(AbstractScreen.this, TrackListScreen.class);
                    intent.putExtra("data", result);
                    intent.putExtra("account", account);
                    if (name != null) {
                        intent.putExtra(C.name, name);
                    }
                    startActivity(intent);
                }
            });
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupLocationClient();
    }

    protected LocationClient mLocationClient;
    protected LocationManager mLocationManager;

    private void setupLocationClient() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mLocationClient = new LocationClient(this, new ConnectionCallbacks() {

            @Override
            public void onDisconnected() {
            }

            @Override
            public void onConnected(Bundle connectionHint) {
            }
        }, new OnConnectionFailedListener() {

            @Override
            public void onConnectionFailed(ConnectionResult result) {
                Log.w("connection to loc client failed");
            }
        });
        mLocationClient.connect();
    }

    @Override
    protected void onDestroy() {
        mLocationClient.disconnect();
        super.onDestroy();
    }

    public Location getLastLocation() {
        return mLocationClient.isConnected() ? mLocationClient.getLastLocation() : mLocationManager
                .getLastKnownLocation(LocationManager.GPS_PROVIDER);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle bundle) {
        if (id == DIALOG_SENDMESSAGE) {
            final String account = bundle.getString("account");
            final String name = bundle.getString("name");
            final AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setMessage(name);
            final EditText input = new EditText(this);
            input.setHint(R.string.EnterMessage);
            alert.setView(input);
            alert.setPositiveButton(getResources().getString(R.string.SendNow), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString().trim();
                    if (value.length() > 0) {
                        sendMessage(account, value, new ResultWorker());
                    } else {
                        Ui.makeText(AbstractScreen.this, R.string.MessageWasEmpty, Toast.LENGTH_LONG).show();
                    }
                    removeDialog(DIALOG_SENDMESSAGE);
                }
            });
            alert.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    removeDialog(DIALOG_SENDMESSAGE);
                }
            });
            return alert.create();
        }
        return super.onCreateDialog(id, bundle);
    }

    public void deleteMessage(long id) {
        try {
            doAction(ACTION_DELMSG, prepareObj().put("id", id), null);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    protected void openMessageDialog(String account, String name) {
        Bundle bundle = new Bundle();
        bundle.putString("account", account);
        bundle.putString("name", name);
        showDialog(DIALOG_SENDMESSAGE, bundle);
    }

    protected JSONObject prepareObj() throws JSONException {
        return prepareObj(getApplicationContext());
    }

    public static JSONObject prepareObj(Context context) throws JSONException {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        String n = String.valueOf(System.currentTimeMillis());
        String u = settings.getString(Prefs.USERNAME, "");
        String p = settings.getString(Prefs.PASSWORD, "");
        String h = TrackingSender.md5(n, p);

        JSONObject obj = new JSONObject();
        obj.put("usr", u);
        obj.put("tok", n);
        obj.put("pwd", h);
        return obj;
    }

    public void onBack(View view) {
        finish();
    }

    public void showTrack(final View view, final String text, final long trackId, final String url,
            final String comments, final int labels, final int actions) {
        try {
            JSONObject obj = prepareObj();
            obj.put("track", trackId);
            SharedPreferences settings = Prefs.get(getApplicationContext());
            String u = settings.getString(Prefs.USERNAME, "");
            obj.put("account", u);
            doAction(ACTION_TRACKCOURSE, obj, getResources().getString(R.string.JustASecond), new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    view.post(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                Intent intent = new Intent(C.BROADCAST_ADDTRACKTOMAP);
                                JSONObject obj = new JSONObject(result);
                                String data = obj.getString("data");
                                intent.putExtra("track", data);
                                intent.putExtra("trackid", trackId);
                                intent.putExtra("text", text);
                                intent.putExtra("url", url);
                                intent.putExtra("comments", comments);
                                intent.putExtra("labels", labels);
                                intent.putExtra("actions", actions);
                                sendBroadcast(intent);
                                finish();
                            } catch (Exception exc) {
                                Log.w(exc);
                            }
                        }
                    });
                }
            });
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public static class Entry {
        public String name;
        public Runnable action;
    }

    public void onLogout(final View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(getResources().getString(R.string.logoutText)).setCancelable(false)
                .setPositiveButton(getResources().getString(R.string.Yes), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        realLogout();
                    }
                }).setNegativeButton(getResources().getString(R.string.No), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    protected void realLogout() {
        Prefs.get(AbstractScreen.this).edit().putString(C.account, null).putBoolean(Prefs.STATUS_ONOFF, false)
                .putString(Prefs.PASSWORD, "").commit();
        stopService();
        setResult(C.RESULTCODE_CLOSEAPP);
        finish();
    }

    protected void inviteByEmail() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.RecommendDesc);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint(getResources().getString(R.string.ContactEmail));
        alert.setView(input);
        alert.setPositiveButton(getResources().getString(R.string.Invite), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString().trim();
                if (value.length() > 0) {
                    if (value.contains("@") && value.contains("."))
                        sendInvitation(AbstractScreen.this, value);
                    else {
                        Ui.makeText(getApplicationContext(), getResources().getString(R.string.invalidEmail),
                                Toast.LENGTH_SHORT).show();
                    }

                }

            }
        });

        alert.setNegativeButton(getResources().getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.cancel();
            }
        });
        alert.show();
    }

    public static void sendInvitation(Context context, String value) {
        try {
            JSONObject obj = prepareObj(context);
            obj.put("invitee", value);
            doAction(context, AbstractScreen.ACTION_INVITE, obj, context.getResources().getString(R.string.Invite),
                    new ResultWorker());
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void onInviteContactBySms(View view) {
        startActivityForResult(mContactAccessor.getPickContactIntent(), C.REQUESTCODE_PICK_CONTACT);
    }

    public void onInviteContactByEmail(View view) {
        if (!isOnline(true))
            return;

        inviteByEmail();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == C.REQUESTCODE_PICK_CONTACT && resultCode == RESULT_OK) {
            loadContactInfo(data.getData(), this);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void loadContactInfo(Uri contactUri, final Context context) {

        AsyncTask<Uri, Void, ContactInfo> task = new AsyncTask<Uri, Void, ContactInfo>() {

            @Override
            protected ContactInfo doInBackground(Uri... uris) {
                return mContactAccessor.loadContact(getContentResolver(), uris[0]);
            }

            @Override
            protected void onPostExecute(ContactInfo result) {
                prepareSMS(context, result.getPhoneNumber());
                sendPendingInvitation(context, result.getDisplayName(), null, result.getPhoneNumber());
            }
        };

        task.execute(contactUri);
    }

    public static void sendPendingInvitation(Context context, String name, String email, String phone) {
        try {
            if (phone == null || email != null) {
                if (phone == null)
                    phone = "";
                if (email == null)
                    email = "";
                JSONObject obj = prepareObj(context);
                obj.put("phone", phone);
                obj.put("email", "");
                obj.put("name", name);
                obj.put("msg", "");
                doAction(context, AbstractScreen.ACTION_PENDINGINVITATION, obj, null, null);
            }
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public static void prepareSMS(Context context, String phone) {
        if (phone == null)
            phone = "";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("sms:" + phone));
        intent.putExtra("sms_body", context.getResources().getString(R.string.SMSRecommendation));
        try {
            context.startActivity(intent);
        } catch (Exception exc) {
            Ui.makeText(context, R.string.CouldNotOpenSMSApp, Toast.LENGTH_LONG).show();
        }
    }

    protected void sendSetValue(String prop, String value) {
        try {
            JSONObject obj = prepareObj();
            obj.put(prop, value);
            doAction(AbstractScreen.ACTION_SETVALUE, obj, null, null);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public static void openMarketDialog(String msg, final Context context) {
        Intent market = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.hellotracks"));
        market.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

        context.startActivity(market);
        try {
            JSONObject obj = prepareObj(context);
            obj.put("rating", "true");

            doAction(context, AbstractScreen.ACTION_SETVALUE, obj, null, null);
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    protected void openMarketDialog(String msg) {
        openMarketDialog(msg, this);
    }

    protected void setupActionBar(int title) {
        getSupportActionBar().show();
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(false);
        getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(R.drawable.header_bg));
        getSupportActionBar().setDisplayShowCustomEnabled(false);
        getSupportActionBar().setTitle(title);
    }

    public void showMessage(String msg) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setMessage(msg);
        b.setCancelable(true);
        AlertDialog dlg = b.create();
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            onBackPressed();
            break;
        }
        return true;
    }

    protected void stopService() {
        stopAllServices(this);
    }

    protected void maybeStartService() {
        maybeStartService(this);
    }

    public static boolean isMyServiceRunning(Context context, Class c) {
        ActivityManager manager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (c.getCanonicalName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static void stopService(Context context, Class[] classes) {
        for (Class c : classes) {
            Log.d("stopping service " + c);
            context.stopService(new Intent(context, c));
        }
    }

    public static void stopAllServices(Context context) {
        Log.d("stopping all services");
        context.stopService(new Intent(context, NewTrackingService.class));
        context.stopService(new Intent(context, OldTrackingService.class));
        context.stopService(new Intent(context, BestTrackingService.class));
    }

    public static void maybeStartService(Context context) {
        Mode mode = Mode.fromString(Prefs.get(context).getString(Prefs.MODE, Mode.automatic.toString()));

        stopService(context, mode.getOtherServiceClasses());

        if (!isMyServiceRunning(context, mode.getTrackingServiceClass())) {
            Log.w("service not running -> start it: mode=" + mode + " service=" + mode.getTrackingServiceClass());
            Intent serviceIntent = new Intent(context, mode.getTrackingServiceClass());
            context.startService(serviceIntent);
        }

    }
    
    public void gaSendChoicePressed(String label, int value) {
        try {
            if (mEasyTracker != null) {
                mEasyTracker.send(MapBuilder.createEvent("ui_action", "choice", label, (long) value).build());
            }
        } catch (Exception exc) {
            Log.e(exc);
        }
    }

    public void gaSendButtonPressed(String label) {
        try {
            if (mEasyTracker != null) {
                mEasyTracker.send(MapBuilder.createEvent("ui_action", "button_pressed", label, null).build());
            }
        } catch (Exception exc) {
            Log.e(exc);
        }
    }
    
    public void gaSendButtonLongPressed(String label) {
        try {
            if (mEasyTracker != null) {
                mEasyTracker.send(MapBuilder.createEvent("ui_action", "button_longpressed", label, null).build());
            }
        } catch (Exception exc) {
            Log.e(exc);
        }
    }
    
    public void gaSendButtonPressed(String label, int value) {
        try {
            if (mEasyTracker != null) {
                mEasyTracker.send(MapBuilder.createEvent("ui_action", "button_pressed", label, (long) value).build());
            }
        } catch (Exception exc) {
            Log.e(exc);
        }
    }

}
