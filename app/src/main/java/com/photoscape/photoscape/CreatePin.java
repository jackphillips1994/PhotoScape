package com.photoscape.photoscape;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * A simple {@link Fragment} subclass.
 */
public class CreatePin extends Fragment {

    private Button discardButton;

    public CreatePin() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View RootView = inflater.inflate(R.layout.fragment_create_pin, container, false);

        // Setup buttons
        setupButtons(RootView);
        return RootView;
    }

    public void setupButtons(View view){
        discardButton = view.findViewById(R.id.discardButton);
        discardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
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
        });
    }

    public static CreatePin newInstance() {
        return new CreatePin();
    }

}
