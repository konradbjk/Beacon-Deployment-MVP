package com.example.admin.deploymentmvp;

import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import static android.content.ContentValues.TAG;

/**
 * Created by Admin on 13.02.2017.
 */

class GPSlistener implements LocationListener {
    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed: Lat: " + location.getLatitude() + " Lng: "+ location.getLongitude());
        String longitude = "Longitude: " + location.getLongitude();
        Log.v(TAG, longitude);
        String latitude = "Latitude: " + location.getLatitude();
        Log.v(TAG, latitude);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d(TAG, "Provider disabled by the user. GPS turned off");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d(TAG, "Provider status changed");
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d(TAG, "Provider enabled by the user. GPS turned on");
    }
}
