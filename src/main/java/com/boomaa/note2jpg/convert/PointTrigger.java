package com.boomaa.note2jpg.convert;

import com.boomaa.note2jpg.create.Point;

import javax.swing.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import static com.boomaa.note2jpg.convert.NFields.*;

public class PointTrigger extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
        double frameWidth = frame.getContentPane().getBounds().getWidth();
        double x = e.getX() * (scaledWidth / frameWidth);
        x -= (frameWidth - displayedWidth) / 2;
        double y = (((JScrollPane) frame.getContentPane()).getVerticalScrollBar().getModel().getValue() + e.getY()) * (originalHeight / displayedHeight);
        Point p = new Point(x, y);

        if (imageBounds.size() < imageList.size()) {
            imageBounds.add(p);
        }
    }
}
