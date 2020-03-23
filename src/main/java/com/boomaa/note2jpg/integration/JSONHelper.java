package com.boomaa.note2jpg.integration;

import com.boomaa.note2jpg.function.NFields;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JSONHelper extends NFields {
    private static final String CONFIG_FILE_NAME = "config.json";
    private static String NEO_CLASS_ID = "1543270";
    private static String GOOGLE_SVC_ACCT_ID = "102602978922283269345";
    private static boolean classIdOverride = false;
    private static boolean svcAcctOverride = false;

    static {
        loadArgs();
        if (hasCustom()) {
            loadCustom();
        }
    }

    public static void loadArgs() {
        if (argsList.contains("--classid")) {
            classIdOverride = true;
            NEO_CLASS_ID = argsList.get(argsList.indexOf("--classid"));
        }
        if (argsList.contains("--gacctid")) {
            svcAcctOverride = true;
            GOOGLE_SVC_ACCT_ID = argsList.get(argsList.indexOf("--gacctid"));
        }
    }

    public static boolean hasCustom() {
        return new File(CONFIG_FILE_NAME).exists();
    }

    public static void loadCustom() {
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            Map<String, String> json = gson.fromJson(new FileReader(CONFIG_FILE_NAME), type);
            if (!classIdOverride) {
                NEO_CLASS_ID = json.get("NEOClassID");
            }
            if (!svcAcctOverride) {
                GOOGLE_SVC_ACCT_ID = json.get("GoogleSvcAcctID");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void generateTemplate() {
        try {
            JsonObject json = new JsonObject();
            json.addProperty("GoogleSvcAcctID", "");
            json.addProperty("NEOClassID", "");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(CONFIG_FILE_NAME);
            gson.toJson(json, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getNEOClassID() {
        return NEO_CLASS_ID;
    }

    public static String getGoogleSvcAcctID() {
        return GOOGLE_SVC_ACCT_ID;
    }
}
