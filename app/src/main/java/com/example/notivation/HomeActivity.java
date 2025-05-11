package com.example.notivation;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import com.bumptech.glide.Glide;
import com.example.notivation.util.DatabaseNoteHelper;
import com.example.notivation.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";
    private static final int PROFILE_SETTINGS_REQUEST_CODE = 100;
    private ExtendedFloatingActionButton fabUpload;
    private BottomNavigationView bottomNavigationView;
    private ImageView profileIcon;
    private RecyclerView notesRecyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private NoteAdapter noteAdapter;
    // We'll use the adapter's list directly instead of maintaining a separate list
    private DatabaseNoteHelper databaseNoteHelper;
    private SessionManager sessionManager;
    private EditText searchEditText;
    private View emptyStateView;
    private TextView welcomeMessageText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Initialize SessionManager and DatabaseNoteHelper
        sessionManager = SessionManager.getInstance(getApplicationContext());
        databaseNoteHelper = new DatabaseNoteHelper(sessionManager);

        // Link views with XML IDs
        fabUpload = findViewById(R.id.fabUpload);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Find views in the included header layout
        View headerView = findViewById(R.id.homeHeader);
        profileIcon = headerView.findViewById(R.id.profile_icon);
        searchEditText = headerView.findViewById(R.id.searchEditText);
        welcomeMessageText = headerView.findViewById(R.id.welcomeMessageText);

        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        emptyStateView = findViewById(R.id.emptyStateView);

        // Configure RecyclerView
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        noteAdapter = new NoteAdapter(this, new ArrayList<>());
        notesRecyclerView.setAdapter(noteAdapter);

        // Log initial adapter state
        Log.d(TAG, "Initial adapter setup complete");

        // Set up swipe-to-delete functionality
        setupSwipeToDelete();

        // Configure SwipeRefreshLayout
        swipeRefreshLayout.setOnRefreshListener(this::loadSavedNotes);
        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        // Load user profile image using Glide
        loadUserProfile();

        // FAB (+ Upload) button click
        fabUpload.setOnClickListener(view -> {
            Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
            startActivity(intent);
        });

        // Bottom Navigation Listener
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.home) {
                return true;
            } else if (id == R.id.upload) {
                Intent intent = new Intent(HomeActivity.this, UploadActivity.class);
                startActivity(intent);
                return true;
            }

            return false;
        });

        // Profile Icon click to go to ProfileSettingsActivity
        profileIcon.setOnClickListener(v -> {
            // Check if user is logged in
            if (sessionManager.isLoggedIn()) {
                // Create an Intent to go to ProfileSettingsActivity
                Intent intent = new Intent(HomeActivity.this, ProfileSettingsActivity.class);

                // Pass user info from SessionManager
                intent.putExtra("USER_EMAIL", sessionManager.getUserEmail());
                intent.putExtra("USER_NAME", sessionManager.getUserName());

                // Start for result to know when to refresh the profile picture
                startActivityForResult(intent, PROFILE_SETTINGS_REQUEST_CODE);
            }
        });

        // Set up search functionality
        setupSearch();

        // Load saved notes from Firebase
        loadSavedNotes();
    }

    private void loadUserProfile() {
        // Check if user is logged in
        if (sessionManager.isLoggedIn()) {
            // Get user name for welcome message
            String userName = sessionManager.getUserName();
            if (userName != null && !userName.isEmpty()) {
                welcomeMessageText.setText("Welcome, " + userName);
            } else {
                welcomeMessageText.setText("Welcome!");
            }

            // Load profile picture
            String profilePictureUri = sessionManager.getProfilePicture();
            Log.d(TAG, "Profile picture URI: " + (profilePictureUri != null ? profilePictureUri : "null"));

            if (profilePictureUri != null) {
                // Load saved profile picture
                try {
                    Uri uri = Uri.parse(profilePictureUri);
                    Log.d(TAG, "Loading profile picture with URI: " + uri);

                    Glide.with(this)
                            .load(uri)
                            .circleCrop()
                            .error(R.drawable.ic_profile_placeholder)
                            .into(profileIcon);
                } catch (Exception e) {
                    Log.e(TAG, "Error loading profile picture", e);
                    // Fallback to placeholder
                    Glide.with(this)
                            .load(R.drawable.ic_profile_placeholder)
                            .circleCrop()
                            .into(profileIcon);
                }
            } else {
                // Load profile image placeholder
                Log.d(TAG, "No profile picture URI, loading placeholder");
                Glide.with(this)
                        .load(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(profileIcon);
            }
        }
    }

    /**
     * Load saved notes from Firebase
     */
    private void loadSavedNotes() {
        try {
            // Show loading indicator
            swipeRefreshLayout.setRefreshing(true);

            // Call DatabaseNoteHelper to get user's notes from Firebase Realtime Database
            databaseNoteHelper.getUserNotes()
                .addOnSuccessListener(result -> {
                    try {
                        // Get the notes
                        List<UserNotesResult.NoteItem> userNotes = result.getNotes();

                        // Update the adapter with the new notes
                        if (userNotes != null) {
                            noteAdapter.updateNotes(userNotes);

                            if (userNotes.isEmpty()) {
                                // Show empty state
                                emptyStateView.setVisibility(View.VISIBLE);
                                notesRecyclerView.setVisibility(View.GONE);
                                Log.d(TAG, "No notes found, showing empty state");
                            } else {
                                // Show notes
                                emptyStateView.setVisibility(View.GONE);
                                notesRecyclerView.setVisibility(View.VISIBLE);
                                Log.d(TAG, "Loaded " + userNotes.size() + " notes");
                            }
                        } else {
                            // Clear the adapter if notes is null
                            noteAdapter.updateNotes(new ArrayList<>());
                            // Show empty state
                            emptyStateView.setVisibility(View.VISIBLE);
                            notesRecyclerView.setVisibility(View.GONE);
                            Log.d(TAG, "Notes is null, showing empty state");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing notes result", e);
                        Toast.makeText(this, "Error processing notes", Toast.LENGTH_SHORT).show();
                    } finally {
                        // Hide loading indicator
                        swipeRefreshLayout.setRefreshing(false);
                    }
                })
                .addOnFailureListener(e -> {
                    // Log the error
                    Log.e(TAG, "Error loading notes", e);

                    // Hide loading indicator
                    swipeRefreshLayout.setRefreshing(false);

                    // Show empty state
                    emptyStateView.setVisibility(View.VISIBLE);
                    notesRecyclerView.setVisibility(View.GONE);

                    // Show a more user-friendly error message
                    Toast.makeText(this,
                        "Could not load notes. Your saved notes will appear here after creating them.",
                        Toast.LENGTH_LONG).show();
                });
        } catch (Exception e) {
            // Handle any unexpected errors
            Log.e(TAG, "Unexpected error in loadSavedNotes", e);
            swipeRefreshLayout.setRefreshing(false);

            // Show empty state
            emptyStateView.setVisibility(View.VISIBLE);
            notesRecyclerView.setVisibility(View.GONE);

            Toast.makeText(this, "Something went wrong. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Set up search functionality
     */
    private void setupSearch() {
        // Add TextWatcher to search EditText
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Not used
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Filter notes as user types
                noteAdapter.getFilter().filter(s);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Not used
            }
        });
    }

    /**
     * Set up swipe-to-delete functionality
     */
    private void setupSwipeToDelete() {
        // Create the swipe callback
        SwipeToDeleteCallback swipeToDeleteCallback = new SwipeToDeleteCallback(this, noteAdapter);

        // Create ItemTouchHelper with the callback
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeToDeleteCallback);

        // Attach to RecyclerView
        itemTouchHelper.attachToRecyclerView(notesRecyclerView);
    }

    /**
     * Get the DatabaseNoteHelper instance
     * @return The DatabaseNoteHelper instance
     */
    public DatabaseNoteHelper getDatabaseNoteHelper() {
        return databaseNoteHelper;
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload notes and profile when returning to this activity
        loadSavedNotes();
        loadUserProfile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PROFILE_SETTINGS_REQUEST_CODE) {
            // Profile settings activity returned, reload profile picture
            Log.d(TAG, "Returned from profile settings, reloading profile picture");
            loadUserProfile();
        }
    }
}
