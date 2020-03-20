package com.boomaa.note2jpg.function;

import static com.boomaa.note2jpg.function.NFields.displayedHeight;
import static com.boomaa.note2jpg.function.NFields.displayedWidth;
import static com.boomaa.note2jpg.function.NFields.frame;
import static com.boomaa.note2jpg.function.NFields.originalHeight;
import static com.boomaa.note2jpg.function.NFields.scaledWidth;
import static com.boomaa.note2jpg.function.NFields.textBoxBounds;
import static com.boomaa.note2jpg.function.NFields.textBoxContents;

import com.boomaa.note2jpg.create.Box;
import com.boomaa.note2jpg.create.Corner;
import com.boomaa.note2jpg.create.Point;

import javax.swing.JScrollPane;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class PointTrigger extends MouseAdapter {
    public static int tbClicked = 0;
    public static Corner selectState = Corner.UPPER_LEFT;

    @Override
    public void mouseClicked(MouseEvent e) {
        double frameWidth = frame.getContentPane().getBounds().getWidth();
        double x = e.getX() * (scaledWidth / frameWidth);
        x -= (frameWidth - displayedWidth) / 2;
        double y = (((JScrollPane) frame.getContentPane()).getVerticalScrollBar().getModel().getValue() + e.getY()) * (originalHeight / displayedHeight);
        Point p = new Point(x, y);

        if (textBoxBounds.size() != textBoxContents.size()
            || textBoxBounds.get(textBoxBounds.size() - 1).getCorner(Corner.BOTTOM_RIGHT) == null) {
            switch (selectState) {
                case UPPER_LEFT:
                    textBoxBounds.add(new Box(p));
                    selectState = selectState.opposite();
                    break;
                case BOTTOM_RIGHT:
                    if (textBoxBounds.get(textBoxBounds.size() - 1).setCorner(Corner.BOTTOM_RIGHT, p)) {
                        selectState = selectState.opposite();
                        tbClicked++;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Invalid select state");
            }
            if (tbClicked < textBoxContents.size()) {
                System.out.print("\rPositioning: " + textBoxContents.get(tbClicked) + " (" + (tbClicked + 1) + " / " + textBoxContents.size() + ") on " + selectState);
            } else {
                System.out.println();
            }
        }
    }
}
