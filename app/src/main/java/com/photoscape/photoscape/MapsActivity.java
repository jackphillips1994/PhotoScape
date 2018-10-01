package com.photoscape.photoscape;

import android.Manifest;
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
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
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

    // Variables to handle the fragements
    public static Boolean isCreatePinFragmentDisplayed = false;
    public static Boolean isAccountPinFragmentDisplayed = false;
    public static Boolean isCreatePinFragmentWasDisplayed = false;

    // Setting up Firebase login providers
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());

    // Marker options for the create pin marker once it has come back from the fragment
    MarkerOptions createPinMarkerOptions = new MarkerOptions();

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
                                Intent intent = new Intent(MapsActivity.this, MyPinsActivity.class);
                                startActivity(intent);
                                break;
                            case "Account":
                                Log.d("NAV_BAR", "Account clicked");
                                displayAccountFragment();
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
            getDeviceLocation();
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
                } else if (!isAccountPinFragmentDisplayed) {
                    // Call marker creation method
                    displayCreatePinFragment(latLng);
                    //setMapMarker(latLng, "New Marker Title");
                } else {
                    closeAccountFragment();
                }
            }
        });
        if(isCreatePinFragmentWasDisplayed == true){
            mMap.addMarker(createPinMarkerOptions);
            isCreatePinFragmentWasDisplayed = false;
        }
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
        // Check to see if the create pin fragment was displayed to make sure that this is only run
        // on resume from the create pin fragment
        Log.d("INFO_RECEIVED", isCreatePinFragmentWasDisplayed.toString());
        if(isCreatePinFragmentWasDisplayed){
            //DETERMINE WHO STARTED THIS ACTIVITY
            final String sender=this.getIntent().getExtras().getString("SENDER_KEY");

            //IF ITS THE FRAGMENT THEN RECEIVE DATA
            if(sender != null)
            {
                this.receiveData();
                Toast.makeText(this, "Received", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void receiveData() {
        // TODO: Setup map location to be saved on pause and set the map location on resume

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
        }
    }

   /* private void getDeviceLocationWithManager() {
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
    }
*/

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
    public void setupMultipleMapMarkers() {
        // TODO:
    }

    // Method to handle map marker creation
    public void setMapMarker(LatLng latLng, String markerTitle, String markerID){
        // Return value from fragment, if save pin then save pin if not discard pin

        //TODO: Add the ability to add the markerID to the marker
        // Creating the marker and setting the marker
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        Log.d("SETUP_MARKER", "Setting up marker");

        // Setting the title for the marker
        markerOptions.title(markerTitle);

        // Clears the markers
        //mMap.clear();

        // Creates marker and moves the camera to the new position
        mMap.addMarker(markerOptions);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_ZOOM));
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
            }
        });
    }
}

