package com.boomaa.note2jpg.config;

import com.boomaa.note2jpg.convert.NFields;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class JSONHelper extends NFields {
    private static final String CONFIG_FILE_NAME = "config.json";
    private static Map<String, String> json;

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
            json = gson.fromJson(new FileReader(CONFIG_FILE_NAME), type);
        } catch (ExceptionInInitializerError | JsonSyntaxException ignored) {
            System.err.println("Invalid JSON detected. Will not read preferences.");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void generateConfig(boolean fillValues) {
        try {
            JsonObject json = new JsonObject();
            for (Parameter p : Parameter.values()) {
                if (!fillValues) {
                    json.addProperty(p.name(), "");
                } else {
                    json.addProperty(p.name(), p.getValue());
                }
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
        return json;
    }

    public static boolean inJsonBoolean(String key) {
        if (json != null && json.containsKey(key) && !json.get(key).equals("")) {
            return Boolean.parseBoolean(json.get(key));
        }
        return false;
    }

    public static boolean inJsonSimple(String key) {
        return json != null && json.containsKey(key) && !json.get(key).equals("");
    }

    public static String getJsonValue(String key) {
        return json.get(key);
    }
}
