package com.boomaa.note2jpg.config;

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
    private static Map<String, String> JSON;

    static {
        if (hasCustom()) {
            loadCustom();
        }
    }

    public static boolean hasCustom() {
        return new File(CONFIG_FILE_NAME).exists();
    }

    public static void loadCustom() {
        try {
            Gson gson = new Gson();
            Type type = new TypeToken<HashMap<String, String>>(){}.getType();
            JSON = gson.fromJson(new FileReader(CONFIG_FILE_NAME), type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void generateTemplate() {
        try {
            JsonObject json = new JsonObject();
            for (Parameter p : Parameter.values()) {
                json.addProperty(p.name(), "");
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(CONFIG_FILE_NAME);
            gson.toJson(json, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, String> getJson() {
        return JSON;
    }

    public static boolean inJson(String key) {
        return JSON != null && JSON.containsKey(key) && !JSON.get(key).equals("");
    }

    public static String getJsonValue(String key) {
        return JSON.get(key);
    }
}
