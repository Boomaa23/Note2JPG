package com.boomaa.note2jpg.create;

import java.awt.Color;

public class Shape {
    protected final Type type;
    protected final Point begin;
    protected final Point end;
    protected final Color color;
    protected final double width;

    public Shape(Type type, Color color, double width, Point begin, Point end) {
        this.type = type;
        this.begin = begin;
        this.end = end;
        this.color = color;
        this.width = width;
    }

    public Type getType() {
        return type;
    }

    public Color getColor() {
        return color;
    }

    public double getWidth() {
        return width;
    }

    public Point getBeginPoint() {
        return begin;
    }

    public Point getEndPoint() {
        return end;
    }

    public enum Type {
        LINE, NPOLYGON, CIRCLE
    }

    public static class NPolygon extends Shape {
        private final boolean closed;
        private final Point[] points;

        public NPolygon(Color color, double width, boolean closed, Point... points) {
            super(Type.NPOLYGON, color, width, points[0], points[points.length - 1]);
            this.closed = closed;
            this.points = points;
        }

        public boolean isClosed() {
            return closed;
        }

        public Point[] getPoints() {
            return points;
        }
    }
}
