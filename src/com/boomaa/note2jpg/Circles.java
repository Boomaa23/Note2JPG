package com.boomaa.note2jpg;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Circles extends JPanel {
    private final Curve[] curves;

    public Circles(Curve[] curves) {
        this.curves = curves;
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
            for (int j = 0;j < points.length;j++) {
                g2.setStroke(new BasicStroke((float) curves[i].getWidth()));
                g2.drawLine(lastPoint.getX(), lastPoint.getY(), points[j].getX(), points[j].getY());
                g2.fillOval(points[j].getX(), points[j].getY(), (int) (curves[i].getWidth() / 2), (int) (curves[i].getWidth() / 2));
                lastPoint = points[j];
            }
        }
    }
}
