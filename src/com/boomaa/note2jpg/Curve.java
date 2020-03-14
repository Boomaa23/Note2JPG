package com.boomaa.note2jpg;

import java.awt.Color;

public class Curve {
    private final Point[] points;
    private final Color color;
    private final double width;

    public Curve(Point[] points, Color color, double width) {
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

    public double getWidth() {
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
