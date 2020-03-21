package com.boomaa.note2jpg.function;

import com.boomaa.note2jpg.integration.NEOExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Args extends NFields {
    public static void determineArgs() {
        filenames = NEOExecutor.parseArgs();
        String filename = parseFlag("-f");
        if (filename != null) {
            filenames.add(filename);
        }
        scaleFactor = parseIntFlagValue(parseFlag("-s"));
        pdfRes = parseIntFlagValue(parseFlag("-p"));

        if (pdfRes == -1) {
            pdfRes = 200;
        } else {
            pdfRes *= 100;
        }
        if (scaleFactor == -1) {
            scaleFactor = 8;
        }
        if (filenames.isEmpty()) {
            if (argsList.contains("--randomfile")) {
                System.err.println("No .note file specified - Selecting randomly");
                determineRandomFilename();
            } else {
                userSelectFilename();
            }
        }
    }

    public static String parseFlag(String flag) {
        if (argsList.contains(flag)) {
            try {
                return argsList.get(argsList.indexOf(flag) + 1);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new NullPointerException("\"" + flag + "\" flag passed without value");
            }
        }
        return null;
    }

    public static int parseIntFlagValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public static void userSelectFilename() {
        File[] dir = Objects.requireNonNull(new File(".").listFiles());
        List<String> notes = new ArrayList<>();
        boolean containsAll = argsList.contains("--all");

        if (!containsAll) {
            System.err.println("No .note file specified - Please select");
        }

        int valid = 1;
        for (File file : dir) {
            String fname = file.getName();
            if (fname.contains(".note")) {
                String fNameNoExt = fname.substring(0, fname.length() - 5);
                if (!containsAll) {
                    System.out.println(valid + ") " + fNameNoExt);
                }
                valid++;
                notes.add(fNameNoExt);
            }
        }

        if (containsAll) {
            if (argsList.contains("-f")) {
                throw new IllegalArgumentException("Cannot specify a filename with -all");
            }
            if (argsList.contains("--neo")) {
                throw new IllegalArgumentException("Cannot use NEO integration with --all");
            }
            System.out.println("Converting all available .note files");
            filenames.addAll(notes);
            return;
        }

        Scanner sc = new Scanner(System.in);
        System.out.print(">> ");
        int selected = 0;
        while (true) {
            if (sc.hasNext() && (selected = Integer.parseInt(sc.next())) != 0 && selected <= notes.size()) {
                break;
            }
            if (selected > notes.size()) {
                System.err.println("Note not found for supplied index. Please try again");
                System.out.println();
                System.out.print(">> ");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                break;
            }
        }
        sc.close();
        filenames.add(notes.get(selected - 1));
        System.out.println();
    }

    public static void determineRandomFilename() {
        File dir = new File(".");
        File[] shuffledDir = shuffleArr(Objects.requireNonNull(dir.listFiles()));
        List<String> notes = new ArrayList<>();
        List<String> jpgs = new ArrayList<>();
        String filename = null;

        if (argsList.contains("--all")) {
            throw new IllegalArgumentException("Cannot select randomly with --all");
        }

        for (File file : shuffledDir) {
            String fname = file.getName();
            if (fname.contains(".note")) {
                String noteName = fname.substring(0, fname.length() - 5);
                if (notes.contains(noteName)) {
                    filename = noteName;
                } else {
                    notes.add(noteName);
                }
            }
            if (fname.contains(".jpg")) {
                jpgs.add(fname.substring(0, fname.length() - 4));
            }
        }

        if (filename == null) {
            for (String note : notes) {
                if (!jpgs.contains(note)) {
                    filename = note;
                }
            }
            if (filename == null) {
                throw new NullPointerException("Filename cannot be determined as there are no free note files");
            }
        }
        filenames.add(filename);
    }

    public static File[] shuffleArr(File[] base) {
        File[] out = new File[base.length];
        for (File file : base) {
            int newIndex = -1;
            while (newIndex < 0 || out[newIndex] != null) {
                newIndex = (int) (Math.random() * out.length);
            }
            out[newIndex] = file;
        }
        return out;
    }
}
