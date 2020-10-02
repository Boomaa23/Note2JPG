package com.boomaa.note2jpg.updater;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ConsolePrintOut extends OutputStream {
    private byte[] oneByte;
    private Appender appender;

    public ConsolePrintOut(JTextArea textArea) {
        this(textArea, 1000);
    }

    public ConsolePrintOut(JTextArea textArea, int maxLine) {
        if (maxLine < 1) {
            throw new IllegalArgumentException("TextAreaOutputStream maximum lines must be positive (value=" + maxLine + ")");
        }
        oneByte = new byte[1];
        appender = new Appender(textArea, maxLine);
    }

    public synchronized void clear() {
        if (appender != null) {
            appender.clear();
        }
    }

    public synchronized void close() {
        appender = null;
    }

    public synchronized void flush() {
    }

    public synchronized void write(int val) {
        oneByte[0] = (byte) val;
        write(oneByte, 0, 1);
    }

    public synchronized void write(byte[] ba) {
        write(ba, 0, ba.length);
    }

    public synchronized void write(byte[] ba, int str, int len) {
        if(appender != null) {
            appender.append(bytesToString(ba, str, len));
        }
    }

    private static String bytesToString(byte[] ba, int str, int len) {
        return new String(ba, str, len, StandardCharsets.UTF_8);
    }

    public static class Appender implements Runnable {
        private final JTextArea textArea;
        private final int maxLines;
        private final LinkedList<Integer> lengths;
        private final List<String> values;

        private int curLength;
        private boolean clear;
        private boolean queue;

        Appender(JTextArea textArea, int maxLines) {
            this.textArea = textArea;
            this.maxLines = maxLines;
            lengths = new LinkedList<>();
            values = new ArrayList<>();

            curLength = 0;
            clear = false;
            queue = true;
        }

        synchronized void append(String val) {
            values.add(val);
            if (queue) {
                queue = false;
                EventQueue.invokeLater(this);
            }
        }

        synchronized void clear() {
            clear = true;
            curLength = 0;
            lengths.clear();
            values.clear();
            if (queue) {
                queue = false;
                EventQueue.invokeLater(this);
            }
        }

        public synchronized void run() {
            if (clear) {
                textArea.setText("");
            }
            for (String val : values) {
                curLength += val.length();
                if (val.endsWith(EOL1) || val.endsWith(EOL2)) {
                    if (lengths.size() >= maxLines) {
                        textArea.replaceRange("", 0, lengths.removeFirst()); }
                    lengths.addLast(curLength);
                    curLength = 0;
                }
                textArea.append(val);
            }
            values.clear();
            clear = false;
            queue = true;
        }

        static private final String EOL1 = "\n";
        static private final String EOL2 = System.getProperty("line.separator", EOL1);
    }
}