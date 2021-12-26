package com.example.heliumproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;


import com.google.android.gms.common.util.concurrent.HandlerExecutor;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.lang.reflect.Array;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SensorEventListener
{

    private static final String TAG = "MainActivity";

    public static final int DEFAULT_UPDATE_INTERVAL = 1;
    private static final int PERMISSION_FINE_LOCATIONS = 99;
    static final int REQUEST_VIDEO_CAPTURE = 1;

    TextView tv_lat, tv_lon, tv_altitude, tv_accuracy, tv_speed, tv_sensor, tv_updates, tv_address, tv_acceleration, tv_gravity;
    Switch sw_gps, sw_locationupdates;
    VideoView videoView;

    // Location request
    LocationRequest locationRequest;


    // Google's API for location services
    FusedLocationProviderClient fusedLocationProviderClient;

    // Location call back function to know when to call the location update function
    LocationCallback locationCallBack;

    // Sensor initiation
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];

    // Declaring for firebase

    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // instance of firebase database.
        firebaseDatabase = FirebaseDatabase.getInstance();
        // below line is used to get reference for our database.
        databaseReference = FirebaseDatabase.getInstance("https://heliumproject-e05a4-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        // TEXT Views --------------------------
        tv_accuracy = findViewById(R.id.tv_accuracy);
        tv_lat = findViewById(R.id.tv_lat);
        tv_lon = findViewById(R.id.tv_lon);
        tv_altitude = findViewById(R.id.tv_altitude);
        tv_speed = findViewById(R.id.tv_speed);
        tv_sensor = findViewById(R.id.tv_sensor);
        tv_updates = findViewById(R.id.tv_updates);
        tv_address = findViewById(R.id.tv_address);
        tv_acceleration = findViewById(R.id.tv_acceleration);
        tv_gravity = findViewById(R.id.tv_gravity);

        // SWITCH _________________________________________
        sw_gps = findViewById(R.id.sw_gps);
        sw_locationupdates = findViewById(R.id.sw_locationsupdates);


        // set all properties of the Location request
        Log.d(TAG, "onCreate: Created a location request");
        locationRequest = LocationRequest.create()
                .setInterval(100 * DEFAULT_UPDATE_INTERVAL)
                .setFastestInterval(10 * DEFAULT_UPDATE_INTERVAL)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setMaxWaitTime(100);

        // Event is called when the location interval is met
        locationCallBack = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                // save the location
                Location location = locationResult.getLastLocation();

                updateUIValues(location);
            }
        };


        //Event listener to change the way to get gps
        sw_gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sw_gps.isChecked()) {
                    locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                    tv_sensor.setText("Using GPS sensors");
                } else {
                    locationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
                    tv_sensor.setText("Using Towers + WIFI");
                }
            }
        });


        //Event listener to activate/deactivate the location updates
        sw_locationupdates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sw_locationupdates.isChecked()) {
                    // turn on tracking
                    startLocationUpdates();
                } else {
                    //turn off tracking
                    stopLocationUpdates();
                }
            }
        });

        updateGPS();
        Log.d(TAG, "onCreate: Initializing Sensor Services");
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        Log.d(TAG, "onCreate: Registered Sensor");


    }// End of on create


    private void stopLocationUpdates() {
        tv_updates.setText("The location is not being tracked");
        fusedLocationProviderClient.removeLocationUpdates(locationCallBack);
        tv_lat.append(" (not updating)");
        tv_lon.append(" (not updating)");
        tv_altitude.append(" (not updating)");
        tv_speed.append(" (not updating)");
        tv_accuracy.append(" (not updating)");

    }


    private void startLocationUpdates() {
        tv_updates.setText("The location is being tracked");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},PackageManager.PERMISSION_GRANTED);
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallBack, null);
        updateGPS();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case PERMISSION_FINE_LOCATIONS:
                if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    updateGPS();
                }
                else{
                    Toast.makeText(this,"This app requires permission to function",Toast.LENGTH_LONG).show();
                    finish();
                }

        }
    }

    private void updateGPS(){
        // Get the permissions to track the gps
        // Get the current location from the client

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(MainActivity.this);

        if(ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION ) == PackageManager.PERMISSION_GRANTED){
            //User provided the persmission
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(@NonNull Location location) {
                    // we got permissions. Put the values of the location. XXX in to the UI components.

                    updateUIValues(location);

                }
            });

        }
        else{
            //Persmission not granted
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                requestPermissions(new String[] { Manifest.permission.ACCESS_FINE_LOCATION},PERMISSION_FINE_LOCATIONS);
            }
        }
    }

    private void updateUIValues(Location location) {
        // update all the elements in the UI
        String latitude = String.valueOf (location.getLatitude());
        String longitude = String.valueOf(location.getLongitude());
        String altitude  = String.valueOf(location.getAltitude());
        float speed;
        Log.d(TAG,"&&&&&&&&&&&&&&Lat: "+location.getLatitude()+" Long: "+location.getLongitude()+" Alt: "+location.getAltitude()+"Speed: "+location.getSpeed());
        tv_lat.setText(latitude);
        Log.d(TAG, "updateUIValues: "+latitude+"!!!!!!!!!!!!");
        tv_lon.setText(longitude);
        tv_accuracy.setText(String.valueOf(location.getAccuracy()));

        if(location.hasAltitude()){
            tv_altitude.setText(altitude);
        }
        else{
            tv_altitude.setText("Not Available");
        }
        if(location.hasSpeed()){
            tv_speed.setText(String.valueOf(location.getSpeed()));
        }
        else{
            tv_speed.setText("Not Available");
        }

        Geocoder geocoder = new Geocoder(this);
        try{
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(),location.getLongitude(),1);
            tv_address.setText(addresses.get(0).getAddressLine(0));

        }

        catch (Exception e){
            tv_address.setText("Unable to get the street address");
        }
        try{
            if(location!=null) {

                addDatatoFirebase(latitude, longitude, altitude, location.getSpeed(), geocoder.getFromLocation(location.getLatitude(),location.getLongitude(), 1).get(0).getAddressLine(0), gravity, linear_acceleration);
                Log.d(TAG, "updateUIValues: This works!!!!!!!!!!!!!!!!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "updateUIValues: This Failed!!!!!!!!!!!!!!!!!!");
        }


    }

    private void addDatatoFirebase(String latitude, String longitude, String altitude, float speed, String addressLine, float[] gravity, float[] linear_acceleration)
    {
        databaseReference.child("Latitude").child("trial").setValue(latitude);
        databaseReference.child("Longitude").child("trial").setValue(longitude);
        databaseReference.child("Altitude").child("trial").setValue(altitude);
        databaseReference.child("Speed").child("trial").setValue(speed);
        databaseReference.child("AddressLine").child("trial").setValue(addressLine);
        databaseReference.child("Gravity").child("X").setValue(gravity[0]);
        databaseReference.child("Gravity").child("Y").setValue(gravity[1]);
        databaseReference.child("Gravity").child("Z").setValue(gravity[2]);
        databaseReference.child("linearAcceleration").child("X").setValue(linear_acceleration[0]);
        databaseReference.child("linearAcceleration").child("Y").setValue(linear_acceleration[1]);
        databaseReference.child("linearAcceleration").child("Z").setValue(linear_acceleration[2]);

    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];
        tv_gravity.setText("X: "+gravity[0]+"m/s^2"+" Y: "+gravity[1]+"m/s^2"+" Z:"+gravity[2]+"m/s^2" );
        Log.d(TAG,"onSensorChanged: X: "+gravity[0]+"m/s^2"+"Y: "+gravity[1]+"m/s^2"+"Z:"+gravity[2]+"m/s^2 ");
        tv_acceleration.setText("X: "+linear_acceleration[0]+"m/s^2"+" Y: "+linear_acceleration[1]+"m/s^2"+" Z:"+linear_acceleration[2]+"m/s^2");
        Log.d(TAG,"onSensorChanged: X: "+linear_acceleration[0]+"m/s^2"+"Y: "+linear_acceleration[1]+"m/s^2"+"Z:"+linear_acceleration[2]+"m/s^2");
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}

