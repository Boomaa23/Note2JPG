package com.boomaa.note2jpg.create;

import javax.swing.*;
import java.awt.*;

public class Circles extends JPanel {
    private final Curve[] curves;
    private final Shape[] shapes;

    public Circles(Curve[] curves, Shape[] shapes) {
        this.curves = curves;
        this.shapes = shapes;
        this.setBackground(Color.WHITE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        for (int i = 0;i < curves.length;i++) {
            g2.setColor(curves[i].getColor());
            Point[] points = curves[i].getPoints();
            Point lastPoint = points[0];

            System.out.print("\r" + "Curve: " + (i + 1) + " / " + curves.length);
            for (int j = 0;j < points.length;j++) {
                g2.setStroke(new BasicStroke((float) curves[i].getWidth()));
                g2.drawLine(lastPoint.getX(), lastPoint.getY(), points[j].getX(), points[j].getY());
                g2.fillOval(points[j].getX(), points[j].getY(), (int) (curves[i].getWidth() / 2), (int) (curves[i].getWidth() / 2));
                lastPoint = points[j];
            }
        }
        System.out.println(curves.length == 0 ? "Curve: None" : "");

        for (int i = 0;i < shapes.length;i++) {
            g2.setColor(shapes[i].getColor());
            g2.setStroke(new BasicStroke((float) shapes[i].getWidth()));
            Point start = shapes[i].getStartPoint();
            Point end = shapes[i].getEndPoint();
            g2.drawLine(start.getX(), start.getY(), end.getX(), end.getY());
            System.out.print("\r" + "Shape: " + (i + 1) + " / " + shapes.length);
        }
        System.out.println(shapes.length == 0 ? "Shape: None" : "");
        g2.dispose();
    }
}
