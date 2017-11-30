package com.example.admin.deploymentmvp;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import static com.example.admin.deploymentmvp.MainScreen.gpsLocation;
import static com.example.admin.deploymentmvp.MainScreen.instanceID;
import static com.example.admin.deploymentmvp.MainScreen.isEddystone;
import static com.example.admin.deploymentmvp.MainScreen.isIBeacon;
import static com.example.admin.deploymentmvp.MainScreen.majorValue;
import static com.example.admin.deploymentmvp.MainScreen.minorValue;
import static com.example.admin.deploymentmvp.MainScreen.namespaceValue;
import static com.example.admin.deploymentmvp.MainScreen.uniqueId;
import static com.example.admin.deploymentmvp.MainScreen.uuidValue;

public class ScanScreen extends AppCompatActivity {

    protected String beaconConfig = "0";
    TextView beaconId;
    TextView secondLine;
    TextView gpsLine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_screen);

        beaconId = (TextView) findViewById(R.id.textView);
        secondLine = (TextView) findViewById(R.id.textView2);
        gpsLine = (TextView) findViewById(R.id.textView3);

        if (isIBeacon){
            beaconConfig = "UUID: " + uuidValue + "/r/n"
                    + "Major: " + majorValue
                    + "Minor: " + minorValue + "/r/n";
        }

        if (isEddystone){
            beaconConfig = "Namespace: " + namespaceValue +"/r/n"
                    + "InstanceID: " + instanceID;
        }

        beaconId.setText(uniqueId);
        gpsLine.setText(gpsLocation);
        secondLine.setText(beaconConfig);
    }
}
