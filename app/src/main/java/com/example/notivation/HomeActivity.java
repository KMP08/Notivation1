package com.example.notivation; // Palitan ng iyong package name

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

public class HomeActivity extends AppCompatActivity {

    private RecyclerView notesRecyclerView;
    private ExtendedFloatingActionButton fabUpload;
    private BottomNavigationView bottomNavigationView;
    private ImageView menuIcon, profileIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home); // Make sure this is the correct layout filename

        // Link views with XML IDs
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        fabUpload = findViewById(R.id.fabUpload);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        menuIcon = findViewById(R.id.menu_icon);
        profileIcon = findViewById(R.id.profile_icon);

        // Handle click for + New Note button
        fabUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Go to UploadActivity
                Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
                startActivity(intent);
            }
        });

        // Handle bottom navigation item clicks
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:
                        // Already in Home screen, do nothing
                        return true;
                    case R.id.upload:
                        // Navigate to UploadActivity
                        Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
                        startActivity(intent);
                        return true;
                    default:
                        return false;
                }
            }
        });

        // Optional: Handle clicks for menu or profile icons
        menuIcon.setOnClickListener(v -> {
            // Add drawer or menu action here
        });

        profileIcon.setOnClickListener(v -> {
            // Add profile settings or user info action here
        });
    }
}
