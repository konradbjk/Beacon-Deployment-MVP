package com.example.admin.deploymentmvp;

import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.kontakt.sdk.android.ble.configuration.ActivityCheckConfiguration;
import com.kontakt.sdk.android.ble.configuration.ScanMode;
import com.kontakt.sdk.android.ble.configuration.ScanPeriod;
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener;
import com.kontakt.sdk.android.ble.filter.eddystone.EddystoneFilter;
import com.kontakt.sdk.android.ble.filter.ibeacon.IBeaconFilter;
import com.kontakt.sdk.android.ble.manager.ProximityManager;
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory;
import com.kontakt.sdk.android.ble.manager.listeners.ScanStatusListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleEddystoneListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleIBeaconListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleScanStatusListener;
import com.kontakt.sdk.android.ble.manager.listeners.simple.SimpleSecureProfileListener;
import com.kontakt.sdk.android.common.model.Activity;
import com.kontakt.sdk.android.common.profile.IBeaconDevice;
import com.kontakt.sdk.android.common.profile.IBeaconRegion;
import com.kontakt.sdk.android.common.profile.IEddystoneDevice;
import com.kontakt.sdk.android.common.profile.IEddystoneNamespace;
import com.kontakt.sdk.android.common.profile.ISecureProfile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class MainScreen extends AppCompatActivity {

    protected static String gpsLocation;
    protected static String batteryLevel;
    protected static String dateString;
    protected static String uniqueId;
    protected static String minorValue;
    protected static String majorValue;
    protected static String uuidValue;
    private String rssiBuffor;
    protected static String instanceID;
    protected static String namespaceValue;

    private static PrintWriter out = null;

    private ProximityManager kontaktManager;
    private String TAG = "MyActivity";

    protected static final long MINIMUM_DISTANCE_CHANGE_FOR_UPDATES = 1; // in Meters
    protected static final long MINIMUM_TIME_BETWEEN_UPDATES = 1000; // in Milliseconds
    private static final int READ_REQUEST_CODE = 42;


    protected String oneBeaconLine = "0";

    protected static int i = 0;
    protected static LocationManager locationManager;
    static String provider;
    protected Button startScanButton;
    protected Button viewMapButton;
    protected String rssi;
    private String filePath;
    protected boolean isCycleStarted = false;
    protected static boolean isShuffled = false;
    protected static boolean isBeaconPro = false;
    protected static boolean isIBeacon = false;
    protected static boolean isEddystone = false;
    protected static boolean gpsLocationTurned = true;
    private final static int REQUEST_ENABLE_BT=1;

    private final Handler handler = new android.os.Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);
        kontaktManager = ProximityManagerFactory.create(this);

        oneTimeConfiguration();

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        final Switch gpsSwitch = (Switch) findViewById(R.id.switchGps);
        gpsSwitch.setChecked(true);
        gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // turn off GPS
                gpsLocationTurned = isChecked;
            }
        });

        final Switch iBeaconSwitch = (Switch) findViewById(R.id.switchIBeacon);
        iBeaconSwitch.setChecked(true);
        iBeaconSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Log.d(TAG, "iBeacon checked");
                    // TODO implement iBeacon
                }
            }
        });

        final Switch eddystoneSwitch = (Switch) findViewById(R.id.switchEddystone);
        eddystoneSwitch.setChecked(false);
        eddystoneSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    Log.d(TAG, "Eddystone checked");
                    // TODO implement eddystone
                }
            }
        });

        viewMapButton = (Button) findViewById(R.id.view_map_button);
        viewMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performFileSearch();
            }
        });
        startScanButton = (Button) findViewById(R.id.scan_button);
        startScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!iBeaconSwitch.isChecked() && !eddystoneSwitch.isChecked()) {
                    showToast("You need to specify at least one beacon profile");
                    return;
                }

                if (iBeaconSwitch.isChecked()) {
                    implementIBeacon();
                }

                if (eddystoneSwitch.isChecked()){
                    implementEddystone();
                }

                startScan();
            }
        });
    }

    @Override
    protected void onStop() {
        kontaktManager.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        kontaktManager.disconnect();
        kontaktManager = null;
        super.onDestroy();
    }

    private void checkPermissions() {
        int checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, Arrays.toString(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}));
        if (PackageManager.PERMISSION_GRANTED == checkSelfPermissionResult) {
            //already granted
            Log.d(TAG, "Permission already granted");
            createFile();
        } else {
            //request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            Log.d(TAG, "Permission request called");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (100 == requestCode) {
                Log.d(TAG, "Permission granted");
                createFile();
            }
        } else {
            Log.d(TAG, "Permission not granted");
            showToast("Kontakt.io SDK require this permission");
        }
    }

    public void oneTimeConfiguration() {
        checkPermissions();
        configureProximityManager();
        configureIBeaconFilters(distanceFilter);
        configureEddystoneFilter(distanceFilterE);
    }

    private void configureProximityManager() {
        kontaktManager.configuration()
                .activityCheckConfiguration(ActivityCheckConfiguration.create(10000, 5000))
                .deviceUpdateCallbackInterval(1000)
                .scanMode(ScanMode.BALANCED)
                .scanPeriod(ScanPeriod.RANGING);

        kontaktManager.setScanStatusListener(createScanStatusListener());
//        kontaktManager.setSecureProfileListener(secureProfileListener());

        Log.d(TAG, "Manager initialised");
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainScreen.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void dialogBox(final String message) {
        kontaktManager.disconnect();

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        //Yes button clicked
                        startCycle();
                        break;

                   /* case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        startScan();
                        break;*/

                    case DialogInterface.BUTTON_NEUTRAL:
                        // List button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(MainScreen.this);
        builder
                .setMessage(message)
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                //.setNeutralButton("List", dialogClickListener)
                .show();
    }

    protected void configureIBeaconFilters(IBeaconFilter filter) {
        kontaktManager.filters().iBeaconFilter(filter);
    }

    protected void configureEddystoneFilter(EddystoneFilter filterE){
        kontaktManager.filters().eddystoneFilter(filterE);
    }

    private ScanStatusListener createScanStatusListener() {
        return new SimpleScanStatusListener() {
            @Override
            public void onScanStart() {
                Log.d(TAG, "Scanning started");
                showToast("Scanning started");
            }

            @Override
            public void onScanStop() {
                Log.d(TAG, "Scanning stopped");
                showToast("Scanning stopped");
            }
        };
    }

/*    private SimpleSecureProfileListener secureProfileListener() {
        return new SimpleSecureProfileListener() {
            @Override
            public void onProfileDiscovered(ISecureProfile profile) {
                isBeaconPro = true;
                dialogBox("Is it Beacon Pro: " + profile.getUniqueId() + " and RSSI : " + profile.getRssi() + " and TX Power : " + profile.getTxPower());
                uniqueId = profile.getUniqueId();
                rssi = String.valueOf(profile.getRssi());
                rssiBuffor = rssi + ",";
                batteryLevel = String.valueOf(profile.getBatteryLevel());
            }
            @Override
            public void onProfilesUpdated(List<ISecureProfile> profiles) {
                for (ISecureProfile profile : profiles) {
                    rssiBuffor += profile.getRssi();
                }
            }

        };
    }*/

    protected void implementIBeacon(){
        kontaktManager.setIBeaconListener(iBeaconListener());
    }

    protected void implementEddystone(){
        kontaktManager.setEddystoneListener(eddystoneListener());
    }

    protected SimpleIBeaconListener iBeaconListener() {
        return new SimpleIBeaconListener() {
            @Override
            public void onIBeaconDiscovered(IBeaconDevice ibeacon, IBeaconRegion region) {
                kontaktManager.stopScanning();
                if (isCycleStarted) {
                    return;
                }
                if (ibeacon.getUniqueId()==null) {
                    isIBeacon=true;
                    isShuffled = true;
                    dialogBox("Is it iBeacon with minor : "+ ibeacon.getMinor()+ "/r/n"
                            + "RSSI : "+ ibeacon.getRssi()+ "/r/n"
                            + "TX Power : "+ ibeacon.getTxPower());
                    uniqueId = "null";
                    uuidValue = String.valueOf(ibeacon.getProximityUUID());
                    majorValue = String.valueOf(ibeacon.getMajor());
                    minorValue = String.valueOf(ibeacon.getMinor());
                    batteryLevel = String.valueOf(ibeacon.getBatteryPower());
                    rssi = String.valueOf(ibeacon.getRssi());
                    rssiBuffor = rssi + ",";

                } else {
                    isIBeacon = true;
                    dialogBox("iBeacon: " + ibeacon.getUniqueId()+ "/r/n"
                            + "Minor: " + ibeacon.getMinor()+ "/r/n"
                            + "RSSI: " + ibeacon.getRssi() +  "/r/n"
                            + "TX Power : "+ ibeacon.getTxPower());
                    uniqueId = ibeacon.getUniqueId();
                    uuidValue = String.valueOf(ibeacon.getProximityUUID());
                    majorValue = String.valueOf(ibeacon.getMajor());
                    minorValue = String.valueOf(ibeacon.getMinor());
                    rssi = String.valueOf(ibeacon.getRssi());
                    rssiBuffor = rssi + ",";
                    batteryLevel = String.valueOf(ibeacon.getBatteryPower());

                }
            }

            @Override
            public void onIBeaconsUpdated(List<IBeaconDevice> ibeacons, IBeaconRegion region) {
                for (IBeaconDevice ibeacon : ibeacons)
                    rssiBuffor += ibeacon.getRssi() + ",";
            }
        };
    }

    protected SimpleEddystoneListener eddystoneListener() {
        return new SimpleEddystoneListener() {
            @Override
            public void onEddystoneDiscovered(IEddystoneDevice eddystone, IEddystoneNamespace namespace) {
                kontaktManager.stopScanning();
                if(isCycleStarted){
                    return;
                }
                if("null".equals(eddystone.getUniqueId())){
                    isEddystone = true;
                    isShuffled = true;
                    dialogBox("Is it Eddystone with InstanceId: " + eddystone.getInstanceId() + "/r/n"
                            + "RSSI : " + eddystone.getRssi()+ "/r/n"
                            + "TX Power : " + eddystone.getTxPower());
                    uniqueId = "null";
                    instanceID = eddystone.getInstanceId();
                    namespaceValue = eddystone.getNamespace();
                    rssi = String.valueOf(eddystone.getRssi());
                    rssiBuffor = rssi + ",";
                    batteryLevel = String.valueOf(eddystone.getBatteryPower());
                }

                else{
                    isEddystone = true;
                    dialogBox("Eddystone: " + eddystone.getUniqueId()+ "/r/n"
                            + "Namespace: " + eddystone.getNamespace()+ "/r/n"
                            + "RSSI: " + eddystone.getRssi()+ "/r/n"
                            + "TX Power " + eddystone.getTxPower());
                    uniqueId = eddystone.getUniqueId();
                    instanceID = eddystone.getInstanceId();
                    namespaceValue = eddystone.getNamespace();
                    rssi = String.valueOf(eddystone.getRssi());
                    rssiBuffor = rssi + ",";
                    batteryLevel = String.valueOf(eddystone.getBatteryPower());
                }
            }

            @Override
            public void onEddystonesUpdated(List<IEddystoneDevice> eddystones, IEddystoneNamespace namespace) {
                for (IEddystoneDevice eddystone : eddystones) {
                    rssiBuffor += eddystone.getRssi() + ",";
                }
            }
        };
    }

    private void startScan() {
        kontaktManager.connect(new OnServiceReadyListener() {
            @Override
            public void onServiceReady() {
                kontaktManager.startScanning();
            }
        });
    }

    //Get the GPS coordinates
    public void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkPermissions();
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

        provider = locationManager.getBestProvider(criteria, true);
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            //Log.d(TAG, "Current Location Longitude: " + location.getLongitude() + " Latitude:" + location.getLatitude());
            gpsLocation = location.getLongitude() + "," + location.getLatitude();
        }
        else{
            gpsLocation = "null , null";
        }
    }

    //start the 5 sec cycle and save the values
    protected void startCycle() {
        final MediaPlayer soundCalculating = MediaPlayer.create(this, R.raw.nolf2_calculating);
        soundCalculating.start();
        isCycleStarted = true;
        tempFilters();
        startScan();
        getLocation();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopCycle();
            }
        }, TimeUnit.SECONDS.toMillis(5));
    }

    private void tempFilters(){
        if (isShuffled) {
            if (isIBeacon) {
                configureIBeaconFilters(minorFilter);
                return;
            }
            if (isEddystone){
                configureEddystoneFilter(instanceFilter);
            }
            else{
                Log.d(TAG, "not eddy or ibeacon");
            }
        }

        else{
            if (isIBeacon) {
                configureIBeaconFilters(uniqueIdFilter);
                return;
            }
            if (isEddystone){
                configureEddystoneFilter(uniqueIdFilterE);
            }
            else{
                Log.d(TAG, "not eddy or ibeacon");
            }
        }
    }

    private void stopCycle() {
            isCycleStarted = false;
            kontaktManager.disconnect();
            configureIBeaconFilters(distanceFilter);
            configureEddystoneFilter(distanceFilterE);
            saveBeacon();
    }

    public void createFile() {
        try {
            dateString = DateFormat.format("dd-MM-yyyy-hh-mm-ss-aa", System.currentTimeMillis()).toString();

            // this will create a new name everytime and unique
            File root = new File(ContextCompat.getExternalFilesDirs(this, null)[0].getAbsolutePath());
            // if external memory exists and folder with name Logs
            if (!root.exists()) {
                root.mkdirs(); // this will create folder.
            }
            String path = root.getAbsolutePath();
            Log.i(TAG, "File path is: " + path);

            File file = new File(root, dateString + ".txt");  // file path to save
            boolean created = file.createNewFile();
            filePath = file.getAbsolutePath();
            Log.i(TAG, file.toString() + " " + created);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Create file exception: " + e.getMessage());
        }
    }

    public void saveBeacon() {
        try {
            out = new PrintWriter(new BufferedWriter(new FileWriter(filePath, true)));

            if (gpsLocationTurned){
                if (isBeaconPro){
                    oneBeaconLine = i + ","+ "Beacon Pro" + "," + uniqueId + "," + dateString +  "," + gpsLocation + ","
                            + batteryLevel + "," + rssiBuffor;
                    saveLine(oneBeaconLine, out);
                    return;
                }

                if (isIBeacon) {
                    oneBeaconLine = i + "," + "iBeacon" + "," + uniqueId + "," + dateString + "," + gpsLocation + ","
                            + uuidValue + "," + majorValue + "," + minorValue + ","
                            + batteryLevel + "," + rssiBuffor;
                    saveLine(oneBeaconLine, out);
                    return;
                }

                if (isEddystone) {
                    oneBeaconLine = i + "," + "Eddystone" + "," + uniqueId + "," + dateString + "," + gpsLocation + ","
                            + namespaceValue + "," + instanceID + "," + batteryLevel + "," + rssiBuffor;
                    saveLine(oneBeaconLine, out);
                }
            }

            else {
                if (isBeaconPro){
                    oneBeaconLine = i + ","+ "Beacon Pro" + "," + uniqueId + "," + dateString +  ","  + ","
                            + batteryLevel + "," + rssiBuffor;
                    saveLine(oneBeaconLine, out);
                    return;
                }

                if (isIBeacon) {
                    oneBeaconLine = i + "," + "iBeacon" + "," + uniqueId + "," + dateString + "," +  ","
                            + uuidValue + "," + majorValue + "," + minorValue + ","
                            + batteryLevel + "," + rssiBuffor;
                    saveLine(oneBeaconLine, out);
                    return;
                }

                if (isEddystone) {
                    oneBeaconLine = i + "," + "Eddystone" + "," + uniqueId + "," + dateString + "," + ","
                            + namespaceValue + "," + instanceID + "," + batteryLevel + "," + rssiBuffor;
                    saveLine(oneBeaconLine, out);
                }
            }

        } catch (IOException e) {
            showToast("Save beacon exception: " + e.getMessage());
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    private void launchScanScreen() {
        Intent intent = new Intent(this, ScanScreen.class);
        startActivity(intent);
    }

    public void performFileSearch() {

        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // Filter to show only images, using the image MIME data type.
        // If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
        // To search for all documents available via installed storage providers,
        // it would be "*/*".
        intent.setType("*/*");

        startActivityForResult(intent, READ_REQUEST_CODE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        // The ACTION_OPEN_DOCUMENT intent was sent with the request code
        // READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
        // response to some other intent, and the code below shouldn't run at all.

        if (requestCode == READ_REQUEST_CODE && resultCode == android.app.Activity.RESULT_OK) {
            // The document selected by the user won't be returned in the intent.
            // Instead, a URI to that document will be contained in the return intent
            // provided to this method as a parameter.
            // Pull that URI using resultData.getData().
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                Log.i(TAG, "Uri: " + uri.toString());
                launchMapViewScreen();
            }
        }
    }

    private void launchMapViewScreen() {
        Intent intent = new Intent(MainScreen.this,ViewMapScreen.class);
        startActivity(intent);
    }

    private void saveLine(String line, PrintWriter out) {
        final MediaPlayer soundAllSystems = MediaPlayer.create(this, R.raw.nolf2_all_systems);
        out.println(line);
        i++;
        showToast("Beacon saved");
        soundAllSystems.start();
        isShuffled = false;
        isIBeacon = false;
        isEddystone = false;
        isBeaconPro = false;
    }

    private final IBeaconFilter uniqueIdFilter = new IBeaconFilter() {
        @Override
        public boolean apply(IBeaconDevice iBeaconDevice) {
            return uniqueId.equalsIgnoreCase(iBeaconDevice.getUniqueId());
        }
    };

    private final IBeaconFilter distanceFilter = new IBeaconFilter() {
        @Override
        public boolean apply(IBeaconDevice iBeaconDevice) {
            return iBeaconDevice.getDistance() < 0.15;
        }
    };

    private final IBeaconFilter minorFilter = new IBeaconFilter() {
        @Override
        public boolean apply(IBeaconDevice iBeaconDevice) {
            return minorValue.equalsIgnoreCase(String.valueOf(iBeaconDevice.getMinor()));
        }
    };

    private final EddystoneFilter uniqueIdFilterE = new EddystoneFilter() {
        @Override
        public boolean apply(IEddystoneDevice iEddystoneDevice) {
            return uniqueId.equalsIgnoreCase(iEddystoneDevice.getUniqueId());
        }
    };
    private final EddystoneFilter distanceFilterE = new EddystoneFilter() {
        @Override
        public boolean apply(IEddystoneDevice iEddystoneDevice) {
            return iEddystoneDevice.getDistance() < 0.15;
        }
    };
    
    private final EddystoneFilter instanceFilter = new EddystoneFilter() {
        @Override
        public boolean apply(IEddystoneDevice iEddystoneDevice) {
            return instanceID.equalsIgnoreCase(iEddystoneDevice.getInstanceId());
        }
    };

}