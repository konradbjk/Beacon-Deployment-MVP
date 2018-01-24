package com.example.admin.deploymentmvp;

import android.*;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import static com.example.admin.deploymentmvp.MainScreen.MINIMUM_DISTANCE_CHANGE_FOR_UPDATES;
import static com.example.admin.deploymentmvp.MainScreen.MINIMUM_TIME_BETWEEN_UPDATES;
import static com.example.admin.deploymentmvp.MainScreen.gpsLocation;
import static com.example.admin.deploymentmvp.MainScreen.locationManager;
import static com.example.admin.deploymentmvp.MainScreen.provider;

/**
 * Created by kbujak on 27/11/2017.
 */

public class ViewMapScreen extends AppCompatActivity
        implements OnMapReadyCallback {

    protected String TAG = "View Map Screen";
    protected static LatLng userLocation;

    private static final float ZOOM_LEVEL = 18f;


    @Override
    public void onMapReady(GoogleMap googleMap) {
            locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MINIMUM_TIME_BETWEEN_UPDATES,
                    MINIMUM_DISTANCE_CHANGE_FOR_UPDATES,
                    new GPSlistener()
            );

            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_FINE);
            criteria.setAltitudeRequired(false);
            criteria.setBearingRequired(false);
            criteria.setCostAllowed(true);
            criteria.setPowerRequirement(Criteria.POWER_HIGH);
        // Add a marker in Sydney, Australia,
        // and move the map's camera to the same location.

        provider = locationManager.getBestProvider(criteria, true);

        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            userLocation = new LatLng(location.getLatitude(), location.getLongitude());
        }
        else{
            userLocation = new LatLng(50.0509184, 19.982907);
        }

        googleMap.addMarker(new MarkerOptions().position(userLocation)
                .title("Your position"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, ZOOM_LEVEL));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve the content view that renders the map.
        setContentView(R.layout.activity_view_map);
        // Get the SupportMapFragment and request notification
        // when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }
}
