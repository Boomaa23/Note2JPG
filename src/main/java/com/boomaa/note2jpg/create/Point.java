package com.boomaa.note2jpg.create;

public class Point {
    private double x;
    private double y;

    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Point(java.awt.Point point) {
        this(point.getX(), point.getY());
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public int getXInt() {
        return (int) x;
    }

    public int getYInt() {
        return (int) y;
    }

    public double getXDbl() {
        return x;
    }

    public double getYDbl() {
        return y;
    }

    public Point add(Point other) {
        return new Point(this.x + other.x,
                this.y + other.y);
    }

    public double distance(Point end) {
        double x = end.getXDbl() - this.getXDbl();
        double y = end.getYDbl() - this.getYDbl();
        return Math.sqrt((x * x) + (y * y));
    }

    public double magnitude() {
        return Math.sqrt(x * x + y * y);
    }

    @Override
    public String toString() {
        return "Point{" +
            "x=" + x +
            ", y=" + y +
            '}';
    }
}
