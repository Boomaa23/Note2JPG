package com.boomaa.note2jpg;

import javax.swing.JPanel;
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

        for (int i = 0;i < curves.length;i++) {
            g2.setColor(curves[i].getColor());
            Point[] points = curves[i].getPoints();
            for (int j = 0;j < points.length;j++) {
                g.fillOval(points[j].getX() * 2, points[j].getY() * 2, curves[i].getWidth() * 2, curves[i].getWidth() * 2);
            }
        }
    }
}
