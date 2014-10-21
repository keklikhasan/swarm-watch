package com.ikimuhendis.wear.swarm.services;

import android.content.Intent;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.ikimuhendis.wear.swarm.activities.SwarmActivity;

public class DataLayerListenerService extends WearableListenerService {

    private static final String TAG = DataLayerListenerService.class.getName();

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/start_activity")) {
            Intent startIntent = new Intent(this, SwarmActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
    }
}