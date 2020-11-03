package com.boomaa.note2jpg.create;

public enum Corner {
    UPPER_LEFT,
    UPPER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT;

    public Corner opposite() {
        return Corner.values()[Corner.values().length - this.ordinal() - 1];
    }

    @Override
    public String toString() {
        StringBuilder name = new StringBuilder(name().toLowerCase());
        if (name.length() > 0) {
            name.replace(0, 1, String.valueOf(name.charAt(0)).toUpperCase());
        }
        for (int i = 0; i < name.length(); i++) {
            if (name.charAt(i) == '_') {
                name.replace(i, i + 1, " ");
            }
            if (name.charAt(i) == ' ' && (i + 1) < name.length()) {
                name.replace(i + 1, i + 2, String.valueOf(name.charAt(i + 1)).toUpperCase());
            }
        }
        return name.toString();
    }
}
