package com.example.notivation;

import android.Manifest;
import android.content.Intent;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notivation.util.SessionManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class UploadActivity extends AppCompatActivity {

    private static final String TAG = "UploadActivity";
    private static final int PICK_FILE_REQUEST = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    private LottieAnimationView imageUploadIcon;
    private ImageView filePreviewIcon;
    private TextView fileNameText, methodLabel;
    private LinearLayout methodButtonsLayout;
    private LinearLayout fileUploadedLayout;
    private LottieAnimationView loadingAnimation;
    private StorageReference storageReference;
    private SessionManager sessionManager;

    // ✅ Global Uri to pass to AIProcessingActivity
    private Uri selectedFileUri;

    // Activity result launcher for document picker
    private final ActivityResultLauncher<Intent> documentPickerLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    handleSelectedFile(uri);
                }
            }
        });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload2);  // Use your actual layout file name

        // Initialize views
        imageUploadIcon = findViewById(R.id.imageUploadIcon);
        filePreviewIcon = findViewById(R.id.filePreviewIcon);
        fileNameText = findViewById(R.id.textSuccess);
        methodLabel = findViewById(R.id.methodLabel);
        methodButtonsLayout = findViewById(R.id.methodButtonsLayout);
        fileUploadedLayout = findViewById(R.id.fileUploadedLayout);
        loadingAnimation = findViewById(R.id.loadingAnimation);

        // Initialize Firebase Storage and SessionManager
        storageReference = FirebaseStorage.getInstance().getReference();
        sessionManager = SessionManager.getInstance(getApplicationContext());

        // Upload icon click opens file picker
        imageUploadIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check and request permissions before opening file picker
                if (checkAndRequestPermissions()) {
                    openDocumentPicker();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (fileUri != null) {
                // Use the common handler method
                handleSelectedFile(fileUri);
            }
        }
    }

    /**
     * Get the file name from a URI
     * @param uri The URI
     * @return The file name
     */
    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadFileToFirebase(Uri fileUri) {
        // Show loading animation
        loadingAnimation.setVisibility(View.VISIBLE);

        // Get user ID from session manager
        String userId = sessionManager.getUserId();
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "User ID is null or empty");
            Toast.makeText(this, "Error: User not logged in properly", Toast.LENGTH_LONG).show();
            loadingAnimation.setVisibility(View.GONE);
            return;
        }

        // Log user ID for debugging
        Log.d(TAG, "Uploading file for user ID: " + userId);

        // Create unique file name with user ID in path
        StorageReference fileReference = storageReference.child("users/" + userId + "/uploads/" + System.currentTimeMillis());

        fileReference.putFile(fileUri)
                .addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        loadingAnimation.setVisibility(View.GONE);
                        filePreviewIcon.setVisibility(View.VISIBLE);

                        // Show success message
                        Toast.makeText(UploadActivity.this, "File uploaded successfully!", Toast.LENGTH_SHORT).show();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                fileUploadedLayout.setVisibility(View.VISIBLE);
                                fadeInMethodSelection();

                                // Set up click listeners for format buttons
                                setupFormatButtons();
                            }
                        }, 300);
                    }
                })
                .addOnFailureListener(e -> {
                    loadingAnimation.setVisibility(View.GONE);

                    // Log the error for debugging
                    Log.e(TAG, "Firebase upload failed", e);

                    // Show a more user-friendly error message
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("permission")) {
                        // This is a permission error
                        Log.d(TAG, "Firebase permission issue, proceeding with local file");

                        // Skip Firebase upload and proceed with local file silently
                        // No toast message to avoid confusing the user since we can still proceed
                        filePreviewIcon.setVisibility(View.VISIBLE);
                        fileUploadedLayout.setVisibility(View.VISIBLE);
                        fadeInMethodSelection();

                        // Set up click listeners for format buttons
                        setupFormatButtons();
                    } else {
                        // Other error that might affect functionality
                        Toast.makeText(UploadActivity.this,
                            "Cloud backup failed: " + e.getMessage() + "\nContinuing with local file.",
                            Toast.LENGTH_LONG).show();

                        // Still proceed with local file
                        filePreviewIcon.setVisibility(View.VISIBLE);
                        fileUploadedLayout.setVisibility(View.VISIBLE);
                        fadeInMethodSelection();
                        setupFormatButtons();
                    }
                });
    }

    private void fadeInMethodSelection() {
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        methodLabel.setVisibility(View.VISIBLE);
        methodButtonsLayout.setVisibility(View.VISIBLE);
        methodLabel.startAnimation(fadeIn);
        methodButtonsLayout.startAnimation(fadeIn);
    }

    /**
     * Set up click listeners for format buttons
     */
    private void setupFormatButtons() {
        findViewById(R.id.buttonCornell).setOnClickListener(v -> {
            Intent intent = new Intent(UploadActivity.this, AIProcessingActivity.class);
            intent.putExtra("format", "cornell");
            intent.putExtra("fileUri", selectedFileUri.toString());
            startActivity(intent);
        });

        findViewById(R.id.buttonOutline).setOnClickListener(v -> {
            Intent intent = new Intent(UploadActivity.this, AIProcessingActivity.class);
            intent.putExtra("format", "outline");
            intent.putExtra("fileUri", selectedFileUri.toString());
            startActivity(intent);
        });

        findViewById(R.id.buttonMindMap).setOnClickListener(v -> {
            Intent intent = new Intent(UploadActivity.this, AIProcessingActivity.class);
            intent.putExtra("format", "mindmap");
            intent.putExtra("fileUri", selectedFileUri.toString());
            startActivity(intent);
        });
    }

    /**
     * Check if we have the necessary permissions and request them if not
     * @return true if we have permissions, false if we need to request them
     */
    private boolean checkAndRequestPermissions() {
        // For Android 13+ (API 33+), we don't need to request permissions for document picker
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // No runtime permissions needed for document picker in Android 13+
            // The system picker handles permissions internally
            return true;
        } else {
            // For older Android versions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Permission not granted, request it
                Log.d(TAG, "READ_EXTERNAL_STORAGE permission not granted, requesting it");
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE);
                return false;
            }
        }

        // We have the necessary permissions
        return true;
    }

    /**
     * Open document picker using the new ActivityResultLauncher API
     */
    private void openDocumentPicker() {
        try {
            // Create intent for document picker
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);

            // Define MIME types for PDF and DOCX
            String[] mimeTypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            // Add flags to grant persistable URI permissions
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                           Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                           Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

            // Launch document picker
            documentPickerLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error opening document picker", e);
            Toast.makeText(this, "Error opening document picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();

            // Fallback to old method if the launcher fails
            openDocumentPickerFallback();
        }
    }

    /**
     * Fallback method to open document picker using the old startActivityForResult API
     */
    private void openDocumentPickerFallback() {
        try {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            String[] mimeTypes = {"application/pdf", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"};
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION |
                           Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                           Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            startActivityForResult(Intent.createChooser(intent, "Choose a PDF or DOCX file"), PICK_FILE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "Error opening document picker fallback", e);
            Toast.makeText(this, "Could not open file picker: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Handle the selected file from either picker method
     */
    private void handleSelectedFile(Uri fileUri) {
        if (fileUri != null) {
            selectedFileUri = fileUri;

            // Take persistable URI permission to maintain access across app restarts
            try {
                // Get current flags
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;

                // Take persistable URI permission
                getContentResolver().takePersistableUriPermission(fileUri, takeFlags);
                Log.d(TAG, "Took persistable URI permission for: " + fileUri);

                // Verify we have the permission
                boolean hasPermission = false;
                for (UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
                    if (permission.getUri().equals(fileUri)) {
                        hasPermission = true;
                        Log.d(TAG, "Verified URI permission: " + permission.getUri() +
                              ", Read: " + permission.isReadPermission() +
                              ", Write: " + permission.isWritePermission());
                        break;
                    }
                }

                if (!hasPermission) {
                    Log.w(TAG, "Could not verify URI permission after taking it");
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to take persistable URI permission", e);
                Toast.makeText(this, "Warning: File access may be limited. Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                // Continue anyway as we might still have access for this session
            }

            // Get the original filename
            String originalFilename = getFileName(fileUri);

            // Get file extension
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
            }

            // Verify it's a PDF or DOCX file
            if (!fileExtension.equals("pdf") && !fileExtension.equals("docx")) {
                Toast.makeText(this, "Please select a PDF or DOCX file", Toast.LENGTH_SHORT).show();
                return;
            }

            // Store the original filename in SessionManager
            if (originalFilename != null && !originalFilename.isEmpty()) {
                sessionManager.setOriginalFilename(originalFilename);
            }

            // Show file type in UI
            if (fileNameText != null) {
                fileNameText.setText(originalFilename);
            }

            // Set appropriate icon based on file type
            if (filePreviewIcon != null) {
                if (fileExtension.equals("pdf")) {
                    filePreviewIcon.setImageResource(R.drawable.ic_pdf);
                } else if (fileExtension.equals("docx")) {
                    filePreviewIcon.setImageResource(R.drawable.ic_file);
                }
            }

            uploadFileToFirebase(fileUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, open document picker
                Log.d(TAG, "Permission granted, opening document picker");
                openDocumentPicker();
            } else {
                // Permission denied
                Log.d(TAG, "Permission denied");

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])) {
                    // User denied permission but didn't check "Don't ask again"
                    new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("This permission is needed to select documents from your device.")
                        .setPositiveButton("Try Again", (dialog, which) -> {
                            // Request permission again
                            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
                        })
                        .setNegativeButton("Cancel", (dialog, which) ->
                            Toast.makeText(this, "Permission denied. Cannot select documents.", Toast.LENGTH_SHORT).show())
                        .create()
                        .show();
                } else {
                    // User denied permission and checked "Don't ask again"
                    new AlertDialog.Builder(this)
                        .setTitle("Permission Required")
                        .setMessage("Permission is permanently denied. Please enable it in app settings.")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            // Open app settings
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            Uri uri = Uri.fromParts("package", getPackageName(), null);
                            intent.setData(uri);
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                        .show();
                }
            }
        }
    }
}
