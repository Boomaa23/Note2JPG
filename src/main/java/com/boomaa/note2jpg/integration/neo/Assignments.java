package com.boomaa.note2jpg.integration.neo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Assignments extends HashMap<String, String> {
    public final List<String> getNames() {
        return new ArrayList<>(keySet());
    }

    public final List<String> getIDs() {
        return new ArrayList<>(values());
    }
}
