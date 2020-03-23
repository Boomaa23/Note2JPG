package com.boomaa.note2jpg.create;

public enum FilenameSource {
    PARAMETER("Filename passed as parameter", "-f"),
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
}
