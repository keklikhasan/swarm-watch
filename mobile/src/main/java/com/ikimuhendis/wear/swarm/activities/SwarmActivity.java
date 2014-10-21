package com.ikimuhendis.wear.swarm.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.foursquare.android.nativeoauth.FoursquareOAuth;
import com.foursquare.android.nativeoauth.model.AccessTokenResponse;
import com.foursquare.android.nativeoauth.model.AuthCodeResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.ikimuhendis.wear.Config;
import com.ikimuhendis.wear.swarm.R;
import com.ikimuhendis.wear.swarm.utils.FourSquareUtil;

import java.util.Collection;
import java.util.HashSet;


public class SwarmActivity extends Activity implements GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = SwarmActivity.class.getName();

    private static final int REQUEST_CODE_FSQ_CONNECT = 0;
    private static final int REQUEST_CODE_FSQ_TOKEN_EXCHANGE = 1;
    private Button buttonAction;
    private Button buttonOpenWear;
    private String accessToken;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_swarm);
        buttonAction = (Button) findViewById(R.id.button_action);
        buttonOpenWear = (Button) findViewById(R.id.button_open_wear_app);
        buttonAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onButtonClick();
            }
        });
        buttonOpenWear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                (new OpenWearAppTask()).execute();
            }
        });
        accessToken = FourSquareUtil.getAccessToken(this);
        refreshViews();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addApi(Wearable.API)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .build();
        mGoogleApiClient.connect();
        if (getIntent() != null) {
            Bundle bundle = getIntent().getExtras();
            if (bundle != null && bundle.getBoolean("start_login")) {
                onButtonClick();
            }
        }
    }

    private void onButtonClick() {
        if (TextUtils.isEmpty(accessToken)) {
            Intent intent = FoursquareOAuth.getConnectIntent(SwarmActivity.this, Config.CLIENT_ID);
            startActivityForResult(intent, REQUEST_CODE_FSQ_CONNECT);
        } else {
            accessToken = null;
            FourSquareUtil.saveAccessToken(SwarmActivity.this, null);
            setAccessToken();
            refreshViews();
        }
    }


    private void refreshViews() {
        if (TextUtils.isEmpty(accessToken)) {
            buttonAction.setText(R.string.button_login);
            buttonOpenWear.setVisibility(View.INVISIBLE);
        } else {
            buttonAction.setText(R.string.button_logout);
            buttonOpenWear.setVisibility(View.VISIBLE);
        }
    }

    private HashSet<String> results;

    private Collection<String> getNodes() {
        if (results == null && results.size() < 1) {
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

    private void openWearApp() {
        Collection<String> nodes = getNodes();
        for (String node : nodes) {
            MessageApi.SendMessageResult result = Wearable.MessageApi.sendMessage(
                mGoogleApiClient, node, "/start_activity", null).await();
            if (!result.getStatus().isSuccess()) {
                Toast.makeText(this, "ERROR: failed to send Message: " + result.getStatus(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "ERROR: failed to send Message: " + result.getStatus());
            }
        }
    }

    private class OpenWearAppTask extends AsyncTask<Void, Void, String> {

        @Override
        protected String doInBackground(Void... params) {
            openWearApp();
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
        }
    }

    private void setAccessToken() {
        if (mGoogleApiClient.isConnected()) {
            PutDataMapRequest dataMap = PutDataMapRequest.create("/access_token");
            dataMap.getDataMap().putString("access_token", accessToken);
            PutDataRequest request = dataMap.asPutDataRequest();
            Wearable.DataApi.putDataItem(mGoogleApiClient, request);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_FSQ_CONNECT:
                    AuthCodeResponse codeResponse = FoursquareOAuth.getAuthCodeFromResult(resultCode, data);
                    String code = codeResponse.getCode();
                    if (!TextUtils.isEmpty(code)) {
                        Intent intent = FoursquareOAuth.getTokenExchangeIntent(this, Config.CLIENT_ID,
                            Config.CLIENT_SECRET, code);
                        startActivityForResult(intent, REQUEST_CODE_FSQ_TOKEN_EXCHANGE);
                    } else {
                        Toast.makeText(this, "FSQ_CONNECT request failed", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "FSQ_CONNECT request failed");
                    }
                    break;
                case REQUEST_CODE_FSQ_TOKEN_EXCHANGE:
                    AccessTokenResponse tokenResponse = FoursquareOAuth.getTokenFromResult(resultCode, data);
                    accessToken = tokenResponse.getAccessToken();
                    if (!TextUtils.isEmpty(accessToken)) {
                        FourSquareUtil.saveAccessToken(this, accessToken);
                        setAccessToken();
                        refreshViews();
                    } else {
                        Toast.makeText(this, "TOKEN_EXCHANGE request failed", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "TOKEN_EXCHANGE request failed");
                    }
                    break;
            }
        } else {
            Toast.makeText(this, "Activity return not ok", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "Activity return not ok");
        }
    }

    @Override
    protected void onDestroy() {
        mGoogleApiClient.unregisterConnectionCallbacks(this);
        mGoogleApiClient.unregisterConnectionFailedListener(this);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.swarm, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_about:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection suspended", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection failed", Toast.LENGTH_SHORT).show();
        Log.e(TAG, "Connection failed");
    }
}
