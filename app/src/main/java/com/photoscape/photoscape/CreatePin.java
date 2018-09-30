package com.photoscape.photoscape;


import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.net.URI;
import java.security.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;


/**
 * The fragment that handles the creation of the map pins
 * The lat and long of the map marker is transferred from the activity to here to be saved in the
 * cloud database. The photos are stored in cloud storage
 */
public class CreatePin extends Fragment {

    private Button discardButton;
    private Button saveButton;
    private ImageButton importPhotoButton;
    private int PICK_IMAGE_REQUEST = 1;
    private int REQUEST_EXTERNAL_STORAGE = 1;

    // Setup inputs
    private EditText nameInput;
    private EditText descriptionInput;
    private EditText instructionsInput;
    private Spinner spinner;

    // Setup location details
    Double latitude;
    Double longitude;
    String username;

    // Setup image details
    Uri imageUri;

    // Setup Firebase DB
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private DatabaseReference dbRef = database.getReference("PhotoScape");
    private StorageReference mStorageRef;

    public CreatePin() {
        // Required empty public constructor
    }

    public static CreatePin newInstance() {
        return new CreatePin();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View RootView = inflater.inflate(R.layout.fragment_create_pin, container, false);

        // Setup buttons
        setupButtons(RootView);

        // Setup spinner
        setupSpinner(RootView);

        // Unpack out data from the bundle
        unpackArguments();

        // Setup text inputs
        setupTextInputs(RootView);

        // Init the firebase storage
        mStorageRef = FirebaseStorage.getInstance().getReference("PhotoScapePhotos");

        return RootView;
    }

    private void setupTextInputs(View view) {
        nameInput = view.findViewById(R.id.editPinName);
        descriptionInput = view.findViewById(R.id.editPinDescription);
        instructionsInput = view.findViewById(R.id.editPinInstructions);
    }

    private void setupSpinner(View RootView) {
        spinner = RootView.findViewById(R.id.bestPhotoTimeSpinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.best_Photo_Time, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
    }

    private void setupButtons(View view){
        // Setup discard button
        discardButton = view.findViewById(R.id.discardButton);
        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeCurrentFragment();
            }
        });

        // Setup Save button
        saveButton = view.findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                savePinDetails();
                closeCurrentFragment();
            }
        });

        // Setup upload button
        importPhotoButton = view.findViewById(R.id.importPhotoButton);
        importPhotoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                choosePhotoFromGallery();
            }
        });
    }

    private void unpackArguments() {
        latitude = this.getArguments().getDouble("LATITUDE");
        longitude = this.getArguments().getDouble("LONGITUDE");
        username = this.getArguments().getString("USERNAME");
        Log.d("TRANSFER_STATUS", "LATITUDE: " + latitude + " " + "LONGITUDE: " + longitude +
                "USERNAME: " + username);
    }

    private void closeCurrentFragment(){
        FragmentManager fragmentManager = getFragmentManager();
        // Check to see if the fragment is already showing
        CreatePin createPin = (CreatePin) fragmentManager
                .findFragmentById(R.id.fragment_container);
        if(createPin != null) {
            // Commit and close the transaction to close the fragment
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(createPin).commit();
        }
        MapsActivity.isFragmentDisplayed = false;
    }

    public void choosePhotoFromGallery() {
        Intent intent = new Intent();
        // Show only images, no videos or anything else
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // Always show the chooser (if there are multiple options available)
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST) {
            this.imageUri = data.getData();
            ImageView imageView = this.getView().findViewById(R.id.photoPreview);
            imageView.setImageURI(imageUri);
        }
    }

    // Method to handle gather all the pin details from the inputted data
    private Map getPinDetails() {
        Map pinDetails = new HashMap();
        pinDetails.put("PinName", getPinName());
        pinDetails.put("PinDescription",getPinDescription());
        pinDetails.put("PinInstructions",getPinInstructions());
        pinDetails.put("PinBestPhotoTime",getPinBestPhotoTime());
        return pinDetails;
    }

    // Method to handle the creation of pin marker
    private void savePinDetails() {
        // Check to see if there is a value for Long, lat and a photo has been selected
        if(latitude == null || longitude == null){
            Toast.makeText(getActivity(),"Unable to get pin location details", Toast.LENGTH_SHORT).show();
        }
        // Gather all data needed to be saved to the DB, then call the save db method and save storage method
        Map pinDetails = getPinDetails();
        pinDetails.put("Username", username);

        // Get uri from image view
        ImageView imageView = this.getView().findViewById(R.id.photoPreview);
        if(imageView == null){
            Toast.makeText(getActivity(),"No photo has been selected", Toast.LENGTH_SHORT).show();
        } else{
            // Call method to generate an id for the marker(EG timestamp that will go across photos and marker details)
            String markerID = generateID();
            pinDetails.put("ID",markerID);

            saveToFirebase(pinDetails);
            Log.d("LOCATION_SAVING", "Pin long and lat have been saved");

            savePhotoToCloud(imageUri, markerID);
            Log.d("PHOTO_SAVING", "Pin photo has been saved");

            LatLng latLng = new LatLng(latitude, longitude);
            String markerName = "New Marker Title";
            // Call method to create new marker
        }
    }

    // Method to handle generating the ID for the marker
    private String generateID(){
        String IDStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
        return IDStamp;
    }

    // Method to handle saving to the DB
    private void saveToFirebase(Map pinDetails) {
        // Get pin timestamp
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("AEST"));
        Date date = new Date();
        String pinCreationTime = dateFormat.format(date);
        pinDetails.put("CreationTime", pinCreationTime);

        // Setup hashmap to be saved
        Map mCoordinate = new HashMap();
        mCoordinate.put("latitude", latitude);
        mCoordinate.put("longitude", longitude);
        pinDetails.put("location", mCoordinate);
        dbRef.push().setValue(pinDetails);
    }

    // Method to save photo to cloud storage
    private void savePhotoToCloud(Uri uri, String markerID) {
        StorageReference storageRef = mStorageRef.child("PhotoScapePhotos/photo" + markerID
                + username + ".jpg");
        if (ActivityCompat.checkSelfPermission(getContext(),
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},REQUEST_EXTERNAL_STORAGE);
            Log.e("DB", "PERMISSION REQUESTED");
        } else {
            Log.e("DB", "PERMISSION GRANTED");
            String path = ImageFilePath.getPath(getActivity().getApplicationContext(), uri);
            Log.d("TRANSFER_STATUS", "Path: " + path);
            Uri file = Uri.fromFile(new File(path));
            UploadTask uploadTask = storageRef.putFile(file);
            uploadTask.addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    Log.d("TRANSFER_STATUS", "Upload Complete");
                }
            });
        }
    }

    // Getters and setters
    public String getPinName(){
        String pinName = nameInput.getText().toString();
        return pinName;
    }

    public String getPinDescription() {
        String pinDescription = descriptionInput.getText().toString();
        return pinDescription;
    }

    public String getPinInstructions() {
        String pinInstructions;
        if(instructionsInput.getText().toString() != null){
            pinInstructions = instructionsInput.getText().toString();
        }else {
            pinInstructions = "No Instructions provided";
        }
        return pinInstructions;
    }

    public String getPinBestPhotoTime() {
        String pinBestPhotoTime = spinner.getSelectedItem().toString();
        return pinBestPhotoTime;
    }
}
