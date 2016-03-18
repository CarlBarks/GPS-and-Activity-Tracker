package com.example.stef.gpstracker;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.Build;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;

import android.support.v4.app.FragmentActivity;
import android.location.Location;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Scanner;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.DetectedActivity;

public class MainActivity extends FragmentActivity implements
        ConnectionCallbacks, OnConnectionFailedListener, LocationListener, OnMapReadyCallback, ResultCallback<Status>{

    protected static final String TAG = "MainActivity";

    private GoogleMap mMap;

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
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /* Stores parameters for requests to the FusedLocationProviderApi.*/
    protected LocationRequest mLocationRequest;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        setContentView(R.layout.activity_maps);

        updateValuesFromBundle(savedInstanceState);

        mydb = new DBHelper(this);

        // Get a receiver for broadcasts from ActivityDetectionIntentService.
        mBroadcastReceiver = new ActivityDetectionBroadcastReceiver();

        buildGoogleApiClient();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker and move the camera.
        LatLng currentmarker = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        mMap.addMarker(new MarkerOptions().position(currentmarker).title("Current Marker"));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentmarker, 12));
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
        savedInstanceState.putLong(LAST_UPDATED_TIME_STRING_KEY, mCurrentLocation.getTime());
        savedInstanceState.putString(Constants.DETECTED_ACTIVITIES, activity_type);
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
                // mCurrentLocationis not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mTimeOfLastLocationEvent = savedInstanceState.getLong(
                        LAST_UPDATED_TIME_STRING_KEY);
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
        // we resume receiving location updates
        if (mGoogleApiClient.isConnected()) {
            // Register the broadcast receiver that informs this activity of the DetectedActivity
            // object broadcast sent by the intent service.
            LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                    new IntentFilter(Constants.BROADCAST_ACTION));
            startLocationUpdates();
        }
        else{
            mGoogleApiClient.connect();
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

        //ArrayList<LatLng> l = mydb.getAllPoints();
        //String s = DatabaseUtils.dumpCursorToString(cursor);
        //System.out.println(l);

        ArrayList<String> l2 = mydb.getAllPointsActivity();
        ArrayList<String> l3 = mydb.getAllPointsTime();
        if (l2.size() > 1) {

            for(int i=1; i<100; i++){
                System.out.println((l2.get(l2.size() - i)) + "  " +(l3.get(l3.size() - i)));
            }
        } else {
            System.out.println("List is empty or bigger than one");
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
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
                    //locationlist.add(new LatLng(lat, lon));

                    //Don't save the last known location. It has already been saved!
                    //mydb.insertTrackpoint(lat, lon, mLastUpdateTime.toString(), activity_type);

                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);

                    ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                            mGoogleApiClient,
                            Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                            getActivityDetectionPendingIntent()
                    ).setResultCallback(this);

                } else {
                    Toast.makeText(this, R.string.no_location_detected, Toast.LENGTH_LONG).show();
                }

            }
        }
        else{
            createLocationRequest();
            //get information from location service
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mCurrentLocation != null) {

                //add code here
                lat = mCurrentLocation.getLatitude();
                lon = mCurrentLocation.getLongitude();
                mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                mTimeOfLastLocationEvent = mCurrentLocation.getTime();
                //locationlist.add(new LatLng(lat, lon));
                //JSONFileWrite();

                //Don't save the last known location. It has already been saved!
                //mydb.insertTrackpoint(lat, lon, mLastUpdateTime.toString(), activity_type);

                SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                        .findFragmentById(R.id.map);
                mapFragment.getMapAsync(this);

                ActivityRecognition.ActivityRecognitionApi.requestActivityUpdates(
                        mGoogleApiClient,
                        Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                        getActivityDetectionPendingIntent()
                ).setResultCallback(this);

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
        mLocationRequest.setFastestInterval(interval / 2);

        //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

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
            mCurrentTime = DateFormat.getDateTimeInstance().format(new Date());

        if (!activity_type.equals("Uninitialized")) {
            switch( activity_type ) {
                case "In a vehicle": {
                    //ensure that we don't get a lot of events
                    // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                    if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000){
                        //be sure to store the time of receiving this event !
                        mTimeOfLastLocationEvent = location.getTime();
                        mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
                    }
                    break;
                }
                case "On a bicycle": {
                    //ensure that we don't get a lot of events
                    // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                    if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000){
                        //be sure to store the time of receiving this event !
                        mTimeOfLastLocationEvent = location.getTime();
                        mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
                    }
                    break;
                }
                case "On foot": {
                    //ensure that we don't get a lot of events
                    // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                    if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000){
                        //be sure to store the time of receiving this event !
                        mTimeOfLastLocationEvent = location.getTime();
                        mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
                    }
                    break;
                }
                case "Running": {
                    //ensure that we don't get a lot of events
                    // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                    if ((location.getTime() - mTimeOfLastLocationEvent) >= 10000){
                        //be sure to store the time of receiving this event !
                        mTimeOfLastLocationEvent = location.getTime();
                        mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
                    }
                    break;
                }
                case "Still": {
                    System.out.println("MPHKA STO STILL LEME!");
                    //ensure that we don't get a lot of events
                    // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                    System.out.println(location.getTime()-mTimeOfLastLocationEvent);
                    System.out.println("last activity = " + mLastactivity_type);
                    System.out.println(mLastactivity_type.equals("Still"));
                    if (mLastactivity_type.equals("Still")){
                        if ((location.getTime() - mTimeOfLastLocationEvent) >= 60000*30){
                            System.out.println("MPHKA PIO MESA!");
                            System.out.println(mTimeOfLastLocationEvent);
                            System.out.println(location.getTime());
                            //be sure to store the time of receiving this event !
                            mTimeOfLastLocationEvent = location.getTime();
                            mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
                        }
                    }
                    else{
                        mTimeOfLastLocationEvent = location.getTime();
                        mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
                    }
                    break;
                }
                case "Tilting": {
                    //ensure that we don't get a lot of events
                    // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                    if ((location.getTime() - mTimeOfLastLocationEvent) >= 60000*15){
                        //be sure to store the time of receiving this event !
                        mTimeOfLastLocationEvent = location.getTime();
                        mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
                    }
                    break;
                }
                case "Walking": {
                    //ensure that we don't get a lot of events
                    // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                    if ((location.getTime() - mTimeOfLastLocationEvent) >= 30000){
                        //be sure to store the time of receiving this event !
                        mTimeOfLastLocationEvent = location.getTime();
                        mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
                    }
                    break;
                }
                case "Unknown activity": {
                    //ensure that we don't get a lot of events
                    // or if enabled, only get more accurate events within mTimeBetweenLocationEvents
                    if ((location.getTime() - mTimeOfLastLocationEvent) >= 30000){
                        //be sure to store the time of receiving this event !
                        mTimeOfLastLocationEvent = location.getTime();
                        mydb.insertTrackpoint(lat, lon, mCurrentTime.toString(), activity_type);
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
    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    private void addHeatMap(GoogleMap map) {

        ArrayList<LatLng> list = mydb.getAllPoints();
        // Create a heat map tile provider, passing it the latlngs of the police stations.
        HeatmapTileProvider mProvider = new HeatmapTileProvider.Builder()
                .data(list)
                .build();
        // Add a tile overlay to the map, using the heat map tile provider.
        TileOverlay mOverlay = map.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
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

            DetectedActivity updatedActivity;
            Bundle extras =
                    intent.getExtras();
            if(extras == null) {
                updatedActivity= null;
                mLastactivity_type=activity_type;
                activity_type=null;
            } else {
                updatedActivity= extras.getParcelable(Constants.ACTIVITY_EXTRA);
                int confidence = updatedActivity.getConfidence();
                if (confidence>50){
                    mLastactivity_type=activity_type;
                    activity_type = Constants.getActivityString(context, updatedActivity.getType());
                    System.out.println(activity_type);
                    switch( updatedActivity.getType() ) {
                        case DetectedActivity.IN_VEHICLE: {
                            updateLocationRequest(10000);
                            break;
                        }
                        case DetectedActivity.ON_BICYCLE: {
                            updateLocationRequest(10000);
                            break;
                        }
                        case DetectedActivity.ON_FOOT: {
                            updateLocationRequest(10000);
                            break;
                        }
                        case DetectedActivity.RUNNING: {
                            updateLocationRequest(10000);
                            break;
                        }
                        case DetectedActivity.STILL: {
                            updateLocationRequest(30000 * 30);
                            break;
                        }
                        case DetectedActivity.TILTING: {
                            updateLocationRequest(30000 * 30);
                            break;
                        }
                        case DetectedActivity.WALKING: {
                            updateLocationRequest(30000);
                            break;
                        }
                        case DetectedActivity.UNKNOWN: {
                            updateLocationRequest(30000);
                            break;
                        }
                    }
                }
                else{
                    mLastactivity_type=activity_type;
                    activity_type="Unknown activity";
                    updateLocationRequest(30000);
                }
            }

            //updateDetectedActivitiesList(updatedActivities);
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
            //
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

    /*public void JSONFileWrite() {

        JSONObject obj = new JSONObject();
        try {
            obj.put("lat", lat);
            obj.put("lon", lon);
            obj.put("time",mLastUpdateTime);

            JSONArray trackpoints = new JSONArray();
            trackpoints.put(obj);

            String s = trackpoints.toString();
            WriteToFile(s);
        }
        catch (JSONException e) {
            //
        }
    }*/

    /*private ArrayList<LatLng> readItems(int resource) throws JSONException {
        ArrayList<LatLng> list = new ArrayList<LatLng>();
        InputStream inputStream = getResources().openRawResource(resource);
        String json = new Scanner(inputStream).useDelimiter("\\A").next();
        JSONArray array = new JSONArray(json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject object = array.getJSONObject(i);
            double lat = object.getDouble("lat");
            double lng = object.getDouble("lng");
            list.add(new LatLng(lat, lng));
        }
        return list;

    }*/

    /* Checks if external storage is available for read and write */
    /*public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }*/

    /*Method responsible for writing data to .txt file*/
    /*public void WriteToFile(String s) {
        if (isExternalStorageWritable()) {
            try {
                File newFolder = new File(Environment.getExternalStorageDirectory(), "GPSTracker");
                if (!newFolder.exists()) {
                    newFolder.mkdir();
                }
                try {
                    File file = new File(newFolder, "Data" + ".json");
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    try {
                        FileOutputStream fOut = new FileOutputStream(file, true);
                        OutputStreamWriter myOutWriter =
                                new OutputStreamWriter(fOut);
                        myOutWriter.append(s);
                        myOutWriter.close();
                        fOut.close();
                    } catch (Exception ex) {
                        System.out.println("ex: " + ex);
                    }

                } catch (Exception e) {
                    System.out.println("e: " + e);
                }
            } catch (Exception e) {
                System.out.println("e: " + e);
            }

        }
    }*/

}

