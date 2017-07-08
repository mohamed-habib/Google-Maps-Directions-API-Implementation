package com.mohamedhabib.map.api;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.maps.model.LatLng;
import com.mohamedhabib.map.R;

/**
 * Call Web APIs from this class
 * Created by Mohamed Habib on 7/07/2017.
 */
public class APIs {
    private static final String BASE_DIRECTIONS_URL = "https://maps.googleapis.com/maps/api/directions/json?";
    private static String ORIGIN_PARAMETER = "origin=";
    private static String DESTINATION_PARAMETER = "destination=";
    private static String KEY_PARAMETER = "key=";
    private static String ALTERNATIVES_PARAMETER = "alternatives=";

    public static void callDirectionsAPI(Context context, LatLng origin, LatLng destination,
                                         boolean alternatives, Response.Listener<String> successListener,
                                         Response.ErrorListener errorListener) {
        String url = appendOrigin(BASE_DIRECTIONS_URL, origin);
        url += "&";
        url = appendDestination(url, destination);
        url += "&";
        url = appendAlternatives(url, alternatives);
        url += "&";
        url = appendKey(url, context.getString(R.string.google_maps_key));
        StringRequest jsonObjectRequest = new StringRequest(Request.Method.GET, url, successListener, errorListener);
        Volley.newRequestQueue(context).add(jsonObjectRequest);
    }

    private static String appendOrigin(String url, LatLng origin) {
        return url + ORIGIN_PARAMETER + origin.latitude + "," + origin.longitude;
    }

    private static String appendDestination(String url, LatLng destination) {
        return url + DESTINATION_PARAMETER + destination.latitude + "," + destination.longitude;
    }

    private static String appendAlternatives(String url, boolean alternatives) {
        return url + ALTERNATIVES_PARAMETER + String.valueOf(alternatives);
    }

    private static String appendKey(String url, String key) {
        return url + KEY_PARAMETER + key;
    }


}
