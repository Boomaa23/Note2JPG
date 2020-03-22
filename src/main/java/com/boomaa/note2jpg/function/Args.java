package com.boomaa.note2jpg.function;

import com.boomaa.note2jpg.create.FilenameSource;
import com.boomaa.note2jpg.integration.GoogleUtils;
import com.boomaa.note2jpg.integration.NEOExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Args extends NFields {
    public static void determineArgs() {
        determineFilenameSource();
        fnSource.getStream().println(fnSource.getMessage());
        switch (fnSource) {
            case ALL:
                filenames.addAll(getAllLocalNotes());
                break;
            case NEO:
                neoExecutor = NEOExecutor.parseArgs().pull();
                filenames = neoExecutor.getAssignments().getNames();
                break;
            case PARAMETER:
                String filename = parseFlag("-f");
                if (filename != null) {
                    filenames.add(filename);
                }
                break;
            case RANDOM:
                determineRandomFilename();
                break;
            case USER_SELECT:
            default:
                userSelectFilename();
                break;
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

        if (argsList.contains("--usedrive")) {
            GoogleUtils.retrieveNoteList();
        }
    }

    public static void determineFilenameSource() {
        int found = 0;
        FilenameSource foundFns = FilenameSource.USER_SELECT;
        for (FilenameSource fns : FilenameSource.values()) {
            if (argsList.contains(fns.getDeterminant())) {
                foundFns = fns;
                found++;
            }
            if (found > 1) {
                throw new IllegalArgumentException("Cannot use multiple filename selectors");
            }
        }
        fnSource = foundFns;
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

    public static List<String> getAllLocalNotes() {
        File[] dir = Objects.requireNonNull(new File(".").listFiles());
        List<String> notes = new ArrayList<>();

        int valid = 1;
        for (File file : dir) {
            String fname = file.getName();
            if (fname.contains(".note")) {
                String fNameNoExt = fname.substring(0, fname.length() - 5);
                if (fnSource != FilenameSource.ALL) {
                    System.out.println(valid + ") " + fNameNoExt);
                }
                valid++;
                notes.add(fNameNoExt);
            }
        }
        return notes;
    }

    public static void userSelectFilename() {
        List<String> notes = getAllLocalNotes();
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
