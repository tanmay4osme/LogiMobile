package com.geraud.android.gps1.SplashScreen;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.geraud.android.gps1.GoogleMap.MapsActivity;
import com.geraud.android.gps1.Onboarding_slides.Walkthrough;
import com.geraud.android.gps1.Registration;
import com.google.firebase.auth.FirebaseAuth;
import com.onesignal.OneSignal;

public class SplashActivity extends AppCompatActivity {
    public final static String SHARED_PREF = "Logi_Shared_Preferences";
    public final static String FIRST_START = "firstStart";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        boolean firstStart = sharedPreferences.getBoolean(FIRST_START, true);

        //if its the first time the user starts the app send them to walkthrough activity
        if (firstStart) {
            Toast.makeText(getApplicationContext(),"firstStart",Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getApplicationContext(), Walkthrough.class)); //goto walkthrough
            //close splash activity
            finish();
        } else {
            //if its not the user first tym of starting the app but has not registered send them to registration activity
            //if a user is already signed in
            if (loggedIn) {
                //if a user is signed in
                startActivity(new Intent(getApplicationContext(), MapsActivity.class)); //goto main app screen
                //close splash activity
                finish();
            } else {
                //if no user is signed in send them to the registration page
                startActivity(new Intent(getApplicationContext(), Registration.class)); //goto main app screen
                //close splash activity
                finish();
            }
        }

    }
}
