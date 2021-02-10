package com.boomaa.note2jpg.create;

import java.awt.Color;

public class Shape {
    protected final Type type;
    protected final Point begin;
    protected final Point end;
    protected final Color strokeColor;
    protected final double width;
    protected final Color fillColor;

    public Shape(Type type, Color strokeColor, double width,
                 Color fillColor, Point begin, Point end) {
        this.type = type;
        this.fillColor = fillColor;
        this.begin = begin;
        this.end = end;
        this.strokeColor = strokeColor;
        this.width = width;
    }

    public Type getType() {
        return type;
    }

    public Color getStrokeColor() {
        return strokeColor;
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

        public NPolygon(Color strokeColor, double width, Color fillColor, int[] xPts, int[] yPts) {
            super(Type.NPOLYGON, strokeColor, width, fillColor, new Point(xPts[0], yPts[0]),
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
