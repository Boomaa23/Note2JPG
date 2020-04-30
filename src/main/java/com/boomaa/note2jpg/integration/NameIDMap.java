package com.boomaa.note2jpg.integration;

import java.util.LinkedHashMap;
import java.util.LinkedList;

public class NameIDMap extends LinkedHashMap<String, String> {
    public final LinkedList<String> getNames() {
        return new LinkedList<>(keySet());
    }

    public final LinkedList<String> getIDs() {
        return new LinkedList<>(values());
    }
}
