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
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.hellotracks.BestTrackingService;
import com.hellotracks.base.AbstractScreen;
import com.hellotracks.base.C;

import de.greenrobot.event.EventBus;

/**
 * Service that receives ActivityRecognition updates. It receives updates in the background, even if the main Activity
 * is not visible.
 */
public class RecognitionIntentService extends IntentService {

    public RecognitionIntentService() {
        // Set the label for the service's background thread
        super("ActivityRecognitionIntentService");
    }

    /**
     * Called when a new activity detection update is available.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        com.hellotracks.Log.i("activity received");

        if (ActivityRecognitionResult.hasResult(intent)) {

            if (!AbstractScreen.isMyServiceRunning(getApplicationContext(), BestTrackingService.class)) {
                Intent serviceIntent = new Intent(getApplicationContext(), BestTrackingService.class);
                startService(serviceIntent);
            }

            ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);
            DetectedActivity mostProbableActivity = result.getMostProbableActivity();
            int confidence = mostProbableActivity.getConfidence();
            int activityType = mostProbableActivity.getType();

            Log.i("recognition", "current activity: " + activityType + "    confidence: " + confidence);
            if (confidence > 70) {
                EventBus.getDefault().post(mostProbableActivity);
                Intent i = new Intent(C.BROADCAST_ACTIVITYRECOGNIZED);
                i.putExtra("confidence", confidence);
                i.putExtra("activityType", activityType);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(i);
            }

        }
    }
}
