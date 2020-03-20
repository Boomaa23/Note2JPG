package com.boomaa.note2jpg.function;

import static com.boomaa.note2jpg.function.NFields.displayedHeight;
import static com.boomaa.note2jpg.function.NFields.frame;
import static com.boomaa.note2jpg.function.NFields.originalHeight;
import static com.boomaa.note2jpg.function.NFields.scaledWidth;
import static com.boomaa.note2jpg.function.NFields.tbClicked;
import static com.boomaa.note2jpg.function.NFields.textBoxPoints;
import static com.boomaa.note2jpg.function.NFields.textBoxes;

import com.boomaa.note2jpg.create.Point;

import javax.swing.JScrollPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PointTrigger extends MouseAdapter {
    @Override
    public void mouseClicked(MouseEvent e) {
        int x = scaledWidth * e.getX() / frame.getContentPane().getWidth();
        double y = (((JScrollPane) frame.getContentPane()).getVerticalScrollBar().getModel().getValue() + e.getY()) * (originalHeight / displayedHeight);
        Point p = new Point(x, y);
        textBoxPoints.add(p);
        tbClicked++;
        if (textBoxes.size() > tbClicked) {
            System.out.print("\rPositioning: " + textBoxes.get(tbClicked) + " (" + (tbClicked + 1) + " / " + textBoxes.size() + ")");
        } else {
            System.out.println();
        }
    }
}
