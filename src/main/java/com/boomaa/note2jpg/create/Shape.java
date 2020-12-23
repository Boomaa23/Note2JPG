package com.boomaa.note2jpg.create;

import java.awt.Color;

public class Shape {
    protected final Type type;
    protected final Point begin;
    protected final Point end;
    protected final Color color;
    protected final double width;
    protected final Color fillColor;

    public Shape(Type type, Color color, double width,
                 Color fillColor, Point begin, Point end) {
        this.type = type;
        this.fillColor = fillColor;
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

    public Color getFillColor() {
        return fillColor;
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
        protected final int[] xPts;
        protected final int[] yPts;

        public NPolygon(Color color, double width, Color fillColor, int[] xPts, int[] yPts) {
            super(Type.NPOLYGON, color, width, fillColor, new Point(xPts[0], yPts[0]),
                    new Point(xPts[xPts.length - 1], yPts[yPts.length - 1]));
            this.xPts = xPts;
            this.yPts = yPts;
        }

        public int[] getXPoints() {
            return xPts;
        }

        public int[] getYPoints() {
            return yPts;
        }
    }
}
