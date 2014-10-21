package com.ikimuhendis.wear.swarm.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.ikimuhendis.wear.swarm.R;
import com.ikimuhendis.wear.swarm.adapters.VenuesAdapter;
import com.ikimuhendis.wear.swarm.models.Venue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class SwarmActivity extends Activity implements
    DataApi.DataListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = SwarmActivity.class.getName();
    private GoogleApiClient mGoogleApiClient;
    private LinearLayout loadingLayout;
    private TextView loadindText;
    private ProgressBar progressBar;
    private LinearLayout loginLayout;
    private Button loginButton;
    private LinearLayout retryLayout;
    private Button retryButton;
    private String accessToken;
    private WearableListView listView;
    private ArrayList<Venue> venues;
    private Venue selectedVenue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swarm);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                loadingLayout = (LinearLayout) stub.findViewById(R.id.loadingLayout);
                loadindText = (TextView) stub.findViewById(R.id.loadindText);
                progressBar = (ProgressBar) stub.findViewById(R.id.progressBar);
                loginLayout = (LinearLayout) stub.findViewById(R.id.loginLayout);
                loginButton = (Button) stub.findViewById(R.id.loginButton);
                loginButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        (new SendLoginTask()).execute();
                    }
                });
                retryLayout = (LinearLayout) stub.findViewById(R.id.retryLayout);
                retryButton = (Button) stub.findViewById(R.id.retryButton);
                retryButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        (new SendGetNearByPlacesTask()).execute();
                    }
                });
                listView = (WearableListView) findViewById(R.id.listview);
                mGoogleApiClient = new GoogleApiClient.Builder(SwarmActivity.this)
                    .addApi(Wearable.API)
                    .addConnectionCallbacks(SwarmActivity.this)
                    .addOnConnectionFailedListener(SwarmActivity.this)
                    .build();
                mGoogleApiClient.connect();
            }
        });
    }

    private class SendLoginTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, node, "/start_login", null).await();
                if (!result.getStatus().isSuccess()) {
                    setLoginButton.sendEmptyMessage(1);
                    Toast.makeText(SwarmActivity.this, R.string.login_error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                }
            }
            setLoading(getString(R.string.loading_text_login), true);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }


    private class CheckInTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            if (selectedVenue != null && selectedVenue.id != null) {
                Collection<String> nodes = getNodes();
                for (String node : nodes) {
                    MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                        mGoogleApiClient, node, "/check_in", selectedVenue.id.getBytes()).await();
                    if (!result.getStatus().isSuccess()) {
                        setLoginButton.sendEmptyMessage(1);
                        Toast.makeText(SwarmActivity.this, R.string.login_error, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                    }
                }
                setLoading(getString(R.string.loading_text_checking_in), true);
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }

    private class SendGetNearByPlacesTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            Collection<String> nodes = getNodes();
            for (String node : nodes) {
                MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, node, "/get_nearby_places", null).await();
                if (!result.getStatus().isSuccess()) {
                    setLoginButton.sendEmptyMessage(1);
                    Toast.makeText(SwarmActivity.this, R.string.login_error, Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
                }
            }
            setLoading(getString(R.string.loading_text_getting_nearby_places), true);
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }

    public void setLoading(String text, boolean progress) {
        Message msg = setLoading.obtainMessage();
        Bundle bundle = new Bundle();
        bundle.putString("text", text);
        bundle.putBoolean("progress", progress);
        msg.setData(bundle);
        setLoading.sendMessage(msg);
    }

    public Handler setLoading = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String text = bundle.getString("text");
            boolean progress = bundle.getBoolean("progress");
            loginLayout.setVisibility(View.INVISIBLE);
            retryLayout.setVisibility(View.INVISIBLE);
            listView.setVisibility(View.INVISIBLE);
            loadingLayout.setVisibility(View.VISIBLE);
            loadindText.setText(text);
            progressBar.setVisibility(progress ? View.VISIBLE : View.INVISIBLE);
        }
    };

    public Handler setLoginButton = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            loadingLayout.setVisibility(View.INVISIBLE);
            retryLayout.setVisibility(View.INVISIBLE);
            listView.setVisibility(View.INVISIBLE);
            loginLayout.setVisibility(View.VISIBLE);
        }
    };

    public Handler setViews = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (!TextUtils.isEmpty(accessToken)) {
                setLoading(getString(R.string.loading_text_getting_nearby_places), true);
                (new SendGetNearByPlacesTask()).execute();
            } else {
                setLoginButton.sendEmptyMessage(1);
            }
        }
    };

    public Handler setRetryView = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            loadingLayout.setVisibility(View.INVISIBLE);
            loginLayout.setVisibility(View.INVISIBLE);
            listView.setVisibility(View.INVISIBLE);
            retryLayout.setVisibility(View.VISIBLE);
        }
    };

    public Handler setListView = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (venues == null || venues.size() < 1) {
                Toast.makeText(SwarmActivity.this, "No Result", Toast.LENGTH_SHORT).show();
                setRetryView.sendEmptyMessage(1);
            } else {
                loadingLayout.setVisibility(View.INVISIBLE);
                loginLayout.setVisibility(View.INVISIBLE);
                retryLayout.setVisibility(View.INVISIBLE);
                listView.setVisibility(View.VISIBLE);
                VenuesAdapter mAdapter = new VenuesAdapter(venues);
                listView.setAdapter(mAdapter);
                listView.setClickListener(new WearableListView.ClickListener() {

                    @Override
                    public void onClick(WearableListView.ViewHolder viewHolder) {
                        selectedVenue = venues.get(viewHolder.getPosition());
                        (new CheckInTask()).execute();
                    }

                    @Override
                    public void onTopEmptyRegionClick() {

                    }
                });
            }
        }
    };


    private HashSet<String> results;

    private Collection<String> getNodes() {
        if (results == null || results.size() < 1) {
            results = new HashSet<String>();
            NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            if (nodes != null && nodes.getNodes() != null) {
                for (Node node : nodes.getNodes()) {
                    results.add(node.getId());
                }
            }
        }
        return results;
    }

    public void getAccessToken() {
        PendingResult<DataItemBuffer> results = Wearable.DataApi.getDataItems(mGoogleApiClient);
        results.setResultCallback(new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                if (dataItems.getCount() != 0) {
                    for (DataItem item : dataItems) {
                        if (item.getUri().getPath().equals("/access_token")) {
                            DataMapItem dataMapItem = DataMapItem.fromDataItem(item);
                            accessToken = dataMapItem.getDataMap().getString("access_token");
                            setViews.sendEmptyMessage(1);
                            break;
                        }
                    }
                }
                dataItems.release();
            }
        });
    }


    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            String message = null;
            if (event.getDataItem().getUri().getPath().startsWith("/access_token")) {
                if (event.getType() == DataEvent.TYPE_DELETED) {
                    accessToken = null;
                } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    accessToken = dataMapItem.getDataMap().getString("access_token");
                }
                setViews.post(new Runnable() {
                    @Override
                    public void run() {
                        setViews.sendEmptyMessage(1);
                    }
                });
            } else if (event.getDataItem().getUri().getPath().startsWith("/search_error")) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    message = dataMapItem.getDataMap().getString("error");
                    setRetryView.sendEmptyMessage(1);
                }
            } else if (event.getDataItem().getUri().getPath().startsWith("/check_in_error")) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    message = dataMapItem.getDataMap().getString("error");
                    setRetryView.sendEmptyMessage(1);
                }
            } else if (event.getDataItem().getUri().getPath().startsWith("/search_result")) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    DataMap dataMap = dataMapItem.getDataMap();
                    ArrayList<DataMap> dataVenues = dataMap.getDataMapArrayList("venues");
                    venues = new ArrayList<Venue>();
                    for (DataMap dMap : dataVenues) {
                        Venue v = Venue.extractFromDataMap(dMap);
                        v.bitmap = loadBitmapFromAsset(dMap.getAsset(v.id));
                        venues.add(v);
                    }
                    setListView.sendEmptyMessage(1);
                }
            } else if (event.getDataItem().getUri().getPath().startsWith("/check_in_result")) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    if (dataMapItem.getDataMap().getBoolean("success")) {
                        message = "Checkin success";
                    } else {
                        message = "Checkin fail";
                    }
                    setListView.sendEmptyMessage(1);
                }
            }
            if (message != null) {
                final String msg = message;
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(SwarmActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

    public Bitmap loadBitmapFromAsset(Asset asset) {
        try {
            if (asset == null) {
                return null;
            }
            InputStream assetInputStream = Wearable.DataApi.getFdForAsset(
                mGoogleApiClient, asset).await().getInputStream();

            if (assetInputStream == null) {
                Log.w(TAG, "Requested an unknown Asset.");
                return null;
            }
            return BitmapFactory.decodeStream(assetInputStream);
        } catch (Exception e) {
            Log.e("Error", "parsing asset", e);
        }
        return null;
    }


    @Override
    public void onConnected(Bundle bundle) {
        Wearable.DataApi.addListener(mGoogleApiClient, this);
        getAccessToken();
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.unregisterConnectionCallbacks(this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {
        setLoading("GApi Connection Suspended", false);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        setLoading("GApi Connection Failed", false);
    }
}
