package com.photoscape.photoscape;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MyPinsActivity extends Activity {

    ListView list;
    //private String emailAddress;
    private String markerID;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();
    private StorageReference mStorageRef = FirebaseStorage.getInstance().getReferenceFromUrl("gs://photoscape-c88b6.appspot.com/PhotoScapePhotos");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_pins);

        // Get data from main activity
        Intent intent = getIntent();
        String emailAddress = intent.getStringExtra("EMAIL_ADDRESS");
        Log.d("EMAIL_CHECK", "onCreate: " + emailAddress);
        Map<String, String[]> pinDetails = getPinMarkerData(emailAddress);
        String[] pinList = pinDetails.get("PinName");
        Integer[] imgNames = {1,2};
        String[] description = pinDetails.get("Descriptions");

        Log.d("FINAL_ARRAY_CHECK", "onCreate: " + pinList);

        CustomListAdapter adapter = new CustomListAdapter(this, pinList, imgNames, description);
        list = (ListView) findViewById(R.id.list);
        list.setAdapter(adapter);
    }


    private Map<String, String[]> getPinMarkerData(String emailAddress) {
        // Setup storage arraylists
        final ArrayList<String> pinNames = new ArrayList<String>();
        final ArrayList<String> descriptions = new ArrayList<String>();
        final Map<String, String[]>  markers = new HashMap<String, String[]>();

        DatabaseReference dbRef = database.getReference("PhotoScape/");
        Log.d("DB_DETAILS", "Database reference: " + dbRef.toString());
        Query memberIDQuery = dbRef.orderByChild("EmailAddress").equalTo(emailAddress);
        Log.d("DB_DETAILS", "Database has been accessed with email: " + emailAddress);
        Log.d("DB_DETAILS", "Database reference: " + memberIDQuery.toString());
        memberIDQuery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // This method is called once with the initial value and again
                // whenever data at this location is updated.
                String value = dataSnapshot.getValue(String.class);
                Log.d("MARKER_DETAILS", "Value is: " + value);
                for (DataSnapshot markerDetailHashName : dataSnapshot.getChildren()){
                    Log.d("MARKER_DETAILS", "Value is: " + markerDetailHashName.getKey());
                    for (DataSnapshot markerDetailIDName : markerDetailHashName.getChildren()){
                        Log.d("MARKER_DETAILS", "Value is: " + markerDetailIDName.getKey());
                        for ( DataSnapshot markerDetail : markerDetailIDName.getChildren()) {
                            Log.d("MARKER_DETAILS", "Value is: " + markerDetail.getKey());
                            switch (markerDetail.getKey()) {
                                case "PinDescription":
                                    if(markerDetail.getValue() == null){
                                        descriptions.add("null");
                                    } else {
                                        descriptions.add(markerDetail.getValue().toString());
                                    }
                                    break;
                                case "PinName":
                                    pinNames.add(markerDetail.getValue().toString());
                                    break;
                                case "ID":
                                    markerID = markerDetail.getValue().toString();
                                    break;
                            }
                        }
                    }
                }

                String pinNamesArray[] = new String[pinNames.size()];
                for(int j = 0; j<pinNames.size(); j++){
                    Log.d("ARRAY_CHECK", "onCreate: " + pinNames.get(j));
                    pinNamesArray[j] = pinNames.get(j);
                }

                String descriptionsArray[] = new String[descriptions.size()];
                for(int j = 0; j < descriptions.size(); j++){
                    Log.d("ARRAY_CHECK", "onCreate: " + descriptions.get(j));
                    pinNamesArray[j] = descriptions.get(j);
                }

                Log.d("ARRAY_CHECK", "onCreate: " + pinNamesArray[0]);
                Log.d("ARRAY_CHECK", "onCreate: " + descriptionsArray[0]);
                markers.put("PinName", pinNamesArray);
                markers.put("Descriptions", descriptionsArray);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("MARKER_DETAILS", "Failed to read value.", error.toException());
            }
        });
        return markers;
    }

  /**  private void downloadPhoto(String markerID) {
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
   **/}
