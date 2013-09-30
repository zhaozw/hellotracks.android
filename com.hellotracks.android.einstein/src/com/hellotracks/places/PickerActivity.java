package com.hellotracks.places;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.View;

import com.facebook.FacebookException;
import com.facebook.model.GraphPlace;
import com.facebook.widget.FriendPickerFragment;
import com.facebook.widget.PickerFragment;
import com.facebook.widget.PlacePickerFragment;
import com.hellotracks.Log;
import com.hellotracks.R;
import com.hellotracks.map.HomeMapScreen;

public class PickerActivity extends FragmentActivity {

    private static final Location SAN_FRANCISCO_LOCATION = new Location("") {
        {
            setLatitude(37.7750);
            setLongitude(-122.4183);
        }
    };

    public static final Uri FRIEND_PICKER = Uri.parse("picker://friend");
    public static final Uri PLACE_PICKER = Uri.parse("picker://place");

    private FriendPickerFragment friendPickerFragment;
    private PlacePickerFragment placePickerFragment;

    private static final int SEARCH_RADIUS_METERS = 1000;
    private static final int SEARCH_RESULT_LIMIT = 50;
    private static final String SEARCH_TEXT = "restaurant";
    private static final int LOCATION_CHANGE_THRESHOLD = 50; // meters

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.anim.from_bottom, R.anim.to_bottom);

        setContentView(R.layout.pickers);

        Bundle args = getIntent().getExtras();
        FragmentManager manager = getSupportFragmentManager();
        Fragment fragmentToShow = null;
        Uri intentUri = getIntent().getData();

        if (PLACE_PICKER.equals(intentUri)) {
            if (savedInstanceState == null) {
                placePickerFragment = new PlacePickerFragment(args);
                placePickerFragment.setLocation(SAN_FRANCISCO_LOCATION);
                placePickerFragment.setRadiusInMeters(SEARCH_RADIUS_METERS);
                placePickerFragment.setSearchText(SEARCH_TEXT);
                placePickerFragment.setResultsLimit(SEARCH_RESULT_LIMIT);
            } else {
                placePickerFragment = (PlacePickerFragment) manager.findFragmentById(R.id.picker_fragment);
            }
            placePickerFragment.setOnSelectionChangedListener(new PickerFragment.OnSelectionChangedListener() {
                @Override
                public void onSelectionChanged(PickerFragment<?> fragment) {
                    finishActivity(); // call finish since you can only pick one place
                }
            });
            placePickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
                @Override
                public void onError(PickerFragment<?> fragment, FacebookException error) {
                    PickerActivity.this.onError(error);
                }
            });
            placePickerFragment.setOnDoneButtonClickedListener(new PickerFragment.OnDoneButtonClickedListener() {
                @Override
                public void onDoneButtonClicked(PickerFragment<?> fragment) {
                    finishActivity();
                }
            });
            fragmentToShow = placePickerFragment;
        } else if (FRIEND_PICKER.equals(intentUri)) {
            if (savedInstanceState == null) {
                friendPickerFragment = new FriendPickerFragment(args);
            } else {
                friendPickerFragment = (FriendPickerFragment) manager.findFragmentById(R.id.picker_fragment);
            }

            // Set the listener to handle errors
            friendPickerFragment.setOnErrorListener(new PickerFragment.OnErrorListener() {
                @Override
                public void onError(PickerFragment<?> fragment, FacebookException error) {
                    PickerActivity.this.onError(error);
                }
            });
            // Set the listener to handle button clicks
            friendPickerFragment.setOnDoneButtonClickedListener(new PickerFragment.OnDoneButtonClickedListener() {
                @Override
                public void onDoneButtonClicked(PickerFragment<?> fragment) {
                    finishActivity();
                }
            });
            fragmentToShow = friendPickerFragment;

        } else {
            // Nothing to do, finish
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        manager.beginTransaction().replace(R.id.picker_fragment, fragmentToShow).commit();
    }

    private void onError(Exception error) {
        onError(error.getLocalizedMessage(), false);
    }

    private void onError(String error, final boolean finishActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("error_dialog_title").setMessage(error)
                .setPositiveButton("error_dialog_button_text", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (finishActivity) {
                            finishActivity();
                        }
                    }
                });
        builder.show();
    }

    private void finishActivity() {
        if (PLACE_PICKER.equals(getIntent().getData())) {
            if (placePickerFragment != null) {
                Log.i(placePickerFragment.getSelection().getInnerJSONObject().toString());
            }
        }
        Intent data = new Intent();
        data.putExtra("place", placePickerFragment.getSelection().getInnerJSONObject().toString());
        setResult(RESULT_OK, data);
        finish();
    }

    public void onBack(View view) {
        finish();
    }

}
