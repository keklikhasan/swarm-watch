package com.ikimuhendis.wear.swarm.restServices;

import com.ikimuhendis.wear.swarm.models.DoCheckinResponse;
import com.ikimuhendis.wear.swarm.models.SearchResponse;

import retrofit.http.Field;
import retrofit.http.FormUrlEncoded;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Query;

public interface RESTInterface {
    @GET("/v2/venues/search")
    SearchResponse search(@Query("client_id") String clientId,
                          @Query("client_secret") String clientSecret,
                          @Query("v") int version,
                          @Query("ll") String location,
                          @Query("limit") int limit);


    @FormUrlEncoded
    @POST("/v2/checkins/add")
    DoCheckinResponse checkin(@Field("client_id") String clientId,
                                @Field("client_secret") String clientSecret,
                                @Field("v") int version,
                                @Field("oauth_token") String oAuthToken,
                                @Field("venueId") String venueId,
                                @Field("broadcast") String broadcast);

}
