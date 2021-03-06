package com.boomaa.note2jpg.config;

import com.boomaa.note2jpg.convert.NFields;
import com.boomaa.note2jpg.integration.NEOExecutor;
import com.boomaa.note2jpg.integration.NameIDMap;
import com.boomaa.note2jpg.integration.s3upload.Connections;
import com.boomaa.note2jpg.state.FilenameSource;
import com.boomaa.note2jpg.uxutil.NEOLoginUI;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Args extends NFields {
    public static void parse() {
        boolean neoFound = Parameter.NEOUsername.inEither() || Parameter.NEOPassword.inEither();
        if (!neoFound && (Parameter.NEOClassID.inEither() || Parameter.NEOAssignment.inEither()
                || Parameter.NEONoLink.inEither() || Parameter.AllowSubmitted.inEither()
                || Parameter.IncludeUnits.inEither() || Parameter.NewNEOFilename.inEither()
                || Parameter.UseAWS.inEither())) {
            if (Parameter.ConsoleOnly.inEither()) {
                throw new IllegalArgumentException("Cannot retrieve missing NEO login credentials from user in console-only mode");
            }
            NEOLoginUI login = new NEOLoginUI();
            login.show();
            login.waitForInput();
            Parameter.NEOUsername.setLinkedField(login.getUsername());
            Parameter.NEOPassword.setLinkedField(login.getPassword());
            login.destroy();
            neoFound = true;
        }
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
                        p.setLinkedField(p.getValueInt());
                        break;
                    case STRING:
                        p.setLinkedField(p.getValue());
                        break;
                    default:
                        break;
                }
            }
        }

        if (Parameter.OutputDirectory.inEither()) {
            if (!Parameter.OutputDirectory.getValue().equals("")) {
                Parameter.OutputDirectory.setLinkedField(Parameter.OutputDirectory.getValue() + "/");
            }

            File folder = new File(Parameter.OutputDirectory.getValue());
            if (!folder.isDirectory()) {
                folder.mkdirs();
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
            Connections.create(Parameter.NEOUsername.getValue(), Parameter.NEOPassword.getValue());
            neoExecutor = new NEOExecutor();

            if (!Parameter.NEONoLink.inEither()) {
                if (Parameter.NEOClassID.getValue().isBlank()) {
                    System.out.println("Select the NEO class to be used for assignment upload");
                    NameIDMap classList = neoExecutor.getClassList();
                    String selected = filenameSelector(classList.getNames(), "");
                    Parameter.NEOClassID.setLinkedField(classList.get(selected));
                }
                neoExecutor.pull();
            }
        }

        if (Parameter.UseDrive.inEither()) {
            Parameter.UseDriveUpload.setLinkedField(true);
            Parameter.UseDriveDownload.setLinkedField(true);
        }
        if (Parameter.UseDriveDownload.inEither() && ((Parameter.Filename.inEither() && neoFound) || found == 0)) {
            Parameter.ConfigVars.FILENAME_SOURCE = FilenameSource.DRIVE;
        }

        if (Parameter.Filename.inEither()) {
            if (Parameter.NoteFilter.inEither()) {
                throw new IllegalArgumentException("Cannot have a filter and filename");
            } else {
                Parameter.NoteFilter.setLinkedField(Parameter.Filename.getValue());
            }
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
                notenames.addAll(Arrays.asList(Parameter.Filename.getValue().split("/")));
                break;
            case RANDOM:
                determineRandomFilename();
                break;
            case DRIVE:
                List<String> tempNoteNames = Connections.getGoogleUtils().getNoteNameList();
                notenames.add(filenameSelector(tempNoteNames.subList(0,
                        Math.min(Parameter.LimitDriveNotes.getValueInt(), tempNoteNames.size())),
                        Parameter.NoteFilter.getValue()));
                break;
            case USER_SELECT:
            default:
                notenames.add(filenameSelector(getAllLocalNotes(),
                        Parameter.NoteFilter.getValue()));
                break;
        }

        Parameter.PDFScaleFactor.setLinkedField(Parameter.PDFScaleFactor.getValueInt());
        Parameter.ImageScaleFactor.setLinkedField(Parameter.ImageScaleFactor.getValueInt());
    }

    public static void check() {
        if (Parameter.NEOAssignment.inEither() && notenames.size() > 1) {
            throw new IllegalArgumentException("Cannot specify an assignment name for multiple notes");
        }

        if (Parameter.ForceDriveDownload.inEither() && !Parameter.UseDriveDownload.inEither()) {
            throw new IllegalArgumentException("Must be downloading from Google Drive to force download from it");
        }

        if (Parameter.PageSelectionIn.inEither()) {
            if (Parameter.FitExactHeight.inEither()) {
                throw new IllegalArgumentException("Cannot select pages with exact height fitting");
            } else if (Parameter.PageCountOut.inEither()) {
                throw new IllegalArgumentException("Page count and selection cannot be simultaneously enabled");
            } else if (notenames.size() > 1) {
                throw new IllegalArgumentException("Page selection from multiple sources not supported");
            }
        }

        if (!(Parameter.NEOUsername.inEither() || Parameter.NEOPassword.inEither()) && Parameter.UseAWS.inEither()) {
            System.err.println("Not uploading to AWS as no NEO credentials were specified");
        }

        if (notenames.isEmpty()) {
            System.err.println("No .note files selected to convert");
        }

        if (Parameter.ImageScaleFactor.getValueInt() <= 0 || Parameter.PDFScaleFactor.getValueInt() <= 0) {
            throw new ArithmeticException("Cannot have a scale factor of zero");
        }
    }

    public static String getPageSelection() throws IOException {
        if (!Parameter.PageSelectionIn.inEither()) {
            System.out.println("Please select the page range(s): default=all range=1-" + (int) Math.ceil(pages));
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            String inRange = br.readLine();
            if (inRange.isBlank() || inRange.equals("all")) {
                inRange = "1-" + (int) Math.ceil(pages);
            }
            return inRange;
        }
        return Parameter.PageSelectionIn.getValue();
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

    public static String filenameSelector(List<String> list, String filter) {
        if (!displayListOptions(list, filter)) {
            System.err.println("No note files matching filter " + filter);
            if (!Parameter.ConsoleOnly.inEither()) {
                System.out.println("Press any key to quit...");
                outputDone = true;
            } else {
                System.exit(1);
            }
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        int selected = -1;
        try {
            selected = Integer.parseInt(ensureSelection(br, list.size()));
        } catch (IOException ignored) {
        }
        System.out.println();
        return list.get(selected - 1);
    }

    private static String ensureSelection(BufferedReader br, int max) throws IOException {
        String raw = br.readLine();
        if (raw == null) {
            System.err.println("FATAL: Cannot parse given value.");
            System.exit(1);
        }
        String out = raw.replaceAll("[^0-9]", "");
        int io = Integer.parseInt(out);
        if (out.isBlank()) {
            System.err.println("Selection index is not a number. Please try again.");
            return ensureSelection(br, max);
        } else if (io <= 0 || io > max || raw.contains("-")) {
            System.err.println("Supplied index out of bounds. Please try again.");
            return ensureSelection(br, max);
        }
        return out;
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

    public static boolean displayListOptions(List<String> options, String filter) {
        boolean hasDisplay = false;
        for (int i = 0; i < options.size(); i++) {
            if (options.get(i).contains(filter)) {
                hasDisplay = true;
                System.out.println((i + 1) + ") " + options.get(i));
            }
        }
        return hasDisplay;
    }
}
