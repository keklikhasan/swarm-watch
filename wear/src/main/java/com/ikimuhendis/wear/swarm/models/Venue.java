package com.ikimuhendis.wear.swarm.models;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.android.gms.wearable.DataMap;

import java.util.List;

public class Venue {

    public String id;
    public String name;
    public Bitmap bitmap;

    public Venue() {
    }

    public Venue(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static Venue extractFromDataMap(DataMap dataMap) {
        String id = dataMap.getString("id");
        String name = dataMap.getString("name");
        return new Venue(id, name);
    }

}