package com.boomaa.note2jpg.create;

import com.boomaa.note2jpg.convert.NFields;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TextBox {
    // Mapped by ending index (exclusive)
    private final Map<Integer, SubRange> subRanges;
    private final Point upperLeft;
    private final Point bottomRight;
    private final double rotationDegrees;
    private String text;

    public TextBox(Point upperLeft, Point bottomRight, String text, double rotationDegrees) {
        this.rotationDegrees = rotationDegrees;
        this.subRanges = new LinkedHashMap<>();
        this.upperLeft = upperLeft;
        this.bottomRight = bottomRight;
        this.text = text;
        NFields.textBoxContents.add(text);
    }

    public Point getUpperLeft() {
        return upperLeft;
    }

    public Point getBottomRight() {
        return bottomRight;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public double getRotationDegrees() {
        return rotationDegrees;
    }

    public void putSubRange(int endingIdx, SubRange range) {
        subRanges.put(endingIdx, range);
    }

    public SubRange rangeOfIndex(int idx) {
        switch (subRanges.size()) {
            case 0:
                return new SubRange();
            case 1:
                return subRanges.get(subRanges.keySet().iterator().next());
            default:
                List<Integer> keys = new ArrayList<>(subRanges.keySet());
                Collections.sort(keys);
                int last = keys.get(0);
                for (int loopIdx : keys) {
                    if (loopIdx >= idx) {
                        return subRanges.get(last);
                    }
                    last = loopIdx;
                }
        }
        throw new ArrayIndexOutOfBoundsException("No subrange for index " + idx + " with text \"" + text.trim() + "\"");
    }

    public Map<Integer, SubRange> getSubRanges() {
        return subRanges;
    }

    public static class SubRange {
        private final Color color;
        private final double fontSize;

        public SubRange(Color color, double fontSize) {
            this.color = color;
            this.fontSize = fontSize;
        }

        public SubRange() {
            this(Color.BLACK, 12);
        }

        public Color getColor() {
            return color;
        }

        public double getFontSize() {
            return fontSize;
        }
    }
}
