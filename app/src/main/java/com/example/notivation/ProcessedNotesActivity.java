package com.example.notivation;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ProcessedNotesActivity extends AppCompatActivity {

    TextView processedTextView;
    Button btnEdit, btnSavePDF, btnSaveDocx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_processed_note);

        processedTextView = findViewById(R.id.processedTextView);
        btnEdit = findViewById(R.id.btnEdit);
        btnSavePDF = findViewById(R.id.btnSavePDF);
        btnSaveDocx = findViewById(R.id.btnSaveDocx);

        // Get the processed text passed from the LoadingActivity
        String processedText = getIntent().getStringExtra("processed_text");

        if (processedText != null) {
            processedTextView.setText(processedText); // Set the AI processed content in TextView
        } else {
            Toast.makeText(this, "No processed text available", Toast.LENGTH_SHORT).show();
        }

        btnEdit.setOnClickListener(v -> {
            Toast.makeText(this, "Edit button clicked", Toast.LENGTH_SHORT).show();
            // Open editable screen (future feature)
        });

        btnSavePDF.setOnClickListener(v -> {
            String text = processedTextView.getText().toString();
            saveAsPdf(text);
        });

        btnSaveDocx.setOnClickListener(v -> {
            String text = processedTextView.getText().toString();
            saveAsDocx(text);
        });
    }

    private void saveAsPdf(String content) {
        // PDF saving logic
    }

    private void saveAsDocx(String content) {
        // DOCX saving logic
    }
}
