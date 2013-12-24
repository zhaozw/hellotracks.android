/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hellotracks.recognition;

import android.app.IntentService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.hellotracks.BestTrackingService;
import com.hellotracks.base.AbstractScreen;

/**
 * Service that receives ActivityRecognition updates. It receives updates in the background, even if the main Activity
 * is not visible.
 */
public class RecognitionIntentService extends IntentService {

    private int confidence, activityType;

    public RecognitionIntentService() {
        // Set the label for the service's background thread
        super("RecognitionIntentService");
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            // This is called when the connection with the service has been
            // established, giving us the service object we can use to
            // interact with the service.  Because we have bound to a explicit
            // service that we know is running in our own process, we can
            // cast its IBinder to a concrete class and directly access it.
            BestTrackingService mBoundService = ((BestTrackingService.LocalBinder) service).getService();
            mBoundService.activityDetected(activityType, confidence, original);
            unbind();
        }

        public void onServiceDisconnected(ComponentName className) {
        }
    };

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        bindService(new Intent(this, BestTrackingService.class), mConnection, Context.BIND_AUTO_CREATE);
    }
    
    private void unbind() {
        try {
            unbindService(mConnection);
        } catch (Exception exc) {
            com.hellotracks.Log.e(exc);
        }
    }

    private DetectedActivity original;

    /**
     * Called when a new activity detection update is available.
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        com.hellotracks.Log.i("activity received");

        if (!AbstractScreen.isMyServiceRunning(getApplicationContext(), BestTrackingService.class)) {
            com.hellotracks.Log.w("restoring BestTrackingService out of RecognitionIntentService");
            Intent serviceIntent = new Intent(getApplicationContext(), BestTrackingService.class);
            startService(serviceIntent);
        }

        if (ActivityRecognitionResult.hasResult(intent)) {

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            confidence = mostProbableActivity.getConfidence();
            activityType = mostProbableActivity.getType();

            original = mostProbableActivity;

            Log.i("recognition", "current activity: " + activityType + " confidence: " + confidence);

            if (activityType == DetectedActivity.TILTING)
                activityType = DetectedActivity.ON_FOOT;

            if (activityType >= 4 && result.getProbableActivities().size() >= 2) {
                DetectedActivity secondMostProb = result.getProbableActivities().get(1);
                if (secondMostProb.getType() <= 2 || secondMostProb.getType() == DetectedActivity.TILTING) {
                    confidence = 70;
                    activityType = DetectedActivity.ON_FOOT;
                }
            }

            Log.i("recognition", result.getProbableActivities().toString());
            if (activityType <= 2 || confidence >= 70) {
                //                Intent i = new Intent(C.BROADCAST_ACTIVITYRECOGNIZED);
                //                i.putExtra("confidence", confidence);
                //                i.putExtra("activityType", activityType);
                //                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
                doBindService();
            }

        }
    }
    
    @Override
    public void onCreate() {
        com.hellotracks.Log.i("creating recognition intent service");
        super.onCreate();
    }
    
    @Override
    public void onDestroy() {
        com.hellotracks.Log.i("recognition intent destroyed");
        super.onDestroy();
    }


}
