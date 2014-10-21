package com.ikimuhendis.wear.swarm.services;

import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.ikimuhendis.wear.swarm.activities.SwarmActivity;
import com.ikimuhendis.wear.swarm.models.DoCheckinResponse;
import com.ikimuhendis.wear.swarm.models.SearchResponse;
import com.ikimuhendis.wear.swarm.models.Venue;
import com.ikimuhendis.wear.swarm.restServices.FoursquareApi;
import com.ikimuhendis.wear.swarm.utils.FourSquareUtil;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

public class DataLayerListenerService extends WearableListenerService {

    private LocationClient locationClient;
    private LocationRequest locationRequest;
    private GoogleApiClient mGoogleApiClient;

    private LocationUpdateListener locationListener = new LocationUpdateListener();
    private GoogleAPIListener googleAPIListener = new GoogleAPIListener();
    private Location location;
    private String venueId;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/start_login")) {
            Intent startIntent = new Intent(this, SwarmActivity.class);
            startIntent.putExtra("start_login", true);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        } else if (messageEvent.getPath().startsWith("/get_nearby_places")) {
            if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
                googleAPIListener.requestLocation = true;
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(googleAPIListener)
                    .addOnConnectionFailedListener(googleAPIListener)
                    .build();
                mGoogleApiClient.connect();
            } else {
                requestLocation();
            }
        } else if (messageEvent.getPath().startsWith("/check_in")) {
            venueId = new String(messageEvent.getData());
            if (mGoogleApiClient == null || !mGoogleApiClient.isConnected()) {
                googleAPIListener.requestLocation = false;
                mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(googleAPIListener)
                    .addOnConnectionFailedListener(googleAPIListener)
                    .build();
                mGoogleApiClient.connect();
            } else {
                (new CheckInTask()).execute();
            }
        }
    }


    private class CheckInTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            if (venueId != null) {
                try {
                    FoursquareApi api = new FoursquareApi();
                    DoCheckinResponse response = api.checkin(FourSquareUtil.getAccessToken(DataLayerListenerService.this), venueId);
                    venueId = null;
                    boolean success = response != null && response.response != null;
                    PutDataMapRequest dataMapRequest = PutDataMapRequest.createWithAutoAppendedId("/check_in_result");
                    DataMap dataMap = dataMapRequest.getDataMap();
                    dataMap.putBoolean("success", success);
                    PutDataRequest request = dataMapRequest.asPutDataRequest();
                    Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                } catch (Exception e) {
                    sendError("/check_in_error", "Foursquare Error");
                }
            } else {
                sendError("/check_in_error", "venue id empty");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }

    private class SearchTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            if (location != null) {
                try {
                    FoursquareApi api = new FoursquareApi();
                    SearchResponse response = api.search(location.getLatitude() + "," + location.getLongitude());
                    final ArrayList<DataMap> dataVenues = new ArrayList<DataMap>();
                    PutDataMapRequest dataMapRequest = PutDataMapRequest.createWithAutoAppendedId("/search_result");
                    DataMap dataMap = dataMapRequest.getDataMap();
                    for (Venue venue : response.getResponse().getVenues()) {
                        DataMap dm = venue.getDataMap();
                        Asset asset = downloadImage(venue.getPrimaryCategoryPNGIconUrl());
                        if (asset != null) {
                            dm.putAsset(venue.id, asset);
                        }
                        dataVenues.add(dm);
                    }
                    dataMap.putDataMapArrayList("venues", dataVenues);
                    PutDataRequest request = dataMapRequest.asPutDataRequest();
                    Wearable.DataApi.putDataItem(mGoogleApiClient, request);
                } catch (Exception e) {
                    sendError("/search_error", "Foursquare Error");
                }
            } else {
                sendError("/search_error", "Location not found");
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }

    public Asset downloadImage(String url) {
        try {
            Bitmap bitmap = Picasso.with(this)
                .load(url).get();
            final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
            return Asset.createFromBytes(byteStream.toByteArray());
        } catch (Exception e) {
            Log.e("Error", "downloadImage", e);
        }
        return null;
    }

    public void requestLocation() {
        locationClient = new LocationClient(DataLayerListenerService.this, locationListener, locationListener);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationClient.connect();
    }

    private void sendError(String type, String error) {
        Message msg = sendError.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("type", type);
        bundle.putString("error", error);
        msg.setData(bundle);
        sendError.sendMessage(msg);
    }

    private Handler sendError = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (mGoogleApiClient.isConnected()) {
                String error = msg.getData().getString("error");
                String type = msg.getData().getString("type");
                PutDataMapRequest dataMap = PutDataMapRequest.create(type);
                dataMap.getDataMap().putString("error", error);
                PutDataRequest request = dataMap.asPutDataRequest();
                Wearable.DataApi.putDataItem(mGoogleApiClient, request);
            }
        }
    };

    public class LocationUpdateListener implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        LocationListener {

        @Override
        public void onConnected(Bundle bundle) {
            if (locationClient.isConnected()) {
                locationClient.requestLocationUpdates(locationRequest,
                    this);
            } else {
                sendError("/search_error", "Connection disconnected");
            }
        }

        @Override
        public void onDisconnected() {
        }

        @Override
        public void onLocationChanged(Location location) {
            if (locationClient.isConnected()) {
                locationClient.removeLocationUpdates(this);
                locationClient.disconnect();
            }
            DataLayerListenerService.this.location = location;
            (new SearchTask()).execute();
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            sendError("/search_error", "Connection failed");
        }
    }

    public class GoogleAPIListener implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

        private boolean requestLocation;

        @Override
        public void onConnected(Bundle bundle) {
            if (requestLocation) {
                requestLocation();
            } else {
                (new CheckInTask()).execute();
            }
        }

        @Override
        public void onConnectionSuspended(int i) {
            sendError("/search_error", "Connection suspended");
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {
            sendError("/search_error", "Connection failed");
        }
    }


}