package com.boomaa.note2jpg.create;

import com.boomaa.note2jpg.config.Parameter;

public enum FilenameSource {
    PARAMETER("Single filename specified", "-f"),
    ALL("Converting all available .note files", "--all"),
    RANDOM("Randomly selecting a .note file to convert", "--randomfile"),
    NEO("Converting files matching unsubmitted NEO assignments", "--neo"),
    USER_SELECT("No note file source specified - Please select one");

    private String message;
    private String determinant;

    FilenameSource(String message, String determinant) {
        this.message = message;
        this.determinant = determinant;
    }

    FilenameSource(String message) {
        this.message = message;
    }

    public String getDeterminant() {
        return determinant;
    }

    public String getMessage() {
        return message;
    }

    public static FilenameSource matches(Parameter p) {
        for (FilenameSource fs : FilenameSource.values()) {
            if (fs.getDeterminant().equals(p.getFlag())) {
                return fs;
            }
        }
        return null;
    }
}
