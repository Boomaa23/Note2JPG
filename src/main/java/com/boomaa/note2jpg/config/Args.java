package com.boomaa.note2jpg.config;

import com.boomaa.note2jpg.create.FilenameSource;
import com.boomaa.note2jpg.function.NFields;
import com.boomaa.note2jpg.integration.GoogleUtils;
import com.boomaa.note2jpg.integration.NEOExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Args extends NFields {
    public static void parse() {
        boolean parseNeo = false;
        int found = 0;
        for (Parameter p : Parameter.values()) {
            if (p.inEither()) {
                switch (p.getType()) {
                    case FILENAME:
                        ConfigVars.FILENAME_SOURCE = FilenameSource.matches(p);
                        found++;
                        if (found > 1) {
                            throw new IllegalArgumentException("Cannot use multiple filename selectors");
                        }
                        break;
                    case INTEGER:
                        p.setLinkedField(Integer.parseInt(p.getPriority()));
                        break;
                    case BOOLEAN:
                        p.setLinkedField(p.inEither());
                        break;
                    case STRING:
                        p.setLinkedField(p.getPriority());
                        break;
                    case NEO:
                        if (!parseNeo) {
                            if (p.inArgs()) {
                                String[] usrPw = p.argsValue(2);
                                Parameter.NEOUsername.setLinkedField(usrPw[0]);
                                Parameter.NEOPassword.setLinkedField(usrPw[1]);
                                parseNeo = true;
                            } else if (p.inJson()) {
                                Parameter.NEOUsername.setLinkedField(JSONHelper.getJsonValue(Parameter.NEOUsername.name()));
                                Parameter.NEOPassword.setLinkedField(JSONHelper.getJsonValue(Parameter.NEOPassword.name()));
                                parseNeo = true;
                            }
                        }
                        break;
                    default:
                }
            }
        }
    }

    public static void logic() {
        if (Parameter.GenerateConfig.inEither()) {
            System.out.println("Note2JPG will now print a blank integrations JSON template to config.json");
            System.out.println("The application will not continue to run after this has completed");
            JSONHelper.generateTemplate();
            System.exit(0);
        }

        System.out.println(ConfigVars.FILENAME_SOURCE.getMessage());
        switch (ConfigVars.FILENAME_SOURCE) {
            case ALL:
                filenames.addAll(getAllLocalNotes());
                break;
            case NEO:
                neoExecutor = NEOExecutor.parseArgs().pull();
                filenames = neoExecutor.getAssignments().getNames();
                break;
            case PARAMETER:
                filenames.add(Parameter.Filename.getPriority());
                break;
            case RANDOM:
                determineRandomFilename();
                break;
            case USER_SELECT:
            default:
                filenameSelector(getAllLocalNotes());
                break;
        }

        Parameter.PDFScaleFactor.setLinkedField(Integer.parseInt(Parameter.PDFScaleFactor.getPriority()));
        Parameter.ImageScaleFactor.setLinkedField(Integer.parseInt(Parameter.PDFScaleFactor.getPriority()));

        if (Parameter.UseGoogleDrive.inEither()) {
            GoogleUtils.retrieveNoteList();
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
                if (ConfigVars.FILENAME_SOURCE != FilenameSource.ALL) {
                    System.out.println(valid + ") " + fNameNoExt);
                }
                valid++;
                notes.add(fNameNoExt);
            }
        }
        return notes;
    }

    public static void filenameSelector(List<String> notes) {
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
