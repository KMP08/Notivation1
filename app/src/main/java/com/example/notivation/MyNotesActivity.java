package com.example.notivation;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;

public class MyNotesActivity extends AppCompatActivity {

    ListView notesListView;
    // Declare back button
    ArrayList<String> noteTitles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_notes);

        notesListView = findViewById(R.id.notesListView);


        // Back Button Logic

        // Temporary dummy notes
        noteTitles = new ArrayList<>();
        noteTitles.add("Cornell - Lecture 1");
        noteTitles.add("Outline - Reading Notes");
        noteTitles.add("Mind Map - Chapter 3");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                noteTitles
        );

        notesListView.setAdapter(adapter);
    }
}
