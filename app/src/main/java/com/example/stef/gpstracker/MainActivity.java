package com.example.stef.gpstracker;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.PowerManager;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.borax12.materialdaterangepicker.date.DatePickerDialog;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import android.location.Location;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

import android.view.Menu;
import android.view.MenuItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;

public class MainActivity extends AppCompatActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener, OnMapReadyCallback, ResultCallback<Status>,
        NavigationView.OnNavigationItemSelectedListener, DatePickerDialog.OnDateSetListener {

    protected static final String TAG = "MainActivity";
    public static final  int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private String datepickedstart="";
    private String datepickedend="";

    private int selected_item;
    private String selected_dates="Choose a time period";

    private GoogleMap mMap;
    private Marker marker;
    private HeatmapTileProvider mProvider;
    private TileOverlay mOverlay;

    private String activity_type="Uninitialized";
    private String mLastactivity_type="Uninitialized";

    DBHelper mydb; //My app's Database
    protected GoogleApiClient mGoogleApiClient; /* Provides the entry point to Google Play services.*/
    protected Location mCurrentLocation; /* Represents a geographical location.*/
    protected String mLastUpdateTime;
    protected String mCurrentTime;
    protected long mTimeOfLastLocationEvent;
    public double lat; //Latitude variable
    public double lon; //Longitude variable
    public ArrayList<LatLng> locationlist = new ArrayList<LatLng>();

    /**
     * A receiver for DetectedActivity objects broadcast by the
     * {@code ActivityDetectionIntentService}.
     */
    protected ActivityDetectionBroadcastReceiver mBroadcastReceiver;
    private BroadcastReceiver mDozeModeReceiver;
    protected BroadcastReceiver bre;
    /**
     * The DetectedActivities that we track in this sample. We use this for initializing the
     * {@code DetectedActivitiesAdapter}. We also use this for persisting state in
     * {@code onSaveInstanceState()} and restoring it in {@code onCreate()}. This ensures that each
     * activity is displayed with the correct confidence level upon orientation changes.
     */

    /* The desired interval for location updates. Inexact. Updates may be more or less frequent.*/
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 30000;

    /* The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.*/
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 3;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /* Stores parameters for requests to the FusedLocationProviderApi.*/
    protected LocationRequest mLocationRequest;

    private FloatingActionButton fab_vehicle;
    private FloatingActionButton fab_bike;
    private FloatingActionButton fab_foot;
    private FloatingActionButton fab_all;

    private TextView mode_textview;
    private DrawerLayout drawer;

    private String mode="All";
    private String time_mode="All Time";

    private NavigationView navigationView;

    private PowerManager pm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        //setContentView(R.layout.activity_maps);

        setContentView(R.layout.activity_menu_slider);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mode_textview = (TextView) findViewById(R.id.current_mode_textview);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        mode_textview.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v)
            {
                drawer.openDrawer(GravityCompat.START);
            }
        });
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final FloatingActionMenu menu = (FloatingActionMenu) findViewById(R.id.menu);

        menu.setOnMenuButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                menu.toggle(true);
            }
        });

        menu.setOnMenuToggleListener(new FloatingActionMenu.OnMenuToggleListener() {
            @Override
            public void onMenuToggle(boolean opened) {
                /*String text = "";
                if (opened) {
                    text = "Menu opened";
                } else {
                    text = "Menu closed";
                }*/
            }
        });

        fab_vehicle = (FloatingActionButton) findViewById(R.id.menu_item_vehicle);
        fab_bike = (FloatingActionButton) findViewById(R.id.menu_item_bike);
        fab_foot = (FloatingActionButton) findViewById(R.id.menu_item_foot);
        fab_all = (FloatingActionButton) findViewById(R.id.menu_item_all);

        fab_vehicle.setOnClickListener(clickListener);
        fab_bike.setOnClickListener(clickListener);
        fab_foot.setOnClickListener(clickListener);
        fab_all.setOnClickListener(clickListener);

        updateValuesFromBundle(savedInstanceState);

        if (time_mode.equals("Date Picked")) {
            mode_textview.setText(selected_dates);
            mode_textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_calendar_text, 0, 0, 0);
        }else{
            if(time_mode.equals("All Time")){
                mode_textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_calendar_multiple, 0, 0, 0);
            } else if(time_mode.equals("Today")){
                mode_textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_calendar_today, 0, 0, 0);
            }
            mode_textview.setText(time_mode);
        }
        MenuItem selected3 = navigationView.getMenu().getItem(2);
        selected3.setTitle(selected_dates);
        if(time_mode.equals("All Time")){
            MenuItem selected = navigationView.getMenu().getItem(0);
            selected.setChecked(true);
        } else if(time_mode.equals("Today")){
            MenuItem selected2 = navigationView.getMenu().getItem(1);
            selected2.setChecked(true);
        } else{
            selected3.setChecked(true);
        }


        mydb = new DBHelper(this);

        // Get a receiver for broadcasts from ActivityDetectionIntentService.
        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter(Constants.BROADCAST_ACTION));


        //Set a receiver for Doze power-state changes
        IntentFilter filter_doze = new IntentFilter(pm.ACTION_DEVICE_IDLE_MODE_CHANGED);
        filter_doze.addAction(pm.ACTION_DEVICE_IDLE_MODE_CHANGED);
        mDozeModeReceiver = new DozeBroadcastReceiver();
        registerReceiver(mDozeModeReceiver, filter_doze);

        //Set a receiver for Screen-On events
        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        bre = new ScreenBroadcaster();
        registerReceiver(bre, filter);

        pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        buildGoogleApiClient();
        if (!mGoogleApiClient.isConnected()) {
            mGoogleApiClient.connect();
        }

    }

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            String text = "";

            switch (v.getId()) {
                case R.id.menu_item_vehicle:
                    ArrayList<LatLng> list = null;
                    if (time_mode.equals("All Time")) {
                        list = mydb.getAllVehicles();
                    } else if (time_mode.equals("Today")){
                        list = mydb.getAllVehiclesToday();
                    } else if (time_mode.equals("Date Picked")){
                        list = mydb.getAllOnVehicleDatePeriod(datepickedstart + " 00:00:00 πμ", datepickedend + " 23:59:59 μμ");
                    }
                    if (!list.isEmpty()) {

                        // Create the gradient.
                        int[] colors_vehicle = {
                                Color.rgb(190,60,125),
                                Color.rgb(86,16,80),
                                Color.rgb(53,1,63)
                        };

                        float[] startPoints_vehicle = {
                                0.2f, 0.8f, 1f
                        };

                        Gradient gradient_vehicle = new Gradient(colors_vehicle, startPoints_vehicle);

                        mProvider.setGradient(gradient_vehicle);

                        mProvider.setData(list);
                        mOverlay.clearTileCache();
                        mode="Vehicle";
                    }
                    else{
                        text="You have no vehicle activity recorded yet!";
                    }
                    break;
                case R.id.menu_item_bike:
                    ArrayList<LatLng> list2 = null;
                    if (time_mode.equals("All Time")) {
                        list2 =  mydb.getAllOnBike();
                    } else if (time_mode.equals("Today")){
                        list2 = mydb.getAllOnBikeToday();
                    } else if (time_mode.equals("Date Picked")){
                        list2  = mydb.getAllOnBikeDatePeriod(datepickedstart+" 00:00:00 πμ", datepickedend+" 23:59:59 μμ");
                    }
                    if (!list2.isEmpty()) {

                        // Create the gradient.
                        int[] colors_bike = {
                                Color.rgb(58,163,193),
                                Color.rgb(40,57,150),
                                Color.rgb(38,9,128)
                        };

                        float[] startPoints_bike = {
                                0.2f, 0.8f, 1f
                        };

                        Gradient gradient_bike = new Gradient(colors_bike, startPoints_bike);

                        mProvider.setData(list2);
                        mProvider.setGradient(gradient_bike);
                        mOverlay.clearTileCache();

                        mode="Bike";

                    }
                    else{
                        text="You have no bike activity recorded yet!";
                    }
                    break;
                case R.id.menu_item_foot:
                    ArrayList<LatLng> list3 = null;
                    if (time_mode.equals("All Time")) {
                        list3 =  mydb.getAllOnFoot();
                    } else if (time_mode.equals("Today")){
                        list3 = mydb.getAllOnFootToday();
                    } else if (time_mode.equals("Date Picked")) {
                        list3 = mydb.getAllOnFootDatePeriod(datepickedstart+" 00:00:00 πμ", datepickedend+" 23:59:59 μμ");
                    }

                    if (!list3.isEmpty()) {

                        // Create the gradient.
                        int[] colors_foot = {
                                Color.rgb(239,172,42),
                                Color.rgb(245,130,125),
                                Color.rgb(250,70,60)
                        };

                        float[] startPoints_foot = {
                                0.2f, 0.6f, 1f
                        };

                        Gradient gradient_foot = new Gradient(colors_foot, startPoints_foot);

                        mProvider.setGradient(gradient_foot);
                        mProvider.setData(list3);
                        mOverlay.clearTileCache();
                        mode="Foot";
                    }
                    else{
                        text="You have no walking or running activity recorded yet!";
                    }
                    break;
                case R.id.menu_item_all:

                    ArrayList<LatLng> list_all = null;
                    if (time_mode.equals("All Time")) {
                        list_all = mydb.getAllPoints();
                    } else if (time_mode.equals("Today")){
                        list_all = mydb.getAllPointsToday();
                    } else if (time_mode.equals("Date Picked")) {
                        list_all = mydb.getAllPointsDatePeriod(datepickedstart+" 00:00:00 πμ", datepickedend+" 23:59:59 μμ");
                    }
                    if (!list_all.isEmpty()) {

                        // Create the gradient.
                        int[] colors_all = {
                                Color.rgb(102, 225, 0), // green
                                Color.rgb(255, 0, 0)    // red
                        };

                        float[] startPoints_all = {
                                0.2f, 1f
                        };

                        Gradient gradient_all = new Gradient(colors_all, startPoints_all);

                        mProvider.setGradient(gradient_all);

                        mProvider.setData(list_all);
                        mOverlay.clearTileCache();

                        mode="All";

                    }else{
                        text=text+"You have no points recorded yet!";
                    }
                    break;
            }

            if (!text.equals("")){
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker and move the camera.
        LatLng currentposition=new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        MarkerOptions currentmarker = new MarkerOptions().position(currentposition).title("Current Location");
        marker = mMap.addMarker(currentmarker);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentposition, 12));
        addHeatMap(mMap);
    }

    /**
     * Builds a GoogleApiClient. Uses the addApi() method to request the LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(ActivityRecognition.API)
                .build();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY,
                true);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        try{
            savedInstanceState.putLong(LAST_UPDATED_TIME_STRING_KEY, mCurrentLocation.getTime());
        }
        catch (NullPointerException e){
            savedInstanceState.putLong(LAST_UPDATED_TIME_STRING_KEY, mTimeOfLastLocationEvent);
        }
        savedInstanceState.putString(Constants.DETECTED_ACTIVITIES, activity_type);
        savedInstanceState.putString("mode", mode);
        savedInstanceState.putString("time mode", time_mode);
        savedInstanceState.putInt("selected", selected_item);
        savedInstanceState.putString("selected dates", selected_dates);
        savedInstanceState.putString("datepickedstart", datepickedstart);
        savedInstanceState.putString("datepickedend", datepickedend);
        super.onSaveInstanceState(savedInstanceState);
    }

    private void updateValuesFromBundle(Bundle savedInstanceState) {
        if (savedInstanceState != null) {

            if (savedInstanceState != null && savedInstanceState.containsKey(
                    Constants.DETECTED_ACTIVITIES)) {
                mLastactivity_type = savedInstanceState.getString(Constants.DETECTED_ACTIVITIES);
            } else {
                mLastactivity_type = "Uninitialized";
            }

            // Update the value of mRequestingLocationUpdates from the Bundle, and
            // make sure that the Start Updates and Stop Updates buttons are
            // correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                //mRequestingLocationUpdates = savedInstanceState.getBoolean(
                //REQUESTING_LOCATION_UPDATES_KEY);
                //setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the
            // UI to show the correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that
                // mCurrentLocation is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mTimeOfLastLocationEvent = savedInstanceState.getLong(
                        LAST_UPDATED_TIME_STRING_KEY);
            }

            if (savedInstanceState != null ) {
                mode = savedInstanceState.getString("mode");
                time_mode = savedInstanceState.getString("time mode");
                selected_item = savedInstanceState.getInt("selected");
                selected_dates =savedInstanceState.getString("selected dates");
                datepickedstart =savedInstanceState.getString("datepickedstart");
                datepickedend =savedInstanceState.getString("datepickedend");
            } else {
                mode = "All";
                time_mode = "All Time";
                mode_textview.setText(time_mode);
                mode_textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_calendar_multiple, 0, 0, 0);
            }
            //updateUI();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        /*if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }*/
    }

    @Override
    public void onResume() {
        super.onResume();

        DatePickerDialog dpd = (DatePickerDialog) getFragmentManager().findFragmentByTag("Datepickerdialog");
        if(dpd != null) dpd.setOnDateSetListener(this);

        // we resume receiving location updates
        if (mGoogleApiClient.isConnected()) {
            // Register the broadcast receiver that informs this activity of the DetectedActivity
            // object broadcast sent by the intent service.
            if (mBroadcastReceiver==null) {
                LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                        new IntentFilter(Constants.BROADCAST_ACTION));
            }
            startLocationUpdates();

            if (bre == null) {
                //Set a receiver for Screen-On events
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_SCREEN_ON);
                bre = new ScreenBroadcaster();
                registerReceiver(bre, filter);
            }

        }
        else{
            mGoogleApiClient.connect();
            // Register the broadcast receiver that informs this activity of the DetectedActivity
            // object broadcast sent by the intent service.
            if (mBroadcastReceiver==null) {
                LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                        new IntentFilter(Constants.BROADCAST_ACTION));
            }

            if (bre == null) {
                //Set a receiver for Screen-On events
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_SCREEN_ON);
                bre = new ScreenBroadcaster();
                registerReceiver(bre, filter);
            }

        }

    }

    @Override
    public void onRestart() {
        super.onRestart();

        DatePickerDialog dpd = (DatePickerDialog) getFragmentManager().findFragmentByTag("Datepickerdialog");
        if(dpd != null) dpd.setOnDateSetListener(this);

        // we resume receiving location updates
        if (mGoogleApiClient.isConnected()) {
            // Register the broadcast receiver that informs this activity of the DetectedActivity
            // object broadcast sent by the intent service.
            if (mBroadcastReceiver==null) {
                LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                        new IntentFilter(Constants.BROADCAST_ACTION));
            }

            if (bre == null) {
                //Set a receiver for Screen-On events
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_SCREEN_ON);
                bre = new ScreenBroadcaster();
                registerReceiver(bre, filter);
            }

            startLocationUpdates();
        }
        else{
            mGoogleApiClient.connect();
            // Register the broadcast receiver that informs this activity of the DetectedActivity
            // object broadcast sent by the intent service.
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                    new IntentFilter(Constants.BROADCAST_ACTION));

            if (bre == null) {
                //Set a receiver for Screen-On events
                IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
                filter.addAction(Intent.ACTION_SCREEN_ON);
                bre = new ScreenBroadcaster();
                registerReceiver(bre, filter);
            }

            startLocationUpdates();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        /*if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }*/

        // Unregister the broadcast receiver that was registered during onResume().
        //LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

        //unregisterReceiver(mDozeModeReceiver);
        //unregisterReceiver(bre);

        ArrayList<String> l2 = mydb.getAllPointsActivity();
        ArrayList<String> l3 = mydb.getAllPointsTime();
        if (!l2.isEmpty()) {
            if (l2.size() > 100) {

                for (int i = 1; i < 100; i++) {
                    System.out.println((l2.get(l2.size() - i)) + "  " + (l3.get(l3.size() - i)));
                }
            } else {
                System.out.println("List is empty or smaller than 100");
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //unregisterReceiver(mBroadcastReceiver);
        //unregisterReceiver(bre);
        mydb.close();
    }

    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.

        //check build version on phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //check that app has permission to access data
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                createLocationRequest();

                //get information from location service
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mCurrentLocation != null) {

                    lat = mCurrentLocation.getLatitude();
                    lon = mCurrentLocation.getLongitude();
                    mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                    mTimeOfLastLocationEvent = mCurrentLocation.getTime();

                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);

                    ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                            mGoogleApiClient,
                            Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                            getActivityDetectionPendingIntent()
                    ).setResultCallback(this);

                    if (mBroadcastReceiver==null) {
                        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                                new IntentFilter(Constants.BROADCAST_ACTION));
                    }

                } else {
                    Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
                }

            }else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            }
        }
        else{
            createLocationRequest();
            //get information from location service
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null) {

                lat = mCurrentLocation.getLatitude();
                lon = mCurrentLocation.getLongitude();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                mTimeOfLastLocationEvent = mCurrentLocation.getTime();

                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);

                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                        mGoogleApiClient,
                        Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                        getActivityDetectionPendingIntent()
                ).setResultCallback(this);

                if (mBroadcastReceiver==null) {
                    LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                            new IntentFilter(Constants.BROADCAST_ACTION));
                }

            } else {
                Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
            }

        }

        //startLocationUpdates();

    }

    protected void createLocationRequest() {

        //remove location updates so that it resets
        //stopLocationUpdates();

        mLocationRequest = new LocationRequest();
        mLocationRequest.setSmallestDisplacement(10);
        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS); //UPDATE_INTERVAL_IN_MILLISECONDS

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS); //

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //restart location updates with the new interval
        startLocationUpdates();
    }

    protected void updateLocationRequest(int interval) {

        //remove location updates so that it resets
        stopLocationUpdates();

        //mLocationRequest = new LocationRequest();
        mLocationRequest.setSmallestDisplacement(10);
        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(interval);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(interval / 3);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        //restart location updates with the new interval
        startLocationUpdates();
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {
        if((location!=mCurrentLocation) || (location.getTime()-mTimeOfLastLocationEvent>=60000)) {
            mCurrentLocation = location;
            lat = mCurrentLocation.getLatitude();
            lon = mCurrentLocation.getLongitude();

            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            Calendar c = Calendar.getInstance();
            Date date = new Date();
            mCurrentTime = originalFormat.format(date);

            if (!activity_type.equals("Uninitialized")) {
                switch( activity_type ) {
                    case "In a vehicle": {
                        //ensure that we don't get a lot of events
                        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                        if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000){
                            //be sure to store the time of receiving this event !
                            mTimeOfLastLocationEvent = location.getTime();
                            mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                            marker.setPosition(new LatLng(lat,lon));
                        }
                        break;
                    }
                    case "On a bicycle": {
                        //ensure that we don't get a lot of events
                        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                        if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000){
                            //be sure to store the time of receiving this event !
                            mTimeOfLastLocationEvent = location.getTime();
                            mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                            marker.setPosition(new LatLng(lat, lon));
                        }
                        break;
                    }
                    case "On foot": {
                        //ensure that we don't get a lot of events
                        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                        if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000){
                            //be sure to store the time of receiving this event !
                            mTimeOfLastLocationEvent = location.getTime();
                            mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                            marker.setPosition(new LatLng(lat, lon));
                        }
                        break;
                    }
                    case "Running": {
                        //ensure that we don't get a lot of events
                        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                        if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000){
                            //be sure to store the time of receiving this event !
                            mTimeOfLastLocationEvent = location.getTime();
                            mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                            marker.setPosition(new LatLng(lat, lon));
                        }
                        break;
                    }
                    case "Still": {
                        //ensure that we don't get a lot of events
                        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                        if (mLastactivity_type.equals("Still")){
                            if ((location.getTime() - mTimeOfLastLocationEvent) >= 60000*30){
                                //be sure to store the time of receiving this event !
                                mTimeOfLastLocationEvent = location.getTime();
                                mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                                marker.setPosition(new LatLng(lat, lon));
                            }
                        }
                        else{
                            if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000) {
                                mTimeOfLastLocationEvent = location.getTime();
                                mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                                marker.setPosition(new LatLng(lat, lon));
                            }
                        }
                        break;
                    }
                    case "Tilting": {
                        //ensure that we don't get a lot of events
                        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                        if ((location.getTime() - mTimeOfLastLocationEvent) >= 60000*15){
                            //be sure to store the time of receiving this event !
                            mTimeOfLastLocationEvent = location.getTime();
                            mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                            marker.setPosition(new LatLng(lat, lon));
                        }
                        break;
                    }
                    case "Walking": {
                        //ensure that we don't get a lot of events
                        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                        if ((location.getTime() - mTimeOfLastLocationEvent) >= 20000){
                            //be sure to store the time of receiving this event !
                            mTimeOfLastLocationEvent = location.getTime();
                            mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                            marker.setPosition(new LatLng(lat, lon));
                        }
                        break;
                    }
                    case "Unknown activity": {
                        //ensure that we don't get a lot of events
                        // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                        if ((location.getTime() - mTimeOfLastLocationEvent) >= 30000){
                            //be sure to store the time of receiving this event !
                            mTimeOfLastLocationEvent = location.getTime();
                            mydb.insertTrackpoint(lat, lon, mCurrentTime, activity_type);
                            marker.setPosition(new LatLng(lat, lon));
                        }
                        break;
                    }
                }

                //locationlist.add(new LatLng(lat, lon));
                //JSONFileWrite();

            }
        }
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        //check build version on phone
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //check that app has permission to access data
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            }
            else {

                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);

            }

            }
        else{
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        // Refer to the javadoc for ConnectionResult to see what error codes might be returned in
        // onConnectionFailed.
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
        //Try to re-connect
        mGoogleApiClient.connect();
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    private void addHeatMap(GoogleMap map) {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        Gradient gradient = null;
        switch (mode){
            case "All": {
                if (time_mode.equals("All Time")) {
                    list = mydb.getAllPoints();
                } else if (time_mode.equals("Today")){
                    list = mydb.getAllPointsToday();
                } else if (time_mode.equals("Date Picked")){
                    list = mydb.getAllPointsDatePeriod(datepickedstart+" 00:00:00 πμ", datepickedend+" 23:59:59 μμ");
                }

                // Create the gradient.
                int[] colors_all = {
                        Color.rgb(102, 225, 0), // green
                        Color.rgb(255, 0, 0)    // red
                };

                float[] startPoints_all = {
                        0.2f, 1f
                };

                gradient = new Gradient(colors_all, startPoints_all);
                break;
            }
            case "Vehicle": {

                if (time_mode.equals("All Time")) {
                    list = mydb.getAllVehicles();
                } else if (time_mode.equals("Today")){
                    list = mydb.getAllVehiclesToday();
                } else if (time_mode.equals("Date Picked")){
                    list = mydb.getAllOnVehicleDatePeriod(datepickedstart + " 00:00:00 πμ", datepickedend + " 23:59:59 μμ");
                }

                // Create the gradient.
                int[] colors_vehicle = {
                        Color.rgb(190,60,125),
                        Color.rgb(86,16,80),
                        Color.rgb(53,1,63)
                };

                float[] startPoints_vehicle = {
                        0.2f, 0.8f, 1f
                };

                gradient = new Gradient(colors_vehicle, startPoints_vehicle);
                break;
            }
            case "Bike": {
                if (time_mode.equals("All Time")) {
                    list = mydb.getAllOnBike();
                } else if (time_mode.equals("Today")){
                    list = mydb.getAllOnBikeToday();
                }  else if (time_mode.equals("Date Picked")){
                    list = mydb.getAllOnBikeDatePeriod(datepickedstart + " 00:00:00 πμ", datepickedend + " 23:59:59 μμ");
                }
                // Create the gradient.
                int[] colors_bike = {
                        Color.rgb(58,163,193),
                        Color.rgb(40,57,150),
                        Color.rgb(38,9,128)
                };

                float[] startPoints_bike = {
                        0.2f, 0.8f, 1f
                };

                gradient = new Gradient(colors_bike, startPoints_bike);

                break;
            }
            case "Foot": {
                if (time_mode.equals("All Time")) {
                    list = mydb.getAllOnFoot();
                } else if (time_mode.equals("Today")){
                    list = mydb.getAllOnFootToday();
                } else if (time_mode.equals("Date Picked")){
                    list = mydb.getAllOnFootDatePeriod(datepickedstart + " 00:00:00 πμ", datepickedend + " 23:59:59 μμ");
                }
                // Create the gradient.
                int[] colors_foot = {
                        Color.rgb(239,172,42),
                        Color.rgb(245,130,125),
                        Color.rgb(250,70,60)
                };

                float[] startPoints_foot = {
                        0.2f, 0.6f, 1f
                };

                gradient = new Gradient(colors_foot, startPoints_foot);
                break;
            }
        }

        if (!list.isEmpty()) {
            // Create a heat map tile provider, passing it the latlngs of the recorded points.
            mProvider = new HeatmapTileProvider.Builder()
                    .data(list)
                    .build();
            if (gradient!=null){
                mProvider.setGradient(gradient);
            }
            // Add a tile overlay to the map, using the heat map tile provider.
            mOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
        }else{
            String text="You have no points recorded yet!";
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Receiver for intents sent by DetectedActivitiesIntentService via a sendBroadcast().
     * Receives a list of one or more DetectedActivity objects associated with the current state of
     * the device.
     */
    public class ActivityDetectionBroadcastReceiver extends BroadcastReceiver {
        protected static final String TAG = "activity-detection-response-receiver";

        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Received the intent");
            DetectedActivity updatedActivity;
            Bundle extras =
                    intent.getExtras();
            System.out.println("Received the intent with extras:" + extras);
            if(extras == null) {
                updatedActivity= null;
                mLastactivity_type=activity_type;
                activity_type=null;
            } else {
                updatedActivity= extras.getParcelable(Constants.ACTIVITY_EXTRA);
                int confidence = updatedActivity.getConfidence();
                if (confidence>60){
                    activity_type = Constants.getActivityString(context, updatedActivity.getType());
                    System.out.println(activity_type);
                    switch( updatedActivity.getType() ) {
                        case DetectedActivity.IN_VEHICLE: {
                            if (!mLastactivity_type.equals("In a vehicle")) {
                                /*NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                                builder.setContentText( "You are now in a vehicle." );
                                builder.setSmallIcon(R.mipmap.ic_launcher);
                                builder.setContentTitle(getString(R.string.app_name));
                                NotificationManagerCompat.from(context).notify(0, builder.build());*/
                                mLastactivity_type=activity_type;
                                updateLocationRequest(10000);
                            }
                            break;
                        }
                        case DetectedActivity.ON_BICYCLE: {
                            if (!mLastactivity_type.equals("On a bicycle")) {
                                /*NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                                builder.setContentText( "You are now biking." );
                                builder.setSmallIcon(R.mipmap.ic_launcher);
                                builder.setContentTitle(getString(R.string.app_name));
                                NotificationManagerCompat.from(context).notify(0, builder.build());*/
                                mLastactivity_type=activity_type;
                                updateLocationRequest(10000);
                            }
                            break;
                        }
                        case DetectedActivity.ON_FOOT: {
                            if (!mLastactivity_type.equals("On foot")) {
                                /*NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                                builder.setContentText( "You are now on foot." );
                                builder.setSmallIcon(R.mipmap.ic_launcher);
                                builder.setContentTitle(getString(R.string.app_name));
                                NotificationManagerCompat.from(context).notify(0, builder.build());*/
                                mLastactivity_type=activity_type;
                                updateLocationRequest(10000);
                            }
                            break;
                        }
                        case DetectedActivity.RUNNING: {
                            if (!mLastactivity_type.equals("Running")) {
                                /*NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                                builder.setContentText( "You are now running." );
                                builder.setSmallIcon(R.mipmap.ic_launcher);
                                builder.setContentTitle(getString(R.string.app_name));
                                NotificationManagerCompat.from(context).notify(0, builder.build());*/
                                mLastactivity_type=activity_type;
                                updateLocationRequest(10000);
                            }
                            break;
                        }
                        case DetectedActivity.STILL: {
                            if (mLastactivity_type.equals("Still")) {
                                updateLocationRequest(30000 * 30);
                            }else{
                                /*NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                                builder.setContentText( "Your phone is now Still" );
                                builder.setSmallIcon( R.mipmap.ic_launcher );
                                builder.setContentTitle( getString( R.string.app_name ) );
                                NotificationManagerCompat.from(context).notify(0, builder.build());*/
                                mLastactivity_type=activity_type;
                            }
                            break;
                        }
                        case DetectedActivity.TILTING: {
                            if (mLastactivity_type.equals("Tilting")) {
                                updateLocationRequest(30000 * 30);
                            } else{
                                mLastactivity_type=activity_type;
                            }
                            break;
                        }
                        case DetectedActivity.WALKING: {
                            if (!mLastactivity_type.equals("Walking")) {
                                /*NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
                                builder.setContentText( "You are now walking." );
                                builder.setSmallIcon(R.mipmap.ic_launcher);
                                builder.setContentTitle(getString(R.string.app_name));
                                NotificationManagerCompat.from(context).notify(0, builder.build());*/
                                mLastactivity_type=activity_type;
                                updateLocationRequest(20000);
                            }
                            break;
                        }
                        case DetectedActivity.UNKNOWN: {
                            mLastactivity_type=activity_type;
                            updateLocationRequest(20000);
                            break;
                        }
                    }
                }
                else{
                    mLastactivity_type=activity_type;
                    activity_type="Unknown activity";
                    updateLocationRequest(10000);
                }
            }
        }
    }

    /**
     * Runs when the result of calling requestActivityUpdates() and removeActivityUpdates() becomes
     * available. Either method can complete successfully or with an error.
     *
     * @param status The Status returned through a PendingIntent when requestActivityUpdates()
     *               or removeActivityUpdates() are called.
     */
    public void onResult(Status status) {
        if (status.isSuccess()) {
            System.out.println("SuCcESS");
        } else {
            Log.e(TAG, "Error adding or removing activity detection: " + status.getStatusMessage());
        }
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Intent intent = new Intent(this, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //For the Drawing Slider

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_slider, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        String text = "";
        if (id == R.id.nav_all) {

            MenuItem selected = navigationView.getMenu().getItem(2);
            selected.setTitle("Choose a time period");
            selected_dates="Choose a time period";

            ArrayList<LatLng> l = mydb.getAllPoints();

            if (!l.isEmpty()) {

                // Create the gradient.
                int[] colors_all = {
                        Color.rgb(102, 225, 0), // green
                        Color.rgb(255, 0, 0)    // red
                };

                float[] startPoints_all = {
                        0.2f, 1f
                };

                Gradient gradient_all = new Gradient(colors_all, startPoints_all);

                mProvider.setGradient(gradient_all);
                mProvider.setData(l);
                mOverlay.clearTileCache();
                mode="All";
                time_mode="All Time";
                mode_textview.setText(time_mode);
                mode_textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_calendar_multiple, 0, 0, 0);

            }else{
                text=text+"You have no points recorded yet!";
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }

        } else if (id == R.id.nav_today) {

            MenuItem selected = navigationView.getMenu().getItem(2);
            selected.setTitle("Choose a time period");
            selected_dates="Choose a time period";

            ArrayList<LatLng> l = mydb.getAllPointsToday();

            if (!l.isEmpty()) {

                // Create the gradient.
                int[] colors_all = {
                        Color.rgb(102, 225, 0), // green
                        Color.rgb(255, 0, 0)    // red
                };

                float[] startPoints_all = {
                        0.2f, 1f
                };

                Gradient gradient_all = new Gradient(colors_all, startPoints_all);

                mProvider.setGradient(gradient_all);

                mProvider.setData(l);
                mOverlay.clearTileCache();
                mode="All";
                time_mode="Today";
                mode_textview.setText(time_mode);
                mode_textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_calendar_today, 0, 0, 0);

            }else{
                text=text+"You have no points recorded yet!";
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
            }


        } else if (id == R.id.nav_picker) {
            Calendar now = Calendar.getInstance();
            DatePickerDialog dpd = com.borax12.materialdaterangepicker.date.DatePickerDialog.newInstance(
                    MainActivity.this,
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH)
            );
            dpd.setAccentColor(R.attr.colorPrimary);
            final MenuItem custom_item=item;
            dpd.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    if(time_mode.equals("All Time")){
                        MenuItem selected = navigationView.getMenu().getItem(0);
                        selected.setChecked(true);
                        custom_item.setChecked(false);
                    } else if(time_mode.equals("Today")){
                        MenuItem selected = navigationView.getMenu().getItem(1);
                        selected.setChecked(true);
                        custom_item.setChecked(false);
                    }
                }
            });
            dpd.show(getFragmentManager(), "Datepickerdialog");
            selected_item=item.getItemId();

        }/*else if (id == R.id.nav_3_days) {
            System.out.println("3 Days");
        } else if (id == R.id.nav_week) {

        } else if (id == R.id.nav_month) {

        } else if (id == R.id.nav_search) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {
            System.out.println("send");
        }*/

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDateSet(DatePickerDialog view, int year, int monthOfYear, int dayOfMonth,int yearEnd, int monthOfYearEnd, int dayOfMonthEnd) {
        String date = "You picked the following date: From- " + dayOfMonth + "/" + (++monthOfYear) + "/" + year + " To " + dayOfMonthEnd + "/" + (++monthOfYearEnd) + "/" + yearEnd;

        datepickedstart=""+dayOfMonth+"/"+(monthOfYear)+"/"+year+"";
        datepickedend=""+dayOfMonthEnd+"/"+(monthOfYearEnd)+"/"+yearEnd+"";
        try {

            DateFormat originalFormat = new SimpleDateFormat("dd/MM/yyyy");
            Date date1 = originalFormat.parse(datepickedstart);
            Date date2 = originalFormat.parse(datepickedend);

            Date current = new Date();

            MenuItem selected = navigationView.getMenu().getItem(2);
            ArrayList<LatLng> list_all;
            list_all = mydb.getAllPointsDatePeriod(datepickedstart + " 00:00:00 πμ", datepickedend + " 23:59:59 μμ");
            String text = "";
            if ((!list_all.isEmpty()) && (!datepickedstart.equals("") && !datepickedend.equals("")) && (date2.after(date1))
                    && !(date1.after(current)) && !(date2.after(current))) {

                selected_dates = datepickedstart + " - " + datepickedend;
                selected.setTitle(selected_dates);
                time_mode = "Date Picked";
                mode_textview.setText(selected_dates);
                mode_textview.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_calendar_text, 0, 0, 0);

                mode = "All";

                // Create the gradient.
                int[] colors_all = {
                    Color.rgb(102, 225, 0), // green
                    Color.rgb(255, 0, 0)    // red
                };

                float[] startPoints_all = {
                    0.2f, 1f
                };

                Gradient gradient_all = new Gradient(colors_all, startPoints_all);

                mProvider.setGradient(gradient_all);
                mProvider.setData(list_all);
                mOverlay.clearTileCache();
                mode = "All";

         } else {
            //Check if days are valid and inform accordingly
            if ((datepickedstart.equals("") | datepickedend.equals("")) | (date1.after(date2))
                    | (date1.after(current)) | (date2.after(current))){
                text = text + "Wrong Input Dates!";

            }else{
                //else it means the database was empty for these dates
                text = text + "You have no points recorded in this period!";
            }
         }

        if (!text.equals("")) {
            if (time_mode.equals("All Time")) {
                MenuItem selected_item = navigationView.getMenu().getItem(0);
                selected_item.setChecked(true);
                selected.setChecked(false);
            } else if (time_mode.equals("Today")) {
                MenuItem selected_item = navigationView.getMenu().getItem(1);
                selected_item.setChecked(true);
                selected.setChecked(false);
            }
            Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
        }catch (ParseException e){
            System.out.println(e);
        }

    }

    //BroadcastReceiver that catches the ACTION_SCREEN_ON broadcast action
    public class DozeBroadcastReceiver extends BroadcastReceiver {
        @TargetApi(23)
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.e(TAG, intent.toString());
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (Build.VERSION.SDK_INT >= (Build.VERSION_CODES.M)) {
                if (pm.isDeviceIdleMode()) {
                    System.out.println("Doze ON!");
                    Log.e(TAG, "Device on Doze Mode");
                } else {
                    System.out.println("Doze OFF!");
                    Log.d(TAG, "Device on Active Mode");

                    // we resume receiving location updates
                    if (!mGoogleApiClient.isConnected()) {
                        System.out.println("I am not connected!");
                        mGoogleApiClient.connect();
                        // Register the broadcast receiver that informs this activity of the DetectedActivity
                        // object broadcast sent by the intent service.
                        if (mBroadcastReceiver==null) {
                            LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                                    new IntentFilter(Constants.BROADCAST_ACTION));
                        }
                    }

                }
            }
        }
    }

    //BroadcastReceiver that catches the ACTION_SCREEN_ON broadcast action
    public class ScreenBroadcaster extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {

                System.out.println("Screen was turned ON!");

                if (mBroadcastReceiver==null) {
                    LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                            new IntentFilter(Constants.BROADCAST_ACTION));
                }

                // we resume receiving location updates
                if (!mGoogleApiClient.isConnected()) {
                    System.out.println("I am not connected!");
                    mGoogleApiClient.connect();
                    // Register the broadcast receiver that informs this activity of the DetectedActivity
                    // object broadcast sent by the intent service.
                    if (mBroadcastReceiver==null) {
                        LocalBroadcastManager.getInstance(context).registerReceiver(mBroadcastReceiver,
                                new IntentFilter(Constants.BROADCAST_ACTION));
                    }
                }

            }
        }
    }

}