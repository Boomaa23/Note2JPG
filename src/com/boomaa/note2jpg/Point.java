package com.boomaa.note2jpg;

public class Point {
    private final int x;
    private final int y;
    private final int num;

    public Point(float x, float y, int num) {
        this.x = (int) x;
        this.y = (int) y;
        this.num = num;
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
            ", num=" + num +
            '}';
    }
}
