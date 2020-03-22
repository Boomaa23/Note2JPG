package com.boomaa.note2jpg.create;

import java.io.PrintStream;

public enum FilenameSource {
    PARAMETER("Filename passed as parameter", "-f"),
    ALL("Converting all available .note files", "--all"),
    RANDOM("Randomly selecting a .note file to convert", "--random"),
    NEO("Converting files matching unsubmitted NEO assignments", "--neo"),
    USER_SELECT("No note file source specified - Please select one", System.err);

    private String message;
    private PrintStream msgStream;
    private String determinant;

    FilenameSource(String message, PrintStream msgStream, String determinant) {
        this.message = message;
        this.msgStream = msgStream;
        this.determinant = determinant;
    }

    FilenameSource(String message, String determinant) {
        this(message, System.out, determinant);
    }

    FilenameSource(String message, PrintStream msgStream) {
        this.message = message;
        this.msgStream = msgStream;
    }

    FilenameSource(String message) {
        this(message, System.out);
    }

    public String getDeterminant() {
        return determinant;
    }

    public String getMessage() {
        return message;
    }

    public PrintStream getStream() {
        return msgStream;
    }
}
