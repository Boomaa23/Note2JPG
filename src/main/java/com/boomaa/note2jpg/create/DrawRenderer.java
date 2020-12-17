package com.boomaa.note2jpg.create;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DrawRenderer extends JPanel {
    private final Curve[] curves;
    private final Shape[] shapes;
    private final BufferedImage pdfs;

    public DrawRenderer(Curve[] curves, Shape[] shapes, BufferedImage pdfs) {
        this.curves = curves;
        this.shapes = shapes;
        this.pdfs = pdfs;
        this.setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g2.drawImage(pdfs, 0, 0, null);

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
            g2.setColor(shapes[i].getColor());
            g2.setStroke(new BasicStroke((float) shapes[i].getWidth()));
            Point begin = shapes[i].getBeginPoint();
            Point end = shapes[i].getEndPoint();
            Shape.Type type = shapes[i].getType();
            if (type == Shape.Type.LINE) {
                g2.drawLine(begin.getXInt(), begin.getYInt(), end.getXInt(), end.getYInt());
            } else if (type == Shape.Type.CIRCLE) {
                g2.drawOval(begin.getXInt(), begin.getYInt(), end.getXInt() - begin.getXInt(), end.getYInt() - begin.getYInt());
            } else if (type == Shape.Type.NPOLYGON && shapes[i] instanceof Shape.NPolygon) {
                Shape.NPolygon poly = (Shape.NPolygon) shapes[i];
                Point[] polyPoints = poly.getPoints();
                Point lastPoint = polyPoints[0];
                int curveRadius = (int) (poly.getWidth() / 2);
                for (Point drawPoint : polyPoints) {
                    g2.drawLine(lastPoint.getXInt(), lastPoint.getYInt(), drawPoint.getXInt(), drawPoint.getYInt());
                    g2.fillOval(drawPoint.getXInt(), drawPoint.getYInt(), curveRadius, curveRadius);
                    lastPoint = drawPoint;
                }
                if (poly.isClosed()) {
                    g2.drawLine(polyPoints[0].getXInt(), polyPoints[0].getYInt(), lastPoint.getXInt(), lastPoint.getYInt());
                }
            }
            System.out.print("\r" + "Shape: " + (i + 1) + " / " + shapes.length);
        }
        System.out.println(shapes.length == 0 ? "Shape: None" : "");
        g2.dispose();
    }
}
