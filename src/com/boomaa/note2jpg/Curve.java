package com.boomaa.note2jpg;

import java.awt.Color;

public class Curve {
    private final Point[] points;
    private final Color color;
    private final int width;

    public Curve(Point[] points, Color color, int width) {
        this.points = points;
        this.color = color;
        this.width = width;
    }

    public Point[] getPoints() {
        return points;
    }

    public Color getColor() {
        return color;
    }

    public int getWidth() {
        return width;
    }

    @Override
    public String toString() {
        return "Curve{" +
            "numPoints=" + points.length +
            ", color=" + color +
            ", width=" + width +
            '}';
    }
}
