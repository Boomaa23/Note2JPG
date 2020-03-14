package com.boomaa.note2jpg;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class Circles extends JPanel {
    private final Point[] points;

    public Circles(Point[] points) {
        this.points = points;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
        for (Point point : points) {
            g2.fillOval(point.getX(), point.getY(), 5, 5);
        }
    }
}
