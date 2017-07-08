package com.mohamedhabib.map.api;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by Mohamed Habib on 08/07/2017.
 * <p>
 * Model Class to hold the Route location coordinates and the duration of that route
 */
public class Route {
    public List<List<HashMap<String, String>>> mRoutesLatLng = null;
    private long mDurationLong;

    public Route(List<List<HashMap<String, String>>> routesLatLng, long duration) {
        mRoutesLatLng = routesLatLng;
        mDurationLong = duration;
    }

    public String getDuration() {
        long hours = mDurationLong / 3600;
        long minutes = (mDurationLong % 3600) / 60;
        String output = "";
        if (hours != 0)
            output += String.format(Locale.getDefault(), "%02d hr ", hours);

        if (minutes != 0)
            output += String.format(Locale.getDefault(), "%02d min", minutes);

        return output;
    }
}
