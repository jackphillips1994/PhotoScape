package com.photoscape.photoscape;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.location.FusedLocationProviderClient;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseAuth authCheck;
    private static int REQUEST_CODE = 102;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private Boolean mLocationPermissionsGranted = false;
    PlaceAutocompleteFragment placeAutoComplete;

    // Variables to handle the fragements
    public static Boolean isFragmentDisplayed = false;

    // Setup Firebase DB
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = database.getReference("PhotoScape");

    // Setting up Firebase login providers
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Check if a user is already logged in
        authCheck = FirebaseAuth.getInstance();
        FirebaseUser currentUser = authCheck.getCurrentUser();
        if (currentUser == null) {
            authenticateUser();
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Handling the sign out button
        Button signOutButton = (Button) findViewById(R.id.signOutButton1);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Sign user out
                signOut();
            }
        });

        // Setting up the mylocation
        getLocationPermission();


        placeAutoComplete = (PlaceAutocompleteFragment) getFragmentManager().findFragmentById(R.id.place_autocomplete);
        placeAutoComplete.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.d("Maps", "Place selected: " + place.getName());
            }

            @Override
            public void onError(Status status) {
                Log.d("Maps", "An error occurred: " + status);
            }
        });
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            Log.d("LOCATION_STATUS", "App has correct permissions to access devices current location");
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
        }
        else{
            Log.d("LOCATION_STATUS", "Was unable to get permissions to get current location");
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Check to see if fragment is open and if so close it
                if(isFragmentDisplayed){
                    closeCreatePinFragment();
                }else {
                    // Call marker creation method
                    setMapMarker(latLng);
                }
            }
        });
    }

    // Authenticate the user by calling the Firebase intent
    private void authenticateUser() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                    .setAvailableProviders(providers)
                    .build(),REQUEST_CODE);
    }

    // Handle the return value from the AuthUI intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == REQUEST_CODE) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Logged in
            }
            else if (response == null) {
                // user cancelled signin

            }
            else if (response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                // Device had no network connection
                return;
            }
            else if (response.getError().getErrorCode() == ErrorCodes.UNKNOWN_ERROR) {
                // Unknown error occured
                return;
            }
        }
    }

    private void signOut() {
        AuthUI.getInstance()
                .signOut(this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {}});
    }

    private void getDeviceLocation() {
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            final Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        //Location has been found
                        Log.d("LOCATION_STATUS", "Getting current location");
                        Location currentLocation = (Location) task.getResult();
                        Log.d("LOCATION_STATUS", "Current location: lat:" + currentLocation.getLatitude() + "long:" + currentLocation.getLatitude());
                        moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()),
                                DEFAULT_ZOOM);
                    }else {
                        //Unable to get current location
                        Log.d("LOCATION_STATUS", "Unable to get current location");
                        Toast.makeText(MapsActivity.this, "Unable to get current location", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }catch (SecurityException e){

        }
    }

    private void getLocationPermission(){
        Log.d("LOCATION_STATUS", "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermissionsGranted = true;
            }else{
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        }else{
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void moveCamera (LatLng latLng, float zoom){
        // Moving the camera to the provided location
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    // Method to handle map marker creation
    private void setMapMarker(LatLng latLng){

        // Get pin timestamp
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("AEST"));
        Date date = new Date();
        String pinCreationTime = dateFormat.format(date).toString();

        displayCreatePinFragment();
        isFragmentDisplayed = true;

        // Creating the marker
        MarkerOptions markerOptions = new MarkerOptions();

        // Setting the marker position
        markerOptions.position(latLng);

        // Setting the title for the marker
        markerOptions.title("New Marker");

        // Clears the markers
        //mMap.clear();

        // Moves the camera to the new position
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));

        // Add Marker to the Map
        mMap.addMarker(markerOptions);
        saveToFirebase(latLng,pinCreationTime);
    }

    public void onPause() {
        super.onPause();
    }

    // Method to handle instantiating fragment
    public void displayCreatePinFragment() {
        CreatePin createPin = new CreatePin();
        // Get the FragementManager and start a transaction
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        // Add the SimpleFragment
        fragmentTransaction.add(R.id.fragment_container,
                createPin).addToBackStack(null).commit();
        isFragmentDisplayed = true;
    }

    // Method to handle closing the fragment
    public void closeCreatePinFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        // Check to see if the fragment is already showing
        CreatePin createPin = (CreatePin) fragmentManager
                .findFragmentById(R.id.fragment_container);
        if(createPin != null) {
            // Commit and close the transaction to close the fragment
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(createPin).commit();
        }
        isFragmentDisplayed = false;
    }

    // Method to handle saving to the DB
    private void saveToFirebase(LatLng mCurrentLocation, String pinCreationTime) {
        Map mLocations = new HashMap();
        mLocations.put("CreationTime", pinCreationTime);
        Map mCoordinate = new HashMap();
        mCoordinate.put("latitude", mCurrentLocation.latitude);
        mCoordinate.put("longitude", mCurrentLocation.longitude);
        mLocations.put("location", mCoordinate);
        dbRef.push().setValue(mLocations);
    }
}

