package com.boomaa.note2jpg.create;

public class Point {
    private int x;
    private int y;

    public Point(float x, float y) {
        this.x = (int) x;
        this.y = (int) y;
    }

    public Point(double x, double y) {
        this((int) x, (int) y);
    }

    public Point(java.awt.Point point) {
        this(point.getX(), point.getY());
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    // Using multiply operation instead of Math.pow
    // because it's faster with powers of two
    public double distance(Point end) {
        double x = end.getX() - this.getX();
        double y = end.getY() - this.getY();
        return Math.sqrt((x * x) + (y * y));
    }

    @Override
    public String toString() {
        return "Point{" +
            "x=" + x +
            ", y=" + y +
            '}';
    }
}
