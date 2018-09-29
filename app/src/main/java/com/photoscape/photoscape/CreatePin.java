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
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
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
import java.util.HashMap;
import java.util.Map;


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
    private StorageReference mStorageRef;
    private int REQUEST_EXTERNAL_STORAGE = 1;

    // Setup Firebase DB
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference dbRef = database.getReference("PhotoScape");

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

        //UNPACK OUR DATA FROM OUR BUNDLE
        Double latitude = this.getArguments().getDouble("LATITUDE");
        Double longitude = this.getArguments().getDouble("LONGITUDE");
        Log.d("TRANSFER_STATUS", "LATITUDE: " + latitude + " " + "LONGITUDE: " + longitude);

        // Init the firebase storage
        mStorageRef = FirebaseStorage.getInstance().getReference("PhotoScapePhotos");

        return RootView;
    }

    public void setupButtons(View view){
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
            Uri uri = data.getData();
            ImageView imageView = (ImageView) this.getView().findViewById(R.id.photoPreview);
            imageView.setImageURI(uri);
            savePhotoToCloud(uri);
        }
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

    // Method to save photo to cloud storage
    private void savePhotoToCloud(Uri uri) {
        StorageReference storageRef = mStorageRef.child("test.jpg");
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
}
