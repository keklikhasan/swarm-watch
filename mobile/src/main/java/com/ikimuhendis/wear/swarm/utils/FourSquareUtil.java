package com.ikimuhendis.wear.swarm.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class FourSquareUtil {

    public static void saveAccessToken(Context context, String accessToken) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("FSQ_ACCESS_TOKEN", accessToken);
        editor.commit();
    }

    public static String getAccessToken(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPref.getString("FSQ_ACCESS_TOKEN", null);
    }
}
