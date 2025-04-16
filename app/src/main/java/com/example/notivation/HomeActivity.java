package com.example.notivation;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class HomeActivity extends AppCompatActivity {

    private static final int FILE_PICKER_REQUEST_CODE = 1;

    TextView greetingText, tvFileName, chooseMethodText;
    ImageView profileImage;
    ImageButton uploadButton, cornellMethodButton, outlineMethodButton, mindMapButton, menuButton;
    ScrollView scrollViewMethods;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // View References
        greetingText = findViewById(R.id.greetingText);
        profileImage = findViewById(R.id.profileImage);
        uploadButton = findViewById(R.id.uploadButton);
        tvFileName = findViewById(R.id.tvFileName);
        chooseMethodText = findViewById(R.id.chooseMethodText);
        scrollViewMethods = findViewById(R.id.scrollView);

        cornellMethodButton = findViewById(R.id.cornellMethodButton);
        outlineMethodButton = findViewById(R.id.outlineMethodButton);
        mindMapButton = findViewById(R.id.mindMapButton);
        menuButton = findViewById(R.id.menuButton); // Menu icon

        // Hide method buttons and text at start
        chooseMethodText.setVisibility(View.INVISIBLE);
        scrollViewMethods.setVisibility(View.INVISIBLE);
        cornellMethodButton.setVisibility(View.INVISIBLE);
        outlineMethodButton.setVisibility(View.INVISIBLE);
        mindMapButton.setVisibility(View.INVISIBLE);

        // Load animation
        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up);

        // Load Google Account Info
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            String fullName = account.getDisplayName();
            String firstName = fullName != null && fullName.contains(" ") ? fullName.split(" ")[0] : fullName;
            greetingText.setText("Hi, " + firstName + "!");

            Uri photoUri = account.getPhotoUrl();
            if (photoUri != null) {
                Glide.with(this)
                        .load(photoUri)
                        .circleCrop()
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.ic_profile_placeholder);
            }
        }

        // 📸 Profile Image Click - Show user info
        profileImage.setOnClickListener(v -> {
            GoogleSignInAccount acc = GoogleSignIn.getLastSignedInAccount(this);
            if (acc != null) {
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_user_info, null);

                ImageView dialogProfileImage = dialogView.findViewById(R.id.dialogProfileImage);
                TextView dialogUserName = dialogView.findViewById(R.id.dialogUserName);
                TextView dialogUserEmail = dialogView.findViewById(R.id.dialogUserEmail);

                dialogUserName.setText(acc.getDisplayName());
                dialogUserEmail.setText(acc.getEmail());

                Uri photoUri = acc.getPhotoUrl();
                if (photoUri != null) {
                    Glide.with(this).load(photoUri).circleCrop().into(dialogProfileImage);
                } else {
                    dialogProfileImage.setImageResource(R.drawable.ic_profile_placeholder);
                }

                new android.app.AlertDialog.Builder(this)
                        .setView(dialogView)
                        .setPositiveButton("Logout", (dialog, which) -> signOut())
                        .setNegativeButton("Close", null)
                        .show();
            }
        });

        // Upload Button Logic
        uploadButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select a file"), FILE_PICKER_REQUEST_CODE);
        });

        // Method Buttons
        cornellMethodButton.setOnClickListener(v -> {
            Toast.makeText(this, "Cornell Method Selected", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, LoadingActivity.class);
            intent.putExtra("method", "Cornell");
            startActivity(intent);
        });

        outlineMethodButton.setOnClickListener(v -> {
            Toast.makeText(this, "Outline Method Selected", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, LoadingActivity.class);
            intent.putExtra("method", "Outline");
            startActivity(intent);
        });

        mindMapButton.setOnClickListener(v -> {
            Toast.makeText(this, "Mind Map Selected", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(HomeActivity.this, LoadingActivity.class);
            intent.putExtra("method", "MindMap");
            startActivity(intent);
        });

        // 📋 Menu Button Logic
        menuButton.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(HomeActivity.this, v);
            popupMenu.getMenuInflater().inflate(R.menu.menu_home, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.menu_my_notes) {
                    startActivity(new Intent(HomeActivity.this, MyNotesActivity.class));
                    return true;
                } else if (id == R.id.menu_settings) {
                    startActivity(new Intent(HomeActivity.this, SettingsActivity.class));
                    return true;

            } else if (id == R.id.menu_logout) {
                    signOut();
                    return true;
                }
                return false;
            });
            popupMenu.show();
        });
    }

    // 🔐 Google Sign Out
    private void signOut() {
        GoogleSignIn.getClient(
                this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        ).signOut().addOnCompleteListener(task -> {
            Intent intent = new Intent(HomeActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // 📁 Handle File Selection
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                String fileName = getFileName(fileUri);
                tvFileName.setText(fileName);

                // Show method buttons with animation
                chooseMethodText.setVisibility(View.VISIBLE);
                scrollViewMethods.setVisibility(View.VISIBLE);

                Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up);
                cornellMethodButton.setVisibility(View.VISIBLE);
                cornellMethodButton.startAnimation(anim);

                outlineMethodButton.setVisibility(View.VISIBLE);
                outlineMethodButton.startAnimation(anim);

                mindMapButton.setVisibility(View.VISIBLE);
                mindMapButton.startAnimation(anim);

                Toast.makeText(this, "File uploaded!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // 📌 Helper: Get File Name
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                    result = cursor.getString(nameIndex);
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
}
