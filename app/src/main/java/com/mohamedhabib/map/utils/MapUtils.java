package com.mohamedhabib.map.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by Mohamed Habib on 07/07/2017.
 */
public class MapUtils {
    /**
     * Get the Address of a location using its coordinates
     * <p>
     * this method takes time, better call it in a the background to avoid blocking the UI.
     *
     * @param context        needed whie initializing the Geocoder class
     * @param locationLatLng to search for
     * @return the address of the specified location latlng
     */
    public static Address getAddressByLatLng(Context context, LatLng locationLatLng) {
        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
        Address returnedAddress = null;
        try {
            List<Address> addresses = geocoder.getFromLocation(locationLatLng.latitude, locationLatLng.longitude, 1);
            if (addresses != null) {
                returnedAddress = addresses.get(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return returnedAddress;
    }

    /**
     * Get the Address of a location by name
     *
     * this method takes time, better call it in a the background to avoid blocking the UI.
     *
     * @param context      needed whie initializing the Geocoder class
     * @param locationName to search for
     * @return the address of the specified location name
     */
    public static Address getAddressByLocationName(Context context, String locationName) {
        Geocoder geoCoder = new Geocoder(context);
        List<Address> addresses = null;
        try {
            addresses = geoCoder.getFromLocationName(locationName, 1);
            if (addresses != null && addresses.size() > 0) {
                return addresses.get(0);
            } else {
                AlertDialog.Builder adb = new AlertDialog.Builder(context);
                adb.setTitle("Google Map");
                adb.setMessage("Please Provide the Proper Place");
                adb.setPositiveButton("Close", null);
                adb.show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
