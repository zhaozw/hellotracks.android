package com.hellotracks.profile;

import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;
import com.hellotracks.Log;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.ActivitiesScreen;
import com.hellotracks.base.C;
import com.hellotracks.map.AbstractMapScreen.TrackLine;
import com.hellotracks.messaging.MessagesScreen;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.Ui;
import com.hellotracks.util.SearchMap.DirectionsResult;
import com.hellotracks.util.lazylist.LazyAdapter;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;
import com.squareup.picasso.Picasso;

import de.greenrobot.event.EventBus;

public class NewProfileScreen extends AbstractScreen {

    private TextView textField;
    private TextView nameField;
    private ImageView picture;
    private View board;
    private LinearLayout activityContainer;

    private Button callButton;
    private Button locationButton;
    private Button tracksButton;
    private Button activitiesButton;
    private Button messagesButton;
    private Button directionsButton;
    private Button updateLocationButton;

    private String profileString = null;
    private int depth = 0;
    private String name;
    private String phone = null;

    private Animation fadeOut;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            account = null;
            refill();
        }
    };

    private BroadcastReceiver mTrackReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent data) {
            finish();
        }

    };

    protected void onResume() {
        registerReceiver(mIntentReceiver, new IntentFilter(Prefs.TAB_PROFILE_INTENT));
        super.onResume();
    };

    @Override
    protected void onPause() {
        unregisterReceiver(mIntentReceiver);
        super.onPause();
    }

    private void openProfileEdit() {
        Intent intent = new Intent(getApplicationContext(), ProfileSettingsScreen.class);
        intent.putExtra(C.profilestring, profileString);
        startActivityForResult(intent, C.REQUESTCODE_CONTACT);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (profileString == null)
            return false;
        menu.clear();

        if (edit) {
            {
                final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.Edit);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
                item.setIcon(R.drawable.ic_action_edit);
                item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                    public boolean onMenuItemClick(MenuItem item) {
                        openProfileEdit();
                        return false;
                    }
                });
            }

        } else if (link && delete) {
            final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, R.string.Remove);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_close);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    openRemoveFromNetworkDialog();
                    return false;
                }
            });
        } else if (!link) {
            final MenuItem item = menu.add(1, Menu.NONE, Menu.NONE, isPlace ? R.string.AddToNetwork
                    : R.string.InviteToMyNetwork);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setIcon(R.drawable.ic_action_add);
            item.setOnMenuItemClickListener(new OnMenuItemClickListener() {

                public boolean onMenuItemClick(MenuItem item) {
                    if (isPlace && !link) {
                        sendInvitation(account, "");
                    } else {
                        openInvitationDialog(account, name);
                    }
                    return false;
                }
            });
        }

        return true;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (depth > 0) {
                onBack(null);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    protected void onDestroy() {
        unregisterReceiver(mTrackReceiver);
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    };

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);

        registerReceiver(mTrackReceiver, new IntentFilter(C.BROADCAST_ADDTRACKTOMAP));

        setContentView(R.layout.screen_profile_new);

        textField = (TextView) findViewById(R.id.text);
        nameField = (TextView) findViewById(R.id.name);
        picture = (ImageView) findViewById(R.id.picture);
        fadeOut = AnimationUtils.loadAnimation(this, R.anim.rotate);
        board = findViewById(R.id.board);
        activityContainer = (LinearLayout) findViewById(R.id.activityContainter);
        updateLocationButton = (Button) findViewById(R.id.buttonUpdateLocation);
        updateLocationButton.setVisibility(View.GONE);

        callButton = (Button) findViewById(R.id.buttonCall);
        locationButton = (Button) findViewById(R.id.buttonLocation);
        directionsButton = (Button) findViewById(R.id.buttonDirections);
        messagesButton = (Button) findViewById(R.id.buttonMessages);
        tracksButton = (Button) findViewById(R.id.buttonTracks);
        activitiesButton = (Button) findViewById(R.id.buttonActivities);

        ImageButton back = (ImageButton) findViewById(R.id.buttonBack);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });

        if (getIntent().hasExtra(C.account)) {
            this.account = getIntent().getStringExtra(C.account);
            refill();
        } else {
            this.account = Prefs.get(this).getString(Prefs.ACCOUNT, null);

            this.nameField.setText(Prefs.get(this).getString(Prefs.NAME, ""));
            String imgurl = Prefs.get(this).getString(Prefs.PROFILE_THUMB, null);
            if (imgurl != null) {
                Picasso.with(this).load(imgurl).into(picture);
                if (depth == 0) {
                    Prefs.get(NewProfileScreen.this).edit().putString(Prefs.PROFILE_THUMB, imgurl).commit();
                }
            }
        }

        try {
            EventBus.getDefault().register(this, SearchMap.DirectionsResult.class);
        } catch (Throwable t) {
            Log.e(t);
        }
    }

    private void disable(Button b, int icon) {
        b.setEnabled(false);
        b.setCompoundDrawablesWithIntrinsicBounds(0, icon, 0, 0);
        b.setTextColor(getResources().getColor(R.color.darkgray));
    }

    protected void refill() {
        try {
            final String uid = account == null ? Prefs.get(this).getString(Prefs.USERNAME, "") : account;

            String profileCache = Prefs.get(this).getString("profile_" + uid, null);
            if (profileCache != null) {
                try {
                    if (!profileCache.equals(profileString)) {
                        activityContainer.removeAllViews();
                        setProfileData(profileCache);
                    }
                } catch (JSONException exc) {
                    Log.w(exc);
                }
            }

            JSONObject obj = prepareObj();
            obj.put(ACCOUNT, uid);
            obj.put("count", 0);
            doAction(ACTION_PROFILE, obj, new ResultWorker() {

                @Override
                public void onResult(final String result, Context context) {
                    try {
                        if (!result.equals(profileString)) {
                            activityContainer.removeAllViews();
                            setProfileData(result);
                            Prefs.get(NewProfileScreen.this).edit().putString("profile_" + uid, result).commit();
                        }
                    } catch (JSONException exc) {
                        Log.w(exc);
                    }
                }
            });

        } catch (Exception exc2) {
            Log.w(exc2);
        }
    }

    private boolean isCompany = false;
    private boolean isPlace = false;
    private boolean edit = false;
    private boolean view = false;
    private boolean link = false;
    private boolean delete = false;
    private double latitude = 0;
    private double longitude = 0;

    protected void setProfileData(String result) throws JSONException {
        JSONObject obj = new JSONObject(profileString = result);
        name = obj.getString("name");
        int tracks = obj.getInt("tracks");
        int activities = obj.has("acts") ? obj.getInt("acts") : 1;
        int contacts = obj.getInt("contacts");
        int places = obj.getInt("places");
        String thumb = obj.getString("thumb");
        phone = obj.has("phone") ? obj.getString("phone") : "";
        String marker = obj.getString("marker");
        final int permissions = obj.getInt("permissions");
        edit = (permissions & MAY_EDIT) > 0;
        view = (permissions & MAY_VIEW) > 0;
        link = (permissions & IS_LINKED) > 0;
        delete = (permissions & MAY_DELETE) > 0;
        String txt = obj.getString("txt");
        String type = obj.getString("type");
        depth = obj.getInt("depth");
        int messages = obj.getInt("messages");
        this.account = obj.getString("account");
        isCompany = obj.has("company_permissions");
        isPlace = C.place.equals(type);
        latitude = obj.has("lat") ? obj.getDouble("lat") : 0;
        longitude = obj.has("lng") ? obj.getDouble("lng") : 0;

        int size = getResources().getDimensionPixelSize(R.dimen.title);
        nameField.setTextSize(TypedValue.COMPLEX_UNIT_PX, name.length() < 15 ? size : (size / 1.5f));

        if (edit) {
            ImageButton editButton = (ImageButton) findViewById(R.id.buttonEdit);
            editButton.setVisibility(View.VISIBLE);
            editButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    openProfileEdit();
                }
            });
        }

        if (depth == 0) {
            if (obj.has("business") && obj.getBoolean("business") && !Prefs.get(this).getBoolean(Prefs.BUSINESS, false)) {
                Prefs.get(this).edit().putBoolean(Prefs.BUSINESS, true).commit();
            } else if (Prefs.get(this).getBoolean(Prefs.BUSINESS, false)) {
                Prefs.get(this).edit().putBoolean(Prefs.BUSINESS, false).commit();
            }
        }

        if (depth == 0) {
            SharedPreferences prefs = Prefs.get(NewProfileScreen.this);
            prefs.edit().putString(Prefs.NAME, name).putInt(Prefs.NO_CONTACTS, contacts)
                    .putInt(Prefs.NO_PLACES, places).putInt(Prefs.NO_ACTIVITIES, activities)
                    .putString(Prefs.PROFILE_THUMB, thumb).putString(Prefs.PROFILE_MARKER, marker)
                    .putString(Prefs.PROFILE_TYPE, type).putString(Prefs.ACCOUNT, account)
                    .putString(Prefs.UNIT_DISTANCE, obj.getString("distance"))
                    .putFloat(Prefs.CURRENT_LAT, (float) obj.getDouble("lat"))
                    .putFloat(Prefs.CURRENT_LNG, (float) obj.getDouble("lng")).commit();

            if (obj.has("tracklabels")) {
                JSONObject tracklabels = obj;// obj.getJSONObject("tracklabels");
                prefs.edit().putString(Prefs.TRACKLABEL_GREEN, tracklabels.getString("green"))
                        .putString(Prefs.TRACKLABEL_YELLOW, tracklabels.getString("yellow"))
                        .putString(Prefs.TRACKLABEL_ORANGE, tracklabels.getString("orange"))
                        .putString(Prefs.TRACKLABEL_RED, tracklabels.getString("red"))
                        .putString(Prefs.TRACKLABEL_VIOLETT, tracklabels.getString("violett"))
                        .putString(Prefs.TRACKLABEL_BLUE, tracklabels.getString("blue")).commit();
            }
        } else if (depth > 0) {
            if (!isPlace && !isCompany && link) {
                updateLocationButton.setVisibility(View.VISIBLE);
            } else {
                updateLocationButton.setVisibility(View.GONE);
            }

            if (obj.has("invitations")) {
                JSONArray array = obj.getJSONArray("invitations");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject invObject = array.getJSONObject(i);
                    String msg = invObject.getString("msg");
                    String text = invObject.getString("text");
                    final long id = invObject.getLong("id");
                    String inviter = invObject.getString("inviter");
                    String invitee = invObject.getString("invitee");
                    if (inviter.equals(account)) {
                        inflateInvitation(msg, text, id);
                    } else if (invitee.equals(account)) {
                        inflateCancelInvitation(id);
                    }
                }
            } else if ((!view || !link) && !isCompany) {
                Button button = new Button(this);
                button.setTextColor(getResources().getColor(R.color.light));
                button.setBackgroundResource(R.drawable.button_flat_payment_plan);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_add_person_light, 0, 0, 0);
                if (Prefs.get(this).getString(Prefs.PROFILE_TYPE, C.person).equals(C.person)) {
                    button.setText(isPlace ? R.string.AddToNetwork : R.string.InviteToMyNetwork);
                    button.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            if (isPlace && !link) {
                                sendInvitation(account, "");
                            } else {
                                openInvitationDialog(account, name);
                            }
                        }
                    });
                } else {
                    button.setText(R.string.IntegrateIntoCompany);
                    button.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            integrateIntoCompany();
                        }
                    });
                }
                activityContainer.addView(button);
            } else if (link && delete) {
                Button button = new Button(this);
                button.setTextColor(getResources().getColor(R.color.light));
                button.setBackgroundResource(R.drawable.button_flat_payment_plan);
                button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_navigation_cancel_light, 0, 0, 0);
                button.setText(isPlace ? R.string.RemoveFromNetwork : R.string.RemoveFromNetwork);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View v) {
                        openRemoveFromNetworkDialog();
                    }
                });
                activityContainer.addView(button);
            }
        }

        this.textField.setText(txt);
        this.nameField.setText(name);
        if (tracks > 1) {
            tracksButton.setText(getResources().getString(R.string.XTracks, String.valueOf(tracks)));
        }

        Picasso.with(this).load(thumb).into(picture);

        if (!view) {
            disable(locationButton, R.drawable.ic_action_location_gray);
            disable(tracksButton, R.drawable.ic_action_tracks_gray);
            disable(activitiesButton, R.drawable.ic_action_activities_gray);
            disable(callButton, R.drawable.ic_action_call_gray);
            disable(directionsButton, R.drawable.ic_action_directions_gray);
        }
        if (depth == 0) {
            disable(messagesButton, R.drawable.ic_action_messages_gray);
        }
        if (isPlace) {
            disable(tracksButton, R.drawable.ic_action_tracks_gray);
            if (!isCompany) {
                disable(messagesButton, R.drawable.ic_action_messages_gray);
            }
        }

        if (phone.length() == 0) {
            disable(callButton, R.drawable.ic_action_call_gray);
        }

        supportInvalidateOptionsMenu();
    }

    private void integrateIntoCompany() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.IntegrateIntoCompany);
        final EditText input = new EditText(this);
        input.setHint(R.string.password);
        alert.setView(input);
        alert.setPositiveButton(R.string.OK, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                final String value = input.getText().toString().trim();
                try {
                    JSONObject obj = prepareObj();
                    obj.put("account", account);
                    obj.put("accountpw", value);
                    doAction(AbstractScreen.ACTION_INTEGRATEINTOCOMPANY, obj, getResources()
                            .getString(R.string.SendNow), new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            Ui.makeText(context, R.string.OK, Toast.LENGTH_LONG).show();
                            refill();
                        }

                        public void onFailure(int failure, Context context) {
                            Ui.makeText(context, R.string.passwordMismatch, Toast.LENGTH_LONG).show();
                        };
                    });
                } catch (Exception exc) {
                    Log.w(exc);
                }

            }
        });
        alert.setNegativeButton(getResources().getString(R.string.Cancel), null);
        AlertDialog dlg = alert.create();
        dlg.setCanceledOnTouchOutside(true);
        dlg.show();
    }

    private void inflateCancelInvitation(final long id) {
        Button button = new Button(this);
        button.setTextColor(getResources().getColor(R.color.white));
        button.setBackgroundResource(R.drawable.button_orange);
        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_close, 0, 0, 0);
        button.setText(R.string.CancelInvitation);
        button.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    JSONObject obj = prepareObj();
                    obj.put(ID, id);
                    doAction(ACTION_CANCELINVITATION, obj, new ResultWorker() {

                        @Override
                        public void onResult(final String result, Context context) {

                            super.onResult(result, context);
                            refill();
                        }
                    });
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        });
        activityContainer.addView(button);
    }

    private void inflateInvitation(String msg, String text, final long id) {
        View v = getLayoutInflater().inflate(R.layout.profile_invitation, null);

        TextView textView = (TextView) v.findViewById(R.id.text);
        textView.setText(text);
        TextView messageView = (TextView) v.findViewById(R.id.message);
        if (msg.trim().length() > 0) {
            messageView.setText(msg);
        } else {
            messageView.setVisibility(View.GONE);
        }
        activityContainer.addView(v);

        Button acceptButton = (Button) v.findViewById(R.id.accept);
        acceptButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_navigation_accept, 0, 0, 0);
        acceptButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    JSONObject obj = prepareObj();
                    obj.put(ID, id);
                    doAction(ACTION_ACCEPTINVITATION, obj, new ResultWorker() {

                        @Override
                        public void onResult(final String result, Context context) {
                            refill();
                        }
                    });
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        });

        Button rejectButton = (Button) v.findViewById(R.id.reject);
        rejectButton.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_close, 0, 0, 0);
        rejectButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    JSONObject obj = prepareObj();
                    obj.put(ID, id);
                    doAction(ACTION_REJECTINVITATION, obj, new ResultWorker() {

                        @Override
                        public void onResult(final String result, Context context) {
                            refill();
                        }
                    });
                } catch (Exception exc) {
                    Log.w(exc);
                }
            }
        });
    }

    public void onBack(View view) {
        finish();
    }

    public void onPicture(View view) {
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == C.REQUESTCODE_CONTACT) {
            if (resultCode == -2) {
                realLogout();
                return;
            } else if (resultCode == -1) {
                finish();
                return;
            } else {
                if (data != null) {
                    this.account = data.getStringExtra(C.account);
                }
                refill();
            }
        }
        if (requestCode == C.REQUESTCODE_EDIT) {
            refill();
        }
    }

    public void onTracks(View view) {
        showTracks(account, name);
    }

    public void onCall(View view) {
        if (phone != null && phone.trim().length() > 0) {
            try {
                String uri = "tel:" + phone.trim();
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse(uri));
                startActivity(intent);
            } catch (ActivityNotFoundException exc) {
                Toast.makeText(this, R.string.NotAvailable, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onDirections(View view) {
        com.hellotracks.types.LatLng origin = new LatLng(getLastLocation());
        com.hellotracks.types.LatLng destination = new com.hellotracks.types.LatLng(latitude, longitude);

        if (origin.lat + origin.lng == 0) {
            Ui.makeText(this, R.string.NoGPSSignal, Toast.LENGTH_LONG).show();
            return;
        }

        SearchMap.asyncGetDirections(this, origin, destination, new SearchMap.Callback<SearchMap.DirectionsResult>() {

            @Override
            public void onResult(boolean success, SearchMap.DirectionsResult result) {
                if (success) {
                    EventBus.getDefault().post(result);
                    finish();
                } else {
                    Ui.makeText(NewProfileScreen.this, R.string.NoEntries, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void onActivities(View view) {
        Intent i = new Intent(this, ActivitiesScreen.class);
        i.putExtra(C.account, account);
        startActivity(i);
    }

    public void onEvent(final SearchMap.DirectionsResult result) {
        finish();
    }

    public void onMessages(View view) {
        showConversation();
    }

    public void onLocation(View view) {
        QuickAction quick = new QuickAction(this);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (pos == 0) {
                    Intent intent = new Intent(C.BROADCAST_SHOWMAP);
                    intent.putExtra(C.account, NewProfileScreen.this.account);
                    sendBroadcast(intent);
                    finish();
                } else if (pos == 1) {
                    String url = "geo:0,0?q=" + latitude + "," + longitude;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                } else if (pos == 2) {
                    String url = "google.navigation:q=" + latitude + "," + longitude;
                    Intent i = new Intent(Intent.ACTION_VIEW);
                    i.setData(Uri.parse(url));
                    startActivity(i);
                }
                //              block3bottom.startAnimation(fadeOut);
            }
        });
        quick.addActionItem(new ActionItem(this, R.string.ShowInMap));
        quick.addActionItem(new ActionItem(this, "Google Maps"));
        quick.addActionItem(new ActionItem(this, "Google Navigation"));
        quick.show(view);
    }

    private void showConversation() {
        Intent intent = new Intent(getApplicationContext(), MessagesScreen.class);
        intent.putExtra(C.account, account);
        intent.putExtra(C.name, name);
        startActivity(intent);
    }

    private String account;

    protected LazyAdapter createAdapter(final JSONArray array) {
        LazyAdapter lazy = new LazyAdapter(this, array) {
            @Override
            protected int getListItemLayoutFor(int index) {
                return R.layout.list_item_profileactivity;
            }
        };
        return lazy;
    }

    protected void openInvitationDialog(final String account, String name) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setMessage(R.string.InviteContact);
        final EditText input = new EditText(this);
        String hint = getResources().getString(R.string.Hello) + " " + name;
        input.setHint(hint);
        alert.setView(input);
        alert.setPositiveButton(getResources().getString(R.string.Invite), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                sendInvitation(account, input.getText().toString().trim());
            }
        });
        alert.show();
    }

    protected void sendRemove(final String account, final boolean finish) {
        try {
            JSONObject obj = prepareObj();
            obj.put("account", account);
            doAction(AbstractScreen.ACTION_REMOVECONTACT, obj, null, new ResultWorker() {
                @Override
                public void onResult(String result, Context context) {
                    if (finish) {
                        finish();
                    } else {
                        refill();
                    }
                }
            });

        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    protected void sendInvitation(final String account, String value) {
        try {
            JSONObject obj = prepareObj();
            obj.put("msg", value);
            obj.put("invitee", account);
            doAction(AbstractScreen.ACTION_CREATEINVITATION, obj, getResources().getString(R.string.SendNow),
                    new ResultWorker() {
                        @Override
                        public void onResult(String result, Context context) {
                            super.onResult(result, context);
                            refill();
                        }
                    });

        } catch (Exception exc) {
            Log.w(exc);
        }
    }

    public void openRemoveFromNetworkDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(NewProfileScreen.this);
        builder.setTitle(R.string.RemoveFromNetwork);
        builder.setNegativeButton(R.string.Cancel, null);
        builder.setPositiveButton(R.string.Remove, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendRemove(account, isPlace);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void onUpdateLocation(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(NewProfileScreen.this);
        builder.setMessage(R.string.UpdateLocationMsg);
        builder.setNegativeButton(R.string.Cancel, null);
        builder.setPositiveButton(R.string.UpdateLocation, new AlertDialog.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                sendMessage(account, C.GCM_CMD_STARTTRACKINGSERVICE, null);
                Ui.showText(NewProfileScreen.this, R.string.MayTakeSomeMinutes);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
}
