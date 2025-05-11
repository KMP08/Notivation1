package com.example.notivation;

import java.util.List;

public class MindMapNode {
    public String title;
    public List<MindMapNode> children;

    public MindMapNode(String title, List<MindMapNode> children) {
        this.title = title;
        this.children = children;
    }
}

