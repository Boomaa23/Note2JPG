package com.boomaa.note2jpg.create;

import javax.swing.*;
import java.awt.*;

public class DrawRenderer extends JPanel {
    private final Curve[] curves;
    private final Shape[] shapes;

    public DrawRenderer(Curve[] curves, Shape[] shapes) {
        this.curves = curves;
        this.shapes = shapes;
        setBackground(new Color(0, 0, 0, 0));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        for (int i = 0; i < curves.length; i++) {
            g2.setColor(curves[i].getColor());
            Point[] points = curves[i].getPoints();
            g2.setStroke(new BasicStroke((float) curves[i].getWidth(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int curveRadius = (int) (curves[i].getWidth() / 2);

            System.out.print("\r" + "Curve: " + (i + 1) + " / " + curves.length);
            int[] xPts = new int[points.length];
            int[] yPts = new int[points.length];
            for (int j = 0; j < points.length; j++) {
                xPts[j] = points[j].getXInt();
                yPts[j] = points[j].getYInt();
                if (curves[i].getColor().getAlpha() == 255) {
                    g2.fillOval(points[j].getXInt(), points[j].getYInt(), curveRadius, curveRadius);
                }
            }
            g2.drawPolyline(xPts, yPts, points.length);
        }
        System.out.println(curves.length == 0 ? "Curve: None" : "");

        for (int i = 0; i < shapes.length; i++) {
            g2.setColor(shapes[i].getStrokeColor());
            g2.setStroke(new BasicStroke((float) shapes[i].getWidth()));
            Point begin = shapes[i].getBeginPoint();
            Point end = shapes[i].getEndPoint();
            Shape.Type type = shapes[i].getType();
            if (type == Shape.Type.LINE) {
                g2.drawLine(begin.getXInt(), begin.getYInt(), end.getXInt(), end.getYInt());
            } else if (type == Shape.Type.CIRCLE) {
                if (shapes[i].getFillColor() != null) {
                    g2.setColor(shapes[i].getFillColor());
                    g2.fillOval(begin.getXInt(), begin.getYInt(), end.getXInt() - begin.getXInt(), end.getYInt() - begin.getYInt());
                }
                g2.setColor(shapes[i].getStrokeColor());
                g2.drawOval(begin.getXInt(), begin.getYInt(), end.getXInt() - begin.getXInt(), end.getYInt() - begin.getYInt());
            } else if (type == Shape.Type.NPOLYGON && shapes[i] instanceof Shape.NPolygon) {
                Shape.NPolygon poly = (Shape.NPolygon) shapes[i];
                if (poly.getFillColor() != null) {
                    g2.setColor(poly.getFillColor());
                    g2.fillPolygon(poly.getXPoints(), poly.getYPoints(), poly.getXPoints().length);
                }
                g2.setColor(poly.getStrokeColor());
                g2.drawPolygon(poly.getXPoints(), poly.getYPoints(), poly.getXPoints().length);
            }
            System.out.print("\r" + "Shape: " + (i + 1) + " / " + shapes.length);
        }
        System.out.println((shapes.length == 0 ? "Shape: None" : "") + "\n");
        g2.dispose();
    }
}
