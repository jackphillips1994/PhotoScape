package com.photoscape.photoscape;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReviewPin extends Fragment {

    private String markerID;
    private TextView pinName;
    private TextView bestTimeForPhotoAnswer;
    private TextView description;
    private TextView instructions;
    private ImageView photoPreview;

    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://photoscape-c88b6.appspot.com/PhotoScapePhotos");

    public ReviewPin() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_review_pin, container, false);

        FirebaseOptions opts = FirebaseApp.getInstance().getOptions();
        Log.i("FIREBASE_INFO", "Bucket = " + opts.getStorageBucket());

        unpackArguments();
        getPinMarkerData(markerID);
        setupInteface(rootView);
        downloadPhoto();

        // Inflate the layout for this fragment
        return rootView;
    }

    private void unpackArguments() {
        markerID = this.getArguments().getString("MARKER_ID");
        Log.d("REVIEW_STATUS","MarkerID: " + markerID);
    }

    private void getPinMarkerData(String markerID) {
        DatabaseReference dbRef = database.getReference("PhotoScape/" + markerID);
        Query memberIDQuery = dbRef.orderByChild("ID").equalTo(markerID);
        memberIDQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                //String value = dataSnapshot.getValue(String.class);
                for (DataSnapshot markerDetailHashName : dataSnapshot.getChildren()){
                    for ( DataSnapshot markerDetail : markerDetailHashName.getChildren()){
                        Log.d("MARKER_DETAILS", "Value is: " + markerDetail.getKey());
                        switch (markerDetail.getKey()) {
                            case "PinBestPhotoTime":
                                bestTimeForPhotoAnswer.setText(markerDetail.getValue().toString());
                                break;
                            case "PinDescription":
                                description.setText(markerDetail.getValue().toString());
                                break;
                            case "PinInstructions":
                                instructions.setText(markerDetail.getValue().toString());
                                break;
                            case "PinName":
                                pinName.setText(markerDetail.getValue().toString());
                                break;
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

    private void setupInteface(View view) {
        pinName = view.findViewById(R.id.pinNameTextView);
        bestTimeForPhotoAnswer = view.findViewById(R.id.bestTimeForPhotoAnswerTextView);
        description = view.findViewById(R.id.descriptionTextView);
        instructions = view.findViewById(R.id.instructionsTextView);
        photoPreview = view.findViewById(R.id.photoPreview);
    }

    private void downloadPhoto() {
        //StorageReference pathRef = mStorageRef.child("PhotoScapePhotos/photo" + markerID + ".jpg");
        Log.w("PHOTO_PREVIEW_DETAILS", "PhotoScapePhotos/photo" + markerID + ".jpg");
        // Create a reference from an HTTPS URL
        // Note that in the URL, characters are URL escaped!
        StorageReference pathRef = mStorageRef.child("PhotoScapePhotos/photo20181002185343.jpg");

        final long ONE_MEGABYTE = 1024 * 1024;
        pathRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
            @Override
            public void onSuccess(byte[] bytes) {
                // Data for "images/island.jpg" is returns, use this as needed
                Bitmap bm = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                DisplayMetrics dm = new DisplayMetrics();
                ((WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE))
                        .getDefaultDisplay().getMetrics(dm);

                photoPreview.setMinimumHeight(dm.heightPixels);
                photoPreview.setMinimumWidth(dm.widthPixels);
                photoPreview.setImageBitmap(bm);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }
}
