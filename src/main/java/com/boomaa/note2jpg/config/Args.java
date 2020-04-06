package com.boomaa.note2jpg.config;

import com.boomaa.note2jpg.convert.NFields;
import com.boomaa.note2jpg.integration.GoogleUtils;
import com.boomaa.note2jpg.integration.NEOExecutor;
import com.boomaa.note2jpg.integration.s3upload.Connections;
import com.boomaa.note2jpg.state.FilenameSource;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Args extends NFields {
    public static void parse() {
        boolean neoFound = Parameter.NEOUsername.inEither() || Parameter.NEOPassword.inEither();
        int found = 0;
        for (Parameter p : Parameter.values()) {
            if (p.inEither()) {
                switch (p.getType()) {
                    case FILENAME:
                        Parameter.ConfigVars.FILENAME_SOURCE = FilenameSource.matches(p);
                        found++;
                        if (found > 1) {
                            throw new IllegalArgumentException("Cannot use multiple filename selectors");
                        }
                        break;
                    case INTEGER:
                        p.setLinkedField(Integer.parseInt(p.getValue()));
                        break;
                    case STRING:
                        p.setLinkedField(p.getValue());
                        break;
                    default:
                        break;
                }
            }
        }
        if (neoFound) {
            if (Parameter.NEOUsername.inArgs() && Parameter.NEOPassword.inArgs()) {
                String[] usrPw = Parameter.NEOUsername.argsValue(2);
                Parameter.NEOUsername.setLinkedField(usrPw[0]);
                Parameter.NEOPassword.setLinkedField(usrPw[1]);
            } else if (Parameter.NEOUsername.inJson() && Parameter.NEOPassword.inJson()) {
                Parameter.NEOUsername.setLinkedField(JSONHelper.getJsonValue(Parameter.NEOUsername.name()));
                Parameter.NEOPassword.setLinkedField(JSONHelper.getJsonValue(Parameter.NEOPassword.name()));
            }
            if (found == 0) {
                Parameter.ConfigVars.FILENAME_SOURCE = FilenameSource.NEO;
            }
            Connections.create(Parameter.NEOUsername.getValue(), Parameter.NEOPassword.getValue());
            neoExecutor = new NEOExecutor(Parameter.NEOClassID.getValue()).pull();
        }

        if (Parameter.UseDrive.inEither()) {
            Parameter.UseDriveUpload.setLinkedField(true);
            Parameter.UseDriveDownload.setLinkedField(true);
        }
        if (Parameter.UseDriveDownload.inEither() && Parameter.Filename.inEither() && neoFound) {
            Parameter.ConfigVars.FILENAME_SOURCE = FilenameSource.PARAMETER;
        }
        if (Parameter.UseDriveDownload.inEither() && found == 0) {
            Parameter.ConfigVars.FILENAME_SOURCE = FilenameSource.DRIVE;
        }
    }

    public static void logic() {
        if (Parameter.GenerateConfig.inEither()) {
            System.out.println("Note2JPG will now print a blank integrations JSON template to config.json");
            System.out.println("The application will not continue to run after this has completed");
            JSONHelper.generateConfig(false);
            System.exit(0);
        }
        if (Parameter.WriteConfig.inEither()) {
            JSONHelper.generateConfig(true);
        }

        System.out.println(Parameter.ConfigVars.FILENAME_SOURCE.getMessage());
        switch (Parameter.ConfigVars.FILENAME_SOURCE) {
            case ALL:
                notenames.addAll(getAllLocalNotes());
                break;
            case NEO:
                notenames = neoExecutor.getAssignments().getNames();
                break;
            case PARAMETER:
                notenames.add(Parameter.Filename.getValue());
                break;
            case RANDOM:
                determineRandomFilename();
                break;
            case DRIVE:
                notenames.add(filenameSelector(GoogleUtils.getNoteNameList()
                        .subList(0, Parameter.LimitDriveNotes.getValueInt())));
                break;
            case USER_SELECT:
            default:
                notenames.add(filenameSelector(getAllLocalNotes()));
                break;
        }

        Parameter.PDFScaleFactor.setLinkedField(Integer.parseInt(Parameter.PDFScaleFactor.getValue()));
        Parameter.ImageScaleFactor.setLinkedField(Integer.parseInt(Parameter.ImageScaleFactor.getValue()));
    }

    public static void check() {
        if (Parameter.NEOAssignment.inEither() && notenames.size() > 1) {
            throw new IllegalArgumentException("Cannot specify an assignment name for multiple notes");
        }

        if (!(Parameter.NEOUsername.inEither() || Parameter.NEOPassword.inEither()) && Parameter.UseAWS.inEither()) {
            System.err.println("Not uploading to AWS as no NEO credentials were specified");
        }

        if (notenames.isEmpty()) {
            System.err.println("No .note files selected to convert");
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
                if (Parameter.ConfigVars.FILENAME_SOURCE != FilenameSource.ALL) {
                    System.out.println(valid + ") " + fNameNoExt);
                }
                valid++;
                notes.add(fNameNoExt);
            }
        }
        return notes;
    }

    public static String filenameSelector(List<String> list) {
        displayListOptions(list);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int selected = -1;
        try {
            selected = Integer.parseInt(br.readLine().replaceAll("[^0-9]", ""));
        } catch (IOException ignored) {
        }

        if (selected > list.size() || selected == 0) {
            System.err.println("Item not found for supplied index. Please try again");
            displayListOptions(list);
        }
        System.out.println();
        return list.get(selected - 1);
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
        notenames.add(filename);
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

    public static void displayListOptions(List<String> options) {
        for (int i = 0;i < options.size();i++) {
            System.out.println((i + 1) + ") " + options.get(i));
        }
    }
}
