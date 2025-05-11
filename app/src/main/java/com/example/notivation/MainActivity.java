package com.example.notivation;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.notivation.model.User;
import com.example.notivation.util.DatabaseHelper;
import com.example.notivation.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextInputEditText emailInput;
    private TextInputEditText passwordInput;
    private MaterialButton loginButton;
    private TextView registerLink;

    private DatabaseHelper databaseHelper;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved dark mode preference
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", false);
        AppCompatDelegate.setDefaultNightMode(
                isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize helpers
        databaseHelper = new DatabaseHelper();
        sessionManager = SessionManager.getInstance(getApplicationContext());

        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            // User is already logged in, go to HomeActivity
            startActivity(new Intent(MainActivity.this, HomeActivity.class));
            finish();
            return;
        }

        // Initialize views
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        registerLink = findViewById(R.id.register_link);

        // Set up login button click listener
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginUser();
            }
        });

        // Set up register link click listener
        registerLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to register screen
                Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }

    /**
     * Login user with email and password
     */
    private void loginUser() {
        // Get input values
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        // Validate inputs
        if (TextUtils.isEmpty(email)) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return;
        }

        // Show progress
        loginButton.setEnabled(false);
        loginButton.setText("Logging in...");

        // Login with Firebase Realtime Database
        databaseHelper.loginUser(email, password, new DatabaseHelper.AuthCallback() {
            @Override
            public void onSuccess(User user) {
                // Create login session
                sessionManager.createLoginSession(user.getId(), user.getName(), user.getEmail());

                // Set profile picture if available
                if (user.getProfilePicture() != null) {
                    Log.d(TAG, "Setting profile picture from database: " + user.getProfilePicture());
                    sessionManager.setProfilePicture(user.getProfilePicture());
                }

                // Show success message
                Toast.makeText(MainActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

                // Navigate to HomeActivity
                Intent intent = new Intent(MainActivity.this, HomeActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                // Show error message
                Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();

                // Reset button
                loginButton.setEnabled(true);
                loginButton.setText("Login");
            }
        });
    }
}
