package com.hellotracks.places;

import java.util.List;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.hellotracks.R;
import com.hellotracks.map.Actions;

/**
 * This class receives geofence transition events from Location Services, in the form of an Intent containing the
 * transition type and geofence id(s) that triggered the event.
 */
public class ReceiveTransitionsIntentService extends IntentService {

    /**
     * Sets an identifier for this class' background thread
     */
    public ReceiveTransitionsIntentService() {
        super("ReceiveTransitionsIntentService");
    }

    /**
     * Handles incoming intents
     * 
     * @param intent
     *            The Intent sent by Location Services. This Intent is provided to Location Services (inside a
     *            PendingIntent) when you call addGeofences()
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        // Create a local broadcast Intent
        Intent broadcastIntent = new Intent();

        // Give it the category for all intents sent by the Intent Service
        broadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        // First check for errors
        if (LocationClient.hasError(intent)) {

            // Get the error code
            int errorCode = LocationClient.getErrorCode(intent);

            // Get the error message
            String errorMessage = LocationServiceErrorMessages.getErrorString(this, errorCode);

            // Log the error
            Log.e(GeofenceUtils.APPTAG, getString(R.string.geofence_transition_error_detail, errorMessage));

            // Set the action and error message for the broadcast intent
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR).putExtra(
                    GeofenceUtils.EXTRA_GEOFENCE_STATUS, errorMessage);

            // Broadcast the error *locally* to other components in this app
            LocalBroadcastManager.getInstance(this).sendBroadcast(broadcastIntent);

            // If there's no error, get the transition type and create a notification
        } else {
            // Get the type of transition (entry or exit)
            int transition = LocationClient.getGeofenceTransition(intent);

            // Test that a valid transition was reported
            if ((transition == Geofence.GEOFENCE_TRANSITION_ENTER) || (transition == Geofence.GEOFENCE_TRANSITION_EXIT)) {

                // Post a notification
                List<Geofence> geofences = LocationClient.getTriggeringGeofences(intent);
                String[] geofenceIds = new String[geofences.size()];
                for (int index = 0; index < geofences.size(); index++) {
                    geofenceIds[index] = geofences.get(index).getRequestId();
                }
                for (String id : geofenceIds) {
                    SimpleGeofence fence = new SimpleGeofenceStore(getApplicationContext()).getGeofence(id);
                    if (fence != null) {
                        Actions.doCheckIn(getApplicationContext(), "", fence.getId(), System.currentTimeMillis(), null);
                    }
                }
            } else {
                // Always log as an error
                Log.e(GeofenceUtils.APPTAG, getString(R.string.geofence_transition_invalid_type, transition));
            }
        }
    }

}
