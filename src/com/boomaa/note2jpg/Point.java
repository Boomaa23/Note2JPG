package com.boomaa.note2jpg;

public class Point {
    private final int x;
    private final int y;

    public Point(float x, float y) {
        this.x = (int) x;
        this.y = (int) y;
    }

    public Point(double x, double y) {
        this.x = (int) x;
        this.y = (int) y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public String toString() {
        return "Point{" +
            "x=" + x +
            ", y=" + y +
            '}';
    }
}
