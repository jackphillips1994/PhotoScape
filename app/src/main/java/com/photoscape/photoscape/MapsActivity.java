package com.photoscape.photoscape;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.location.FusedLocationProviderClient;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

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
    public LocationManager locationManager;
    public Criteria criteria;
    public String bestProvider;

    // Variables to handle the saving of the map state
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";
    private Location previousCurrentLocation;
    private CameraPosition previousCameraPosition;
    private Location mLastKnownLocation;

    // Variables to handle the fragements
    public static Boolean isCreatePinFragmentDisplayed = false;
    public static Boolean isAccountPinFragmentDisplayed = false;
    public static Boolean isCreatePinFragmentWasDisplayed = false;
    public static Boolean isReviewPinFragmentDisplayed = false;

    // Setting up Firebase login providers
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());

    // Marker options for the create pin marker once it has come back from the fragment
    MarkerOptions createPinMarkerOptions = new MarkerOptions();
    String markerCreationID;

    // Variables for db
    private FirebaseDatabase database = FirebaseDatabase.getInstance();

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

        if(savedInstanceState != null){
            previousCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            previousCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }
        // Setting up the mylocation
        getLocationPermission();

        // Setup search bar
        setupPlacesSearch();

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        Log.d("NAV_BAR", item.getTitle().toString());
                        switch (item.getTitle().toString()) {
                            case "My Pins":
                                Log.d("NAV_BAR", "My Pins clicked");
                                FirebaseUser currentUser = authCheck.getCurrentUser();
                                String emailAddress = currentUser.getEmail();
                                Log.d("NAV_BAR", "Email: " + emailAddress);
                                Intent intent = new Intent(getApplicationContext(), MyPinsActivity.class);
                                intent.putExtra("EMAIL_ADDRESS", emailAddress);
                                startActivity(intent);
                                break;
                            case "Account":
                                Log.d("NAV_BAR", "Account clicked");
                                if(isAccountPinFragmentDisplayed){
                                    closeAccountFragment();
                                } else {
                                    displayAccountFragment();
                                }
                                break;
                        }
                        return true;
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

            if (previousCurrentLocation != null) {
                moveCamera(new LatLng(previousCurrentLocation.getLatitude(), previousCurrentLocation.getLongitude()),
                        DEFAULT_ZOOM);
            } else {

            }
            //getDeviceLocationWithManager();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mMap.setMyLocationEnabled(true);
        } else {
            Log.d("LOCATION_STATUS", "Was unable to get permissions to get current location");
        }

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Check to see if fragment is open and if so close it
                if (isCreatePinFragmentDisplayed) {
                    closeCreatePinFragment();
                } else if (!isAccountPinFragmentDisplayed && !isReviewPinFragmentDisplayed && !isCreatePinFragmentDisplayed) {
                    // Call marker creation method
                    displayCreatePinFragment(latLng);
                    //setMapMarker(latLng, "New Marker Title");
                } else {
                    closeAccountFragment();
                    closeReviewPinFragment();
                }
            }
        });
        if(isCreatePinFragmentWasDisplayed){
            Marker mMarker = mMap.addMarker(createPinMarkerOptions);
            mMarker.setTag(markerCreationID);
            isCreatePinFragmentWasDisplayed = false;
        }

        // Setup marker on click listener
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                displayReviewPinFragment(marker.getTag().toString());
                return false;
            }
        });

        // Download and display map markers
        downloadMarkerDetails();
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

    @Override
    protected void onResume() {
        super.onResume();
        getDeviceLocation();
        // Check to see if the create pin fragment was displayed to make sure that this is only run
        // on resume from the create pin fragment
        Log.d("INFO_RECEIVED", isCreatePinFragmentWasDisplayed.toString());
        if(isCreatePinFragmentWasDisplayed){
            //DETERMINE WHO STARTED THIS ACTIVITY
            final String sender=this.getIntent().getExtras().getString("SENDER_KEY");

            //IF ITS THE FRAGMENT THEN RECEIVE DATA
            if(sender != null) {
                this.receiveData();
                Toast.makeText(this, "Pin Created", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    private void receiveData() {
        //RECEIVE DATA VIA INTENT
        Intent intent = getIntent();
        String result = intent.getStringExtra("RESULT");
        Log.d("INFO_RECEIVED", result);
        if(result.equals("Save")){
            // Getting the info from the intent
            String markerID = intent.getStringExtra("MARKER_ID");
            String markerTitle = intent.getStringExtra("MARKER_TITLE");
            Double markerLat = intent.getDoubleExtra("LATITUDE",0);
            Double markerLong = intent.getDoubleExtra("LONGITUDE",0);
            LatLng markerLocation = new LatLng(markerLat, markerLong);
            Log.d("INFO_RECEIVED", "MarkerID: " + markerID + " MarkerTitle: " +
            markerTitle + " markerLat: " + markerLat + " markerLong: " + markerLong);

            // Creating the marker and setting the marker
            createPinMarkerOptions.position(markerLocation);
            Log.d("SETUP_MARKER", "Setting up marker");

            // Setting the title for the marker
            createPinMarkerOptions.title(markerTitle);
            markerCreationID = markerID;
        }
    }

  /** private void getDeviceLocationWithManager() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        bestProvider = String.valueOf(locationManager.getBestProvider(criteria, true));
        Location location = locationManager.getLastKnownLocation(bestProvider);
        if(location != null){
            Log.d("LOCATION_STATUS", "Current location: lat:" + location.getLatitude() + "long:" + location.getLatitude());
        }else {
            // Request location update
            locationManager.requestLocationUpdates(bestProvider, 1000, 0, );
        }
    } **/


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

    // Method to handle creating all markers at the opening of the map
    public void downloadMarkerDetails() {
        DatabaseReference dbRef = database.getReference("PhotoScape/");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String latitude = null;
                String longitude = null;
                String markerName = null;
                String markerID = null;
                for (DataSnapshot markerDetailHashName : dataSnapshot.getChildren()){
                    for (DataSnapshot markerDetailIDName : markerDetailHashName.getChildren()){
                        for ( DataSnapshot markerDetail : markerDetailIDName.getChildren()) {
                            Log.d("PRE_MARKER_DETAILS", "Value is: " + markerDetail.getKey());
                            if(markerDetail.getKey().equals("location")){
                                for ( DataSnapshot locationDetails : markerDetail.getChildren()){
                                    if(locationDetails.getKey().equals("latitude")){
                                        latitude = locationDetails.getValue().toString();
                                        Log.d("PRE_MARKER_DETAILS", "Latitude: " + latitude);
                                    } else if ( locationDetails.getKey().equals("longitude")){
                                        longitude = locationDetails.getValue().toString();
                                        Log.d("PRE_MARKER_DETAILS", "longitude: " + longitude);
                                    }
                                }
                            }
                            switch (markerDetail.getKey()) {
                                case "ID":
                                    markerID = markerDetail.getValue().toString();
                                    break;
                                case "PinName":
                                    markerName = markerDetail.getValue().toString();
                                    break;
                            }
                            if (latitude != null && longitude != null){
                                double lat = Double.parseDouble(latitude);
                                double longi = Double.parseDouble(longitude);
                                LatLng latLng = new LatLng(lat, longi);
                                setMapMarker(latLng, markerName, markerID);
                            }
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("MARKER_DETAILS", "Failed to read value.", error.toException());
            }
        });
    }

    // Method to handle map marker creation
    public void setMapMarker(LatLng latLng, String markerTitle, String markerID){
        // Creating the marker and setting the marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        Log.d("SETUP_MARKER", "Setting up marker");

        // Setting the title for the marker
        markerOptions.title(markerTitle);

        // Creates marker and moves the camera to the new position
        mMap.addMarker(markerOptions).setTag(markerID);
    }

    // Method to handle instantiating fragment
    public void displayCreatePinFragment(LatLng latLng) {
        //Package latlong data to be sent to fragment
        Bundle bundle = new Bundle();
        bundle.putDouble("LATITUDE",latLng.latitude);
        bundle.putDouble("LONGITUDE",latLng.longitude);

        // Get username to pass to the pin
        authCheck = FirebaseAuth.getInstance();
        FirebaseUser currentUser = authCheck.getCurrentUser();
        String username;
        if (currentUser == null) {
            username = "Username_Unavailable";
        } else {
            username = currentUser.getEmail();
        }
        bundle.putString("USERNAME",username);

        // Create fragment and pass data through
        CreatePin createPin = new CreatePin();
        createPin.setArguments(bundle);

        // Get the FragementManager and start a transaction
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Add the SimpleFragment
        fragmentTransaction.add(R.id.fragment_container,
                createPin).addToBackStack(null).commit();
        isCreatePinFragmentDisplayed = true;
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
        isCreatePinFragmentDisplayed = false;
    }

    // Method to display account information fragment
    public void displayAccountFragment() {
        Bundle bundle = new Bundle();

        // Get username to pass to the accounts fragment
        authCheck = FirebaseAuth.getInstance();
        FirebaseUser currentUser = authCheck.getCurrentUser();
        String username;
        if (currentUser == null) {
            username = "Username_Unavailable";
        } else {
            username = currentUser.getEmail();
        }
        bundle.putString("EMAIL_ADDRESS",username);

        // Create fragment and pass data through
        Account account = new Account();
        account.setArguments(bundle);

        // Get the FragementManager and start a transaction
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Add the SimpleFragment
        fragmentTransaction.add(R.id.account_fragment,account).addToBackStack(null).commit();
        isAccountPinFragmentDisplayed = true;
    }

    // Method to close account information fragment
    public void closeAccountFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        // Check to see if the fragment is already showing
        Account account = (Account) fragmentManager
                .findFragmentById(R.id.account_fragment);
        if(account != null) {
            // Commit and close the transaction to close the fragment
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(account).commit();
            isAccountPinFragmentDisplayed = false;
        }
    }

    // Method to display the review pin fragment
    public void displayReviewPinFragment(String markerID) {
        Log.d("MARKER_CLICK", "Marker: " + markerID);
        Bundle bundle = new Bundle();

        bundle.putString("MARKER_ID", markerID);

        // Create fragment and pass data through
        ReviewPin reviewPin = new ReviewPin();
        reviewPin.setArguments(bundle);

        // Get the FragementManager and start a transaction
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // Add the SimpleFragment
        fragmentTransaction.add(R.id.reviewpin_fragment,reviewPin).addToBackStack(null).commit();
        isReviewPinFragmentDisplayed = true;
    }

    // Method to close the review pin fragment
    public void closeReviewPinFragment() {
        Log.d("FRAGEMENT_MANAGER", "Closing review pin fragment");
        FragmentManager fragmentManager = getSupportFragmentManager();
        // Check to see if the fragment is already showing
        ReviewPin reviewPin = (ReviewPin) fragmentManager
                .findFragmentById(R.id.reviewpin_fragment);
        if(reviewPin != null) {
            Log.d("FRAGEMENT_MANAGER", "Calling closing review pin fragment");
            // Commit and close the transaction to close the fragment
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(reviewPin).commit();
            isReviewPinFragmentDisplayed = false;
        }
    }

    // Method to setup places search
    private void setupPlacesSearch() {
        final LatLng[] nLocation = new LatLng[1];
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.d("PLACES_SEARCH", "Place: " + place.getName());
                nLocation[0] = place.getLatLng();
                moveCamera(nLocation[0], DEFAULT_ZOOM);
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.d("PLACES_SEARCH", "An error occurred: " + status);
                Toast.makeText(MapsActivity.this, "Error: Unable to change location",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}

