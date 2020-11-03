package com.boomaa.note2jpg.create;

import java.awt.Color;

public class Shape {
    private final Point start;
    private final Point end;
    private final Color color;
    private final double width;

    public Shape(Point start, Point end, Color color, double width) {
        this.start = start;
        this.end = end;
        this.color = color;
        this.width = width;
    }

    public Point getStartPoint() {
        return start;
    }

    public Point getEndPoint() {
        return end;
    }

    public Color getColor() {
        return color;
    }

    public double getWidth() {
        return width;
    }
}
