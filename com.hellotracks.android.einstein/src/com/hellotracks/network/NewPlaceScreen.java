package com.hellotracks.network;

import java.util.Locale;
import java.util.TimeZone;

import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
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

import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;
import com.hellotracks.map.Actions;
import com.hellotracks.types.LatLng;
import com.hellotracks.util.GeoUtils;
import com.hellotracks.util.ResultWorker;
import com.hellotracks.util.SearchMap;
import com.hellotracks.util.Ui;
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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.screen_place_create);

        textField = (TextView) findViewById(R.id.text);
        nameField = (TextView) findViewById(R.id.name);
        picture = (ImageView) findViewById(R.id.picture);
        button_back = (ImageButton) findViewById(R.id.button_back);
        fadeOut = AnimationUtils.loadAnimation(this, R.anim.rotate);
        board = findViewById(R.id.board);
        activityContainer = (LinearLayout) findViewById(R.id.activityContainter);

        directionsButton = (Button) findViewById(R.id.buttonDirections);

        latitude = getIntent().getDoubleExtra("lat", 0);
        longitude = getIntent().getDoubleExtra("lng", 0);
        name = getIntent().getStringExtra("name");

        nameField.setText(name);
        textField.setText(GeoUtils.format(new LatLng(latitude, longitude)));

        picture.setVisibility(View.GONE);

        inflateCreatePlace();

        ImageButton back = (ImageButton) findViewById(R.id.buttonBack);
        back.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
                    Ui.makeText(NewPlaceScreen.this, R.string.NoEntries, Toast.LENGTH_LONG).show();
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
                try {
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
                } catch (ActivityNotFoundException exc) {
                    Ui.showText(NewPlaceScreen.this, R.string.NotAvailable);
                    Logger.e(exc);
                }
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
                String name = nameField.getText().toString();
                Actions.doCreateNewPlace(NewPlaceScreen.this, name, latitude, longitude, true);
            }
        });
        activityContainer.addView(v);
    }

}
