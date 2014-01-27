package com.hellotracks.map;

import android.content.Context;
import android.location.Location;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.maps.android.SphericalUtil;
import com.hellotracks.Logger;
import com.hellotracks.Prefs;
import com.hellotracks.R;
import com.hellotracks.util.Time;
import com.hellotracks.util.UnitUtils;

public class Contextual {
    public static void doShowContextual(final AbstractMapScreen screen, final Marker marker, final MarkerEntry e,
            final GoogleMap mMap, final Animation fromBottomAnimation, final Animation toBottomAnimation) {
        if (e != null && e.isPerson()) {
            handlePerson(screen, e, mMap, fromBottomAnimation);
        } else {
            handlePlace(screen, marker, e, mMap, fromBottomAnimation, toBottomAnimation);
        }
        screen.findViewById(R.id.cross).setVisibility(View.GONE);
    }

    protected static void handlePlace(final AbstractMapScreen screen, final Marker m, final MarkerEntry e,
            final GoogleMap mMap, final Animation fromBottomAnimation, final Animation toBottomAnimation) {

        final View contextualPlace = screen.findViewById(R.id.contextualPlace);
        Button buttonDirections = (Button) contextualPlace.findViewById(R.id.buttonDirections);
        buttonDirections.setText(getNiceDistance(m.getPosition(), screen.getLastLocation()));
        buttonDirections.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Actions.doOnDirections(screen, screen.getLastLocation(), m.getPosition().latitude,
                        m.getPosition().longitude);
            }
        });
        contextualPlace.findViewById(R.id.buttonSendLocation).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                //                String uri = "geo:" + coord + "?q=" + coord;
                //                screen.startActivity(new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri)));                            
                Actions.doShareLocation(screen, m, false);
            }
        });

        screen.findViewById(R.id.layoutCreatePlace).setVisibility(View.GONE);
        screen.findViewById(R.id.layoutButtons).setVisibility(View.VISIBLE);

        contextualPlace.findViewById(R.id.buttonEdit).setVisibility(View.GONE);
        contextualPlace.findViewById(R.id.buttonSave).setVisibility(View.GONE);

        if (e != null) {
            // existing place
            contextualPlace.findViewById(R.id.buttonActivities).setVisibility(View.VISIBLE);
            contextualPlace.findViewById(R.id.buttonActivities).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Actions.doShowActivities(screen, e.account);
                }
            });
            Button buttonEdit = (Button) contextualPlace.findViewById(R.id.buttonEdit);
            buttonEdit.setVisibility(View.VISIBLE);
            buttonEdit.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Actions.doOpenProfile(screen, e.account, e.name);
                }
            });
        } else {
            // new place
            contextualPlace.findViewById(R.id.buttonActivities).setVisibility(View.GONE);

            Button buttonSave = (Button) contextualPlace.findViewById(R.id.buttonSave);
            buttonSave.setVisibility(View.VISIBLE);
            buttonSave.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    screen.findViewById(R.id.layoutCreatePlace).setVisibility(View.VISIBLE);
                    screen.findViewById(R.id.layoutCreatePlace).startAnimation(fromBottomAnimation);
                    screen.findViewById(R.id.layoutButtons).setVisibility(View.GONE);
                    screen.findViewById(R.id.layoutButtons).startAnimation(toBottomAnimation);

                    final EditText textName = (EditText) screen.findViewById(R.id.editTextPlaceName);
                    textName.setText(m.getTitle());
                    textName.requestFocus();
                    final CheckBox box = (CheckBox) contextualPlace.findViewById(R.id.checkBoxCreateForNetwork);
                    box.setChecked(Prefs.get(screen).getBoolean(Prefs.CREATE_PLACE_NETWORK_ACTIVATED, true));

                    Button buttonCreate = (Button) contextualPlace.findViewById(R.id.buttonCreatePlace);
                    buttonCreate.setOnClickListener(new OnClickListener() {

                        @Override
                        public void onClick(View v) {
                            Prefs.get(screen).edit().putBoolean(Prefs.CREATE_PLACE_NETWORK_ACTIVATED, box.isChecked())
                                    .commit();

                            Actions.registerPlace(screen, textName.getText().toString().trim(),
                                    m.getPosition().latitude, m.getPosition().longitude, false, box.isChecked());
                            screen.hideContextual();

                            InputMethodManager imm = (InputMethodManager) screen
                                    .getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(textName.getWindowToken(), 0);
                        }
                    });
                }
            });

        }

        // 
        contextualPlace.setVisibility(View.VISIBLE);
        contextualPlace.startAnimation(fromBottomAnimation);
        mMap.setPadding(0, 0, 0, screen.getResources().getDimensionPixelSize((R.dimen.contextual_place_height)));
    }

    protected static void handlePerson(final AbstractMapScreen screen, final MarkerEntry e, final GoogleMap mMap,
            final Animation fromBottomAnimation) {
        View v = screen.findViewById(R.id.contextualPerson);
        v.findViewById(R.id.buttonSendLocation).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Actions.doShareLocation(screen, e.marker, e.isMe());
            }
        });
        v.findViewById(R.id.buttonTracks).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                screen.showTracks(e.account, e.name);
            }
        });
        v.findViewById(R.id.buttonMessages).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Actions.doShowConversation(screen, e.isMe() ? null : e.account, e.name);
            }
        });
        v.findViewById(R.id.buttonActivities).setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Actions.doShowActivities(screen, e.account);
            }
        });

        if (e.isMe()) {
            v.findViewById(R.id.buttonUpdateLocation).setVisibility(View.GONE);
            v.findViewById(R.id.buttonDirections).setVisibility(View.GONE);
        } else {
            Button b = (Button) v.findViewById(R.id.buttonDirections);
            b.setVisibility(View.VISIBLE);
            b.setText(getNiceDistance(e.point, screen.getLastLocation()));
            b.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Actions.doOnDirections(screen, screen.getLastLocation(), e.point.latitude, e.point.longitude);
                }
            });

            v.findViewById(R.id.buttonUpdateLocation).setVisibility(View.VISIBLE);
            v.findViewById(R.id.buttonUpdateLocation).setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    Actions.doOnUpdateLocation(screen, e.account);
                }
            });
        }
        TextView textLocation = (TextView) screen.findViewById(R.id.textLocation);
        String timeText = "";
        if (e.accuracy > 0) {
            if (!Prefs.isDistanceUS(screen)) {
                timeText = screen.getResources().getString(R.string.Within) + " " + e.accuracy + "m\n";
            } else {
                timeText = screen.getResources().getString(R.string.Within) + " " + (int) (3.28084 * e.accuracy)
                        + "ft\n";
            }
        }
        timeText += Time.formatTimePassed(screen, e.timestamp);
        textLocation.setText(timeText);
        v.setVisibility(View.VISIBLE);
        v.startAnimation(fromBottomAnimation);
        mMap.setPadding(0, 0, 0, screen.getResources().getDimensionPixelSize((R.dimen.contextual_person_height)));
    }

    private static CharSequence getNiceDistance(LatLng point, Location last) {
        try {
            double dist = SphericalUtil
                    .computeDistanceBetween(new com.hellotracks.types.LatLng(last).toGoogle(), point);
            return UnitUtils.distanceToKM(dist);
        } catch (Exception exc) {
            Logger.e(exc);
            return "";
        }
    }

    public static boolean isOpen(HomeMapScreen screen) {
        return screen.findViewById(R.id.contextualPerson).getVisibility() == View.VISIBLE
                || screen.findViewById(R.id.contextualPlace).getVisibility() == View.VISIBLE;
    }

    public static void doHideContextual(AbstractMapScreen screen, GoogleMap mMap, Animation fromBottomAnimation,
            Animation toBottomAnimation) {
        View v1 = screen.findViewById(R.id.contextualPlace);
        if (v1.getVisibility() == View.VISIBLE) {
            v1.setVisibility(View.GONE);
            v1.startAnimation(toBottomAnimation);
            mMap.setPadding(0, 0, 0, 0);
            screen.jumpTo(mMap.getCameraPosition().target);
        }

        View v2 = screen.findViewById(R.id.contextualPerson);
        if (v2.getVisibility() == View.VISIBLE) {
            v2.setVisibility(View.GONE);
            v2.startAnimation(toBottomAnimation);
            mMap.setPadding(0, 0, 0, 0);
            screen.jumpTo(mMap.getCameraPosition().target);
        }
        screen.findViewById(R.id.cross).setVisibility(View.VISIBLE);
        screen.removeAllTremporaryMarkers();
    }
}
