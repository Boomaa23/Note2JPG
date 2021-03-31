package com.boomaa.note2jpg.config;

import com.boomaa.note2jpg.convert.NFields;
import com.boomaa.note2jpg.state.FilenameSource;

import java.lang.reflect.Field;

public enum Parameter {
    Filename("-f", Type.FILENAME, "FILENAME_SOURCE"),
    ImageScaleFactor("-s", Type.INTEGER, "IMAGE_SCALE_FACTOR"),
    PDFScaleFactor("-p", Type.INTEGER, "PDF_SCALE_FACTOR"),
    Concatenate("--concat", Type.BOOLEAN),
    ConvertAll("--all", Type.FILENAME, "FILENAME_SOURCE"),
    DisplayConverted("--display", Type.BOOLEAN),
    PageCountOut("--pgct", Type.INTEGER, "PAGE_COUNT"),
    PageSelectionIn("--pgsel", Type.STRING, "PAGE_SELECTION"),
    NoPagePrompt("--npp", Type.BOOLEAN),
    NoteFilter("--filter", Type.STRING, "NOTE_FILTER"),
    FitExactHeight("--hfit", Type.BOOLEAN),
    OutputDirectory("--outdir", Type.STRING, "OUTPUT_DIRECTORY"),
    NoFileOutput("--nofile", Type.BOOLEAN),
    RandomFile("--randomfile", Type.FILENAME, "FILENAME_SOURCE"),
    GenerateConfig("--genconfig", Type.BOOLEAN),
    WriteConfig("--writeconfig", Type.BOOLEAN),
    ConsoleOnly("--console", Type.BOOLEAN),
    NEOUsername("--neo", Type.NEO, "NEO_USR"),
    NEOPassword("--neo", Type.NEO, "NEO_PW"),
    NEOClassID("--classid", Type.STRING, "NEO_CLASS_ID"),
    NEOAssignment("-a", Type.STRING, "ASSIGNMENT_NAME"),
    NEONoLink("--neonolink", Type.BOOLEAN),
    AllowSubmitted("--allowsubmitted", Type.BOOLEAN),
    IncludeUnits("--inclunits", Type.BOOLEAN),
    NewNEOFilename("--newneofn", Type.BOOLEAN),
    WipeUploaded("--wipeup", Type.BOOLEAN),
    UseAWS("--aws", Type.BOOLEAN),
    UseDrive("--gdrive", Type.BOOLEAN),
    UseDriveDownload("--gdrivedl", Type.BOOLEAN),
    UseDriveUpload("--gdriveup", Type.BOOLEAN),
    ForceDriveDownload("--fgdl", Type.BOOLEAN),
    GoogleSvcAcct("--gsvc", Type.BOOLEAN),
    GoogleRelog("--grelog", Type.BOOLEAN),
    LimitDriveNotes("--gdrivelim", Type.INTEGER, "GDRIVE_LIMIT_NOTES");

    private final String flag;
    private final Type type;
    private String linkedField;
    private boolean inJson;
    private Boolean inArgs = null;
    private boolean setOverride = false;

    Parameter(String flag, Type type, String linkedField) {
        this.flag = flag;
        this.type = type;
        this.linkedField = linkedField;
        this.inJson = JSONHelper.inJsonSimple(name());
    }

    Parameter(String flag, Type type) {
        if (type != Type.BOOLEAN) {
            throw new IllegalArgumentException("Must have a linked field for non-boolean types");
        }
        this.flag = flag;
        this.type = type;
        this.inJson = JSONHelper.inJsonBoolean(name());
    }

    public boolean inEither() {
        return inArgs() || inJson();
    }

    public boolean inJson() {
        return inJson || (setOverride && type == Type.BOOLEAN);
    }

    public boolean inArgs() {
        if (inArgs == null) {
            inArgs = NFields.argsList.contains(this.flag);
        }
        return inArgs || (setOverride && type == Type.BOOLEAN);
    }

    public String[] argsValue(int buffer) {
        String[] args = new String[buffer];
        int ioFlag = NFields.argsList.indexOf(flag);
        for (int i = 0; i < args.length; i++) {
            args[i] = NFields.argsList.get(ioFlag + i + 1);
        }
        return args;
    }

    public String argsValue() {
        return NFields.argsList.get(NFields.argsList.indexOf(flag) + 1);
    }

    public int getValueInt() {
        return Integer.parseInt(getValue());
    }

    public String getValue() {
        if (type == Type.BOOLEAN) {
            return Boolean.toString(inEither());
        }

        if (inArgs) {
            if (this.equals(NEOUsername)) {
                return argsValue();
            } else if (this.equals(NEOPassword)) {
                return argsValue(2)[1];
            }
        }

        if (!setOverride) {
            if (inArgs()) {
                return argsValue();
            } else if (inJson()) {
                return JSONHelper.getJsonValue(name());
            }
        }

        try {
            var def = getLinkedField().get(null);
            if (def instanceof String) {
                return (String) def;
            } else if (def instanceof Boolean) {
                return Boolean.toString((boolean) def);
            } else if (def instanceof Integer) {
                return Integer.toString((int) def);
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return "";
    }

    public Field getLinkedField() {
        return getLinkedField(Parameter.ConfigVars.class);
    }

    public Field getLinkedField(Class<?> targetClass) {
        try {
            return targetClass.getDeclaredField(this.linkedField);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> void setLinkedField(T value) {
        setOverride = true;
        if (type == Type.BOOLEAN) {
            return;
        }
        try {
            getLinkedField().set(null, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public String getFlag() {
        return flag;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        FILENAME, INTEGER, BOOLEAN, STRING, NEO
    }

    public static class ConfigVars {
        public static FilenameSource FILENAME_SOURCE = FilenameSource.USER_SELECT;
        public static int PAGE_COUNT = 1;
        public static int IMAGE_SCALE_FACTOR = 8;
        public static int PDF_SCALE_FACTOR = 2;
        public static int GDRIVE_LIMIT_NOTES = 20;
        public static String NOTE_FILTER = "";
        public static String ASSIGNMENT_NAME = "";
        public static String PAGE_SELECTION = "";
        public static String NEO_CLASS_ID = "";
        public static String NEO_USR = "";
        public static String NEO_PW = "";
        public static String OUTPUT_DIRECTORY = "";
    }
}

