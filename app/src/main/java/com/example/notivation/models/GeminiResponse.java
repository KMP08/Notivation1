package com.example.notivation.models;

import java.util.List;

public class GeminiResponse {
    private List<Candidate> candidates;

    // Getter and Setter
    public List<Candidate> getCandidates() {
        return candidates;
    }

    public void setCandidates(List<Candidate> candidates) {
        this.candidates = candidates;
    }

    // Candidate class (inside GeminiResponse)
    public static class Candidate {
        private Content content;

        // Getter and Setter
        public Content getContent() {
            return content;
        }

        public void setContent(Content content) {
            this.content = content;
        }
    }

    // Content class (inside GeminiResponse)
    public static class Content {
        private List<Part> parts;

        // Getter and Setter
        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    // Part class (inside GeminiResponse)
    public static class Part {
        private String text;

        // Constructor
        public Part(String text) {
            this.text = text;
        }

        // Getter and Setter
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
