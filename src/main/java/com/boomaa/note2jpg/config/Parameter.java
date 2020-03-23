package com.boomaa.note2jpg.config;

import com.boomaa.note2jpg.function.NFields;

import java.lang.reflect.Field;

public enum Parameter {
    Filename("-f", Type.FILENAME, "FILENAME_SOURCE"),
    RandomFile("--randomfile", Type.FILENAME, "FILENAME_SOURCE"),
    ConvertAll("--all", Type.FILENAME, "FILENAME_SOURCE"),
    ImageScaleFactor("-s", Type.INTEGER, "IMAGE_SCALE_FACTOR"),
    PDFScaleFactor("-p", Type.INTEGER, "PDF_SCALE_FACTOR"),
    DisplayConverted("--display", Type.BOOLEAN, "DISPLAY_CONVERTED"),
    NoFileOutput("--nofile", Type.BOOLEAN, "DISPLAY_CONVERTED"),
    NoTextBoxes("--notextboxes", Type.BOOLEAN, "DISPLAY_CONVERTED"),
    GenerateConfig("--genconfig", Type.BOOLEAN, "GENERATE_CONFIG"),
    NEOUsername("--neo", Type.NEO, "NEO_USR"),
    NEOPassword("--neo", Type.NEO, "NEO_PW"),
    NEOClassID("--classid", Type.STRING, "NEO_CLASS_ID"),
    UseGoogleDrive("--usedrive", Type.BOOLEAN, "USE_GOOGLE_DRIVE"),
    GoogleSvcAcctID("--gacctid", Type.STRING, "GOOGLE_SVC_ACCT_ID");

    private final String flag;
    private final Type type;
    private String linkedField;
    private boolean inJson;
    private Boolean inArgs = null;

    Parameter(String flag, Type type, String linkedField) {
        this.flag = flag;
        this.type = type;
        this.linkedField = linkedField;
        this.inJson = JSONHelper.inJson(name());
    }

    public boolean inEither() {
        return inArgs() || inJson();
    }

    public boolean inJson() {
        return inJson;
    }

    public boolean inArgs() {
        if (inArgs == null) {
            inArgs = NFields.argsList.contains(this.flag);
        }
        return inArgs;
    }

    public String[] argsValue(int buffer) {
        String[] args = new String[buffer];
        int ioFlag = NFields.argsList.indexOf(flag);
        for (int i = 0;i < args.length;i++) {
            args[i] = NFields.argsList.get(ioFlag + i + 1);
        }
        return args;
    }

    public String argsValue() {
        return NFields.argsList.get(NFields.argsList.indexOf(flag) + 1);
    }

    public int getPriorityInt() {
        return Integer.parseInt(getPriority());
    }

    public String getPriority() {
        if (inArgs()) {
            return argsValue();
        } else if (inJson()) {
            return JSONHelper.getJsonValue(name());
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
        return getLinkedField(ConfigVars.class);
    }

    public Field getLinkedField(Class<?> targetClass) {
        try {
            return Class.forName(targetClass.getName()).getDeclaredField(linkedField);
        } catch (NoSuchFieldException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public <T> void setLinkedField(T value) {
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

}

