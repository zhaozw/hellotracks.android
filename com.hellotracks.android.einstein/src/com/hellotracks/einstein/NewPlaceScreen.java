package com.hellotracks.einstein;

import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
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
import com.hellotracks.activities.AbstractScreen;
import com.hellotracks.activities.RegisterPlaceScreen;
import com.hellotracks.model.ResultWorker;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.GeoUtils;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.quickaction.ActionItem;
import com.hellotracks.util.quickaction.QuickAction;
import com.hellotracks.util.quickaction.QuickAction.OnActionItemClickListener;
import de.greenrobot.event.EventBus;

public class NewPlaceScreen extends AbstractScreen {

    private TextView textField;
    private TextView nameField;
    private ImageButton button_back;
    private ImageView picture;
    private View board;
    private LinearLayout activityContainer;

    private Button callButton;
    private Button locationButton;
    private Button tracksButton;
    private Button activitiesButton;
    private Button messagesButton;
    private Button directionsButton;

    private String profileString = null;
    private int depth = 0;
    private String name;

    private Animation fadeOut;

    private double latitude = 0;
    private double longitude = 0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (depth > 0) {
                onBack(null);
                return true;
            } else {

            }
        }
        return super.onKeyDown(keyCode, event);
    }
  
    
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            finish();
            break;
        }
        return true;
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.screen_profile_new);

        textField = (TextView) findViewById(R.id.text);
        nameField = (TextView) findViewById(R.id.name);
        picture = (ImageView) findViewById(R.id.picture);
        button_back = (ImageButton) findViewById(R.id.button_back);
        fadeOut = AnimationUtils.loadAnimation(this, R.anim.fadeout);
        board = findViewById(R.id.board);
        activityContainer = (LinearLayout) findViewById(R.id.activityContainter);

        callButton = (Button) findViewById(R.id.buttonCall);
        locationButton = (Button) findViewById(R.id.buttonLocation);
        directionsButton = (Button) findViewById(R.id.buttonDirections);
        messagesButton = (Button) findViewById(R.id.buttonMessages);
        tracksButton = (Button) findViewById(R.id.buttonTracks);
        activitiesButton = (Button) findViewById(R.id.buttonActivities);

        latitude = getIntent().getDoubleExtra("lat", 0);
        longitude = getIntent().getDoubleExtra("lng", 0);
        name = getIntent().getStringExtra("name");

        nameField.setText(name);
        textField.setText(GeoUtils.format(new LatLng(latitude, longitude)));

        picture.setVisibility(View.GONE);
        disable(callButton, R.drawable.ic_action_call_gray);
        disable(activitiesButton, R.drawable.ic_action_activities_gray);
        disable(tracksButton, R.drawable.ic_action_tracks_gray);
        disable(messagesButton, R.drawable.ic_action_messages_gray);

        inflateCreatePlace();
        
        setupActionBar(R.string.Back);
    }

    private void disable(Button b, int icon) {
        b.setEnabled(false);
        b.setCompoundDrawablesWithIntrinsicBounds(0, icon, 0, 0);
        b.setTextColor(getResources().getColor(R.color.darkgray));
    }

    public void onBack(View view) {
        finish();
    }

    public void onPicture(View view) {

    }

    public void onTracks(View view) {
    }

    public void onCall(View view) {

    }
    
    public void onDirections(View view) {
        Location last = getLastLocation();
        if (last == null)
            return;
        com.hellotracks.types.LatLng origin = new LatLng(last);
        com.hellotracks.types.LatLng destination = new com.hellotracks.types.LatLng(latitude, longitude);
        
        if (origin.lat + origin.lng == 0) {
            Toast.makeText(this, R.string.NoGPSSignal, Toast.LENGTH_LONG).show();
            return;
        }

        SearchMap.asyncGetDirections(this, origin, destination, new SearchMap.Callback<SearchMap.DirectionsResult>() {

            @Override
            public void onResult(boolean success, SearchMap.DirectionsResult result) {
                if (success) {
                    EventBus.getDefault().post(result);
                    finish();
                } else {
                    Toast.makeText(NewPlaceScreen.this, R.string.NoEntries, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void onActivities(View view) {
    }

    public void onMessages(View view) {
    }

    public void onLocation(View view) {
        QuickAction quick = new QuickAction(this);
        quick.setOnActionItemClickListener(new OnActionItemClickListener() {

            @Override
            public void onItemClick(QuickAction source, int pos, int actionId) {
                if (pos == 0) {
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

    private void inflateCreatePlace() {
        View v = getLayoutInflater().inflate(R.layout.profile_addplace, null);
        Button addButton = (Button) v.findViewById(R.id.button);
        addButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isOnline(true)) {
                    String name = nameField.getText().toString();
                    final AlertDialog.Builder alert = new AlertDialog.Builder(NewPlaceScreen.this);
                    alert.setTitle(R.string.CreatePlace);
                    final EditText input = new EditText(NewPlaceScreen.this);
                    input.setHint(R.string.PlaceName);
                    if (!name.equals(getResources().getString(R.string.CreateNewPlace)))
                        input.setText(name);
                    alert.setView(input);
                    alert.setPositiveButton(getResources().getString(R.string.Create),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    String value = input.getText().toString().trim();
                                    send(value);
                                }
                            });

                    AlertDialog dlg = alert.create();
                    dlg.setCanceledOnTouchOutside(true);
                    dlg.show();

                }

            }
        });
        activityContainer.addView(v);
    }

    private void send(String name) {
        try {
            String owner = Prefs.get(this).getString(Prefs.USERNAME, "");
            int radiusMeter = 400;

            JSONObject registerObj = new JSONObject();
            Locale locale = Locale.getDefault();
            TimeZone timezone = TimeZone.getDefault();
            registerObj.put("language", locale.getLanguage());
            registerObj.put("country", locale.getCountry());
            registerObj.put("timezone", timezone.getID());
            registerObj.put("accounttype", "place");
            registerObj.put("name", name);
            registerObj.put("owner", owner);
            registerObj.put("extension", radiusMeter * 2);
            if (latitude + longitude != 0) {
                registerObj.put("latitude", latitude);
                registerObj.put("longitude", longitude);
            }
            String msg = getResources().getString(R.string.registering) + " " + name + "...";
            doAction(AbstractScreen.ACTION_REGISTER, registerObj, msg, new ResultWorker() {

                @Override
                public void onResult(String result, Context context) {
                    Toast.makeText(NewPlaceScreen.this, getResources().getString(R.string.placeRegisteredSuccessfully),
                            Toast.LENGTH_LONG).show();
                    finish();
                }

            });
        } catch (Exception exc) {
            Log.w(exc);
        }
    }

}
