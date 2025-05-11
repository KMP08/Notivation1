package com.example.notivation;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;
import java.util.ArrayList;

public class MindMapView extends View {

    private Paint paint;
    private ArrayList<Node> nodes;

    public MindMapView(Context context) {
        super(context);
        paint = new Paint();
        nodes = new ArrayList<>();
        // Add default nodes here (you can dynamically add them later)
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw nodes
        for (Node node : nodes) {
            paint.setColor(Color.BLUE);  // Color for nodes
            canvas.drawCircle(node.x, node.y, 50, paint);  // Draw node circle
            paint.setColor(Color.WHITE);  // Text color
            paint.setTextSize(30);  // Font size
            canvas.drawText(node.text, node.x - 20, node.y + 10, paint);  // Draw node text
        }

        // Draw edges (connections between nodes)
        paint.setColor(Color.BLACK);  // Color for edges
        for (Node node : nodes) {
            for (Node child : node.children) {
                canvas.drawLine(node.x, node.y, child.x, child.y, paint);  // Draw line between nodes
            }
        }
    }

    // Method to add a node to the mind map
    public void addNode(String text, float x, float y) {
        Node newNode = new Node(text, x, y);
        nodes.add(newNode);
    }

    // Method to connect two nodes
    public void connectNodes(Node parent, Node child) {
        parent.addChild(child);
    }

    // Helper class to represent a Node in the mind map
    public static class Node {
        String text;
        float x, y;
        ArrayList<Node> children;

        Node(String text, float x, float y) {
            this.text = text;
            this.x = x;
            this.y = y;
            children = new ArrayList<>();
        }

        // Method to add a child node to this node
        public void addChild(Node child) {
            children.add(child);
        }
    }
}
