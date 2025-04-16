package com.example.notivation.api;

import com.example.notivation.models.GeminiRequest;
import com.example.notivation.models.GeminiResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface GeminiApiService {

    @POST("https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent")
    Call<GeminiResponse> generateContent(
            @Header("Authorization") String authHeader, // Make sure token is correct
            @Body GeminiRequest request
    );
}
