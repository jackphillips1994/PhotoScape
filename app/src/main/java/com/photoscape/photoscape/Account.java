package com.photoscape.photoscape;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.Arrays;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class Account extends Fragment {

    private Button signOutButton;
    private TextView emailAddressDisplay;
    private static int REQUEST_CODE = 102;

    // Setting up Firebase login providers
    List<AuthUI.IdpConfig> providers = Arrays.asList(
            new AuthUI.IdpConfig.EmailBuilder().build(),
            new AuthUI.IdpConfig.GoogleBuilder().build());

    public Account() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_account, container, false);

        emailAddressDisplay = rootView.findViewById(R.id.emailAddressText);
        String emailAddress = this.getArguments().getString("EMAIL_ADDRESS");
        emailAddressDisplay.setText(emailAddress);

        signOutButton = rootView.findViewById(R.id.signOutButton);
        signOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                signOut();
                closeCurrentFragment();
            }
        });

        return rootView;
    }

    private void signOut() {
        AuthUI.getInstance()
                .signOut(getActivity())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {}});
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .build(),REQUEST_CODE);
    }

    private void closeCurrentFragment(){
        FragmentManager fragmentManager = getFragmentManager();
        // Check to see if the fragment is already showing
        Account account = (Account) fragmentManager
                .findFragmentById(R.id.account_fragment);
        if(account != null) {
            // Commit and close the transaction to close the fragment
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.remove(account).commit();
        }
    }
}
