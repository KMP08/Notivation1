package com.example.notivation;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.PopupMenu;
import android.content.Intent;

import com.example.notivation.util.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MindMapActivity extends AppCompatActivity {

    private static final String TAG = "MindMapActivity";
    private static final int STORAGE_PERMISSION_CODE = 101;

    private WebView mindMapWebView;
    private String mindMapContent;
    private ProgressBar progressBar;
    private String format = "mindmap"; // Default format
    private boolean pendingSaveAsImage = false;
    private boolean pendingSaveAsPdf = false;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    private String noteId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mind_map);

        // Initialize SessionManager and Firebase Database
        sessionManager = SessionManager.getInstance(getApplicationContext());
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Set up toolbar as action bar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Configure action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("Mind Map");
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setElevation(8f); // Add shadow to make it stand out
        }

        // Set up menu directly on toolbar
        toolbar.inflateMenu(R.menu.menu_mindmap);
        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.action_save_image) {
                captureMindMapAsImage();
                return true;
            } else if (itemId == R.id.action_save_pdf) {
                captureMindMapAsPdf();
                return true;
            }
            return false;
        });

        // Get the mind map content, format, and noteId from the intent
        mindMapContent = getIntent().getStringExtra("ai_result");
        if (getIntent().hasExtra("format")) {
            format = getIntent().getStringExtra("format");
        }

        // Get the note ID from the intent or generate a new one if not provided
        if (getIntent().hasExtra("note_id")) {
            noteId = getIntent().getStringExtra("note_id");
        } else {
            noteId = UUID.randomUUID().toString();
        }

        if (mindMapContent == null || mindMapContent.isEmpty()) {
            Log.e(TAG, "No mind map content provided");
            Toast.makeText(this, "Error: No mind map content provided", Toast.LENGTH_SHORT).show();
            mindMapContent = "graph TD;\nA[Error] --> B[No content provided]";
        }

        // Initialize views
        mindMapWebView = findViewById(R.id.mindMapWebView);
        progressBar = findViewById(R.id.progressBar);


        // Set up WebView with optimized settings for mindmap display
        mindMapWebView.getSettings().setJavaScriptEnabled(true);
        mindMapWebView.getSettings().setDomStorageEnabled(true);
        mindMapWebView.getSettings().setAllowFileAccess(true);
        mindMapWebView.getSettings().setAllowContentAccess(true);
        mindMapWebView.getSettings().setBuiltInZoomControls(true);
        mindMapWebView.getSettings().setDisplayZoomControls(false);
        mindMapWebView.getSettings().setUseWideViewPort(true);
        mindMapWebView.getSettings().setLoadWithOverviewMode(true);
        mindMapWebView.getSettings().setSupportZoom(true);

        // Set WebView background to white for better visibility
        mindMapWebView.setBackgroundColor(android.graphics.Color.WHITE);

        // Set WebView clients with enhanced error handling
        mindMapWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.d(TAG, "WebView page finished loading: " + url);

                // When the page is loaded, pass the mind map content to JavaScript
                injectMindMapData();

                // Hide progress bar after a longer delay to ensure the mind map is rendered
                new android.os.Handler().postDelayed(() -> {
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    // Check if the mindmap is visible (D3.js uses SVG for visualization)
                    mindMapWebView.evaluateJavascript(
                        "(function() { " +
                        "  var svgElement = document.querySelector('#mindmap svg'); " +
                        "  return svgElement && svgElement.childNodes.length > 0 ? " +
                        "    'SVG found with size: ' + svgElement.getBoundingClientRect().width : 'No SVG found'; " +
                        "})();",
                        value -> {
                            Log.d(TAG, "Mindmap check: " + value);
                            if (value.contains("No SVG found") || value.contains("0")) {
                                // If SVG is not found or has zero width, try reinjecting the data
                                Log.d(TAG, "Mindmap not properly rendered, trying again...");
                                new android.os.Handler().postDelayed(() -> {
                                    injectMindMapData();
                                }, 500);
                            }
                        }
                    );
                }, 1500);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                Log.e(TAG, "WebView error: " + description + " (code: " + errorCode + ") for URL: " + failingUrl);
                Toast.makeText(MindMapActivity.this, "Error loading mindmap: " + description, Toast.LENGTH_SHORT).show();
            }
        });
        mindMapWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                Log.e(TAG, "WebView Console: " + consoleMessage.message() +
                      " -- From line " + consoleMessage.lineNumber() +
                      " of " + consoleMessage.sourceId());
                return true;
            }
        });

        // Add JavaScript Interface to communicate with Android
        mindMapWebView.addJavascriptInterface(new WebAppInterface(), "Android");

        // Load the D3.js force-directed mind map HTML
        mindMapWebView.loadUrl("file:///android_asset/mindmap_d3.html");

        // Set a fallback timer to check if the WebView loaded properly
        new android.os.Handler().postDelayed(() -> {
            // Check if the WebView is still loading
            if (mindMapWebView.getProgress() < 100) {
                Log.d(TAG, "WebView still loading after delay, progress: " + mindMapWebView.getProgress());
                // Try reloading
                mindMapWebView.reload();
            } else {
                Log.d(TAG, "WebView loaded, checking for mindmap content");
                // Check if the mindmap content is visible (D3.js SVG)
                mindMapWebView.evaluateJavascript(
                    "(function() { " +
                    "  var svgElement = document.querySelector('#mindmap svg'); " +
                    "  return svgElement && svgElement.childNodes.length > 0 ? 'SVG found' : 'No SVG found'; " +
                    "})();",
                    value -> {
                        Log.d(TAG, "SVG check result: " + value);
                        if (value.contains("No SVG found")) {
                            // Try injecting the data again
                            injectMindMapData();
                        }
                    }
                );
            }
        }, 2000);
    }

    // Inject the mind map data into the WebView with enhanced error handling
    private void injectMindMapData() {
        try {
            // Clean and format the mind map content
            String cleanedContent = cleanMindMapContent(mindMapContent);

            // Log the cleaned content for debugging
            Log.d(TAG, "Cleaned mind map content: " + cleanedContent);

            // Escape the content for JavaScript
            String escapedContent = cleanedContent
                    .replace("\\", "\\\\")
                    .replace("'", "\\'")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\"", "\\\"");

            // Call the JavaScript function to set the mind map data
            String javascript = "javascript:setMindMapData('" + escapedContent + "')";
            mindMapWebView.evaluateJavascript(javascript, value -> {
                Log.d(TAG, "setMindMapData result: " + value);

                // Check if the mindmap is rendered after a short delay
                new android.os.Handler().postDelayed(() -> {
                    mindMapWebView.evaluateJavascript(
                        "(function() { " +
                        "  try {" +
                        "    var svgElement = document.querySelector('#mindmap svg'); " +
                        "    if (svgElement && svgElement.childNodes.length > 0) {" +
                        "      return 'SVG found with dimensions: ' + svgElement.getBoundingClientRect().width + 'x' + svgElement.getBoundingClientRect().height;" +
                        "    } else {" +
                        "      return 'No SVG found';" +
                        "    }" +
                        "  } catch(e) {" +
                        "    return 'Error checking SVG: ' + e.message;" +
                        "  }" +
                        "})();",
                        svgCheck -> {
                            Log.d(TAG, "SVG check after injection: " + svgCheck);
                        }
                    );
                }, 1000);
            });

            // Also try the fallback method of directly setting window.mindMapData
            String fallbackJs = "javascript:window.mindMapData = '" + escapedContent + "';";
            mindMapWebView.evaluateJavascript(fallbackJs, null);

            // Force a reload of the page if needed
            if (mindMapWebView.getProgress() == 100) {
                mindMapWebView.evaluateJavascript(
                    "javascript:if(typeof renderMindMap === 'function') { renderMindMap(window.mindMapData); }",
                    null
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error injecting mind map data", e);
            Toast.makeText(this, "Error rendering mind map: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Clean and format the mind map content to ensure it's valid Mermaid.js syntax
     * @param content The raw mind map content
     * @return The cleaned and formatted content
     */
    private String cleanMindMapContent(String content) {
        if (content == null || content.isEmpty()) {
            return "graph TD;\nA((Error)) --> B((No content provided));\n" +
                   "style A fill:#ffebee,stroke:#f44336,stroke-width:4px;\n" +
                   "style B fill:#ffebee,stroke:#f44336,stroke-width:3.5px;\n" +
                   "linkStyle 0 stroke:#f44336,stroke-width:4px;";
        }

        // Extract the actual diagram code if it's wrapped in text
        String diagramCode = extractDiagramCode(content);

        // If we couldn't extract a valid diagram, create a simple one with portrait orientation
        if (diagramCode == null || diagramCode.isEmpty()) {
            return "graph TD;\nA((Main Topic)) --> B((Subtopic 1));\nA --> C((Subtopic 2));\n" +
                   "style A fill:#e3f2fd,stroke:#2196F3,stroke-width:4px;\n" +
                   "style B fill:#fff8e1,stroke:#FFC107,stroke-width:3.5px;\n" +
                   "style C fill:#fff8e1,stroke:#FFC107,stroke-width:3.5px;\n" +
                   "linkStyle 0 stroke:#2196F3,stroke-width:4px;\n" +
                   "linkStyle 1 stroke:#2196F3,stroke-width:4px;";
        }

        // Ensure the diagram starts with "graph TD;" for portrait orientation
        if (!diagramCode.trim().startsWith("graph TD;") && !diagramCode.trim().startsWith("graph TD ") &&
            !diagramCode.trim().startsWith("graph LR;") && !diagramCode.trim().startsWith("graph LR ")) {
            diagramCode = "graph TD;\n" + diagramCode;
        }

        // Convert any LR (left-right) orientation to TD (top-down) for portrait mode
        diagramCode = diagramCode.replace("graph LR;", "graph TD;")
                                .replace("graph LR ", "graph TD ");

        // Replace any double semicolons with single ones
        diagramCode = diagramCode.replace(";;", ";");

        // Ensure each line ends with a semicolon if it contains an arrow
        StringBuilder formattedCode = new StringBuilder();
        String[] lines = diagramCode.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                if (line.contains("-->") && !line.endsWith(";")) {
                    line += ";";
                }
                formattedCode.append(line).append("\n");
            }
        }

        return formattedCode.toString();
    }

    /**
     * Extract the actual diagram code from the content
     * @param content The raw content
     * @return The extracted diagram code
     */
    private String extractDiagramCode(String content) {
        // Try to find the diagram code between common markers
        String[] markers = {
            "```mermaid", "```",
            "graph TD;", "```",
            "Here is the mind map:", "",
            "Mind Map:", ""
        };

        for (int i = 0; i < markers.length; i += 2) {
            String startMarker = markers[i];
            String endMarker = markers[i + 1];

            int startIndex = content.indexOf(startMarker);
            if (startIndex != -1) {
                startIndex += startMarker.length();
                int endIndex = endMarker.isEmpty() ? content.length() : content.indexOf(endMarker, startIndex);
                if (endIndex == -1) endIndex = content.length();

                return content.substring(startIndex, endIndex).trim();
            }
        }

        // If no markers found, check if it starts with "graph TD"
        if (content.trim().startsWith("graph TD")) {
            return content.trim();
        }

        // If all else fails, just return the content
        return content;
    }

    // Interface to receive data from JavaScript
    class WebAppInterface {
        @JavascriptInterface
        public void onNodeClicked(String clickedNode) {
            // We're not showing toasts anymore when nodes are clicked
            // The D3.js implementation now handles node clicks internally
            Log.d(TAG, "Node clicked: " + clickedNode);
        }

        @JavascriptInterface
        public void onMindmapReady() {
            // Called when the mindmap is fully rendered
            Log.d(TAG, "Mindmap is ready");
        }

        @JavascriptInterface
        public void onCaptureComplete(String dimensions) {
            // Called when the mindmap dimensions are calculated for capture
            Log.d(TAG, "Capture dimensions: " + dimensions);
        }
    }

    // AI processing function (not used anymore)
    private void processNodeData(String nodeData) {
        // This method is kept for future functionality but doesn't do anything now
        Log.d(TAG, "Node data: " + nodeData);
    }

    // Save methods removed to simplify the interface

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar
        getMenuInflater().inflate(R.menu.menu_mindmap, menu);

        // Make sure the menu items are visible
        for (int i = 0; i < menu.size(); i++) {
            MenuItem item = menu.getItem(i);
            item.setVisible(true);
            item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_save_image) {
            captureMindMapAsImage();
            return true;
        } else if (itemId == R.id.action_save_pdf) {
            captureMindMapAsPdf();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Capture the WebView content as a bitmap, ensuring the entire mindmap is captured
     */
    private void captureMindMapView(final BitmapCallback callback) {
        // Show a toast to indicate capturing is in progress
        Toast.makeText(this, "Capturing mindmap...", Toast.LENGTH_SHORT).show();

        // Since we've already reset the zoom in the confirmation dialog,
        // we can directly capture the current view which should show the entire mindmap

        // Give the WebView time to fully render before capturing
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            try {
                // Get the dimensions of the WebView
                int width = mindMapWebView.getWidth();
                int height = mindMapWebView.getHeight();

                if (width <= 0 || height <= 0) {
                    callback.onError("Invalid WebView dimensions");
                    return;
                }

                // Create a bitmap with the same dimensions as the WebView
                Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);

                // Draw the WebView content onto the canvas
                mindMapWebView.draw(canvas);

                // Pass the bitmap to the callback
                callback.onBitmapReady(bitmap);

            } catch (Exception e) {
                Log.e(TAG, "Error capturing mindmap: " + e.getMessage());
                callback.onError("Failed to capture mindmap: " + e.getMessage());
            }
        }, 500); // Wait 500ms to ensure the WebView is fully rendered
    }

    // Removed unused methods since we now handle capture directly after zoom reset

    /**
     * Interface for bitmap capture callback
     */
    interface BitmapCallback {
        void onBitmapReady(Bitmap bitmap);
        void onError(String errorMessage);
    }

    /**
     * Save the mindmap as an image (PNG)
     */
    private void captureMindMapAsImage() {
        // Check if we have storage permission
        if (checkStoragePermission()) {
            // Show confirmation dialog with preview
            showCaptureConfirmationDialog("image");
        } else {
            // If we don't have permission, request it and set a flag to continue after permission is granted
            pendingSaveAsImage = true;
            pendingSaveAsPdf = false;
            requestStoragePermission();
        }
    }

    /**
     * Save the mindmap as a PDF
     */
    private void captureMindMapAsPdf() {
        // Check if we have storage permission
        if (checkStoragePermission()) {
            // Show confirmation dialog with preview
            showCaptureConfirmationDialog("pdf");
        } else {
            // If we don't have permission, request it and set a flag to continue after permission is granted
            pendingSaveAsImage = false;
            pendingSaveAsPdf = true;
            requestStoragePermission();
        }
    }

    /**
     * Show a confirmation dialog with instructions to fit the entire mindmap
     */
    private void showCaptureConfirmationDialog(String format) {
        // First, reset the zoom to show the entire mindmap
        mindMapWebView.evaluateJavascript(
            "resetZoom(); 'Zoom reset'",
            result -> {
                // Create and show the dialog
                androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
                builder.setTitle("Save Mindmap");

                // Set message based on format
                if (format.equals("image")) {
                    builder.setMessage("The mindmap has been zoomed out to show all content.\n\n" +
                                      "This is how it will appear in your saved image.\n\n" +
                                      "Ready to save?");
                } else {
                    builder.setMessage("The mindmap has been zoomed out to show all content.\n\n" +
                                      "This is how it will appear in your saved PDF.\n\n" +
                                      "Ready to save?");
                }

                // Add buttons
                builder.setPositiveButton("Save", (dialog, which) -> {
                    // Proceed with capture
                    if (format.equals("image")) {
                        captureMindMapView(new BitmapCallback() {
                            @Override
                            public void onBitmapReady(Bitmap bitmap) {
                                saveImageToStorage(bitmap);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(MindMapActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    } else {
                        captureMindMapView(new BitmapCallback() {
                            @Override
                            public void onBitmapReady(Bitmap bitmap) {
                                savePdfToStorage(bitmap);
                            }

                            @Override
                            public void onError(String errorMessage) {
                                Toast.makeText(MindMapActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });

                builder.setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                });

                // Show the dialog
                builder.create().show();
            }
        );
    }

    /**
     * Check if we have storage permission
     */
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10 and above, we don't need explicit permission for app-specific storage
            return true;
        } else {
            // For older Android versions, check for WRITE_EXTERNAL_STORAGE permission
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    /**
     * Request storage permission
     */
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                STORAGE_PERMISSION_CODE
            );
        }
    }

    /**
     * Handle permission request result
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with the pending operation
                if (pendingSaveAsImage) {
                    captureMindMapAsImage();
                } else if (pendingSaveAsPdf) {
                    captureMindMapAsPdf();
                }
            } else {
                // Permission denied
                Toast.makeText(this, "Storage permission is required to save files", Toast.LENGTH_LONG).show();
            }

            // Reset pending flags
            pendingSaveAsImage = false;
            pendingSaveAsPdf = false;
        }
    }

    /**
     * Save bitmap as image to storage
     */
    private void saveImageToStorage(Bitmap bitmap) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Mindmap_" + timestamp + ".png";
        String fileUri = "";

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Notivation");

                Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                if (imageUri != null) {
                    OutputStream outputStream = resolver.openOutputStream(imageUri);
                    if (outputStream != null) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                        outputStream.close();
                        fileUri = imageUri.toString();
                        Toast.makeText(this, "Mindmap saved as image to Pictures/Notivation", Toast.LENGTH_LONG).show();

                        // Save note metadata to Firebase and go to home screen
                        saveNoteMetadata(fileName, fileUri, "png");
                    }
                }
            } else {
                // For older Android versions
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Notivation");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File file = new File(directory, fileName);
                FileOutputStream outputStream = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outputStream.close();

                // Make the file visible in the gallery
                Uri contentUri = Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), file.getAbsolutePath(), fileName, null));
                fileUri = contentUri != null ? contentUri.toString() : "file://" + file.getAbsolutePath();

                Toast.makeText(this, "Mindmap saved as image to Pictures/Notivation", Toast.LENGTH_LONG).show();

                // Save note metadata to Firebase and go to home screen
                saveNoteMetadata(fileName, fileUri, "png");
            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            Toast.makeText(this, "Failed to save image: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Save bitmap as PDF to storage
     */
    private void savePdfToStorage(Bitmap bitmap) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "Mindmap_" + timestamp + ".pdf";
        String fileUri = "";

        try {
            // Create a new PDF document
            PdfDocument document = new PdfDocument();

            // Calculate the page size based on the bitmap dimensions
            int pageWidth = bitmap.getWidth();
            int pageHeight = bitmap.getHeight();

            // Create a page with the bitmap dimensions
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create();
            PdfDocument.Page page = document.startPage(pageInfo);

            // Draw the bitmap onto the page
            Canvas canvas = page.getCanvas();
            canvas.drawBitmap(bitmap, 0, 0, null);

            // Finish the page
            document.finishPage(page);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                ContentResolver resolver = getContentResolver();
                ContentValues contentValues = new ContentValues();
                contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/Notivation");

                Uri pdfUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues);
                if (pdfUri != null) {
                    OutputStream outputStream = resolver.openOutputStream(pdfUri);
                    if (outputStream != null) {
                        document.writeTo(outputStream);
                        outputStream.close();
                        fileUri = pdfUri.toString();
                        Toast.makeText(this, "Mindmap saved as PDF to Documents/Notivation", Toast.LENGTH_LONG).show();

                        // Save note metadata to Firebase and go to home screen
                        saveNoteMetadata(fileName, fileUri, "pdf");
                    }
                }
            } else {
                // For older Android versions
                File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "Notivation");
                if (!directory.exists()) {
                    directory.mkdirs();
                }

                File file = new File(directory, fileName);
                FileOutputStream outputStream = new FileOutputStream(file);
                document.writeTo(outputStream);
                outputStream.close();

                fileUri = "file://" + file.getAbsolutePath();
                Toast.makeText(this, "Mindmap saved as PDF to Documents/Notivation", Toast.LENGTH_LONG).show();

                // Save note metadata to Firebase and go to home screen
                saveNoteMetadata(fileName, fileUri, "pdf");
            }

            // Close the document
            document.close();

        } catch (IOException e) {
            Log.e(TAG, "Error saving PDF", e);
            Toast.makeText(this, "Failed to save PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Save note metadata to Firebase Realtime Database
     * @param filename The filename
     * @param fileUri The file URI
     * @param fileType The file type (png or pdf)
     */
    private void saveNoteMetadata(String filename, String fileUri, String fileType) {
        try {
            // Get user ID from session
            String userId = sessionManager.getUserId();
            if (userId == null) {
                Log.w(TAG, "User ID is null, using anonymous");
                userId = "anonymous";
            }

            // Create note metadata
            Map<String, Object> noteData = new HashMap<>();
            noteData.put("id", noteId);
            noteData.put("userId", userId);
            noteData.put("filename", filename);
            noteData.put("format", format);
            noteData.put("fileType", fileType);
            noteData.put("fileUrl", fileUri);
            noteData.put("timestamp", System.currentTimeMillis());

            // Save to Firebase Realtime Database
            mDatabase.child("processedNotes").child(noteId).setValue(noteData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Note metadata saved successfully");
                    goToHomeScreen(fileUri);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error saving note metadata", e);
                    // Still go to home screen since the file was saved locally
                    goToHomeScreen(fileUri);
                });
        } catch (Exception e) {
            Log.e(TAG, "Error saving note metadata", e);
            // Still go to home screen
            goToHomeScreen(fileUri);
        }
    }

    /**
     * Navigate to the home screen
     * @param fileUrl The URL of the saved file
     */
    private void goToHomeScreen(String fileUrl) {
        Intent intent = new Intent(MindMapActivity.this, HomeActivity.class);
        intent.putExtra("file_url", fileUrl);  // Pass the saved file URL to HomeActivity
        startActivity(intent);
        finish();
    }
}
