package com.ikimuhendis.wear.swarm.models;

import android.graphics.Bitmap;
import android.os.Bundle;

import com.google.android.gms.wearable.DataMap;

import java.util.List;

public class Venue {

    public String id;
    public String name;
    public List<Category> categories;

    public Venue() {
    }

    public Venue(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public static void fillBundle(Bundle bundle, String[] nameArray, String[] venueIdArray) {
        bundle.putStringArray("names", nameArray);
        bundle.putStringArray("ids", venueIdArray);
    }

    public static Venue extractFromDataMap(DataMap dataMap) {
        String id = dataMap.getString("id");
        String name = dataMap.getString("name");
        return new Venue(id, name);
    }

    public DataMap getDataMap() {
        final DataMap dataMap = new DataMap();
        dataMap.putString("id", this.id);
        dataMap.putString("name", this.name);
        return dataMap;
    }

    public String getPrimaryCategoryPNGIconUrl() {
        if (categories != null) {
            for (Category category : categories) {
                Icon icon = category.getIcon();
                if (category.isPrimary() && ".png".equals(icon.getSuffix().toLowerCase())) {
                    return icon.getPrefix() + "44" + icon.getSuffix();
                }
            }
        }

        return null;
    }

}