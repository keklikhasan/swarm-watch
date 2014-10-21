package com.ikimuhendis.wear.swarm.restServices;

import com.ikimuhendis.wear.Config;
import com.ikimuhendis.wear.swarm.models.DoCheckinResponse;
import com.ikimuhendis.wear.swarm.models.SearchResponse;

import retrofit.RestAdapter;

public class FoursquareApi {

    private RESTInterface api;

    public FoursquareApi() {
        init();
    }

    public void init() {
        RestAdapter restAdapter = new RestAdapter.Builder()
            .setEndpoint(Config.BASE_URL)
            .build();
        api = restAdapter.create(RESTInterface.class);
    }

    public SearchResponse search(String location) {
        return api.search(Config.CLIENT_ID, Config.CLIENT_SECRET, Config.VERSION, location, 5);
    }

    public DoCheckinResponse checkin(String token, String venueId) {
        return api.checkin(Config.CLIENT_ID, Config.CLIENT_SECRET, Config.VERSION, token, venueId, "private");
    }

}
