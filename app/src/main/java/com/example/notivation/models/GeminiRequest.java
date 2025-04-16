package com.example.notivation.models;

import java.util.List;

public class GeminiRequest {
    private List<Content> content;

    // Constructor
    public GeminiRequest(List<Content> content) {
        this.content = content;
    }

    // Getter and Setter
    public List<Content> getContent() {
        return content;
    }

    public void setContent(List<Content> content) {
        this.content = content;
    }

    // Content class
    public static class Content {
        private List<Part> parts;

        // Constructor
        public Content(List<Part> parts) {
            this.parts = parts;
        }

        // Getter and Setter
        public List<Part> getParts() {
            return parts;
        }

        public void setParts(List<Part> parts) {
            this.parts = parts;
        }
    }

    // Part class
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
