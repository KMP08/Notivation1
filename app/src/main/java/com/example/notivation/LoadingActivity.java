package com.example.notivation;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;
import com.example.notivation.api.GeminiApiService;
import com.example.notivation.api.RetrofitClient;
import com.example.notivation.models.GeminiRequest;
import com.example.notivation.models.GeminiResponse;

import java.io.IOException;
import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoadingActivity extends AppCompatActivity {

    private static final String TAG = "Gemini_API";
    private static final String API_KEY = "Bearer AIzaSyARuz_vuSKXDNSfoISvAg_Of0BrZeivfvI"; // 🔐 Replace with your actual API key
    LottieAnimationView loadingAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading);

        loadingAnimation = findViewById(R.id.loading_animation);

        // Get user selection and notes
        String method = getIntent().getStringExtra("method");    // Cornell, Outline, Mind Map
        String rawNote = getIntent().getStringExtra("raw_note"); // File text or typed note

        // ✨ Custom Gemini prompt
        String prompt = "Convert the following notes into a " + method + " format:\n\n" + rawNote;

        GeminiRequest.Content content = new GeminiRequest.Content(Collections.singletonList(
                new GeminiRequest.Part(prompt)
        ));
        GeminiRequest request = new GeminiRequest(Collections.singletonList(content));

        GeminiApiService service = RetrofitClient.getGeminiClient().create(GeminiApiService.class);
        Call<GeminiResponse> call = service.generateContent(API_KEY, request);

        call.enqueue(new Callback<GeminiResponse>() {
            @Override
            public void onResponse(Call<GeminiResponse> call, Response<GeminiResponse> response) {
                if (response.isSuccessful() && response.body() != null &&
                        response.body().getCandidates() != null && !response.body().getCandidates().isEmpty()) {

                    String processedText = response.body().getCandidates().get(0).getContent().getParts().get(0).getText();

                    // Go to ProcessedNoteActivity
                    Intent intent = new Intent(LoadingActivity.this, ProcessedNotesActivity.class);
                    intent.putExtra("processed_text", processedText);
                    startActivity(intent);
                    finish();
                } else {
                    try {
                        String errorMsg = response.errorBody() != null ? response.errorBody().string() : "Unknown error";
                        Log.e(TAG, "Gemini API Error - Code: " + response.code() + ", Body: " + errorMsg);
                    } catch (IOException e) {
                        Log.e(TAG, "Error parsing error body", e);
                    }
                    Toast.makeText(LoadingActivity.this, "Gemini API Error: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<GeminiResponse> call, Throwable t) {
                Log.e(TAG, "Gemini API Failure: " + t.getMessage());
                Toast.makeText(LoadingActivity.this, "Connection failed: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
