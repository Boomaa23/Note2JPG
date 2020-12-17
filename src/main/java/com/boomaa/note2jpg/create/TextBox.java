package com.boomaa.note2jpg.create;

import com.boomaa.note2jpg.convert.NFields;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

public class TextBox {
    // Mapped by ending index (exclusive)
    private final Map<Integer, SubRange> subRanges;
    private final Point upperLeft;
    private final Point bottomRight;
    private String text;

    public TextBox(Point upperLeft, Point bottomRight, String text) {
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

    public void putSubRange(int endingIdx, SubRange range) {
        subRanges.put(endingIdx, range);
    }

    public SubRange rangeOfIndex(int idx) {
        if (subRanges.size() == 1) {
            return subRanges.get(subRanges.keySet().iterator().next());
        }
        int last = -1;
        for (int loopIdx : subRanges.keySet()) {
            if (idx >= loopIdx) {
                return subRanges.get(last);
            }
            last = loopIdx;
        }
        return null;
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

        public Color getColor() {
            return color;
        }

        public double getFontSize() {
            return fontSize;
        }
    }
}
