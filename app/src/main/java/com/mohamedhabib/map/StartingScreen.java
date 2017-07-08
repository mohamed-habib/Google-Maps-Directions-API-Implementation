package com.mohamedhabib.map;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class StartingScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starting_screen);
    }

    public void goToCodingChallenge(View view) {
        startActivity(new Intent(StartingScreen.this, MapActivity
                .class));
    }
}
